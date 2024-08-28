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
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * This class has common methods that are related to Software Management fragments, required for all activities of ECIM Software Upgrade Use Case.
 * 
 * @author xvishsr
 * 
 */
@Profiled
@Traceable
@ApplicationScoped
public class EcimSwMUtils {

    private static final Logger logger = LoggerFactory.getLogger(EcimSwMUtils.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobPropertyUtils jobPropertyUtils;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private FragmentHandler fragmentHandler;

    /**
     * Creates a POJO having all the required fields which are needed during execution of ECIM Software Upgrade activity.
     * 
     * @param activityJobId
     * @return UpgradeEnvironment
     * @throws JobDataNotFoundException
     * @throws MoNotFoundException
     */
    @SuppressWarnings("unchecked")
    public EcimUpgradeInfo getEcimUpgradeInformation(final long activityJobId) throws JobDataNotFoundException, MoNotFoundException {
        final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        return getEcimUpgradePackageInfoByNeJobStaticData(activityJobId, neJobStaticData);
    }

    private EcimUpgradeInfo getEcimUpgradePackageInfoByNeJobStaticData(final long activityJobId, final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        String lastTriggeredAction = null;
        Boolean ignoreBreakPoints = true;
        String swPkgName = null;
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> activityJobAttributes = jobEnvironment.getActivityJobAttributes();
        final Map<String, Object> mainJobAttributes = jobEnvironment.getMainJobAttributes(neJobStaticData.getMainJobId());
        final String nodeName = neJobStaticData.getNodeName();
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(nodeName);
        final String neType = networkElementRetrievalBean.getNeType(nodeName);
        final String platform = neJobStaticData.getPlatformType();

        // To get the last triggered action value
        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (activityJobPropertyList != null) {
            for (final Map<String, Object> jobProperty : activityJobPropertyList) {
                if (EcimCommonConstants.ACTION_TRIGGERED.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    lastTriggeredAction = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    break;
                }
            }
        }

        // Retrieves the NE Type Job Properties
        final List<String> keys = new ArrayList<String>();
        keys.add(UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS);
        keys.add(UpgradeActivityConstants.SWP_NAME);
        keys.add(UpgradeActivityConstants.UPGRADE_TYPE);
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keys, jobConfigurationDetails, nodeName, neType, platform);
        if (keyValueMap.get(UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS) != null) {
            ignoreBreakPoints = Boolean.valueOf(keyValueMap.get(UpgradePackageMoConstants.UP_MO_IGNORE_BREAK_POINTS));
        } else {
            ignoreBreakPoints = true;
            logger.debug("For SGSN nodes step by step activation is not supported,so directly setting ignore break points as true");
        }
        logger.debug("Ignore break points ? : {}", ignoreBreakPoints);

        // To get the Upgrade Package File Path.
        swPkgName = String.valueOf(keyValueMap.get(UpgradeActivityConstants.SWP_NAME));
        final List<String> upgradePackageFilePath = remoteSoftwarePackageManager.getUpgradePackageDetails(swPkgName);
        final EcimUpgradeInfo ecimUpgradeInfo = new EcimUpgradeInfo();
        if (upgradePackageFilePath.size() > 0) {
            ecimUpgradeInfo.setUpgradePackageFilePath(upgradePackageFilePath.get(0));
            logger.debug("Software package Name : {} and filePath is : {}", swPkgName, upgradePackageFilePath.get(0));
        }
        //To get the UpgradeType
        final String upgradeType = keyValueMap.get(UpgradeActivityConstants.UPGRADE_TYPE);
        if (upgradeType != null && !upgradeType.isEmpty()) {
            ecimUpgradeInfo.setUpgradeType(upgradeType);
        }
        ecimUpgradeInfo.setActionTriggered(lastTriggeredAction);
        ecimUpgradeInfo.setIgnoreBreakPoints(ignoreBreakPoints);
        ecimUpgradeInfo.setNeJobStaticData(neJobStaticData);
        ecimUpgradeInfo.setActivityJobId(activityJobId);
        ecimUpgradeInfo.setJobEnvironment(jobEnvironment);
        logger.debug("Upgrade environment attributes for {}: {}", activityJobId, ecimUpgradeInfo);
        return ecimUpgradeInfo;
    }

    public void initializeActivityJobLogs(final String nodeName, final String activityName, final List<Map<String, Object>> jobLogList) {

        jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_INITIATED, activityName), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());

        final String treatAsInfo = activityUtils.isTreatAs(nodeName, FragmentType.ECIM_SWM_TYPE, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
        if (treatAsInfo != null) {
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, treatAsInfo, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
        }
        fragmentHandler.logNodeFragmentInfo(nodeName, FragmentType.ECIM_SWM_TYPE, jobLogList);
    }

    /**
     * Creates a POJO having all the required fields which are needed during execution of ECIM Software Upgrade activity.
     *
     * @param activityJobId
     * @param neJobStaticData
     * @return EcimUpgradeInfo
     * @throws MoNotFoundException
     */
    public EcimUpgradeInfo getEcimUpgradeInformation(final long activityJobId, final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        return getEcimUpgradePackageInfoByNeJobStaticData(activityJobId, neJobStaticData);
    }

}
