/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.axe.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.jobservice.axe.OpsInputData;
import com.ericsson.oss.services.shm.jobservice.axe.OpsResponseData;
import com.ericsson.oss.services.shm.jobservice.common.NEJobInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesPlatformData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;

/**
 * This class implements ShmAxeService and provide responses accordingly
 * 
 * @author Team Royals
 *
 */
@Stateless
public class ShmAxeServiceImpl implements ShmAxeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmAxeServiceImpl.class);

    @Inject
    private SHMJobService shmJobService;
    @Inject
    private AxeNeTypesValidator axeNeTypesValidator;
    @Inject
    private OpsDetailsProvider opsDetailsProvider;
    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Override
    public OpsResponseData getSessionIdAndClusterId(final OpsInputData opsInputData) {
        final OpsResponseData opsResponseData = new OpsResponseData();
        final Map<String, List<NEJobInfo>> neTypesToNeJobsMap = new HashMap<>(opsInputData.getNeTypeToNeJobs());

        final NeTypesInfo neTypesInfo = new NeTypesInfo();
        neTypesInfo.setJobType(opsInputData.getJobType());
        neTypesInfo.setNeTypes(opsInputData.getNeTypeToNeJobs().keySet());
        final NeTypesPlatformData neTypesPlatformData = shmJobService.getNeTypesPlatforms(neTypesInfo);
        final Set<String> supportedNeTypes = axeNeTypesValidator.getAxeNeTypes(neTypesPlatformData.getSupportedNeTypesByPlatforms());
        LOGGER.debug("Supported neTypes: {}", supportedNeTypes);
        opsResponseData.setOpsSessionAndClusterIdInfo(opsDetailsProvider.getSessionIdAndClusterId(supportedNeTypes, opsInputData.getNeTypeToNeJobs()));
        opsResponseData.setUnSupportedNodes(axeNeTypesValidator.getUnSupportedNodes(supportedNeTypes, neTypesToNeJobsMap));
        try {
            final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(opsInputData.getMainJobId());
            LOGGER.debug("jobStaticData :{}", jobStaticData);
            if (null != jobStaticData && jobStaticData.getOwner().equals(opsInputData.getUser())) {
                opsResponseData.setHasAccessToOPSGUI(true);
            } else {
                opsResponseData.setHasAccessToOPSGUI(false);
            }
        } catch (JobDataNotFoundException e) {
            LOGGER.error("Unable to retrieve the user data to validate");
        }
        return opsResponseData;

    }
}
