/*
 *
 *  Copyright 2017 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.web.spring.autoconfigure.tasks.leader;

import com.netflix.genie.common.internal.util.GenieHostInfo;
import com.netflix.genie.web.data.services.AgentConnectionPersistenceService;
import com.netflix.genie.web.data.services.ApplicationPersistenceService;
import com.netflix.genie.web.data.services.ClusterPersistenceService;
import com.netflix.genie.web.data.services.CommandPersistenceService;
import com.netflix.genie.web.data.services.DataServices;
import com.netflix.genie.web.data.services.FilePersistenceService;
import com.netflix.genie.web.data.services.JobPersistenceService;
import com.netflix.genie.web.data.services.JobSearchService;
import com.netflix.genie.web.data.services.TagPersistenceService;
import com.netflix.genie.web.events.GenieEventBus;
import com.netflix.genie.web.properties.AgentCleanupProperties;
import com.netflix.genie.web.properties.ClusterCheckerProperties;
import com.netflix.genie.web.properties.DatabaseCleanupProperties;
import com.netflix.genie.web.properties.JobsProperties;
import com.netflix.genie.web.properties.LeadershipProperties;
import com.netflix.genie.web.properties.UserMetricsProperties;
import com.netflix.genie.web.properties.ZookeeperLeaderProperties;
import com.netflix.genie.web.services.ClusterLeaderService;
import com.netflix.genie.web.spring.actuators.LeaderElectionActuator;
import com.netflix.genie.web.spring.autoconfigure.tasks.TasksAutoConfiguration;
import com.netflix.genie.web.tasks.leader.AgentJobCleanupTask;
import com.netflix.genie.web.tasks.leader.ClusterCheckerTask;
import com.netflix.genie.web.tasks.leader.DatabaseCleanupTask;
import com.netflix.genie.web.tasks.leader.LeaderTasksCoordinator;
import com.netflix.genie.web.tasks.leader.LocalLeader;
import com.netflix.genie.web.tasks.leader.UserMetricsTask;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.listen.Listenable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.zookeeper.config.LeaderInitiatorFactoryBean;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for the {@link LeaderAutoConfiguration} class.
 *
 * @author tgianos
 * @since 3.1.0
 */
class LeaderAutoConfigurationTest {

    private ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    TasksAutoConfiguration.class,
                    LeaderAutoConfiguration.class
                )
            )
            .withUserConfiguration(MockBeanConfig.class);

    /**
     * All the expected default beans exist.
     */
    @Test
    void expectedBeansExist() {
        this.contextRunner.run(
            context -> {
                Assertions.assertThat(context).hasSingleBean(AgentCleanupProperties.class);
                Assertions.assertThat(context).hasSingleBean(ClusterCheckerProperties.class);
                Assertions.assertThat(context).hasSingleBean(DatabaseCleanupProperties.class);
                Assertions.assertThat(context).hasSingleBean(LeadershipProperties.class);
                Assertions.assertThat(context).hasSingleBean(UserMetricsProperties.class);
                Assertions.assertThat(context).hasSingleBean(ZookeeperLeaderProperties.class);

                Assertions.assertThat(context).hasSingleBean(LeaderTasksCoordinator.class);
                Assertions.assertThat(context).doesNotHaveBean(LeaderInitiatorFactoryBean.class);
                Assertions.assertThat(context).hasSingleBean(LocalLeader.class);
                Assertions.assertThat(context).hasSingleBean(ClusterCheckerTask.class);
                Assertions.assertThat(context).hasSingleBean(ClusterLeaderService.class);
                Assertions.assertThat(context).hasSingleBean(LeaderElectionActuator.class);

                // Optional beans
                Assertions.assertThat(context).doesNotHaveBean(DatabaseCleanupTask.class);
                Assertions.assertThat(context).doesNotHaveBean(UserMetricsTask.class);
                Assertions.assertThat(context).doesNotHaveBean(AgentJobCleanupTask.class);
                Assertions.assertThat(context).doesNotHaveBean(LeaderInitiatorFactoryBean.class);
            }
        );
    }

    /**
     * All the expected optional beans exist.
     */
    @Test
    void optionalBeansCreated() {
        this.contextRunner
            .withPropertyValues(
                "genie.tasks.database-cleanup.enabled=true",
                "genie.tasks.user-metrics.enabled=true",
                "genie.tasks.agent-cleanup.enabled=true"
            )
            .run(
                context -> {
                    Assertions.assertThat(context).hasSingleBean(AgentCleanupProperties.class);
                    Assertions.assertThat(context).hasSingleBean(ClusterCheckerProperties.class);
                    Assertions.assertThat(context).hasSingleBean(DatabaseCleanupProperties.class);
                    Assertions.assertThat(context).hasSingleBean(LeadershipProperties.class);
                    Assertions.assertThat(context).hasSingleBean(UserMetricsProperties.class);
                    Assertions.assertThat(context).hasSingleBean(ZookeeperLeaderProperties.class);

                    Assertions.assertThat(context).hasSingleBean(LeaderTasksCoordinator.class);
                    Assertions.assertThat(context).doesNotHaveBean(LeaderInitiatorFactoryBean.class);
                    Assertions.assertThat(context).hasSingleBean(LocalLeader.class);
                    Assertions.assertThat(context).hasSingleBean(ClusterCheckerTask.class);

                    Assertions.assertThat(context).hasSingleBean(ClusterLeaderService.class);
                    Assertions.assertThat(context).hasSingleBean(LeaderElectionActuator.class);

                    // Optional beans
                    Assertions.assertThat(context).hasSingleBean(DatabaseCleanupTask.class);
                    Assertions.assertThat(context).hasSingleBean(UserMetricsTask.class);
                    Assertions.assertThat(context).hasSingleBean(AgentJobCleanupTask.class);
                    Assertions.assertThat(context).doesNotHaveBean(LeaderInitiatorFactoryBean.class);
                }
            );
    }

    /**
     * All the expected beans exist when zookeeper is enabled.
     */
    @Test
    void expectedZookeeperBeansExist() {
        this.contextRunner
            .withUserConfiguration(ZookeeperMockConfig.class)
            .run(
                context -> {
                    Assertions.assertThat(context).hasSingleBean(AgentCleanupProperties.class);
                    Assertions.assertThat(context).hasSingleBean(ClusterCheckerProperties.class);
                    Assertions.assertThat(context).hasSingleBean(DatabaseCleanupProperties.class);
                    Assertions.assertThat(context).hasSingleBean(LeadershipProperties.class);
                    Assertions.assertThat(context).hasSingleBean(UserMetricsProperties.class);
                    Assertions.assertThat(context).hasSingleBean(ZookeeperLeaderProperties.class);

                    Assertions.assertThat(context).hasSingleBean(LeaderTasksCoordinator.class);
                    Assertions.assertThat(context).hasSingleBean(LeaderInitiatorFactoryBean.class);
                    Assertions.assertThat(context).doesNotHaveBean(LocalLeader.class);
                    Assertions.assertThat(context).hasSingleBean(ClusterCheckerTask.class);

                    Assertions.assertThat(context).hasSingleBean(ClusterLeaderService.class);
                    Assertions.assertThat(context).hasSingleBean(LeaderElectionActuator.class);

                    // Optional beans
                    Assertions.assertThat(context).doesNotHaveBean(DatabaseCleanupTask.class);
                    Assertions.assertThat(context).doesNotHaveBean(UserMetricsTask.class);
                    Assertions.assertThat(context).doesNotHaveBean(AgentJobCleanupTask.class);
                }
            );
    }

    /**
     * Configuration for beans that are dependencies of the auto configured beans in {@link TasksAutoConfiguration}.
     *
     * @author tgianos
     * @since 4.0.0
     */
    static class MockBeanConfig {

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        GenieHostInfo genieHostInfo() {
            return Mockito.mock(GenieHostInfo.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        JobSearchService jobSearchService() {
            return Mockito.mock(JobSearchService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        JobPersistenceService jobPersistenceService() {
            return Mockito.mock(JobPersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        ClusterPersistenceService clusterPersistenceService() {
            return Mockito.mock(ClusterPersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        FilePersistenceService filePersistenceService() {
            return Mockito.mock(FilePersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        JobsProperties jobsProperties() {
            return JobsProperties.getJobsPropertiesDefaults();
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        TagPersistenceService tagPersistenceService() {
            return Mockito.mock(TagPersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        RestTemplate genieRestTemplate() {
            return Mockito.mock(RestTemplate.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        WebEndpointProperties webEndpointProperties() {
            return Mockito.mock(WebEndpointProperties.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        MeterRegistry meterRegistry() {
            return Mockito.mock(MeterRegistry.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        GenieEventBus genieEventBus() {
            return Mockito.mock(GenieEventBus.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        AgentConnectionPersistenceService agentConnectionPersistenceService() {
            return Mockito.mock(AgentConnectionPersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        ApplicationPersistenceService applicationPersistenceService() {
            return Mockito.mock(ApplicationPersistenceService.class);
        }

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance
         */
        @Bean
        CommandPersistenceService commandPersistenceService() {
            return Mockito.mock(CommandPersistenceService.class);
        }

        /**
         * Encapsulation containing mocked instances.
         *
         * @param agentConnectionPersistenceService Mocked
         * @param applicationPersistenceService     Mocked
         * @param clusterPersistenceService         Mocked
         * @param commandPersistenceService         Mocked
         * @param filePersistenceService            Mocked
         * @param jobPersistenceService             Mocked
         * @param jobSearchService                  Mocked
         * @param tagPersistenceService             Mocked
         * @return {@link DataServices} instance
         */
        @Bean
        DataServices genieDataServices(
            final AgentConnectionPersistenceService agentConnectionPersistenceService,
            final ApplicationPersistenceService applicationPersistenceService,
            final ClusterPersistenceService clusterPersistenceService,
            final CommandPersistenceService commandPersistenceService,
            final FilePersistenceService filePersistenceService,
            final JobPersistenceService jobPersistenceService,
            final JobSearchService jobSearchService,
            final TagPersistenceService tagPersistenceService
        ) {
            return new DataServices(
                agentConnectionPersistenceService,
                applicationPersistenceService,
                clusterPersistenceService,
                commandPersistenceService,
                filePersistenceService,
                jobPersistenceService,
                jobSearchService,
                tagPersistenceService
            );
        }
    }

    /**
     * Mock configuration for pretending zookeeper is enabled.
     */
    @Configuration
    static class ZookeeperMockConfig {

        /**
         * Mocked bean.
         *
         * @return Mocked bean instance.
         */
        @Bean
        @SuppressWarnings("unchecked")
        CuratorFramework curatorFramework() {
            final CuratorFramework curatorFramework = Mockito.mock(CuratorFramework.class);
            Mockito
                .when(curatorFramework.getConnectionStateListenable())
                .thenReturn(Mockito.mock(Listenable.class));
            return curatorFramework;
        }
    }
}
