/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.client.automator;

import com.google.common.base.Stopwatch;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.telemetry.MetricsContainer;
import com.netflix.conductor.client.worker.PropertyFactory;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.utils.RetryUtil;
import com.netflix.discovery.EurekaClient;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理worker使用的线程池，用来轮询server、server交流、轮询任务等。
 *
 * Manages the threadPool used by the workers for execution and server communication (polling and task update).
 */
class TaskPollExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPollExecutor.class);

    // 服务发现
    private EurekaClient eurekaClient;

    //任务客户端：轮询任务、更新任务状态、执行任务之后更新任务结果
    private TaskClient taskClient;

    // worker执行完任务后、更新任务状态的重试次数
    private int updateRetryCount;

    //线程池
    private ExecutorService executorService;
    //轮询信号量
    private PollingSemaphore pollingSemaphore;

    private static final String DOMAIN = "domain";
    private static final String ALL_WORKERS = "all";


    //任务客户端、线程数量、更新重试数量、worker名称前缀
    TaskPollExecutor(EurekaClient eurekaClient, TaskClient taskClient,
                     int threadCount, int updateRetryCount, String workerNamePrefix) {
        this.eurekaClient = eurekaClient;
        this.taskClient = taskClient;
        this.updateRetryCount = updateRetryCount;

        LOGGER.info("Initialized the TaskPollExecutor with {} threads", threadCount);

        AtomicInteger count = new AtomicInteger(0);

        //固定大小线程池：线程池数量、线程池工厂(线程名称、异常处理器)
        this.executorService = Executors.newFixedThreadPool(threadCount,
                new BasicThreadFactory.Builder()
                        .namingPattern(workerNamePrefix + count.getAndIncrement())
                        .uncaughtExceptionHandler(uncaughtExceptionHandler)
                        .build());

        this.pollingSemaphore = new PollingSemaphore(threadCount);
    }

    //fixme 使用定时任务执行
    void pollAndExecute(Worker worker) {

        // 如果示例状态不为 null、但是没有准备好接收流量、则返回
        if (eurekaClient != null && !eurekaClient.getInstanceRemoteStatus().equals(InstanceStatus.UP)) {
            LOGGER.debug("Instance is NOT UP in discovery - will not poll");
            return;
        }

        // 如果worker处于停止轮询的状态，则incrementTaskPausedCount并返回
        if (worker.paused()) {
            MetricsContainer.incrementTaskPausedCount(worker.getTaskDefName());
            LOGGER.debug("Worker {} has been paused. Not polling anymore!", worker.getClass());
            return;
        }


        Task task;
        try {
            //是否可以获取信号量的许可
            if (!pollingSemaphore.canPoll()) {
                return;
            }

            // 获取worker定义中的任务名称
            String taskType = worker.getTaskDefName();
            // 获取属性domain名称
            String domain = Optional.ofNullable(PropertyFactory.getString(taskType, DOMAIN, null))
                .orElse(PropertyFactory.getString(ALL_WORKERS, DOMAIN, null));
            LOGGER.debug("Polling task of type: {} in domain: '{}'", taskType, domain);

            // todo 对特定类型的任务执行轮询
            // taskType：要被轮询的任务类型；
            // worker.getIdentity()： 任务类型的域
            // domain：用来记录日志
            Callable<Task> callable = () -> taskClient.pollTask(taskType, worker.getIdentity(), domain);
            task =
                    // 获取任务名称为taskType的轮询时长
                    MetricsContainer.getPollTimer(taskType)
                // 执行任务、并且记录time taken
                .record(callable);

            // 乳沟任务不为空，且
            if (Objects.nonNull(task) && StringUtils.isNotBlank(task.getTaskId())) {
                // 特定类型的任务又被拉取了一次
                MetricsContainer.incrementTaskPollCount(taskType, 1);
                LOGGER.debug("Polled task: {} of type: {} in domain: '{}', from worker: {}",
                    task.getTaskId(), taskType, domain, worker.getIdentity());

                // 使用指定线程池，执行任务
                CompletableFuture<Task> taskCompletableFuture = CompletableFuture.supplyAsync(() ->
                    processTask(task, worker), executorService);

                taskCompletableFuture.whenComplete(this::finalizeTask);
            } else {
                // no task was returned in the poll, release the permit
                pollingSemaphore.complete();
            }
        } catch (Exception e) {
            // release the permit if exception is thrown during polling, because the thread would not be busy
            pollingSemaphore.complete();
            MetricsContainer.incrementTaskPollErrorCount(worker.getTaskDefName(), e);
            LOGGER.error("Error when polling for tasks", e);
        }
    }

    void shutdown() {
        shutdownExecutorService(executorService);
    }

    /**关闭线程池：
     *  1. 指定时间内是否关闭成功：即任务是否执行完毕；
     *  2. 调用shutdownNow强制关闭线程池。
     */
    void shutdownExecutorService(ExecutorService executorService) {
        int timeout = 10;
        try {
            // awaitTermination 等待指定的时间完成任务并关闭线程池；
            if (executorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
                LOGGER.debug("tasks completed, shutting down");
            } else {
                LOGGER.warn(String.format("forcing shutdown after waiting for %s second", timeout));
                // 如果指定的时间没有关闭，则强制关闭
                executorService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            LOGGER.warn("shutdown interrupted, invoking shutdownNow");
            executorService.shutdownNow();
            // 将当前线程标记为中断
            Thread.currentThread().interrupt();
        }
    }

    //未捕获的异常处理器：递增MetricsContainer相关指标
    @SuppressWarnings("FieldCanBeLocal")
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, error) -> {
        // JVM may be in unstable state, try to send metrics then exit
        MetricsContainer.incrementUncaughtExceptionCount();
        LOGGER.error("Uncaught exception. Thread {} will exit now", thread, error);
    };


    private Task processTask(Task task, Worker worker) {
        LOGGER.debug("Executing task: {} of type: {} in worker: {} at {}",
                task.getTaskId(),
                task.getTaskDefName(),
                worker.getClass().getSimpleName(),
                worker.getIdentity());

        try {
            // 使用worker执行task
            executeTask(worker, task);
        } catch (Throwable t) {
            //抛异常的时候、任务状态设置失败
            task.setStatus(Task.Status.FAILED);
            TaskResult result = new TaskResult(task);
            handleException(t, result, worker, task);
        } finally {
            pollingSemaphore.complete();
        }
        return task;
    }

    /**fixme Stopwatch 记录任务执行时长
     * @param worker 执行者
     * @param task 被执行的任务
     */
    private void executeTask(Worker worker, Task task) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        TaskResult result = null;
        try {
            LOGGER.debug("Executing task: {} in worker: {} at {}"
                    , task.getTaskId(), worker.getClass().getSimpleName(), worker.getIdentity());
            //fixme 执行任务、获取结果
            result = worker.execute(task);

            //设置任务结果的工作流实例id、任务id和worker的Id
            result.setWorkflowInstanceId(task.getWorkflowInstanceId());
            result.setTaskId(task.getTaskId());
            result.setWorkerId(worker.getIdentity());
        } catch (Exception e) {
            LOGGER.error("Unable to execute task: {} of type: {}", task.getTaskId(), task.getTaskDefName(), e);
            //如果任务执行结果不为空
            if (result == null) {
                task.setStatus(Task.Status.FAILED);
                result = new TaskResult(task);
            }
            handleException(e, result, worker, task);
        } finally {
            stopwatch.stop();
            MetricsContainer.getExecutionTimer(worker.getTaskDefName())
                .record(stopwatch.elapsed(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }

        LOGGER.debug("Task: {} executed by worker: {} at {} with status: {}",
                task.getTaskId(), worker.getClass().getSimpleName(), worker.getIdentity(), result.getStatus());

        //更新任务信息(状态)
        updateWithRetry(updateRetryCount, task, result, worker);
    }

    //finalize 使结束
    private void finalizeTask(Task task, Throwable throwable) {
        if (throwable != null) {
            LOGGER.error("Error processing task: {} of type: {}", task.getTaskId(), task.getTaskType(), throwable);
            MetricsContainer.incrementTaskExecutionErrorCount(task.getTaskType(), throwable);
        } else {
            LOGGER.debug("Task:{} of type:{} finished processing with status:{}", task.getTaskId(),
                task.getTaskDefName(), task.getStatus());
        }
    }

    /**
     *
     * @param count worker执行完任务后、更新任务状态的重试次数
     * @param task 执行完的任务
     * @param result 任务执行完的结果
     * @param worker 执行任务的worker
     */
    private void updateWithRetry(int count, Task task, TaskResult result, Worker worker) {
        try {
            // "更新任务描述"
            String updateTaskDesc = String.format(
                    "Retry updating task result: %s for task: %s in worker: %s",
                    result.toString(),
                    task.getTaskDefName(),
                    worker.getIdentity());

            // 评估任务 payload
            String evaluatePayloadDesc = String.format(
                    "Evaluate Task payload(静负荷) for task: %s in worker: %s",
                    task.getTaskDefName(),
                    worker.getIdentity());

            String methodName = "updateWithRetry";

            //最终的结果
            TaskResult finalResult = new RetryUtil<TaskResult>().retryOnException(
                    //健壮性比较差的任务、需要多次执行
                    () -> {
                        //复制结果
                        TaskResult taskResult = result.copy();

                        // 评估和更新大型 payload
                        taskClient.evaluateAndUploadLargePayload(taskResult, task.getTaskType());
                        return taskResult;
                    }
                    //重试次数、评估负荷描述，方法名称
                    , null, null, count, evaluatePayloadDesc, methodName);


            new RetryUtil<>().retryOnException(
                    () -> {
                        taskClient.updateTask(finalResult);
                        return null;
                    }
                    , null, null, count, updateTaskDesc, methodName);

        } catch (Exception e) {
            worker.onErrorUpdate(task);
            MetricsContainer.incrementTaskUpdateErrorCount(worker.getTaskDefName(), e);

            //todo 为啥搞怎么多无法控制的最高级别的日志
            LOGGER.error(String.format(
                        "Failed to update result: %s for task: %s in worker: %s"
                        , result.toString()
                        , task.getTaskDefName()
                        , worker.getIdentity())
                    , e);
        }
    }

    /**
     * 处理任务执行抛异常的情况
     * @param t 任务异常
     * @param result 任务结果
     * @param worker
     * @param task
     */
    private void handleException(Throwable t, TaskResult result, Worker worker, Task task) {
        LOGGER.error(String.format("Error while executing task %s", task.toString()), t);
        MetricsContainer.incrementTaskExecutionErrorCount(worker.getTaskDefName(), t);
        result.setStatus(TaskResult.Status.FAILED);
        result.setReasonForIncompletion("Error while executing the task: " + t);

        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        result.log(stringWriter.toString());

        updateWithRetry(updateRetryCount, task, result, worker);
    }
}
