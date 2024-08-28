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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.EcimLicenseConstants;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMHandler;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LicenseResponse;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

@Stateless
public class LicenseMoService {

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private LMVersionHandlersProviderFactory lmVersionHandlersProviderFactory;

    @Inject
    private EcimCommonUtils ecimCommonUtils;

    @Inject
    private EcimLmUtils ecimLmUtils;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseMoService.class);

    private static final String INSTALL_LICENSE_UNSUPPORTED_NODE_MODEL = "Install License action can't be triggered on the node due to unsupported node model.";
    private static final String ASYNC_ACTION_PROGRESS_UNSUPPORTED_NODE_MODEL = "Async action progress is not available due to unsupported node model.";

    public String getNotifiableMoFdn(final NetworkElementData networkElement, final String activityName) throws UnsupportedFragmentException, MoNotFoundException {
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(String.format(JobLogConstants.MO_NOT_FOUND_UNSUPPORTED_NODE_MODEL, "Key File Management"));
        }
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final String keyFileMgmtMOFdn = lmHandler.getNotifiableMoFdn(networkElement, activityName, ossModelInfo);
        return keyFileMgmtMOFdn;
    }

    public short executeMoAction(final LicensePrecheckResponse licensePrecheckResponse, final NEJobStaticData neJobStaticData) throws MoNotFoundException, UnsupportedFragmentException,
            ArgumentBuilderException {
        final NetworkElementData networkElement = licensePrecheckResponse.getNetworkElement();
        final String nodeName = neJobStaticData.getNodeName();
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(INSTALL_LICENSE_UNSUPPORTED_NODE_MODEL);
        }
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final String licenseKeyFileName = licensePrecheckResponse.getEcimLicenseInfo().getLicenseKeyFilePath();
        final String keyFileMgmtMOFdn = licensePrecheckResponse.getEcimLicenseInfo().getLicenseMoFdn();
        return lmHandler.installLicense(keyFileMgmtMOFdn, networkElement.getNeType(), licenseKeyFileName, nodeName);
    }

    public AsyncActionProgress getActionProgressOfKeyFileMgmtMO(final NetworkElementData networkElement) throws UnsupportedFragmentException, MoNotFoundException {
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(ASYNC_ACTION_PROGRESS_UNSUPPORTED_NODE_MODEL);
        }
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final AsyncActionProgress actionProgress = lmHandler.getActionProgressOfKeyFileMgmtMO(networkElement, ossModelInfo);
        return actionProgress;
    }

    public boolean isLicenseKeyFileExistsInSMRS(final String licenseKeyFileName) throws MoNotFoundException {
        LOGGER.info("licenseKeyFilePath in  isLicenseKeyFileExistsInSMRS {}", licenseKeyFileName);
        final boolean fileExists = ecimLmUtils.isLicenseKeyFileExistsInSMRS(licenseKeyFileName);
        return fileExists;
    }

    public AsyncActionProgress getValidAsyncActionProgress(final String nodeName, final Map<String, AttributeChangeData> modifiedAttributes, final String activityName)
            throws UnsupportedFragmentException, MoNotFoundException {
        final AsyncActionProgress actionProgress = ecimCommonUtils.getValidAsyncActionProgress(activityName, modifiedAttributes);
        return actionProgress;
    }

    public boolean isLicensingPOExists(final String licenseKeyFileName) throws UnsupportedFragmentException, MoNotFoundException {
        final boolean isLicensePOsExists = ecimLmUtils.isLicensingPOExists(licenseKeyFileName);
        LOGGER.debug("isLicensePOsExists  {} ", isLicensePOsExists);
        return isLicensePOsExists;
    }

    public String getLicenseKeyFileName(final NEJobStaticData neJobStaticData, final NetworkElementData networkElement) {
        final Map<String, Object> mainJobProperties = jobConfigurationServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId());
        return ecimLmUtils.getLicenseKeyFilePath(neJobStaticData, networkElement, mainJobProperties);
    }

    public String getLicenseKeyFileNameFromFingerPrint(final String fingerprint) {

        return ecimLmUtils.getLicenseKeyFilePathFromFingerPrint(fingerprint);
    }

    public String getSequenceNumber(final String fingerprint) {

        return ecimLmUtils.getSequenceNumber(fingerprint);
    }

    public boolean getEmergencyUnlockActivationState(final NetworkElementData networkElement) throws UnsupportedFragmentException, MoNotFoundException {
        LmActivationState lmActivationState = null;
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(String.format(JobLogConstants.MO_NOT_FOUND_UNSUPPORTED_NODE_MODEL, "EmergencyUnlock"));
        }
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final ManagedObject EmergencyUnlockMo = lmHandler.getFailsafeEmergencyUnlockMO(networkElement, ossModelInfo);
        if (EmergencyUnlockMo != null) {
            final Map<String, Object> emergencyUnlockAttrs = EmergencyUnlockMo.getAllAttributes();
            LOGGER.debug("EmergencyUnlockMo attributes are {} ", emergencyUnlockAttrs);
            final String emergencyUnlockActivationState = (String) emergencyUnlockAttrs.get(EcimLicenseConstants.ACTIVATIONSTATE);
            LOGGER.debug("EmergencyUnlockMo lmActivationState are {} ", emergencyUnlockActivationState);
            lmActivationState = LmActivationState.getActivationState(emergencyUnlockActivationState);
        }
        final boolean lmActivationStateFlag = lmActivationState != LmActivationState.INACTIVE ? false : true;
        LOGGER.info("EmergencyUnlock lmActivationStateFlag is {} for nodeFdn {}", lmActivationStateFlag, networkElement.getNeFdn());
        return lmActivationStateFlag;
    }

    public String getProducTypeOfLKF(final NetworkElementData networkElement, final String licenseKeyFileName) throws MoNotFoundException {
        String lkfproductType = null;
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(CommonLicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseKeyFileName);
        final List<PersistenceObject> licensePOs = ecimLmUtils.getAttributesListOfLicensePOs(restrictionAttributes);
        if (licensePOs == null || licensePOs.isEmpty()) {
            LOGGER.error("The reason is:{}", "There is no data present in the database for LicensePO.");
        } else {
            lkfproductType = licensePOs.get(0).getAttribute(CommonLicensingActivityConstants.PRODUCTTYPE);
            LOGGER.debug("ProductType value is {}, for the node : {} ", lkfproductType, networkElement.getNeType());
        }
        return lkfproductType;
    }

    public String getFingerPrintFromNode(final NetworkElementData networkElementData) throws UnsupportedFragmentException, MoNotFoundException {
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(ASYNC_ACTION_PROGRESS_UNSUPPORTED_NODE_MODEL);
        }
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setOssModelIdentity(networkElementData.getOssModelIdentity());
        networkElement.setNodeRootFdn(networkElementData.getNodeRootFdn());
        networkElement.setNeType(networkElementData.getNeType());
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final LicenseResponse licenseResponse = lmHandler.getFingerprintForNetworkElement(networkElement);
        final Map<String, NetworkElement> supportedNodes = licenseResponse.getSupportedNodes();
        String fingerprint = null;
        final Set<String> fingerprints = supportedNodes.keySet();
        if (fingerprints != null && fingerprints.size() > 0) {
            fingerprint = (String) fingerprints.toArray()[0];
        }
        LOGGER.info("Retrieved Fingerprint from node is {}", fingerprint);
        return fingerprint;
    }

    public String getSequenceNumberFromNode(final NetworkElementData networkElementData) throws UnsupportedFragmentException, MoNotFoundException {
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName());
        if (ossModelInfo == null) {
            throw new MoNotFoundException(ASYNC_ACTION_PROGRESS_UNSUPPORTED_NODE_MODEL);
        }
        final NetworkElement networkElement = new NetworkElement();
        networkElement.setOssModelIdentity(networkElementData.getOssModelIdentity());
        networkElement.setNodeRootFdn(networkElementData.getNodeRootFdn());
        networkElement.setNeType(networkElementData.getNeType());
        final LMHandler lmHandler = lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion());
        final LicenseResponse licenseResponse = lmHandler.getSequenceNumberForNetworkElement(networkElement, ossModelInfo);
        final Map<String, NetworkElement> supportedNodes = licenseResponse.getSupportedNodes();
        String sequenceNumber = null;
        final Set<String> sequenceNumbers = supportedNodes.keySet();
        if (sequenceNumbers != null && !sequenceNumbers.isEmpty()) {
            sequenceNumber = (String) sequenceNumbers.toArray()[0];
        }
        LOGGER.info("Retrieved Sequence Number from node{} is {}", networkElementData.getNeType(), sequenceNumber);
        return sequenceNumber;

    }

}
