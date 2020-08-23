package com.netflix.conductor.grpc.server;

import com.netflix.conductor.service.Lifecycle;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;

@Singleton
public class GRPCServer implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(GRPCServer.class);

    private final Server server;

    // 服务端口
    public GRPCServer(int port, BindableService... services) {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);

        // 添加service实现到handler注册中心
        // Adds a service implementation to the handler registry
        Arrays.stream(services).forEach(builder::addService);
        server = builder.build();
    }

    @Override
    public void start() throws IOException {
        // 注册钩子方法-关闭jvm时进行的操作
        registerShutdownHook();

        // 绑定并启动服务；打印端口
        server.start();
        logger.info("grpc: Server started, listening on " + server.getPort());
    }

    @Override
    public void stop() {
        if (server != null) {
            logger.info("grpc: server shutting down");
            server.shutdown();
        }
    }
}
