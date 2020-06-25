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
package com.netflix.conductor.client.sample;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {

        //任务client
        TaskClient taskClient = new TaskClient();
        //服务api的根uri
        taskClient.setRootURI("http://localhost:8080/api/");        //Point this to the server API

        //指定worker线程数量，为了避免线程饥饿、线程数量不应该小于worker数量
        //number of threads used to execute workers.  To avoid starvation, should be same or more than number of workers
        int threadCount = 2;

        //创建worker
        Worker worker1 = new SampleWorker("task_1");
        Worker worker2 = new SampleWorker("task_5");

        // Create TaskRunnerConfigurer
        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, Arrays.asList(worker1, worker2))
            .withThreadCount(threadCount)
            .build();

        // Start the polling and execution of tasks
        configurer.init();
    }
}
