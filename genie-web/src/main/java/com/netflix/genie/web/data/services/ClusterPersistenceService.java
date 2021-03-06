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
package com.netflix.genie.web.data.services;

import com.github.fge.jsonpatch.JsonPatch;
import com.netflix.genie.common.dto.JobRequest;
import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.external.dtos.v4.Cluster;
import com.netflix.genie.common.external.dtos.v4.ClusterRequest;
import com.netflix.genie.common.external.dtos.v4.ClusterStatus;
import com.netflix.genie.common.external.dtos.v4.Command;
import com.netflix.genie.common.external.dtos.v4.CommandStatus;
import com.netflix.genie.common.external.dtos.v4.Criterion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstraction layer to encapsulate data ClusterConfig functionality.<br>
 * Classes implementing this abstraction layer must be thread-safe
 *
 * @author amsharma
 * @author tgianos
 */
@Validated
public interface ClusterPersistenceService {

    /**
     * Create new cluster configuration.
     *
     * @param request The cluster information to create
     * @return The created clusters id
     * @throws GenieException if there is an error
     */
    String createCluster(
        @NotNull(message = "No cluster request entered. Unable to create.")
        @Valid ClusterRequest request
    ) throws GenieException;

    /**
     * Get the cluster configuration by id.
     *
     * @param id unique id of cluster configuration to return
     * @return The cluster configuration
     * @throws GenieException For any error
     */
    Cluster getCluster(
        @NotBlank(message = "No id entered. Unable to get.") String id
    ) throws GenieException;

    /**
     * Get cluster info for various parameters. Null or empty parameters are
     * ignored.
     *
     * @param name          cluster name
     * @param statuses      valid types - Types.ClusterStatus
     * @param tags          tags allocated to this cluster
     * @param minUpdateTime min time when cluster configuration was updated
     * @param maxUpdateTime max time when cluster configuration was updated
     * @param page          The page to get
     * @return All the clusters matching the criteria
     */
    Page<Cluster> getClusters(
        @Nullable String name,
        @Nullable Set<ClusterStatus> statuses,
        @Nullable Set<String> tags,
        @Nullable Instant minUpdateTime,
        @Nullable Instant maxUpdateTime,
        Pageable page
    );

    /**
     * Find the clusters and commands that can run a job given the criteria the user asked for in the job.
     *
     * @param jobRequest The request to runt he job. Not null.
     * @return a map of cluster to the unique id of the command to use if that cluster is used
     * @throws GenieException if there is an error
     */
    Map<Cluster, String> findClustersAndCommandsForJob(
        @NotNull(message = "JobRequest object is null. Unable to continue.") JobRequest jobRequest
    ) throws GenieException;

    /**
     * Find the clusters and commands that can run a job given the criteria the user asked for in the job.
     *
     * @param clusterCriteria  The ordered list of cluster criterion provided by user to select a cluster for a job
     * @param commandCriterion The criterion to use to select a command for a job on a cluster
     * @return a map of cluster to the unique id of the command to use if that cluster is used
     * @throws GenieException if there is an error
     */
    Map<Cluster, String> findClustersAndCommandsForCriteria(
        @NotEmpty List<@NotNull Criterion> clusterCriteria,
        @NotNull Criterion commandCriterion
    ) throws GenieException;

    /**
     * Update a cluster.
     *
     * @param id            The id of the cluster to update
     * @param updateCluster The information to update the cluster with
     * @throws GenieException if there is an error
     */
    void updateCluster(
        @NotBlank(message = "No cluster id entered. Unable to update.") String id,
        @NotNull(message = "No cluster information entered. Unable to update.")
        @Valid Cluster updateCluster
    ) throws GenieException;

    /**
     * Patch a cluster with the given json patch.
     *
     * @param id    The id of the cluster to update
     * @param patch The json patch to use to update the given cluster
     * @throws GenieException if there is an error
     */
    void patchCluster(@NotBlank String id, @NotNull JsonPatch patch) throws GenieException;

    /**
     * Delete all clusters from database.
     *
     * @throws GenieException if there is an error
     */
    void deleteAllClusters() throws GenieException;

    /**
     * Delete a cluster configuration by id.
     *
     * @param id unique id for cluster to delete
     * @throws GenieException if there is an error
     */
    void deleteCluster(
        @NotBlank(message = "No id entered unable to delete.") String id
    ) throws GenieException;

    /**
     * Add configuration files to the cluster.
     *
     * @param id      The id of the cluster to add the configuration file to. Not
     *                null/empty/blank.
     * @param configs The configuration files to add. Not null/empty.
     * @throws GenieException if there is an error
     */
    void addConfigsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to add configurations.") String id,
        @NotEmpty(message = "No configuration files entered. Unable to add.") Set<String> configs
    ) throws GenieException;

    /**
     * Get the set of configuration files associated with the cluster with given
     * id.
     *
     * @param id The id of the cluster to get the configuration files for. Not
     *           null/empty/blank.
     * @return The set of configuration files as paths
     * @throws GenieException if there is an error
     */
    Set<String> getConfigsForCluster(
        @NotBlank(message = "No cluster id sent. Cannot retrieve configurations.") String id
    ) throws GenieException;

    /**
     * Update the set of configuration files associated with the cluster with
     * given id.
     *
     * @param id      The id of the cluster to update the configuration files for.
     *                Not null/empty/blank.
     * @param configs The configuration files to replace existing configurations
     *                with. Not null/empty.
     * @throws GenieException if there is an error
     */
    void updateConfigsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to update configurations.") String id,
        @NotEmpty(message = "No configs entered. Unable to update.") Set<String> configs
    ) throws GenieException;

    /**
     * Remove all configuration files from the cluster.
     *
     * @param id The id of the cluster to remove the configuration file from.
     *           Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeAllConfigsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove configs.") String id
    ) throws GenieException;

    /**
     * Add dependency files to the cluster.
     *
     * @param id           The id of the cluster to add the dependency file to. Not
     *                     null/empty/blank.
     * @param dependencies The dependency files to add. Not null.
     * @throws GenieException if there is an error
     */
    void addDependenciesForCluster(
        @NotBlank(message = "No cluster id entered. Unable to add dependencies.") String id,
        @NotEmpty(message = "No dependencies entered. Unable to add dependencies.") Set<String> dependencies
    ) throws GenieException;

    /**
     * Get the set of dependency files associated with the cluster with given id.
     *
     * @param id The id of the cluster to get the dependency files for. Not
     *           null/empty/blank.
     * @return The set of dependency files as paths
     * @throws GenieException if there is an error
     */
    Set<String> getDependenciesForCluster(
        @NotBlank(message = "No cluster id entered. Unable to get dependencies.") String id
    ) throws GenieException;

    /**
     * Update the set of dependency files associated with the cluster with given
     * id.
     *
     * @param id           The id of the cluster to update the dependency files for. Not
     *                     null/empty/blank.
     * @param dependencies The dependency files to replace existing dependencies with. Not null/empty.
     * @throws GenieException if there is an error
     */
    void updateDependenciesForCluster(
        @NotBlank(message = "No cluster id entered. Unable to update dependencies.") String id,
        @NotNull(message = "No dependencies entered. Unable to update.") Set<String> dependencies
    ) throws GenieException;

    /**
     * Remove all dependency files from the cluster.
     *
     * @param id The id of the cluster to remove the configuration file
     *           from. Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeAllDependenciesForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove dependencies.") String id
    ) throws GenieException;

    /**
     * Remove a dependency file from the cluster.
     *
     * @param id         The id of the cluster to remove the dependency file from. Not
     *                   null/empty/blank.
     * @param dependency The dependency file to remove. Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeDependencyForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove dependency.") String id,
        @NotBlank(message = "No dependency entered. Unable to remove dependency.") String dependency
    ) throws GenieException;

    /**
     * Add tags to the cluster.
     *
     * @param id   The id of the cluster to add the tags to. Not
     *             null/empty/blank.
     * @param tags The tags to add. Not null/empty.
     * @throws GenieException if there is an error
     */
    void addTagsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to add tags.") String id,
        @NotEmpty(message = "No tags entered. Unable to add to tags.") Set<String> tags
    ) throws GenieException;

    /**
     * Get the set of tags associated with the cluster with given
     * id.
     *
     * @param id The id of the cluster to get the tags for. Not
     *           null/empty/blank.
     * @return The set of tags as paths
     * @throws GenieException if there is an error
     */
    Set<String> getTagsForCluster(
        @NotBlank(message = "No cluster id sent. Cannot retrieve tags.") String id
    ) throws GenieException;

    /**
     * Update the set of tags associated with the cluster with
     * given id.
     *
     * @param id   The id of the cluster to update the tags for.
     *             Not null/empty/blank.
     * @param tags The tags to replace existing tags
     *             with. Not null/empty.
     * @throws GenieException if there is an error
     */
    void updateTagsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to update tags.") String id,
        @NotEmpty(message = "No tags entered. Unable to update.") Set<String> tags
    ) throws GenieException;

    /**
     * Remove all tags from the cluster.
     *
     * @param id The id of the cluster to remove the tags from.
     *           Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeAllTagsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove tags.") String id
    ) throws GenieException;

    /**
     * Remove a tag from the cluster.
     *
     * @param id  The id of the cluster to remove the tag from. Not
     *            null/empty/blank.
     * @param tag The tag to remove. Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeTagForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove tag.") String id,
        @NotBlank(message = "No tag entered. Unable to remove.") String tag
    ) throws GenieException;

    /**
     * Add commands to the cluster.
     *
     * @param id         The id of the cluster to add the command file to. Not
     *                   null/empty/blank.
     * @param commandIds The ids of the commands to add. Not null/empty.
     * @throws GenieException if there is an error
     */
    void addCommandsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to add commands.") String id,
        @NotEmpty(message = "No command ids entered. Unable to add commands.") List<String> commandIds
    ) throws GenieException;

    /**
     * Get the set of commands associated with the cluster with given id.
     *
     * @param id       The id of the cluster to get the commands for. Not
     *                 null/empty/blank.
     * @param statuses The statuses to get commands for
     * @return The list of commands
     * @throws GenieException if there is an error
     */
    List<Command> getCommandsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to get commands.") String id,
        @Nullable Set<CommandStatus> statuses
    ) throws GenieException;

    /**
     * Update the set of command files associated with the cluster with
     * given id.
     *
     * @param id         The id of the cluster to update the command files for. Not
     *                   null/empty/blank.
     * @param commandIds The ids of the commands to replace existing
     *                   commands with. Not null/empty.
     * @throws GenieException if there is an error
     */
    void setCommandsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to update commands.") String id,
        @NotNull(message = "No command ids entered. Unable to update commands.") List<String> commandIds
    ) throws GenieException;

    /**
     * Remove all commands from the cluster.
     *
     * @param id The id of the cluster to remove the commands from. Not
     *           null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeAllCommandsForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove commands.") String id
    ) throws GenieException;

    /**
     * Remove a command from the cluster.
     *
     * @param id    The id of the cluster to remove the command from. Not
     *              null/empty/blank.
     * @param cmdId The id of the command to remove. Not null/empty/blank.
     * @throws GenieException if there is an error
     */
    void removeCommandForCluster(
        @NotBlank(message = "No cluster id entered. Unable to remove command.") String id,
        @NotBlank(message = "No command id entered. Unable to remove command.") String cmdId
    ) throws GenieException;

    /**
     * Delete all clusters that are in one of the given states, aren't attached to any jobs and were created before
     * the given time.
     *
     * @param deleteStatuses The set of {@link ClusterStatus} a cluster must be in to be considered for deletion.
     * @param clusterCreatedThreshold The instant in time before which a cluster must have been created to be
     *                                considered for deletion. Exclusive.
     * @return The number of clusters deleted
     */
    long deleteUnusedClusters(Set<ClusterStatus> deleteStatuses, Instant clusterCreatedThreshold);

    /**
     * Find all the {@link Cluster}'s that match the given {@link Criterion}.
     *
     * @param criterion        The {@link Criterion} supplied that each cluster needs to completely match to be returned
     * @param addDefaultStatus {@literal true} if the a default status should be added to the supplied
     *                         {@link Criterion} if the supplied criterion doesn't already have a status
     * @return All the {@link Cluster}'s which matched the {@link Criterion}
     */
    Set<Cluster> findClustersMatchingCriterion(@Valid Criterion criterion, boolean addDefaultStatus);
}
