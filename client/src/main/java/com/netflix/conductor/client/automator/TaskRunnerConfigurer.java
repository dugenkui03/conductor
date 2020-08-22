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

import com.google.common.base.Preconditions;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.telemetry.MetricsContainer;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.discovery.EurekaClient;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** 通过注册的Worker配置和TaskClient，自动轮询和执行任务。
 * Configures automated polling(轮询) of tasks and execution via the registered {@link Worker}s.
 */
public class TaskRunnerConfigurer {

    private ScheduledExecutorService scheduledExecutorService;

    // 用于识别服务端是否在discovery状态中
    // EurekaClient：用于服务发现：https://juejin.im/entry/6844903639778066446
    private final EurekaClient eurekaClient;


    private final TaskClient taskClient;
    private List<Worker> workers = new LinkedList<>();
    private final int sleepWhenRetry;
    private final int updateRetryCount;
    // 赋给worker线程的数量，应该至少为taskWorkers的数量、来避免线程饥饿
    private final int threadCount;
    private final String workerNamePrefix;

    private TaskPollExecutor taskPollExecutor;

    /**
     * @see TaskRunnerConfigurer.Builder
     * @see TaskRunnerConfigurer#init()
     */
    private TaskRunnerConfigurer(Builder builder) {
        this.eurekaClient = builder.eurekaClient;
        this.taskClient = builder.taskClient;
        this.sleepWhenRetry = builder.sleepWhenRetry;
        this.updateRetryCount = builder.updateRetryCount;
        this.workerNamePrefix = builder.workerNamePrefix;
        builder.workers.forEach(workers::add);
        this.threadCount = (builder.threadCount == -1) ? workers.size() : builder.threadCount;
    }

    /**
     * Builder used to create the instances of TaskRunnerConfigurer
     */
    public static class Builder {

        private String workerNamePrefix = "workflow-worker-";
        private int sleepWhenRetry = 500;
        private int updateRetryCount = 3;

        // 赋给worker线程的数量，应该至少为taskWorkers的数量、来避免线程饥饿
        private int threadCount = -1;
        private Iterable<Worker> workers;
        private EurekaClient eurekaClient;
        private TaskClient taskClient;

        // 设置 任务客户端  和 worker列表
        public Builder(TaskClient taskClient, Iterable<Worker> workers) {
            Preconditions.checkNotNull(taskClient, "TaskClient cannot be null");
            Preconditions.checkNotNull(workers, "Workers cannot be null");
            this.taskClient = taskClient;
            this.workers = workers;
        }

        // worker名称的前缀、如果没有提供使用"workflow-worker-"作为默认值
        public Builder withWorkerNamePrefix(String workerNamePrefix) {
            this.workerNamePrefix = workerNamePrefix;
            return this;
        }

        // 当任务状态更新为失败的时候，任务重试之前、线程sleep的毫秒数
        // ime in milliseconds, for which the thread should sleep when task update call fails, before retrying the operation.
        public Builder withSleepWhenRetry(int sleepWhenRetry) {
            this.sleepWhenRetry = sleepWhenRetry;
            return this;
        }

        // 失败的"更新任务"操作的重试次数。
        // number of times to retry the failed updateTask operation
        public Builder withUpdateRetryCount(int updateRetryCount) {
            this.updateRetryCount = updateRetryCount;
            return this;
        }

        /**
         * 赋给worker线程的数量，应该至少为taskWorkers的数量、来避免线程饥饿。
         * @param threadCount # of threads assigned to the workers. Should be at-least the size of taskWorkers to avoid
         *                    starvation in a busy system.
         * @return Builder instance
         */
        public Builder withThreadCount(int threadCount) {
            if (threadCount < 1) {
                throw new IllegalArgumentException("No. of threads cannot be less than 1");
            }
            this.threadCount = threadCount;
            return this;
        }

        /**用来识别服务器是否会被发现：当服务无法被发现的时候、轮询停止；如果返回null、"服务发现检测"则不停止。
         * @param eurekaClient Eureka client - used to identify if the server is in discovery or not.  When the server
         *                     goes out of discovery, the polling is terminated. If passed null, discovery check is not
         *                     done.
         * @return Builder instance
         */
        public Builder withEurekaClient(EurekaClient eurekaClient) {
            this.eurekaClient = eurekaClient;
            return this;
        }

        /**
         * Builds an instance of the TaskRunnerConfigurer.
         * <p>
         * Please see {@link TaskRunnerConfigurer#init()} method. The method must be called after this constructor for
         * the polling to start.
         * </p>
         */
        public TaskRunnerConfigurer build() {
            return new TaskRunnerConfigurer(this);
        }
    }

    /**
     * @return Thread Count for the executor pool
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * @return sleep time in millisecond before task update retry is done when receiving error from the Conductor server
     */
    public int getSleepWhenRetry() {
        return sleepWhenRetry;
    }

    /**
     * @return Number of times updateTask should be retried when receiving error from Conductor server
     */
    public int getUpdateRetryCount() {
        return updateRetryCount;
    }

    /**
     * @return prefix used for worker names
     */
    public String getWorkerNamePrefix() {
        return workerNamePrefix;
    }

    // fixme 学习：怎么使用配置的、怎么完成了conductor的服务模型
    //  创建完实例之后，开始轮询、以及执行任务。
    public synchronized void init() {
        // 增加初始化数
        MetricsContainer.incrementInitializationCount(this.getClass().getCanonicalName());

        //fixme 任务轮询执行器
        this.taskPollExecutor = new TaskPollExecutor(
                eurekaClient,
                taskClient,
                threadCount,
                updateRetryCount,
                workerNamePrefix);

        //初始化一个固定任务线程池
        this.scheduledExecutorService = Executors.newScheduledThreadPool(workers.size());

        // fixme 对于每一个woker都有一个 taskPollExecutor(任务轮询执行器) 进行驱动
        for (Worker worker : workers) {
            scheduledExecutorService.scheduleWithFixedDelay(
                    // 任务定义
                    () -> taskPollExecutor.pollAndExecute(worker),
                    // 轮询周期定义
                    worker.getPollingInterval(),
                    worker.getPollingInterval(),
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Invoke this method within a PreDestroy(销毁方法) block within your application
     * to facilitate(促进) a graceful(优美的、平滑的) shutdown of your worker, during process termination.
     */
    public void shutdown() {
        taskPollExecutor.shutdownExecutorService(scheduledExecutorService);
    }
}
