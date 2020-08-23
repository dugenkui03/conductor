/**
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
/**
 *
 */
package com.netflix.conductor.core.events.queue;

import rx.Observable;

import java.util.List;

/**
 * 可观察的队列？
 */
public interface ObservableQueue {

	// 返回给定队列的观察者 @return An observable for the given queue
	Observable<Message> observe();

	// Type of the queue
	String getType();

	// Name of the queue
	String getName();

	// 队列的URI标志符 URI identifier for the queue.
	String getURI();

	// 传入需要被ack的消息；
	// 返回不能被ack的消息id；
	List<String> ack(List<Message> messages);

	//要被公布的消息
	void publish(List<Message> messages);

	/**
	 * Used to determine if the queue supports unack/visibility timeout such that the messages
     * will re-appear on the queue after a specific period and are available to be picked up again and retried.
     *
	 * @return - false if the queue message need not be re-published to the queue for retriability
     *         - true if the message must be re-published to the queue for retriability
	 */
	 default boolean rePublishIfNoAck() {
	     return false;
	 }

	/**
	 * Extend the lease of the unacknowledged message for longer period.
	 * @param message Message for which the timeout has to be changed
	 * @param unackTimeout timeout in milliseconds for which the unack lease should be extended. (replaces the current value with this value)
	 */
	void setUnackTimeout(Message message, long unackTimeout);

	// 队列的大小 - no. messages pending(挂起)
	// 注意：取决于其实现，这个可以是个近似值。
	long size();

	// 用于关闭队列实例
	// Used to close queue instance prior to remove from queues
	default  void close() { }
}
