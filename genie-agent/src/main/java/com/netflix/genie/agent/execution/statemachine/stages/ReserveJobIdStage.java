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
package com.netflix.genie.agent.execution.statemachine.stages;

import com.netflix.genie.agent.cli.UserConsole;
import com.netflix.genie.agent.execution.exceptions.JobIdUnavailableException;
import com.netflix.genie.agent.execution.exceptions.JobReservationException;
import com.netflix.genie.agent.execution.services.AgentJobService;
import com.netflix.genie.agent.execution.statemachine.ExecutionContext;
import com.netflix.genie.agent.execution.statemachine.ExecutionStage;
import com.netflix.genie.agent.execution.statemachine.FatalJobExecutionException;
import com.netflix.genie.agent.execution.statemachine.RetryableJobExecutionException;
import com.netflix.genie.agent.execution.statemachine.States;
import com.netflix.genie.common.external.dtos.v4.AgentClientMetadata;
import com.netflix.genie.common.external.dtos.v4.AgentJobRequest;
import com.netflix.genie.common.external.dtos.v4.JobStatus;
import com.netflix.genie.common.internal.exceptions.unchecked.GenieRuntimeException;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs job reservation, or ensures the job is pre-reserved and ready to be claimed.
 *
 * @author mprimi
 * @since 4.0.0
 */
@Slf4j
public class ReserveJobIdStage extends ExecutionStage {
    private final AgentJobService agentJobService;

    /**
     * Constructor.
     *
     * @param agentJobService agent job service.
     */
    public ReserveJobIdStage(final AgentJobService agentJobService) {
        super(States.RESERVE_JOB_ID);
        this.agentJobService = agentJobService;
    }

    @Override
    protected void attemptStageAction(
        final ExecutionContext executionContext
    ) throws RetryableJobExecutionException, FatalJobExecutionException {

        final String requestedJobId = executionContext.getRequestedJobId();
        final String reservedJobId;

        if (executionContext.isPreResolved()) {
            assert requestedJobId != null;
            log.info("Confirming job reservation");

            // TODO create protocol to verify server-side status is indeed ACCEPTED
            executionContext.setCurrentJobStatus(JobStatus.ACCEPTED);
            reservedJobId = requestedJobId;

        } else {
            log.info("Requesting job id reservation");

            final AgentJobRequest jobRequest = executionContext.getAgentJobRequest();
            final AgentClientMetadata agentClientMetadata = executionContext.getAgentClientMetadata();

            assert jobRequest != null;
            assert agentClientMetadata != null;

            try {
                reservedJobId = this.agentJobService.reserveJobId(jobRequest, agentClientMetadata);
            } catch (final GenieRuntimeException e) {
                throw createRetryableException(e);
            } catch (final JobIdUnavailableException | JobReservationException e) {
                throw createFatalException(e);
            }

            executionContext.setCurrentJobStatus(JobStatus.RESERVED);

            UserConsole.getLogger().info("Successfully reserved job id: {}", reservedJobId);
        }

        executionContext.setReservedJobId(reservedJobId);
    }
}
