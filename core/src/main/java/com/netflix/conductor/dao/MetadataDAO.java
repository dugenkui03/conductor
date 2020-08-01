/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.conductor.dao;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import java.util.List;
import java.util.Optional;

/**
 * 数据获取层：获取任务定义、工作流定义
 * Data access layer for the workflow metadata - task definitions and workflow definitions
 */
public interface MetadataDAO {

    /**
     * ================================== 任务定义 ==============================================
     */
    // 创建任务定义
    void createTaskDef(TaskDef taskDef);

    // 更新任务定义
    String updateTaskDef(TaskDef taskDef);

    //获取指定名称的任务定义
    TaskDef getTaskDef(String name);

    //获取所有任务定义
    List<TaskDef> getAllTaskDefs();

    //移除任务定义
    void removeTaskDef(String name);

    /**
     * ================================== 工作流定义 ==============================================
     */
    // 创建工作流
    void createWorkflowDef(WorkflowDef def);

    // 更新工作流
    void updateWorkflowDef(WorkflowDef def);

    // 根据名称获取最新的工作流定义
    Optional<WorkflowDef> getLatestWorkflowDef(String name);

    // 获取指定名称和版本的工作流定义？
    Optional<WorkflowDef> getWorkflowDef(String name, int version);

    // 移除指定名称和版本的工作流定义
    void removeWorkflowDef(String name, Integer version);

    // 获取所有的工作流定义
    List<WorkflowDef> getAllWorkflowDefs();
}
