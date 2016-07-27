/*
 *
 *  Copyright 2015 Netflix, Inc.
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
package com.netflix.genie.core.jobs.workflow.impl;

import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.core.jobs.AdminResources;
import com.netflix.genie.core.jobs.FileType;
import com.netflix.genie.core.jobs.JobConstants;
import com.netflix.genie.core.jobs.JobExecutionEnvironment;
import com.netflix.genie.core.services.impl.GenieFileTransferService;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the workflow task for processing command information.
 *
 * @author amsharma
 * @since 3.0.0
 */
@Slf4j
public class CommandTask extends GenieBaseTask {

    private final Timer timer;
    private final GenieFileTransferService fts;

    /**
     * Constructor.
     *
     * @param registry The metrics registry to use
     * @param fts File transfer service
     */
    public CommandTask(@NotNull final Registry registry,
            @NotNull final GenieFileTransferService fts) {
        this.timer = registry.timer("genie.jobs.tasks.commandTask.timer");
        this.fts = fts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeTask(@NotNull final Map<String, Object> context) throws GenieException, IOException {
        final long start = System.nanoTime();
        try {
            log.debug("Executing Command Task in the workflow.");

            final JobExecutionEnvironment jobExecEnv =
                (JobExecutionEnvironment) context.get(JobConstants.JOB_EXECUTION_ENV_KEY);
            final String jobWorkingDirectory = jobExecEnv.getJobWorkingDir().getCanonicalPath();
            final String genieDir = jobWorkingDirectory
                + JobConstants.FILE_PATH_DELIMITER
                + JobConstants.GENIE_PATH_VAR;
            final Writer writer = (Writer) context.get(JobConstants.WRITER_KEY);

            // Create the directory for this command under command dir in the cwd
            createEntityInstanceDirectory(
                genieDir,
                jobExecEnv.getCommand().getId(),
                AdminResources.COMMAND
            );

            // Create the config directory for this id
            createEntityInstanceConfigDirectory(
                genieDir,
                jobExecEnv.getCommand().getId(),
                AdminResources.COMMAND
            );

            // Get the setup file if specified and add it as source command in launcher script
            final String commandSetupFile = jobExecEnv.getCommand().getSetupFile();
            if (commandSetupFile != null && StringUtils.isNotBlank(commandSetupFile)) {
                final String localPath = super.buildLocalFilePath(
                    jobWorkingDirectory,
                    jobExecEnv.getCommand().getId(),
                    commandSetupFile,
                    FileType.SETUP,
                    AdminResources.COMMAND
                );

                fts.getFile(commandSetupFile, localPath);

                super.generateSetupFileSourceSnippet(
                    jobExecEnv.getCommand().getId(),
                    "Command:",
                    localPath,
                    writer,
                    jobWorkingDirectory);
            }

            // Iterate over and get all configuration files
            for (final String configFile : jobExecEnv.getCommand().getConfigs()) {
                final String localPath = super.buildLocalFilePath(
                    jobWorkingDirectory,
                    jobExecEnv.getCommand().getId(),
                    configFile,
                    FileType.CONFIG,
                    AdminResources.COMMAND
                );
                fts.getFile(configFile, localPath);
            }
        } finally {
            final long finish = System.nanoTime();
            this.timer.record(finish - start, TimeUnit.NANOSECONDS);
        }
    }
}
