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
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.exception.DuplicateEntityException;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class JobTemplatePersistenceService {

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobTemplatePersistenceService.class);
    private static final String TIME_CONSUMPTION_FOR_DPS_CALLS_DURING_JOB_CREATION = "Time consumed to fetch job templates by job name : %d milliseconds. Time consumed to fetch main jobs by job template id : %d milliseconds. Time consumed to delete job template, if no main job is associated with it : %d milliseconds. Time consumed to create job template : %d milliseconds. Total time consumed : %d milliseconds.";

    public long createJobTemplate(final Map<String, Object> jobTemplate, final String name) {
        PersistenceObject jobTemplatePO = null;
        final Date startDate = new Date();
        Date fetchingJobTemplatesByJobName = null;
        Date receivedJobTemplatesByJobName = null;
        Date fetchingMainJobsByJobTemplateId = null;
        Date receivedMainJobsByJobTemplateId = null;
        Date deletingJobTemplate = null;
        Date jobTemplateDeleted = null;
        Date creatingJobTemplate = null;
        Date jobTemplateCreated = null;
        long timeConsumedToFetchJobTemplatesByJobName = -1;
        long timeConsumedToFetchMainJobsByJobTemplateId = -1;
        long timeConsumedToDeleteJobTemplate = -1;
        long timeConsumedToCreateJobTemplate = -1;
        long totalTimeConsumed = -1;

        boolean isDuplicateJob = false;
        try {
            fetchingJobTemplatesByJobName = new Date();
            systemRecorder.recordEvent(SHMEvents.JOB_TEMPLATE_CREATION, EventLevel.COARSE, ShmConstants.SOURCE, "Fetching Job Template by job name", name);
            final List<PersistenceObject> jobTemplates = getJobTemplatesByJobName(name);
            receivedJobTemplatesByJobName = new Date();
            timeConsumedToFetchJobTemplatesByJobName = getTimeDuration(receivedJobTemplatesByJobName, fetchingJobTemplatesByJobName);
            if (jobTemplates != null && !jobTemplates.isEmpty()) {
                final long templateJobId = jobTemplates.get(0).getPoId();
                fetchingMainJobsByJobTemplateId = new Date();
                final List<PersistenceObject> mainJobPos = getMainJobPosByTemplateJobId(templateJobId);
                receivedMainJobsByJobTemplateId = new Date();
                timeConsumedToFetchMainJobsByJobTemplateId = getTimeDuration(receivedMainJobsByJobTemplateId, fetchingMainJobsByJobTemplateId);
                deletingJobTemplate = new Date();
                isDuplicateJob = isJobExists(templateJobId, mainJobPos);
                jobTemplateDeleted = new Date();
                timeConsumedToDeleteJobTemplate = getTimeDuration(jobTemplateDeleted, deletingJobTemplate);
            }
        } catch (final Exception ex) {
            LOGGER.error("Caught exception while retrieving Job Template : {}", ex);
        }
        if (isDuplicateJob) {
            final String message = "Creation of job \"" + name + "\" failed, as there is another job created with the same name.";
            LOGGER.error(message);
            totalTimeConsumed = getTimeDuration(new Date(), startDate);
            systemRecorder.recordEvent(SHMEvents.JOB_TEMPLATE_CREATION, EventLevel.COARSE, ShmConstants.SOURCE, "Duplicate Job Template Creation", String.format(
                    TIME_CONSUMPTION_FOR_DPS_CALLS_DURING_JOB_CREATION, timeConsumedToFetchJobTemplatesByJobName, timeConsumedToFetchMainJobsByJobTemplateId, timeConsumedToDeleteJobTemplate,
                    timeConsumedToCreateJobTemplate, totalTimeConsumed));
            throw new DuplicateEntityException(message);
        }
        //only for dps writer exception
        try {
            creatingJobTemplate = new Date();
            systemRecorder.recordEvent(SHMEvents.JOB_TEMPLATE_CREATION, EventLevel.COARSE, ShmConstants.SOURCE, "Creating Job Template PO", name);
            jobTemplatePO = dpsWriter.createPO(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, ShmConstants.VERSION, jobTemplate);
            jobTemplateCreated = new Date();
            timeConsumedToCreateJobTemplate = getTimeDuration(jobTemplateCreated, creatingJobTemplate);
            totalTimeConsumed = getTimeDuration(jobTemplateCreated, startDate);
            systemRecorder.recordEvent(SHMEvents.JOB_TEMPLATE_CREATION, EventLevel.COARSE, ShmConstants.SOURCE, "Job Template Creation", String.format(
                    TIME_CONSUMPTION_FOR_DPS_CALLS_DURING_JOB_CREATION, timeConsumedToFetchJobTemplatesByJobName, timeConsumedToFetchMainJobsByJobTemplateId, timeConsumedToDeleteJobTemplate,
                    timeConsumedToCreateJobTemplate, totalTimeConsumed));
        } catch (final Exception ex) {
            LOGGER.error("Caught exception while creation of job in dps : {}", ex);
            jobTemplatePO = null;
            totalTimeConsumed = getTimeDuration(new Date(), startDate);
            systemRecorder.recordEvent(SHMEvents.JOB_TEMPLATE_CREATION, EventLevel.COARSE, ShmConstants.SOURCE, "Job Template Creation Failed", String.format(
                    TIME_CONSUMPTION_FOR_DPS_CALLS_DURING_JOB_CREATION, timeConsumedToFetchJobTemplatesByJobName, timeConsumedToFetchMainJobsByJobTemplateId, timeConsumedToDeleteJobTemplate,
                    timeConsumedToCreateJobTemplate, totalTimeConsumed));
        }

        if (jobTemplatePO != null) {
            LOGGER.info("The job template persistence object is created : {}", jobTemplatePO);
            return jobTemplatePO.getPoId();
        } else {
            return 0;
        }
    }

    private long getTimeDuration(final Date endTime, final Date startTime) {
        return endTime.getTime() - startTime.getTime();
    }

    private List<PersistenceObject> getJobTemplatesByJobName(final String jobName) {
        final Map<String, Object> attributesMap = new HashMap<>();
        attributesMap.put(ShmConstants.NAME, jobName);
        return dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, attributesMap);
    }

    private List<PersistenceObject> getMainJobPosByTemplateJobId(final long templateJobId) {
        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(ShmConstants.JOB_TEMPLATE_ID, templateJobId);
        return dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictions);
    }

    private boolean isJobExists(final long templateJobId, final List<PersistenceObject> mainJobPos) {
        if (mainJobPos == null || mainJobPos.isEmpty()) {
            dpsWriter.deletePoByPoId(templateJobId);
        } else {
            return true;
        }
        return false;
    }
}
