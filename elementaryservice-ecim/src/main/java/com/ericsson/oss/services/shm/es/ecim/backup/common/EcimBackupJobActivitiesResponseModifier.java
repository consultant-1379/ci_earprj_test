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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.DefaultDomainTypeNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.DefaultDomainTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.BrmBackupManagerMoData;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimBackupItem;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.ActivityParams;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.job.activity.NodeparamType;
import com.ericsson.oss.services.shm.job.impl.ManageBackupActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * To Retrieve the Node information and updates it to the Response.
 * 
 * @author xrajeke
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.ECIM, jobType = com.ericsson.oss.services.shm.job.activity.JobType.BACKUP)
public class EcimBackupJobActivitiesResponseModifier implements SHMJobActivitiesResponseModifier, ManageBackupActivitiesResponseModifier {

    private static final Logger logger = LoggerFactory.getLogger(EcimBackupJobActivitiesResponseModifier.class);

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    @Inject
    private DefaultDomainTypeProviderImpl defaultDomainTypeProviderImpl;

    /**
     * To Retrieve the Domain and Backup type from Node and updates it to the Response if they found.
     * 
     * @param jobActivitiesQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getUpdatedJobActivities(final NeInfoQuery neInfoQuery, final JobActivitiesResponse jobActivitiesResponse) {
        logger.info("Json Modification will start.");
        final Map<String, List<EcimBackupItem>> allNodesBackupItems = ecimBackupUtils.getNodeBackupActivityItems(neInfoQuery);

        final String neType = neInfoQuery.getNeType();
        if (neType != null && neType.length() != 0) {
            logger.debug("neType :: {}", neType);
            String defaultDomainAndType = null;
            try {
                final BrmBackupManagerMoData defaultDomainTypeFromCapabilityModel = defaultDomainTypeProviderImpl.getDefaultDomainType(neType);
                defaultDomainAndType = defaultDomainTypeFromCapabilityModel.getBackupDomain() + ActivityConstants.SLASH + defaultDomainTypeFromCapabilityModel.getBackupType();
            } catch (DefaultDomainTypeNotFoundException ex) {
                logger.error("Unable to fetch Default Backup Domain and Type for the node type {} due to : {}", neType, ex.getMessage());
            }
            if (defaultDomainAndType != null && defaultDomainAndType.length() != 0) {
                setDefaultDomainTypeForNodesWithNoDomainType(jobActivitiesResponse, allNodesBackupItems, defaultDomainAndType);
            }
        } else {
            logger.info("neType is null. Cannot fetch default Domain and Type.");
        }

        final Set<String> intersectedItems = ecimBackupUtils.getCommonBackupActivityItems(allNodesBackupItems);
        logger.info("Intersection of backup Items operation completed, and now they are :: {}", intersectedItems);
        if (intersectedItems != null) {
            updateNodeParams(jobActivitiesResponse, intersectedItems);
        }
        return jobActivitiesResponse;
    }

    /**
     * Returns the activities needed for ManageBackup
     * 
     * @param jobActivitiesQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getManageBackupActivities(final JobActivitiesResponse jobActivitiesResponse, final Boolean multipleBackups) {
        logger.info("Json Modification will start.");
        for (final NeActivityInformation neActivityInformation : jobActivitiesResponse.getNeActivityInformation()) {
            for (final Iterator<Activity> activityIter = neActivityInformation.getActivity().listIterator(); activityIter.hasNext();) {
                if (ShmConstants.CREATE_BACKUP.equals(activityIter.next().getName())) {
                    activityIter.remove();
                }
            }
        }
        return jobActivitiesResponse;
    }

    /**
     * @param jobActivitiesResponseList
     * @param unionedItems
     */
    private void updateNodeParams(final JobActivitiesResponse jobActivitiesResponse, final Set<String> unionedItems) {
        for (final NeActivityInformation neActivityInformation : jobActivitiesResponse.getNeActivityInformation()) {
            for (final Activity activity : neActivityInformation.getActivity()) {
                for (final ActivityParams activityParamType : activity.getActivityParams()) {
                    for (final NodeparamType nodeparamType : activityParamType.getNodeparam()) {
                        logger.info("existing nodeParam items:{}", nodeparamType.getItem());
                        nodeparamType.getItem().addAll(unionedItems);
                        logger.info("After updating the nodeParam items from the node are :{}", nodeparamType.getItem());
                    }
                }

            }

        }
    }

    /**
     * Adds the default domain and type to the nodes which do not have a domain and type
     * 
     * @param jobActivitiesResponse
     * @param allNodesBackupItems
     * @param defaultDomainAndTypeString
     */
    private void setDefaultDomainTypeForNodesWithNoDomainType(final JobActivitiesResponse jobActivitiesResponse, final Map<String, List<EcimBackupItem>> allNodesBackupItems,
            final String defaultDomainAndTypeString) {
        if (allNodesBackupItems.size() == 0) {
            logger.info("No Domain activity information elements. Adding default domain type!");
            for (final NeActivityInformation neActivityInformation : jobActivitiesResponse.getNeActivityInformation()) {
                for (final Activity activity : neActivityInformation.getActivity()) {
                    for (final ActivityParams activityParamType : activity.getActivityParams()) {
                        for (final NodeparamType nodeparamType : activityParamType.getNodeparam()) {
                            logger.debug("existing nodeParam items:{}", nodeparamType.getItem());
                            //Setting the default domain and type
                            nodeparamType.getItem().add(defaultDomainAndTypeString);
                            logger.debug("After adding the default domain and type, node param items from the node are :{}", nodeparamType.getItem());
                        }
                    }
                }
            }
        }
    }

}
