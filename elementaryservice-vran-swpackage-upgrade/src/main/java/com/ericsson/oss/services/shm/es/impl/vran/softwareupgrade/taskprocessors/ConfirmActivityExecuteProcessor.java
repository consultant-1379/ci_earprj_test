/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.taskprocessors;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.exception.NoVnfIdFoundException;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.NeJobPropertiesPersistenceProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider.VnfInformationProvider;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.tasks.ExecuteTask;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobLogMessageTemplate;

public class ConfirmActivityExecuteProcessor extends ExecuteTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmActivityExecuteProcessor.class);
    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private NeJobPropertiesPersistenceProvider neJobPropertiesPersistenceProvider;

    @Inject
    private VnfInformationProvider vnfInformationProvider;

    @Override
    public String getActivityToBeTriggered() {
        return ActivityConstants.CONFIRM;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addVnfNeJobProperties(final long activityJobId) {
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);

        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);

        if (!isFullUpgradeJob(neJobProperties)) {
            neJobPropertiesPersistenceProvider.persistVnfInformation(activityJobId);
            upgradePackageContext = buildUpgradeJobContext(activityJobId);
            final String toVnfId = upgradePackageContext.getVnfId();
            final String fromVnfId = vnfInformationProvider.getVnfId(activityJobId, toVnfId, VranJobConstants.TO_VNF_ID, upgradePackageContext.getNodeName());
            upgradePackageContext.setVnfId(fromVnfId);
            LOGGER.info("ActivityJob ID - [{}] : Retrieved fromVnfId: {} for toVnfId: {}", activityJobId, fromVnfId, toVnfId);
            if (isFromVnfIdValid(fromVnfId)) {
                neJobPropertiesPersistenceProvider.persistFromAndToVnfIds(activityJobId, upgradePackageContext.getVnfId(), toVnfId, upgradePackageContext.getJobEnvironment());
            } else {
                throw new NoVnfIdFoundException(String.format(VranJobLogMessageTemplate.NO_FROM_VNFID, toVnfId, upgradePackageContext.getNodeName()));
            }
        }

    }

    private boolean isFullUpgradeJob(final List<Map<String, Object>> neJobProperties) {
        return neJobProperties != null;
    }

    private boolean isFromVnfIdValid(final String fromVnfId) {

        return fromVnfId != null && !fromVnfId.isEmpty();
    }

}
