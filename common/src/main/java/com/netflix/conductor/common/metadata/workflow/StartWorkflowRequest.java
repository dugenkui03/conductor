/*
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.common.metadata.workflow;

import com.github.vmg.protogen.annotations.ProtoField;
import com.github.vmg.protogen.annotations.ProtoMessage;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

// fixme 开始工作流请求
@ProtoMessage
public class StartWorkflowRequest {
    @ProtoField(id = 1)
	@NotNull(message = "Workflow name cannot be null or empty")
	private String name;

    @ProtoField(id = 2)
	private Integer version;

    // Unique Id that correlates(关联) multiple Workflow executions
	// 关联多个工作流执行的唯一键
    @ProtoField(id = 3)
	private String correlationId;

    @ProtoField(id = 4)
	private Map<String, Object> input = new HashMap<>();

    // 任务域有组于支持任务开发，理念同"任务定义相同"、任务可以在不同的域中实现。
    @ProtoField(id = 5)
	private Map<String, String> taskToDomain = new HashMap<>();

    // 工作流定义
    @ProtoField(id = 6)
    @Valid
    private WorkflowDef workflowDef;

    // 只需java客户端关注
	// https://netflix.github.io/conductor/externalpayloadstorage/
    @ProtoField(id = 7)
    private String externalInputPayloadStoragePath;

    // 工作流中的任务优先级定义
	@ProtoField(id = 8)
	@Min(value = 0, message = "priority: ${validatedValue} should be minimum {value}")
	@Max(value = 99, message = "priority: ${validatedValue} should be maximum {value}")
	private Integer priority;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StartWorkflowRequest withName(String name) {
		this.name = name;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public StartWorkflowRequest withVersion(Integer version) {
		this.version = version;
		return this;
	}

	public String getCorrelationId() {
		return correlationId;
	}
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	public StartWorkflowRequest withCorrelationId(String correlationId) {
		this.correlationId = correlationId;
		return this;
	}

	public String getExternalInputPayloadStoragePath() {
		return externalInputPayloadStoragePath;
	}
	public void setExternalInputPayloadStoragePath(String externalInputPayloadStoragePath) {
		this.externalInputPayloadStoragePath = externalInputPayloadStoragePath;
	}
	public StartWorkflowRequest withExternalInputPayloadStoragePath(String externalInputPayloadStoragePath) {
		this.externalInputPayloadStoragePath = externalInputPayloadStoragePath;
		return this;
	}

	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	public StartWorkflowRequest withPriority(Integer priority) {
		this.priority = priority;
		return this;
	}

	public Map<String, Object> getInput() {
		return input;
	}
	public void setInput(Map<String, Object> input) {
		this.input = input;
	}
	public StartWorkflowRequest withInput(Map<String, Object> input) {
		this.input = input;
		return this;
	}

	public Map<String, String> getTaskToDomain() {
		return taskToDomain;
	}
	public void setTaskToDomain(Map<String, String> taskToDomain) {
		this.taskToDomain = taskToDomain;
	}
	public StartWorkflowRequest withTaskToDomain(Map<String, String> taskToDomain) {
		this.taskToDomain = taskToDomain;
		return this;
	}

    public WorkflowDef getWorkflowDef() {
        return workflowDef;
    }
    public void setWorkflowDef(WorkflowDef workflowDef) {
        this.workflowDef = workflowDef;
    }
    public StartWorkflowRequest withWorkflowDef(WorkflowDef workflowDef) {
        this.workflowDef = workflowDef;
        return this;
    }
}
