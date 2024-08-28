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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.nms.security.smrs.api.CommonAccountType;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.ecim.common.EcimCommonUtils;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMHandler;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMVersionHandlersProviderFactory;
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LicenseResponse;
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

@RunWith(MockitoJUnitRunner.class)
public class LicenseMoServiceTest {

    @InjectMocks
    private LicenseMoService licenseMoService;

    @Mock
    private EcimLicensingInfo ecimLicensingInfo;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private FdnServiceBean fdnServiceBean;

    @Mock
    private NetworkElementData networkElement;

    @Mock
    private OssModelInfoProvider ossModelInfoProvider;

    @Mock
    private OssModelInfo ossModelInfo;

    @Mock
    private LMVersionHandlersProviderFactory lmVersionHandlersProviderFactory;

    @Mock
    private LMHandler lmHandler;

    @Mock
    private JobPropertyUtils jobPropertyUtils;

    @Mock
    private SmrsFileStoreService smrsFileStoreService;

    @Mock
    private SmrsAccountInfo smrsAccountInfo;

    @Mock
    private EcimLmUtils ecimLmUtils;

    @Mock
    private EcimCommonUtils ecimCommonUtils;

    @Mock
    private AsyncActionProgress asyncActionProgress;

    @Mock
    private ActionRetryPolicy moActionRetryPolicy;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    NEJobStaticData neJobStaticData;

    @Mock
    LicensePrecheckResponse licensePrecheckResponse;

    @Mock
    private OssModelInfoProvider ossModelInfoProviderMock;

    @Mock
    private OssModelInfo ossModelInfoMock;

    @Mock
    private LicenseResponse licenseResponse;

    @Mock
    private NetworkElement networkElementMock;

    private static final String nodeName = "nodeName";
    private static final String neType = "RadioNode";
    private static final String ossModelIdentity = "2042-630-876";
    private static final String MIMVersion = "LMVersion";
    private static final String filePath = "path";
    private static final String filePathWitSmrsPath = "/home/smrs";
    private static final String activityName = "install";
    private static final short actionId = 1;
    private static final String moFdn = "LTE02dg200001";

    @Before
    public void setUp() throws UnsupportedFragmentException, JobDataNotFoundException, MoNotFoundException {
        new ArrayList<NetworkElement>();

        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElement);
        when(neJobStaticDataProvider.getNeJobStaticData(actionId, SHMCapabilities.LICENSE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElement.getNeType()).thenReturn(neType);
        when(networkElement.getOssModelIdentity()).thenReturn(ossModelIdentity);
        when(neJobStaticData.getPlatformType()).thenReturn(PlatformTypeEnum.ECIM.getName());
        when(ossModelInfo.getReferenceMIMNameSpace()).thenReturn(FragmentType.ECIM_LM_TYPE.getFragmentName());
        when(ossModelInfo.getReferenceMIMVersion()).thenReturn(MIMVersion);
        when(ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_LM_TYPE.getFragmentName())).thenReturn(ossModelInfo);
        when(lmVersionHandlersProviderFactory.getLMHandler(ossModelInfo.getReferenceMIMVersion())).thenReturn(lmHandler);
    }

    @Test
    public void testGetNotifiableMoFdn() throws UnsupportedFragmentException, MoNotFoundException {
        when(lmHandler.getNotifiableMoFdn(networkElement, activityName, ossModelInfo)).thenReturn(moFdn);
        Assert.assertEquals(licenseMoService.getNotifiableMoFdn(networkElement, activityName), moFdn);
    }

    @Test
    public void testIsLicensingPOExists() throws UnsupportedFragmentException, MoNotFoundException {
        when(ecimLmUtils.isLicensingPOExists(filePath)).thenReturn(true);
        Assert.assertEquals(true, licenseMoService.isLicensingPOExists(filePath));
    }

    @Test
    public void testGetlicensingPOAttributes() throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> jobConfigurationDetails = null;
        final Map<String, String> propertyValue = new HashMap<String, String>();
        propertyValue.put(CommonLicensingActivityConstants.LICENSE_FILE_PATH, filePath);
        when(jobPropertyUtils.getPropertyValue(Arrays.asList(CommonLicensingActivityConstants.LICENSE_FILE_PATH), jobConfigurationDetails, nodeName, networkElement.getNeType(), "ECIM"))
                .thenReturn(propertyValue);
        when(smrsAccountInfo.getHomeDirectory()).thenReturn("/home/smrs");
        when(smrsFileStoreService.getSmrsDetails(CommonAccountType.LICENCE.name(), neType, nodeName)).thenReturn(smrsAccountInfo);
        licenseMoService.isLicensingPOExists(filePath);
    }

    @Test
    public void testGetlicensingPOAttributesSMRS() throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, Object> jobConfigurationDetails = null;
        final Map<String, String> propertyValue = new HashMap<String, String>();
        propertyValue.put(CommonLicensingActivityConstants.LICENSE_FILE_PATH, filePathWitSmrsPath);
        when(jobPropertyUtils.getPropertyValue(Arrays.asList(CommonLicensingActivityConstants.LICENSE_FILE_PATH), jobConfigurationDetails, nodeName, networkElement.getNeType(), "ECIM"))
                .thenReturn(propertyValue);
        when(smrsAccountInfo.getHomeDirectory()).thenReturn("/home/smrs");
        when(smrsFileStoreService.getSmrsDetails(CommonAccountType.LICENCE.name(), neType, nodeName)).thenReturn(smrsAccountInfo);
        licenseMoService.isLicensingPOExists(filePath);
    }

    @Test
    public void testGetValidAsyncActionProgress() throws UnsupportedFragmentException, MoNotFoundException {
        final Map<String, AttributeChangeData> modifiedAttributes = new HashMap<String, AttributeChangeData>();
        when(ecimCommonUtils.getValidAsyncActionProgress(activityName, modifiedAttributes)).thenReturn(asyncActionProgress);
        licenseMoService.getValidAsyncActionProgress(nodeName, modifiedAttributes, activityName);
    }

    @Test
    public void testGetValidAsyncActionProgressFromKeyFileManagementMO() throws UnsupportedFragmentException, MoNotFoundException {
        licenseMoService.getActionProgressOfKeyFileMgmtMO(networkElement);
    }

    @Test
    public void testExecuteMoAction() throws MoNotFoundException, UnsupportedFragmentException, ArgumentBuilderException {
        final Map<String, Object> jobConfigurationDetails = null;
        final Map<String, String> propertyValue = new HashMap<String, String>();
        propertyValue.put(CommonLicensingActivityConstants.LICENSE_FILE_PATH, filePath);
        when(jobPropertyUtils.getPropertyValue(Arrays.asList(CommonLicensingActivityConstants.LICENSE_FILE_PATH), jobConfigurationDetails, nodeName, networkElement.getNeType(), "ECIM"))
                .thenReturn(propertyValue);
        when(licensePrecheckResponse.getNetworkElement()).thenReturn(networkElement);
        when(licensePrecheckResponse.getEcimLicenseInfo()).thenReturn(ecimLicensingInfo);
        when(ecimLicensingInfo.getLicenseMoFdn()).thenReturn(moFdn);
        when(ecimLicensingInfo.getLicenseKeyFilePath()).thenReturn(filePath);
        when(smrsAccountInfo.getHomeDirectory()).thenReturn("/home/smrs");
        when(smrsFileStoreService.getSmrsDetails(CommonAccountType.LICENCE.name(), neType, nodeName)).thenReturn(smrsAccountInfo);
        when(moActionRetryPolicy.getDpsMoActionRetryPolicy()).thenReturn(retryPolicyMock);
        when(lmHandler.installLicense(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(actionId);
        Assert.assertEquals(licenseMoService.executeMoAction(licensePrecheckResponse, neJobStaticData), actionId);
    }

    @Test
    public void testIsLicenseKeyFileExistsInSMRS() throws MoNotFoundException, UnsupportedFragmentException {
        when(ecimLmUtils.isLicenseKeyFileExistsInSMRS(filePath)).thenReturn(true);
        final boolean fileExists = licenseMoService.isLicenseKeyFileExistsInSMRS(filePath);
        Assert.assertEquals(true, fileExists);
    }

    @Test
    public void testGetFingerPrintFromNode() throws UnsupportedFragmentException, MoNotFoundException {
        // NetworkElement networkElementMock = new NetworkElement();
        when(ossModelInfoProvider.getOssModelInfo("RadioNode", "17X-24J", FragmentType.ECIM_LM_TYPE.getFragmentName())).thenReturn(ossModelInfo);
        when(ossModelInfo.getNamespace()).thenReturn("RbsLicense");
        when(ossModelInfo.getReferenceMIMVersion()).thenReturn("2.2.2");
        when(lmVersionHandlersProviderFactory.getLMHandler(Matchers.anyString())).thenReturn(lmHandler);
        when(lmHandler.getFingerprintForNetworkElement(Matchers.any(NetworkElement.class))).thenReturn(licenseResponse);
        Map<String, NetworkElement> supportedNodes = new HashMap();
        supportedNodes.put("fingerprint", networkElementMock);
        when(licenseResponse.getSupportedNodes()).thenReturn(supportedNodes);
        final String fingerprint = licenseMoService.getFingerPrintFromNode(networkElement);
        Assert.assertEquals(fingerprint, "fingerprint");
    }
}
