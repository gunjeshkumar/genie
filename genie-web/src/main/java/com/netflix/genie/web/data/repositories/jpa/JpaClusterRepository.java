/*
 * Copyright 2015 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.genie.web.data.repositories.jpa;

import com.netflix.genie.web.data.entities.ClusterEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Set;

/**
 * Cluster repository.
 *
 * @author tgianos
 */
public interface JpaClusterRepository extends JpaBaseRepository<ClusterEntity>, CriteriaResolutionRepository {

    /**
     * The SQL to find all clusters that aren't attached to any jobs still in the database and were created before
     * a certain point in time.
     */
    String FIND_UNUSED_CLUSTERS_SQL =
        "SELECT id"
            + " FROM clusters"
            + " WHERE status IN (:unusedStatuses)"
            + " AND created < :clusterCreatedThreshold"
            + " AND id NOT IN (SELECT DISTINCT(cluster_id) FROM jobs WHERE cluster_id IS NOT NULL)";

    /**
     * Find all the clusters that aren't attached to any jobs in the database, were created before the given time
     * and have one of the given statuses.
     *
     * @param unusedStatuses          The set of statuses a cluster must have to be considered unused
     * @param clusterCreatedThreshold The instant in time which a cluster must have been created before to be considered
     *                                unused. Exclusive.
     * @return The ids of the clusters that are considered unused
     */
    @Query(value = FIND_UNUSED_CLUSTERS_SQL, nativeQuery = true)
    // TODO: Could use different lock mode
    Set<Long> findUnusedClusters(
        @Param("unusedStatuses") Set<String> unusedStatuses,
        @Param("clusterCreatedThreshold") Instant clusterCreatedThreshold
    );
}
