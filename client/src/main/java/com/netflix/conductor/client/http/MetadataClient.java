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

package com.netflix.conductor.client.http;

import com.google.common.base.Preconditions;
import com.netflix.conductor.client.config.ConductorClientConfiguration;
import com.netflix.conductor.client.config.DefaultConductorClientConfiguration;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.apache.commons.lang.StringUtils;

import java.util.List;

// https://netflix.github.io/conductor/gettingstarted/client/
// 元数据客户端：注册工作流定义、任务定义等
public class MetadataClient extends ClientBase {

    // 工作流 定义列表
    private static GenericType<List<WorkflowDef>> workflowDefList = new GenericType<List<WorkflowDef>>() {};

    // 任务定义列表
    private static GenericType<List<TaskDef>> taskDefList = new GenericType<List<TaskDef>>() {};

    /**
     * ======================= 构造函数 ======================
     */
    // 创建一个默认的元数据定义 Creates a default metadata client
    public MetadataClient() {
        this(new DefaultClientConfig(), new DefaultConductorClientConfiguration(), null);
    }

    // rest客户端配置：REST Client configuration
    public MetadataClient(ClientConfig clientConfig) {
        this(clientConfig, new DefaultConductorClientConfiguration(), null);
    }

    // clientHandler：Useful when plugging(插入) in various(各种个样的) http client interaction modules
    public MetadataClient(ClientConfig clientConfig, ClientHandler clientHandler) {
        this(clientConfig, new DefaultConductorClientConfiguration(), clientHandler);
    }

    // filters 应用到每个请求的客户端过滤链 Chain of client side filters to be applied per request
    public MetadataClient(ClientConfig config, ClientHandler handler, ClientFilter... filters) {
        this(config, new DefaultConductorClientConfiguration(), handler, filters);
    }

    // clientConfiguration 配置到client的特定的属性
    public MetadataClient(ClientConfig config, ConductorClientConfiguration clientConfiguration, ClientHandler handler, ClientFilter... filters) {
        super(config, clientConfiguration, handler);
        for (ClientFilter filter : filters) {
            super.client.addFilter(filter);
        }
    }


    /**
     * ======================= Workflow Metadata Operations 工作流元数据操作 ======================
     */
    // Register a workflow definition with the server
    // 注册工作流定义
    public void registerWorkflowDef(WorkflowDef workflowDef) {
        Preconditions.checkNotNull(workflowDef, "Worfklow definition cannot be null");
        postForEntityWithRequestOnly("metadata/workflow", workflowDef);
    }

    // 更新工作流定义
    public void updateWorkflowDefs(List<WorkflowDef> workflowDefs) {
        Preconditions.checkNotNull(workflowDefs, "Workflow defs list cannot be null");
        put("metadata/workflow", null, workflowDefs);
    }

    // 获取(retrieve)指定名称和版本的工作流定义
    public WorkflowDef getWorkflowDef(String name, Integer version) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        return getForEntity("metadata/workflow/{name}", new Object[]{"version", version}, WorkflowDef.class, name);
    }

    // 从conductor服务中移除 指定名称和version 的工作流定义，并不移除相关的工作流，小心使用。
    public void unregisterWorkflowDef(String name, Integer version) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "Workflow name cannot be blank");
        Preconditions.checkNotNull(version, "Version cannot be null");
        delete("metadata/workflow/{name}/{version}", name, version);
    }


    /**
     * ======================= Task Metadata Operations 任务元数据操作 ======================
     */
    //fixme 注册一个任务列表 Registers a list of task types with the conductor server
    public void registerTaskDefs(List<TaskDef> taskDefs) {
        Preconditions.checkNotNull(taskDefs, "Task defs list cannot be null");
        // 接口metadata/taskdefs 参见 JettyServer
        postForEntityWithRequestOnly("metadata/taskdefs", taskDefs);
    }

    // 更新一个已经存在的任务定义
    public void updateTaskDef(TaskDef taskDef) {
        Preconditions.checkNotNull(taskDef, "Task definition cannot be null");
        put("metadata/taskdefs", null, taskDef);
    }

    // 检索指定类型的任务定义，taskType对应哪个字段？
    public TaskDef getTaskDef(String taskType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");
        return getForEntity("metadata/taskdefs/{tasktype}", null, TaskDef.class, taskType);
    }

    // 从conductor中移除任务定义
    public void unregisterTaskDef(String taskType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(taskType), "Task type cannot be blank");
        delete("metadata/taskdefs/{tasktype}", taskType);
    }
}
