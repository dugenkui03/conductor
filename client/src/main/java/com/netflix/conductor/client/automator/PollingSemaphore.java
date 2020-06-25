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
// automator 自动监控器
package com.netflix.conductor.client.automator;

import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 轮询信号量：包装信号量的类、持有用来轮询和执行任务的许可数量。
 *
 * A class wrapping a semaphore which holds the number of permits(许可) available for polling and executing tasks.
 */
class PollingSemaphore {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingSemaphore.class);

    private Semaphore semaphore;

    //slot：槽、位置
    PollingSemaphore(int numSlots) {
        LOGGER.debug("Polling semaphore initialized with {} permits", numSlots);
        semaphore = new Semaphore(numSlots);
    }

    /**
     * Signals if polling is allowed based on whether a permit can be acquired.
     *
     * @return {@code true} - if permit is acquired 是否可以获取到轮询许可
     *         {@code false} - if permit could not be acquired
     */
    boolean canPoll() {
        boolean acquired = semaphore.tryAcquire();
        LOGGER.debug("Trying to acquire permit: {}", acquired);
        return acquired;
    }

    /**
     * 处理完一个任务后、释放许可
     *
     * Signals that processing is complete and the permit can be released.
     */
    void complete() {
        LOGGER.debug("Completed execution; releasing permit");
        semaphore.release();
    }

    /**
     * 获取可用于轮询的线程数量。
     *
     * Gets the number of threads available for processing.
     *
     * @return number of available permits
     */
    int availableThreads() {
        int available = semaphore.availablePermits();
        LOGGER.debug("Number of available permits: {}", available);
        return available;
    }
}
