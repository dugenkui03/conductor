/*
 * Copyright 2017 Netflix, Inc.
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
package com.netflix.conductor.jetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.GuiceFilter;
import com.netflix.conductor.bootstrap.Main;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.utils.JsonMapperProvider;
import com.netflix.conductor.service.Lifecycle;
import com.sun.jersey.api.client.Client;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.ws.rs.core.MediaType;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.eclipse.jetty.util.log.Log.getLog;

/**
 * @author Viren
 */
public class JettyServer implements Lifecycle {

    private static Logger logger = LoggerFactory.getLogger(JettyServer.class);

    //端口
    private final int port;
    private final boolean join;

    //jetty服务(组合)：提供HTTP服务
    // https://developer.ibm.com/zh/articles/j-lo-jetty/
    // todo volatile、防止指令重排
    private Server server;


    public JettyServer(int port, boolean join) {
        this.port = port;
        this.join = join;
    }


    @Override
    public synchronized void start() throws Exception {
        //如果服务已经在运行了
        if (server != null) {
            throw new IllegalStateException("Server is already running");
        }

        //指定端口新建jetty服务
        this.server = new Server(port);

        /**
         * 创建上下文处理器：ContextHandler
         */
        //servlet上下文处理器、还是jetty的东西
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        contextHandler.setWelcomeFiles(new String[]{"index.html"});
        server.setHandler(contextHandler);

        // 查看系统属性是否有 enableJMX，有的话 加载bean和监听器
        if (Boolean.getBoolean("enableJMX")) {
            System.out.println("configure MBean container...");
            configureMBeanContainer(server);
        }

        // 开始启动jetty服务
        server.start();
        System.out.println("Started server on http://localhost:" + port + "/");
        try {
            // 是否加载样例
//            if (Boolean.getBoolean("loadSample")) {
                System.out.println("Creating kitchensink workflow");
                createKitchenSink(port);
//            }
        } catch (Exception e) {
            logger.error("Error loading sample!", e);
        }

        // 阻塞、直至线程池stop
        if (join) {
            server.join();
        }

    }

    // 停止服务
    public synchronized void stop() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Server is not running.  call #start() method to start the server");
        }
        server.stop();
        server = null;
    }


    private static void createKitchenSink(int port) throws Exception {
        Client client = Client.create();
        ObjectMapper objectMapper = new JsonMapperProvider().get();

        List<TaskDef> taskDefs = new LinkedList<>();
        TaskDef taskDef;
        for (int i = 0; i < 40; i++) {
            taskDef = new TaskDef("task_" + i, "task_" + i, 1, 0);
            taskDef.setOwnerEmail("example@email.com");
            taskDefs.add(taskDef);
        }

        taskDef = new TaskDef("search_elasticsearch", "search_elasticsearch", 1, 0);
        taskDef.setOwnerEmail("example@email.com");
        taskDefs.add(taskDef);

        client.resource("http://localhost:" + port + "/api/metadata/taskdefs").type(MediaType.APPLICATION_JSON).post(objectMapper.writeValueAsString(taskDefs));

        /*
         * Kitchensink example (stored workflow with stored tasks)
         */
        InputStream stream = Main.class.getResourceAsStream("/kitchensink.json");
        client.resource("http://localhost:" + port + "/api/metadata/workflow").type(MediaType.APPLICATION_JSON).post(stream);

        stream = Main.class.getResourceAsStream("/sub_flow_1.json");
        client.resource("http://localhost:" + port + "/api/metadata/workflow").type(MediaType.APPLICATION_JSON).post(stream);

        Map<String, Object> payload = ImmutableMap.of("task2Name", "task_5");
        String payloadStr = objectMapper.writeValueAsString(payload);
        client.resource("http://localhost:" + port + "/api/workflow/kitchensink").type(MediaType.APPLICATION_JSON).post(payloadStr);

        logger.info("Kitchen sink workflow is created!");

        /*
         * Kitchensink example with ephemeral workflow and stored tasks
         */
        InputStream ephemeralInputStream = Main.class.getResourceAsStream("/kitchenSink-ephemeralWorkflowWithStoredTasks.json");
        client.resource("http://localhost:" + port + "/api/workflow/").type(MediaType.APPLICATION_JSON).post(ephemeralInputStream);
        logger.info("Ephemeral Kitchen sink workflow with stored tasks is created!");

        /*
         * Kitchensink example with ephemeral workflow and ephemeral tasks
         */
        ephemeralInputStream = Main.class.getResourceAsStream("/kitchenSink-ephemeralWorkflowWithEphemeralTasks.json");
        client.resource("http://localhost:" + port + "/api/workflow/").type(MediaType.APPLICATION_JSON).post(ephemeralInputStream);
        logger.info("Ephemeral Kitchen sink workflow with ephemeral tasks is created!");

    }

    /**加载bean和监听器
     * Enabled JMX reporting:
     * https://docs.newrelic.com/docs/agents/java-agent/troubleshooting/application-server-jmx-setup
     * https://www.eclipse.org/jetty/documentation/current/jmx-chapter
     */
    private void configureMBeanContainer(final Server server){
        final MBeanContainer mbContainer = new MBeanContainer(getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        server.addBean(getLog());
    }
}
