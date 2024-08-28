/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.resources;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.ManageBackupActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.ManagedBackupActivity;

@Path("/jobs")
@Traceable
public class ActivityDataFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityDataFacade.class);

    @Inject
    private JobActivitiesProvider jobActivitiesProvider;

    @Inject
    private ManagedBackupActivity managedBackupActivity;

    /**
     * @URl : "http://localhost:8080/oss/shm/rest/jobs/activityData"
     * @param jobActivitiesQueryList
     * @Json [ { "jobType": "backup", "neTypes": [ { "neType": "ERBS", "neFdns": [ "NetworkElement=node1", "NetworkElement=node2" ] } ] }, { "jobType": "backup", "neTypes": [ { "neType": "RadioNode",
     *       "neFdns": [ "NetworkElement=node3", "NetworkElement=node4" ] } ] } ]
     * 
     * @return List<JobActivitiesResponse>
     */
    @POST
    @Path("/activityData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobActivitiesResponse> getJobActivitInformation(final List<JobActivitiesQuery> jobActivitiesQueryList) {
        logRequest(jobActivitiesQueryList);
        final List<JobActivitiesResponse> jobActivitiesResponse = jobActivitiesProvider.getNeTypeActivities(jobActivitiesQueryList);
        logResponse(jobActivitiesResponse);
        return jobActivitiesResponse;
    }

    /**
     * @URl : "http://localhost:8080/oss/shm/rest/jobs/manageBackup"
     * @param jobActivitiesQueryList
     * @Json :"[{"neType" : "ERBS", "multipleBackups":true},{"neType" : "SGSN-MME ", " multipleBackups": false},{"neType" : "RadioNode", "multipleBackups ": false}]";
     * 
     * @return List<JobActivitiesResponse>
     */
    @POST
    @Path("/manageBackup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobActivitiesResponse> getManageBackpupActivityInformation(final List<ManageBackupActivitiesQuery> jobActivitiesQueryList) {
        logRequestManageBackup(jobActivitiesQueryList);
        final List<JobActivitiesResponse> jobActivitiesResponse = managedBackupActivity.getManageBackupNeTypeActivities(jobActivitiesQueryList);
        logResponse(jobActivitiesResponse);
        return jobActivitiesResponse;
    }

    private void logRequest(final List<JobActivitiesQuery> jobActivitiesQueryList) {
        final StringBuilder jobActivitiesQueryBuilder = new StringBuilder();
        if (jobActivitiesQueryList != null && (!jobActivitiesQueryList.isEmpty())) {
            for (final JobActivitiesQuery jobActivitiesQuery : jobActivitiesQueryList) {
                jobActivitiesQueryBuilder.append(jobActivitiesQuery).append(";");
            }
        }
        final String jobActivitiesQueryLog = jobActivitiesQueryBuilder.toString();
        LOGGER.info("In ActivityDataFacade, jobActivitiesQueryList : {}", jobActivitiesQueryLog);
    }

    private void logRequestManageBackup(final List<ManageBackupActivitiesQuery> jobActivitiesQueryList) {
        final StringBuilder jobActivitiesQueryBuilder = new StringBuilder();
        if (jobActivitiesQueryList != null && (!jobActivitiesQueryList.isEmpty())) {
            for (final ManageBackupActivitiesQuery jobActivitiesQuery : jobActivitiesQueryList) {
                jobActivitiesQueryBuilder.append(jobActivitiesQuery).append(";");
            }
        }
        LOGGER.info("In ActivityDataFacade, jobActivitiesQueryList for ManageBackUp: {}", jobActivitiesQueryBuilder.toString());
    }

    private void logResponse(final List<JobActivitiesResponse> jobActivitiesResponseList) {
        final StringBuilder jobActivitiesResponseBuilder = new StringBuilder();
        if (jobActivitiesResponseList != null && (!jobActivitiesResponseList.isEmpty())) {
            for (final JobActivitiesResponse jobActivitiesResponse : jobActivitiesResponseList) {
                jobActivitiesResponseBuilder.append(jobActivitiesResponse).append(";");
            }
        }
        final String jobActivitiesResponseLog = jobActivitiesResponseBuilder.toString();
        LOGGER.info("In ActivityDataFacade, jobActivitiesResponse : {}", jobActivitiesResponseLog);
    }

}
