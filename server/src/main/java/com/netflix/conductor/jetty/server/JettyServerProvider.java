package com.netflix.conductor.jetty.server;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * jetty服务provider
 */
public class JettyServerProvider implements Provider<Optional<JettyServer>> {
    //jetty服务配置
    private final JettyServerConfiguration configuration;

    @Inject
    public JettyServerProvider(JettyServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<JettyServer> get() {
        return
                //判断 conductor.jetty.server.enabled
                configuration.isEnabled() ?
                //true值
                Optional.of(
                        new JettyServer(
                                configuration.getPort(),
                                configuration.isJoin()
                        ))
                //false值
                : Optional.empty();
    }
}
