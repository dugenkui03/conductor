package com.netflix.conductor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface provides a means to help handle objects, especially those that are injected, that have a lifecycle
 * component.  Guice explicitly does not support this and recommends a patter much like this.  This should be used by
 * anything that needs to create resources or clean them up when the application is started or stopped, such as server
 * listeners, clients, etc.
 *
 * @see <a href="https://github.com/google/guice/wiki/ModulesShouldBeFastAndSideEffectFree">ModulesShouldBeFastAndSideEffectFree</a>
 */
public interface Lifecycle {

    Logger logger = LoggerFactory.getLogger(Lifecycle.class);

    //开始
    default void start() throws Exception {
        registerShutdownHook();
    }

    //停止
    void stop() throws Exception;

    //fixme Runtime.getRuntime().addShutdownHook：jvm关闭的时候执行hook方法。
    default void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                //尝试关闭 生命周期组件 的时候失败
                logger.error("Error when trying to shutdown a lifecycle component: " + this.getClass().getName(), e);
            }
        }));
    }
}
