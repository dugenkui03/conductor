/*
 * Copyright 2016 Netflix, Inc.
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

package com.netflix.conductor.common.metadata.tasks;

import com.github.vmg.protogen.annotations.ProtoEnum;
import com.github.vmg.protogen.annotations.ProtoField;
import com.github.vmg.protogen.annotations.ProtoMessage;
import com.netflix.conductor.common.constraints.OwnerEmailMandatoryConstraint;
import com.netflix.conductor.common.constraints.TaskTimeoutConstraint;
import com.netflix.conductor.common.metadata.Auditable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author Viren
 * Defines a workflow task definition 
 */
@ProtoMessage
@TaskTimeoutConstraint
@Valid
public class TaskDef extends Auditable {

	// 任务超时策略：
	// 重试
	// TIME_OUT_WF：Workflow 工作流别标识为超时、并终止
	// ALERT_ONLY: Registers a counter (task_timeout)
	@ProtoEnum
	public enum TimeoutPolicy {RETRY, TIME_OUT_WF, ALERT_ONLY}

	@ProtoEnum
	public enum RetryLogic {FIXED, EXPONENTIAL_BACKOFF}

	private static final int ONE_HOUR = 60 * 60;

	// fixme name是唯一的，用于标识任务。唯一、唯一、唯一、唯一、唯一
	@NotEmpty(message = "TaskDef name cannot be null or empty")
	@ProtoField(id = 1)
	private String name;

	// 描述任务
	@ProtoField(id = 2)
	private String description;

	// 任务执行失败的时候、重试的次数，必须大于等于0
	@ProtoField(id = 3)
	@Min(value = 0, message = "TaskDef retryCount: {value} must be >= 0")
	private int retryCount = 3; // Default

	// 超时时间
	@ProtoField(id = 4)
	@NotNull
	private long timeoutSeconds;

	/** inputKeys 和 outputKeys是任务的输入和输出值。参考：
	 *
	 *  https://netflix.github.io/conductor/configuration/taskdef/#using-inputkeys-and-outputkeys
	 */

	// 输入的键值列表，用户记录任务的输入。
	// "Array of keys of task's expected input. Used for documenting task's input"
	@ProtoField(id = 5)
	private List<String> inputKeys = new ArrayList<String>();

	@ProtoField(id = 6)
	private List<String> outputKeys = new ArrayList<String>();

	@ProtoField(id = 7)
	private TimeoutPolicy timeoutPolicy = TimeoutPolicy.TIME_OUT_WF;

	// 重试机制：
	// FIXED：Reschedule the task after the retryDelaySeconds
	// EXPONENTIAL_BACKOFF：reschedule after retryDelaySeconds  * attemptNumber
	@ProtoField(id = 8)
	private RetryLogic retryLogic = RetryLogic.FIXED;

	// 重试延迟时间
	@ProtoField(id = 9)
	private int retryDelaySeconds = 60;

	// 0 < this < timeoutSeconds，默认3600s；
	// "如果在指定时间内没有更新状态，则会重新安排(schedule)任务，"
	// 在worker轮询任务但是遇到错误的时候很有用
	@ProtoField(id = 10)
	@Min(value = 1, message = "TaskDef responseTimeoutSeconds: ${validatedValue} should be minimum {value} second")
	private long responseTimeoutSeconds = ONE_HOUR;

	// 当前可执行的任务数量
	@ProtoField(id = 11)
	private Integer concurrentExecLimit;

	//https://netflix.github.io/conductor/configuration/taskdef/#using-inputtemplate
	@ProtoField(id = 12)
	private Map<String, Object> inputTemplate = new HashMap<>();

	// This field is deprecated, do not use id 13.
	//	@ProtoField(id = 13)
	//	private Integer rateLimitPerSecond;

	/** https://netflix.github.io/conductor/configuration/taskdef/#task-rate-limits  rate:比率
	 *
	 * 和rateLimitFrequencyInSeconds共用、定义了在 频率窗口 可提交给worker的任务数量；
	 * rateLimitFrequencyInSeconds设置频率窗口，例如1s, 5s, 60s, 300s。
	 */
	@ProtoField(id = 14)
	private Integer rateLimitPerFrequency;

	@ProtoField(id = 15)
	private Integer rateLimitFrequencyInSeconds;

	/** https://netflix.github.io/conductor/configuration/isolationgroups/#isolation-group-id
	 *  piles up：堆积
	 *
	 * 对于延迟较高的任务，使用单独的队列和线程池去执行这些任务，避免影响到其他任务。
	 */
	@ProtoField(id = 16)
	private String isolationGroupId;

	/**https://netflix.github.io/conductor/configuration/isolationgroups/#execution-name-space
	 *
	 */
	@ProtoField(id = 17)
	private String executionNameSpace;

	@ProtoField(id = 18)
	@OwnerEmailMandatoryConstraint
	@Email(message = "ownerEmail should be valid email address")
	private String ownerEmail;

	@ProtoField(id = 19)
	@Min(value = 0, message = "TaskDef pollTimeoutSeconds: {value} must be >= 0")
	private Integer pollTimeoutSeconds;

	/**
	 * ======================================== 构造函数 =================================================
	 */
	public TaskDef() { }

	public TaskDef(String name) {
		this.name = name;
	}

	public TaskDef(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public TaskDef(String name, String description, int retryCount, long timeoutSeconds) {
		this.name = name;
		this.description = description;
		this.retryCount = retryCount;
		this.timeoutSeconds = timeoutSeconds;
	}

	public TaskDef(String name, String description, String ownerEmail, int retryCount, long timeoutSeconds, long responseTimeoutSeconds) {
		this.name = name;
		this.description = description;
		this.ownerEmail = ownerEmail;
		this.retryCount = retryCount;
		this.timeoutSeconds = timeoutSeconds;
		this.responseTimeoutSeconds = responseTimeoutSeconds;
	}



	// 唯一键
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public List<String> getInputKeys() {
		return inputKeys;
	}

	public void setInputKeys(List<String> inputKeys) {
		this.inputKeys = inputKeys;
	}

	public List<String> getOutputKeys() {
		return outputKeys;
	}

	public void setOutputKeys(List<String> outputKeys) {
		this.outputKeys = outputKeys;
	}


	public TimeoutPolicy getTimeoutPolicy() {
		return timeoutPolicy;
	}

	public void setTimeoutPolicy(TimeoutPolicy timeoutPolicy) {
		this.timeoutPolicy = timeoutPolicy;
	}

	public RetryLogic getRetryLogic() {
		return retryLogic;
	}

	public void setRetryLogic(RetryLogic retryLogic) {
		this.retryLogic = retryLogic;
	}

	public int getRetryDelaySeconds() {
		return retryDelaySeconds;
	}

	// 任务发送响应的超时时间，超过该时间、任务将会被重新入队。
	public long getResponseTimeoutSeconds() {
		return responseTimeoutSeconds;
	}

	public void setResponseTimeoutSeconds(long responseTimeoutSeconds) {
		this.responseTimeoutSeconds = responseTimeoutSeconds;
	}

	public void setRetryDelaySeconds(int retryDelaySeconds) {
		this.retryDelaySeconds = retryDelaySeconds;
	}

	public Map<String, Object> getInputTemplate() {
		return inputTemplate;
	}

	// The max number of tasks that will be allowed to be executed per rateLimitFrequencyInSeconds.
	public Integer getRateLimitPerFrequency() {
		return rateLimitPerFrequency == null ? 0 : rateLimitPerFrequency;
	}

	// The max number of tasks that will be allowed to be executed per rateLimitFrequencyInSeconds.
	// Setting the value to 0 removes the rate limit
	public void setRateLimitPerFrequency(Integer rateLimitPerFrequency) {
		this.rateLimitPerFrequency = rateLimitPerFrequency;
	}

	// 默认值为1：The time bucket that is used to rate limit tasks based on {@link #getRateLimitPerFrequency()}
	public Integer getRateLimitFrequencyInSeconds() {
		return rateLimitFrequencyInSeconds == null ? 1 : rateLimitFrequencyInSeconds;
	}

	/**
	 *
	 * @param rateLimitFrequencyInSeconds: The time window/bucket for which the rate limit needs to be applied. This will only have affect if {@link #getRateLimitPerFrequency()} is greater than zero
	 */
	public void setRateLimitFrequencyInSeconds(Integer rateLimitFrequencyInSeconds) {
		this.rateLimitFrequencyInSeconds = rateLimitFrequencyInSeconds;
	}

	/**
	 *
	 * @param concurrentExecLimit Limit of number of concurrent task that can be  IN_PROGRESS at a given time.  Seting the value to 0 removes the limit.
	 */
	public void setConcurrentExecLimit(Integer concurrentExecLimit) {
		this.concurrentExecLimit = concurrentExecLimit;
	}

	/**
	 *
	 * @return Limit of number of concurrent task that can be  IN_PROGRESS at a given time
	 */
	public Integer getConcurrentExecLimit() {
		return concurrentExecLimit;
	}
	/**
	 *
	 * @return concurrency limit
	 */
	public int concurrencyLimit() {
		return concurrentExecLimit == null ? 0 : concurrentExecLimit;
	}

	/**
	 * @param inputTemplate the inputTemplate to set
	 *
	 */
	public void setInputTemplate(Map<String, Object> inputTemplate) {
		this.inputTemplate = inputTemplate;
	}

	public String getIsolationGroupId() {
		return isolationGroupId;
	}

	public void setIsolationGroupId(String isolationGroupId) {
		this.isolationGroupId = isolationGroupId;
	}

	public String getExecutionNameSpace() {
		return executionNameSpace;
	}

	public void setExecutionNameSpace(String executionNameSpace) {
		this.executionNameSpace = executionNameSpace;
	}

	/**
	 * @return the email of the owner of this task definition
	 */
	public String getOwnerEmail() {
		return ownerEmail;
	}

	/**
	 * @param ownerEmail the owner email to set
	 */
	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	/**
	 * @param pollTimeoutSeconds the poll timeout to set
	 */
	public void setPollTimeoutSeconds(Integer pollTimeoutSeconds) {
		this.pollTimeoutSeconds = pollTimeoutSeconds;
	}

	/**
	 * @return the poll timeout of this task definition
	 */
	public Integer getPollTimeoutSeconds() {
		return pollTimeoutSeconds;
	}

	@Override
	public String toString(){
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TaskDef taskDef = (TaskDef) o;
		return getRetryCount() == taskDef.getRetryCount() &&
				getTimeoutSeconds() == taskDef.getTimeoutSeconds() &&
				getRetryDelaySeconds() == taskDef.getRetryDelaySeconds() &&
				getResponseTimeoutSeconds() == taskDef.getResponseTimeoutSeconds() &&
				Objects.equals(getName(), taskDef.getName()) &&
				Objects.equals(getDescription(), taskDef.getDescription()) &&
				Objects.equals(getInputKeys(), taskDef.getInputKeys()) &&
				Objects.equals(getOutputKeys(), taskDef.getOutputKeys()) &&
				getTimeoutPolicy() == taskDef.getTimeoutPolicy() &&
				getRetryLogic() == taskDef.getRetryLogic() &&
				Objects.equals(getConcurrentExecLimit(), taskDef.getConcurrentExecLimit()) &&
				Objects.equals(getRateLimitPerFrequency(), taskDef.getRateLimitPerFrequency()) &&
				Objects.equals(getInputTemplate(), taskDef.getInputTemplate()) &&
				Objects.equals(getIsolationGroupId(), taskDef.getIsolationGroupId()) &&
				Objects.equals(getExecutionNameSpace(), taskDef.getExecutionNameSpace()) &&
				Objects.equals(getOwnerEmail(), taskDef.getOwnerEmail());
	}

	@Override
	public int hashCode() {

		return Objects.hash(getName(), getDescription(), getRetryCount(), getTimeoutSeconds(), getInputKeys(),
				getOutputKeys(), getTimeoutPolicy(), getRetryLogic(), getRetryDelaySeconds(),
				getResponseTimeoutSeconds(), getConcurrentExecLimit(), getRateLimitPerFrequency(), getInputTemplate(),
				getIsolationGroupId(), getExecutionNameSpace(), getOwnerEmail());
	}
}
