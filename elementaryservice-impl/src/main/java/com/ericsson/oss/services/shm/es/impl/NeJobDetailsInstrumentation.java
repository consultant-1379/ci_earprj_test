/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;

/**
 * This class facilitates the functionality to record NE Job result in DDP metrics based on NE types
 *
 * @author zchourv
 *
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class NeJobDetailsInstrumentation {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeJobDetailsInstrumentation.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    public void recordNeJobResultBasedOnNeType(final long mainJobId, final Map<String, Object> mainJobPoAttributes) {
        LOGGER.debug("Entering recordNeJobResultBasedOnNeType() with Main Job {} ", mainJobId);
        long templateJobId = 0;
        String jobName = "";
        String jobType = "";
        List<Map<String, Object>> activities = new ArrayList<>();
        try {
            LOGGER.debug("Main job attributes from DB : {}", mainJobPoAttributes);
            if (MapUtils.isNotEmpty(mainJobPoAttributes)) {
                templateJobId = (long) mainJobPoAttributes.get(ShmConstants.JOBTEMPLATEID);
            } else {
                LOGGER.error("Main Job PO attributes for MainJobPO: {} are empty. Skipping NE job details instrumentation.", mainJobId);
                return;
            }
            if (templateJobId != 0) {
                final Map<String, Object> jobTemplateAttributes = jobConfigurationServiceRetryProxy.getPOAttributes(templateJobId);
                LOGGER.debug("Job template attributes from DB: {} with template Id: {}", jobTemplateAttributes, templateJobId);
                if (MapUtils.isNotEmpty(jobTemplateAttributes)) {
                    jobName = (String) jobTemplateAttributes.get(ShmConstants.NAME);
                    jobType = (String) jobTemplateAttributes.get(ShmConstants.JOB_TYPE);
                    final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) jobTemplateAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
                    activities = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.JOB_ACTIVITIES);
                } else {
                    LOGGER.error("Job template attributes for templateJobId: {} are null. Skipping NE job details instrumentation.", templateJobId);
                    return;
                }
            } else {
                LOGGER.error("Invalid job template Id. templateJobId: {}. Skipping NE job details instrumentation.", templateJobId);
                return;
            }
            final Map<String, String> neBasedActivityMap = buildActivitiesMap(activities);
            final List<Object[]> neJobList = getNeJobsDataList(mainJobId);
            final Map<String, Map<String, Object>> nodeTypeResultFinalMap = buildNeJobDataMap(neJobList, neBasedActivityMap);
            if (!nodeTypeResultFinalMap.isEmpty()) {
                for (final Map.Entry<String, Map<String, Object>> entry : nodeTypeResultFinalMap.entrySet()) {
                    final Map<String, Object> eventData = new HashMap<>();
                    eventData.put(ShmConstants.JOB_TYPE, jobType);
                    eventData.put(ShmConstants.JOBNAME, jobName);
                    eventData.put(ShmConstants.NETYPE, entry.getKey());
                    final Map<String, Object> neTypeEntries = entry.getValue();
                    for (final Map.Entry<String, Object> eachNeTypeEntry : neTypeEntries.entrySet()) {
                        eventData.put(eachNeTypeEntry.getKey(), eachNeTypeEntry.getValue());
                    }
                    systemRecorder.recordEventData(SHMEvents.MAIN_JOB_COMPLETED, eventData);
                }
            } else {
                LOGGER.warn("Failed to record the NE Job Details Instrumentation for JobName : {} and JobType : {}", jobName, jobType);
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception encountered during recordNeJobResultBasedOnNeType(): ", exception);
        }
    }

    private Map<String, String> buildActivitiesMap(final List<Map<String, Object>> activityList) {
        final Map<String, String> activityMap = new HashMap<>();

        for (final Map<String, Object> activityEntry : activityList) {
            String activities;
            if (activityMap.containsKey(activityEntry.get(ShmConstants.NETYPE))) {
                activities = activityMap.get(activityEntry.get(ShmConstants.NETYPE)).concat(",").concat((String) activityEntry.get(ShmConstants.NAME));
            } else {
                activities = (String) activityEntry.get(ShmConstants.NAME);
            }
            activityMap.put((String) activityEntry.get(ShmConstants.NETYPE), activities);
        }
        return activityMap;
    }

    private List<Object[]> getNeJobsDataList(final Long mainJobId) {
        LOGGER.debug("Fetching network element list from DB with main job {}", mainJobId);
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
        final DataBucket dataBucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor queryExecutor = dataBucket.getQueryExecutor();
        final Restriction restriction = query.getRestrictionBuilder().equalTo(ShmConstants.MAINJOBID, mainJobId);
        query.setRestriction(restriction);
        return queryExecutor.executeProjection(query, ProjectionBuilder.attribute(ShmConstants.NETYPE), ProjectionBuilder.attribute(ShmConstants.RESULT));
    }

    private Map<String, Map<String, Object>> buildNeJobDataMap(final List<Object[]> neList, final Map<String, String> neBasedActivityMap) {
        final Map<String, Map<String, Object>> nodeTypeResultFinalMap = new HashMap<>();
        for (final Object[] neRecord : neList) {
            final String neType = (String) neRecord[0];
            final String neResult = (String) neRecord[1];
            if (StringUtils.isNotEmpty(neType) && StringUtils.isNotEmpty(neResult)) {
                final Map<String, Object> neTypeTempMap = buildNETypeSpecificData(neType, neResult, nodeTypeResultFinalMap, neBasedActivityMap);
                nodeTypeResultFinalMap.put(neType, neTypeTempMap);
            }
        }
        return nodeTypeResultFinalMap;
    }

    private Map<String, Object> buildNETypeSpecificData(final String neType, final String neResult, final Map<String, Map<String, Object>> nodeTypeResultFinalMap,
            final Map<String, String> neBasedActivityMap) {
        Map<String, Object> neTypeTempMap;
        if (nodeTypeResultFinalMap.containsKey(neType)) {
            neTypeTempMap = nodeTypeResultFinalMap.get(neType);
        } else {
            neTypeTempMap = new HashMap<>();
            neTypeTempMap.put(ShmConstants.SUCCESS_COUNT, 0);
            neTypeTempMap.put(ShmConstants.FAILED_COUNT, 0);
            neTypeTempMap.put(ShmConstants.CANCELLED_COUNT, 0);
            neTypeTempMap.put(ShmConstants.SKIPPED_COUNT, 0);
            neTypeTempMap.put(ShmConstants.TOTAL_COUNT, 0);
        }
        neTypeTempMap.put(ShmConstants.ACTIVITIES, neBasedActivityMap.get(neType));

        updateResult(neResult, neTypeTempMap);
        neTypeTempMap.put(ShmConstants.TOTAL_COUNT, (Integer) neTypeTempMap.get(ShmConstants.TOTAL_COUNT) + 1);
        return neTypeTempMap;
    }

    private void updateResult(final String nodeResultVar, final Map<String, Object> neTypeTempMap) {
        final JobResult nodeResult = JobResult.valueOf(nodeResultVar);
        switch (nodeResult) {
        case SUCCESS:
            setResultCount(ShmConstants.SUCCESS_COUNT, neTypeTempMap);
            break;

        case FAILED:
            setResultCount(ShmConstants.FAILED_COUNT, neTypeTempMap);
            break;

        case SKIPPED:
            setResultCount(ShmConstants.SKIPPED_COUNT, neTypeTempMap);
            break;

        case CANCELLED:
            setResultCount(ShmConstants.CANCELLED_COUNT, neTypeTempMap);
            break;
        }
    }

    private void setResultCount(final String resultCountKey, final Map<String, Object> neTypeTempMap) {
        Integer count = (Integer) neTypeTempMap.get(resultCountKey);
        neTypeTempMap.put(resultCountKey, ++count);
    }
}