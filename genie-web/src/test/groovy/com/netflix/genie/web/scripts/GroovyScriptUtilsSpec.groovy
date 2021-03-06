/*
 *
 *  Copyright 2020 Netflix, Inc.
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
package com.netflix.genie.web.scripts

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.netflix.genie.common.external.dtos.v4.Cluster
import com.netflix.genie.common.external.dtos.v4.Command
import com.netflix.genie.common.external.dtos.v4.ExecutionResourceCriteria
import com.netflix.genie.common.external.dtos.v4.JobMetadata
import com.netflix.genie.common.external.dtos.v4.JobRequest
import spock.lang.Specification

/**
 * Specifications for {@link GroovyScriptUtils}.
 *
 * @author tgianos
 */
class GroovyScriptUtilsSpec extends Specification {

    Binding scriptBinding

    def setup() {
        this.scriptBinding = new Binding()
    }

    def "Can get job id"() {
        when:
        GroovyScriptUtils.getJobId(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(ResourceSelectorScript.JOB_ID_BINDING, 1234L)
        GroovyScriptUtils.getJobId(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        def expectedJobId = UUID.randomUUID().toString()
        this.scriptBinding.setVariable(ResourceSelectorScript.JOB_ID_BINDING, expectedJobId)
        def jobId = GroovyScriptUtils.getJobId(this.scriptBinding)

        then:
        jobId == expectedJobId
    }

    def "Can get job request"() {
        when:
        GroovyScriptUtils.getJobRequest(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(ResourceSelectorScript.JOB_REQUEST_BINDING, 1234L)
        GroovyScriptUtils.getJobRequest(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        def expectedJobRequest = new JobRequest(
            null,
            null,
            Lists.newArrayList(UUID.randomUUID().toString()),
            new JobMetadata.Builder(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            ).build(),
            Mock(ExecutionResourceCriteria),
            null,
            null,
            null
        )
        this.scriptBinding.setVariable(ResourceSelectorScript.JOB_REQUEST_BINDING, expectedJobRequest)
        def jobRequest = GroovyScriptUtils.getJobRequest(this.scriptBinding)

        then:
        jobRequest == expectedJobRequest
    }

    def "Can get clusters"() {
        when:
        GroovyScriptUtils.getClusters(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(ClusterSelectorManagedScript.CLUSTERS_BINDING, Lists.newArrayList())
        GroovyScriptUtils.getClusters(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(ClusterSelectorManagedScript.CLUSTERS_BINDING, Sets.newHashSet())
        GroovyScriptUtils.getClusters(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(
            ClusterSelectorManagedScript.CLUSTERS_BINDING,
            Sets.newHashSet(Mock(Cluster), "not a cluster")
        )
        GroovyScriptUtils.getClusters(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        def expectedClusters = Sets.newHashSet(Mock(Cluster), Mock(Cluster))
        this.scriptBinding.setVariable(ClusterSelectorManagedScript.CLUSTERS_BINDING, expectedClusters)
        def clusters = GroovyScriptUtils.getClusters(this.scriptBinding)

        then:
        clusters == expectedClusters
    }

    def "Can get commands"() {
        when:
        GroovyScriptUtils.getCommands(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(CommandSelectorManagedScript.COMMANDS_BINDING, Lists.newArrayList())
        GroovyScriptUtils.getCommands(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(CommandSelectorManagedScript.COMMANDS_BINDING, Sets.newHashSet())
        GroovyScriptUtils.getCommands(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        this.scriptBinding.setVariable(
            CommandSelectorManagedScript.COMMANDS_BINDING,
            Sets.newHashSet(Mock(Command), "not a command")
        )
        GroovyScriptUtils.getCommands(this.scriptBinding)

        then:
        thrown(IllegalArgumentException)

        when:
        def expectedCommands = Sets.newHashSet(Mock(Command), Mock(Command))
        this.scriptBinding.setVariable(CommandSelectorManagedScript.COMMANDS_BINDING, expectedCommands)
        def commands = GroovyScriptUtils.getCommands(this.scriptBinding)

        then:
        commands == expectedCommands
    }
}
