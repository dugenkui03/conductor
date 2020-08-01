package com.netflix.conductor.jetty.server;

import com.google.inject.AbstractModule;

//jetty 码头
public class JettyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(JettyServerConfiguration.class).to(JettyServerSystemConfiguration.class);
        bind(JettyServerProvider.class);
    }
}
