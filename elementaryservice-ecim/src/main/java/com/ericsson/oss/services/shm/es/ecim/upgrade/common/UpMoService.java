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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.NodeAttributesReaderException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedAttributeException;
import com.ericsson.oss.services.shm.common.exception.UpgradePackageMoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants.SwMConstants;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.SoftwarePackageNameNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivateState;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActivityAllowed;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.EcimSwMConstants;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SoftwarePackagePoNotFound;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMHandler;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.SwMVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.software.ecim.api.UpgradePackageState;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * This class has common methods that are related to operations on upgrade package MO.
 * 
 * @author xmanush, xvishsr
 */
@Stateless
public class UpMoService {

    private static final Logger logger = LoggerFactory.getLogger(UpMoService.class);

    @Inject
    private SwMVersionHandlersProviderFactory swMprovidersFactory;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    protected JobPropertyUtils jobPropertyUtils;

    @Inject
    private SmrsFileStoreService smrsServiceUtil;

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    /**
     * This method checks whether the current ECIM Upgrade activity is allowed for execution based on the upgrade package state.
     * 
     * @param activityName
     *            , environmentAttributes
     * @return boolean
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     * @throws NodeAttributesReaderException
     * @throws UpgradePackageMoNotFoundException
     */
    public ActivityAllowed isActivityAllowed(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, MoActionAbortRetryException, NodeAttributesReaderException, UpgradePackageMoNotFoundException {

        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);

        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();

        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.isActivityAllowed(activityName, filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    /**
     * This method provides implementation to execute MO action for ECIM Upgrade activities. The return type of the method will be the type passed along with method call as returnType parameter.
     * 
     * @param ecimUpgradeInfo
     *            , activityName, returnType
     * @return <T>
     * @throws ArgumentBuilderException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     */
    public ActionResult executeMoAction(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {
        logger.debug("Entered into UpMoService:executeMoAction with activityName : {} ", activityName);
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(UpgradeActivityConstants.UP_PO_FILE_PATH, ecimUpgradeInfo.getUpgradePackageFilePath());
        if (ecimUpgradeInfo.getUpgradeType() != null) {
            attributes.put(SwMConstants.UP_MO_UPGRADE_TYPE, ecimUpgradeInfo.getUpgradeType());
        }
        return softwareManagementHandler.executeMoAction(activityName, attributes, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    public ActionResult executeCancelAction(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException, MoNotFoundException, ArgumentBuilderException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound {

        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.executeCancelAction(filePath, upMoFdnFromNeJobProperty, nodeName, activityName, networkElementData);
    }

    public AsyncActionProgress getAsyncActionProgress(final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {

        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();

        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.getAsyncActionProgress(ecimUpgradeInfo.getActionTriggered(), filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    /**
     * This method returns the AsyncActionProgress ENUM containing the attributes having details of current activity in progress.
     * 
     * @param nodeName
     *            , activityName, modifiedAttributes
     * @return AsyncActionProgress
     */
    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes) throws UnsupportedFragmentException {

        final SwMHandler softwareManagementHandler = swMprovidersFactory.getSoftwareManagementHandler(getFragmentVersion(nodeName));
        return softwareManagementHandler.getValidAsyncActionProgress(modifiedAttributes);
    }

    /**
     * This method returns the FDN of Upgrade package Managed Object on which MO action has been triggered .
     * 
     * @param activityName
     *            , modifiedAttributes
     * @return String
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public String getNotifiableMoFdn(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws MoNotFoundException, UnsupportedFragmentException, SoftwarePackagePoNotFound,
            ArgumentBuilderException {
        logger.debug("Entered into UpMoService:getNotifiableMoFdn with node : {} and activityName : {}", ecimUpgradeInfo.getNeJobStaticData().getNodeName(), activityName);
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.getNotifiableMoFdn(activityName, filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    /**
     * This method updates the Upgrade Package MO attributes.
     * 
     * @param changedAttributes
     *            ,FDN
     * @return void
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     */
    public void updateMOAttributes(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> changedAttributes) throws UnsupportedFragmentException, MoNotFoundException,
            SoftwarePackageNameNotFound, SoftwarePackagePoNotFound, ArgumentBuilderException {

        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();

        updateUpMoAttributes(changedAttributes, filePath, upMoFdnFromNeJobProperty, nodeName);

    }

    private void updateUpMoAttributes(final Map<String, Object> changedAttributes, final String filePath, final String upMoFdnFromNeJobProperty, final String nodeName) {

        SwMHandler softwareManagementHandler = null;
        try {
            final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
            softwareManagementHandler = getSoftwareManagementHandler(nodeName);
            softwareManagementHandler.updateMOAttributes(changedAttributes, filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            logger.error("UnsupportedFragmentException occured while getting softwareManagementHandler {}", unsupportedFragmentException);
        } catch (MoNotFoundException | SoftwarePackagePoNotFound | ArgumentBuilderException e) {
            logger.error("Exception occured while updating UpMoAttributes {}", e);
        }
    }

    /**
     * This method checks whether the current ECIM Upgrade activity is completed based on the upgrade package state.
     * 
     * @param activityName
     *            , environmentAttributes
     * @return boolean
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     * @throws NodeAttributesReaderException
     */
    public boolean isActivityCompleted(final String activityName, final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.isActivityCompleted(activityName, filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    /**
     * This method checks whether the current ECIM Upgrade activity is completed based on the upgrade package state.
     * 
     * @param activityName
     *            , environmentAttributes
     * @return boolean
     * @throws UnsupportedFragmentException
     * @throws MoNotFoundException
     * @throws SoftwarePackageNameNotFound
     * @throws SoftwarePackagePoNotFound
     * @throws ArgumentBuilderException
     * @throws NodeAttributesReaderException
     */
    public UpgradePackageState getUpgradePackageState(final EcimUpgradeInfo ecimUpgradeInfo) throws UnsupportedFragmentException, MoNotFoundException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException {
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.getUpgradePkgState(filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    /**
     * This method obtains the software management handler based on the fragment type.
     * 
     * @param ecimUpgradeInfo
     * @return SwMHandler
     * @throws UnsupportedFragmentException
     */
    private SwMHandler getSoftwareManagementHandler(final String nodeName) throws UnsupportedFragmentException {

        final SwMHandler softwareManagementHandler = swMprovidersFactory.getSoftwareManagementHandler(getFragmentVersion(nodeName));
        return softwareManagementHandler;
    }

    @SuppressWarnings("unchecked")
    private String getUpMoFdnFromNeJobProperty(final Map<String, Object> neJobAttributes) {

        String upMoFdn = null;

        final List<Map<String, Object>> neJobPropertyList = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (neJobPropertyList != null) {
            for (final Map<String, Object> jobProperty : neJobPropertyList) {
                if (UpgradeActivityConstants.UP_FDN.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    upMoFdn = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    break;
                }
            }
        }
        return upMoFdn;
    }

    @SuppressWarnings("unchecked")
    public String getSwPkgName(final EcimUpgradeInfo ecimUpgradeInfo) throws SoftwarePackageNameNotFound, MoNotFoundException {
        String swPkgName = null;
        final NEJobStaticData neJobStaticData = ecimUpgradeInfo.getNeJobStaticData();
        final Map<String, Object> mainJobAttributes = ecimUpgradeInfo.getJobEnvironment().getMainJobAttributes(neJobStaticData.getMainJobId());
        final String neType = networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName());

        final Map<String, Object> jobConfiguration = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        final Map<String, String> softwarePackageNameMap = jobPropertyUtils.getPropertyValue(Arrays.asList(UpgradeActivityConstants.SWP_NAME), jobConfiguration, ecimUpgradeInfo.getNeJobStaticData()
                .getNodeName(), neType, neJobStaticData.getPlatformType());

        swPkgName = softwarePackageNameMap.get(UpgradeActivityConstants.SWP_NAME);

        logger.debug("Fetched SW Package Name for activity {} : {}", ecimUpgradeInfo.getActivityJobId(), swPkgName);

        if (swPkgName == null) {

            throw new SoftwarePackageNameNotFound("Software Package name not found in NE Job property list with Ne Job Id " + ecimUpgradeInfo.getNeJobStaticData().getNeJobId() + " for activity "
                    + ecimUpgradeInfo.getActivityJobId());
        }

        return swPkgName;
    }

    public boolean isUpgradePackageMoExists(final EcimUpgradeInfo ecimUpgradeInfo) {

        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        NetworkElementData networkElementData;
        try {
            networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        } catch (MoNotFoundException ex) {
            logger.error("Exception occurred while fetching networkElement information : {}. Exception is: ", nodeName, ex);
            return false;
        }
        SwMHandler softwareManagementHandler = null;
        try {
            softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        } catch (final UnsupportedFragmentException unsupportedFragmentException) {
            logger.error("Exception occurred while fetching node fragment version for the node, Exception is:", nodeName, unsupportedFragmentException);
            return false;
        }

        return softwareManagementHandler.isUpgradePackageMoExists(filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);

    }

    /**
     * Based on the data from <code>UpgradePackageData</code> verifies that whether the last triggered action i,e VERIFY is successful or not. And also updates the job logs to the Database.
     * 
     * @param activityJobId
     * @param upData
     * @return
     */
    public Map<String, Object> isActionCompleted(final AsyncActionProgress reportProgress) {
        JobResult jobResult = JobResult.FAILED;
        boolean isActionCompleted = false;

        String logmessage = null;
        // below if statements are needed to identify the proper logmessage, but
        // the result will be similar in all cases
        if (reportProgress.getState() == ActionStateType.FINISHED) {
            if (reportProgress.getResult() == ActionResultType.SUCCESS) {
                jobResult = JobResult.SUCCESS;
                logmessage = String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, reportProgress.getActionName());
                isActionCompleted = true;
            } else if (reportProgress.getResult() == ActionResultType.FAILURE) {
                jobResult = JobResult.FAILED;
                logmessage = String.format(JobLogConstants.ACTIVITY_FAILED, reportProgress.getActionName());
                isActionCompleted = true;
            }
        } else if (reportProgress.getState() == ActionStateType.CANCELLED) {
            jobResult = JobResult.CANCELLED;
            logmessage = String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, reportProgress.getActionName());
            isActionCompleted = true;
        }

        final Map<String, Object> actionStatusMap = new HashMap<String, Object>();
        actionStatusMap.put(ActivityConstants.JOB_RESULT, jobResult);
        actionStatusMap.put(EcimSwMConstants.ACTION_STATUS, isActionCompleted);
        actionStatusMap.put(ActivityConstants.JOB_LOG_MESSAGE, logmessage);
        return actionStatusMap;
    }

    public boolean isCreateActionTriggered(final EcimUpgradeInfo ecimUpgradeInfo, final String activityName) throws UnsupportedFragmentException {
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(ecimUpgradeInfo.getNeJobStaticData().getNodeName());

        return softwareManagementHandler.isCreateActionTriggered(activityName);
    }

    public boolean isValidAsyncActionProgress(final EcimUpgradeInfo ecimUpgradeInfo, final AsyncActionProgress asyncActionProgress, final String activityName) throws UnsupportedFragmentException {
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(ecimUpgradeInfo.getNeJobStaticData().getNodeName());

        return softwareManagementHandler.isValidAsyncActionProgress(activityName, asyncActionProgress);
    }

    public int getActivationSteps(final EcimUpgradeInfo ecimUpgradeInfo, final Map<String, Object> changedAttributes) throws UnsupportedFragmentException, SoftwarePackageNameNotFound,
            SoftwarePackagePoNotFound, MoNotFoundException, ArgumentBuilderException, NodeAttributesReaderException {
        logger.debug("Entered into getActivationSteps() with attributes {}", changedAttributes);
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.getActivationSteps(changedAttributes, filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    public Map<String, Object> getUpgradePackageUri(final EcimUpgradeInfo ecimUpgradeInfo) throws SoftwarePackageNameNotFound, UnsupportedFragmentException, MoNotFoundException,
            SoftwarePackagePoNotFound, ArgumentBuilderException, NodeAttributesReaderException, UnsupportedAttributeException {
        final Map<String, Object> neJobAttributes = ecimUpgradeInfo.getJobEnvironment().getNeJobAttributes(ecimUpgradeInfo.getNeJobStaticData().getNeJobId());
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();

        final String upMoFdnFromNeJobProperty = getUpMoFdnFromNeJobProperty(neJobAttributes);
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        logger.info("UpMoService class - getUpgradePackageUri sending attributes: filePath:{} ,upMoFdnFromNeJobProperty:{}, nodeName:{}", filePath, upMoFdnFromNeJobProperty, nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.getUpgradePackageFileServerDetails(filePath, upMoFdnFromNeJobProperty, nodeName, networkElementData);
    }

    public Map<String, Object> buildUpgradePackageUri(final EcimUpgradeInfo ecimUpgradeInfo) throws ArgumentBuilderException, MoNotFoundException, UnsupportedFragmentException {

        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        final String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return softwareManagementHandler.prepareActionArguments(nodeName, filePath, networkElementData);
    }

    /**
     * This method returns the ActivateState ENUM containing the attributes having details of current UpgradePackage State.
     * 
     * @param nodeName
     *            , activityName, modifiedAttributes
     * @return ActivateState
     */
    public ActivateState getUpgradePackageState(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes) throws UnsupportedFragmentException {

        final SwMHandler softwareManagementHandler = swMprovidersFactory.getSoftwareManagementHandler(getFragmentVersion(nodeName));
        return softwareManagementHandler.getUpgradePackageState(modifiedAttributes);
    }

    private String getFragmentVersion(final String nodeName) {
        NetworkElementData networkElementData = null;
        try {
            networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
        } catch (final Exception ex) {
            logger.warn("Failed to get Reference MIM Version of the node :{}. Reason is: ", nodeName, ex);
            return "";
        }
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity(), FragmentType.ECIM_SWM_TYPE.getFragmentName());
        return ossModelInfo == null ? "" : ossModelInfo.getReferenceMIMVersion();
    }

    public String getUriFromUpgradePackageFdn(final String nodeName, final String upgradePackageMOFdn) throws UnsupportedFragmentException {
        final SwMHandler softwareManagementHandler = getSoftwareManagementHandler(nodeName);
        return softwareManagementHandler.getUriFromUpgradePackageFdn(upgradePackageMOFdn);
    }

    public String getFilePath(final EcimUpgradeInfo ecimUpgradeInfo) throws MoNotFoundException, ArgumentBuilderException {
        String filePath = ecimUpgradeInfo.getUpgradePackageFilePath();
        final String nodeName = ecimUpgradeInfo.getNeJobStaticData().getNodeName();
        logger.info("For node {}, retrieved filePath from EcimUpgradeInfo {}", nodeName, filePath);

        final String neType = networkElementRetrievalBean.getNeType(nodeName);

        final SmrsAccountInfo smrsDetails = retrieveSmrsDetails(neType, nodeName);
        filePath = filePath.substring(smrsDetails.getSmrsRootDirectory().length());
        logger.info("For node {}, relative filePath for comparison during CREATE notification {}", nodeName, filePath);
        return filePath;
    }

    private SmrsAccountInfo retrieveSmrsDetails(final String neType, final String nodeName) throws ArgumentBuilderException {
        SmrsAccountInfo smrsDetails = null;
        try {
            smrsDetails = smrsServiceUtil.getSmrsDetails(SmrsServiceConstants.SOFTWARE_ACCOUNT, neType, nodeName);
        } catch (final Exception exception) {
            logger.error("Exception occurred while fetching smrs details for the node: {}. Exception is:", nodeName, exception);
            throw new ArgumentBuilderException(exception.getMessage());

        }
        return smrsDetails;
    }

    public Map<String, String> getActiveSoftwareDetailsFromNode(final String nodeName) throws UnsupportedFragmentException, MoNotFoundException {
        final SwMHandler softwareManagementHandler = swMprovidersFactory.getSoftwareManagementHandler(getFragmentVersion(nodeName));
        return softwareManagementHandler.getActiveSoftwareDetailsFromNode(nodeName);
    }

}
