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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.impl.JobConfigurationServiceImpl;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * Axe specific implementation for updating job logs and progress percentage.In upgrade job as OPS script execution can be resumed after interruption SHM may receive notifications which has duplicates logs and less
 * progress percentage values than the current ones in DB.So this will not have restriction on persisting of duplicate logs and lower progress than current unlike other platforms
 * 
 * @author xaniama
 */
@Stateless
@PlatformAnnotation(name = PlatformTypeEnum.AXE)
public class AxeJobConfigurationServiceImpl  extends JobConfigurationServiceImpl{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AxeJobConfigurationServiceImpl.class);
    /**
     * @param activityProgressPercentage
     * @param validatedAttributes
     * @param activityJobAttr
     */
    @Override
    protected void prepareLatestActivityProgresstoPersistInDB(final Double activityProgressPercentage, final Map<String, Object> validatedAttributes, final Map<String, Object> activityJobAttr) {
        if (activityProgressPercentage != null && activityProgressPercentage >= 0.0) {
            if (activityProgressPercentage >= 100) {
                validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, 100.0);
            } else {
                validatedAttributes.put(ShmConstants.PROGRESSPERCENTAGE, activityProgressPercentage);
            }
            LOGGER.debug("validatedAttributes in persistLatestActivityProgressIntoDB {}",validatedAttributes);
        }
    }
    
    /**
     * @param jobLogList
     * @param validatedAttributes
     * @param activityJobAttr
     */
    @Override
    protected void prepareLatestJobLogsToPersistIntoDB(final List<Map<String, Object>> jobLogList, final Map<String, Object> validatedAttributes, final Map<String, Object> activityJobAttr) {
        if (jobLogList != null && !jobLogList.isEmpty() && !activityJobAttr.isEmpty()) {
                List<Map<String, Object>> activityJobLogList = new ArrayList();
                if (activityJobAttr.get(ActivityConstants.JOB_LOG) != null) {
                    activityJobLogList = (List<Map<String, Object>>) activityJobAttr.get(ActivityConstants.JOB_LOG);
                }
                addJobLog(jobLogList, activityJobLogList);
                if (!activityJobAttr.containsKey(ShmConstants.JOB_TEMPLATE_ID)) {
                    final String lastLogMessage = retrieveLastLogMessage(activityJobLogList);
                    validatedAttributes.put(ShmConstants.LAST_LOG_MESSAGE, lastLogMessage);
                }
                validatedAttributes.put(ActivityConstants.JOB_LOG, activityJobLogList);
                LOGGER.debug("validatedAttributes in persistLatestJobLogsIntoDB {}",validatedAttributes);
        }
        
    }
    
    private void addJobLog(final List<Map<String, Object>> jobLogList, final List<Map<String, Object>> activityJobLogList) {
        for (final Map<String, Object> jobLog : jobLogList) {
                activityJobLogList.add(jobLog);
        }
    }
    
}
