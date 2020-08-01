/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.dyno;

import com.netflix.conductor.core.config.Configuration;

//优质 配置
public interface DynomiteConfiguration extends Configuration {
    // FIXME Are cluster and cluster name really different things?
    // FIXME 集群和集群名字有必要做区分吗

    //集群
    String CLUSTER_PROPERTY_NAME = "workflow.dynomite.cluster";
    String CLUSTER_DEFAULT_VALUE = null;

    //集群名称
    String CLUSTER_NAME_PROPERTY_NAME = "workflow.dynomite.cluster.name";
    String HOSTS_PROPERTY_NAME = "workflow.dynomite.cluster.hosts";

    //每个主机最大链接数？
    String MAX_CONNECTIONS_PER_HOST_PROPERTY_NAME = "workflow.dynomite.connection.maxConnsPerHost";
    int MAX_CONNECTIONS_PER_HOST_DEFAULT_VALUE = 10;

    // 队列前缀：命名空间
    String ROOT_NAMESPACE_PROPERTY_NAME = "workflow.namespace.queue.prefix";
    String ROOT_NAMESPACE_DEFAULT_VALUE = null;

    //dyno：功率计
    //dynomite：优质的、极好的
    String DOMAIN_PROPERTY_NAME = "workflow.dyno.keyspace.domain";
    String DOMAIN_DEFAULT_VALUE = null;

    //quorum：仲裁集、法定人数
    String NON_QUORUM_PORT_PROPERTY_NAME = "queues.dynomite.nonQuorum.port";
    int NON_QUORUM_PORT_DEFAULT_VALUE = 22122;

    default String getCluster() {
        return getProperty(CLUSTER_PROPERTY_NAME, CLUSTER_DEFAULT_VALUE);
    }

    default String getClusterName() {
        return getProperty(CLUSTER_NAME_PROPERTY_NAME, "");
    }

    default String getHosts() {
        return getProperty(HOSTS_PROPERTY_NAME, null);
    }

    default String getRootNamespace() {
        return getProperty(ROOT_NAMESPACE_PROPERTY_NAME, ROOT_NAMESPACE_DEFAULT_VALUE);
    }

    default String getDomain() {
        return getProperty(DOMAIN_PROPERTY_NAME, DOMAIN_DEFAULT_VALUE);
    }

    default int getMaxConnectionsPerHost() {
        return getIntProperty(
                MAX_CONNECTIONS_PER_HOST_PROPERTY_NAME,
                MAX_CONNECTIONS_PER_HOST_DEFAULT_VALUE
        );
    }

    default int getNonQuorumPort() {
        return getIntProperty(NON_QUORUM_PORT_PROPERTY_NAME, NON_QUORUM_PORT_DEFAULT_VALUE);
    }

    default String getQueuePrefix() {
        String prefix = getRootNamespace() + "." + getStack();

        if (getDomain() != null) {
            prefix = prefix + "." + getDomain();
        }

        return prefix;
    }
}
