/*
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.client.worker;

import com.amazonaws.util.EC2MetadataUtils;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;

/**
 * worker名称、执行任务、更新出错、暂停、获取标识、创建worker、获取轮询时间
 */
public interface Worker {
    /**
     * worker正执行的的任务定义的名称。
     *
     * Retrieve the name of the task definition the worker is currently working on.
     *
     * @return the name of the task definition.
     */
    String getTaskDefName();

    /**
     * 执行任务、并返回更新的任务。
     * Executes a task and returns the updated task.
     *
     * @param task Task to be executed.
     * @return the {@link TaskResult} object
     * If the task is not completed yet, return with the status as IN_PROGRESS.
     */
    TaskResult execute(Task task);

    /**
     * This method has been marked as deprecated and will be removed in a future release.
     */
    @Deprecated
    default boolean preAck(Task task) {
        return true;
    }

    /**
     * 当任务协调器失败的时候、更新任务到服务。client应该存储任务的id、并且稍后重试更新。
     *
     * Called when the task coordinator(协调器) fails to update the task to the server.
     * Client should store the task id (in a database) and retry the update later
     *
     * @param task Task which cannot be updated back to the server."无法更新会服务的任务"
     */
    default void onErrorUpdate(Task task) {

    }

    /** 为true的时候，worker停止轮询。
     *  Override this method to pause the worker from polling.
     *
     * @return true if the worker is paused and no more tasks should be polled(轮询) from server.
     *          worker停止、则返回true。
     */
    default boolean paused() {
        return PropertyFactory.getBoolean(getTaskDefName(), "paused", false);
    }

    /**
     * 根据应用程序、重写此方法。
     *
     * Override this method to app specific rules.
     *
     * @return returns the serverId as the id of the instance that the worker is running.
     *         返回serverId作为实例的id(worker正在运行其上的实例)
     */
    default String getIdentity() {
        String serverId;
        try {
            //获取主机名称
            serverId = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            serverId = System.getenv("HOSTNAME");
        }
        if (serverId == null) {
            serverId = (EC2MetadataUtils.getInstanceId() == null) ? System.getProperty("user.name") : EC2MetadataUtils.getInstanceId();
        }
        LoggerHolder.logger.debug("Setting worker id to {}", serverId);
        return serverId;
    }

    /**
     * This method has been marked as deprecated and will be removed in a future release.
     */
    @Deprecated
    default int getPollCount() {
        return PropertyFactory.getInteger(getTaskDefName(), "pollCount", 1);
    }

    // interval in millisecond at which the server should be polled for worker tasks.
    // server轮询工作任务的间隔、单位毫秒。
    default int getPollingInterval() {
        return PropertyFactory.getInteger(getTaskDefName(), "pollInterval", 1000);
    }

    /**
     * This method has been marked as deprecated and will be removed in a future release.
     */
    @Deprecated
    default int getLongPollTimeoutInMS() {
        return PropertyFactory.getInteger(getTaskDefName(), "longPollTimeout", 100);
    }

    /**
     * @param taskType 任务类型
     * @param executor 任务执行器：任务、返回值 - Function<InputType,ReturnType>
     * @return 创建的新的worker对象
     */
    static Worker create(String taskType, Function<Task, TaskResult> executor) {

        return new Worker() {
            //任务名称
            @Override
            public String getTaskDefName() {
                return taskType;
            }

            //执行任务
            @Override
            public TaskResult execute(Task task) {
                return executor.apply(task);
            }

            //停止任务
            @Override
            public boolean paused() {
                return Worker.super.paused();
            }
        };
    }
}

final class LoggerHolder {
    static final Logger logger = LoggerFactory.getLogger(Worker.class);
}
