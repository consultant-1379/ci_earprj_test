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
package com.ericsson.oss.services.shm.es.impl.ecim.deleteup;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

@RunWith(MockitoJUnitRunner.class)
public class PreCheckHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreCheckHandlerTest.class);

    @InjectMocks
    PreCheckHandler objectUnderTest;

    @Mock
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Mock
    private NEJobStaticData neJobStaticData;

    @Mock
    private JobStaticDataProvider jobStaticDataProvider;

    @Mock
    private JobStaticData jobStaticData;

    @Mock
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Mock
    private NetworkElementData networkElementData;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private JobEnvironment jobEnvironment;

    @Mock
    private ActivityJobTBACValidator activityJobTBACValidator;

    @Mock
    private JobLogUtil jobLogUtil;

    @Mock
    private DeleteUpgradePackageDataCollector deleteUpgradePackageDataCollector;

    @Mock
    private JobUpdateService jobUpdateService;

    @Mock
    private Map<String, String> userInputData;

    @Mock
    private Set<String> inputProductDataSet;

    long activityJobId = 1;
    long mainJobId = 3;
    String platformType = "ECIM";
    String nodeName = "LTE01dg2";
    String nodeType = "RadioNode";
    String ossModelIdentity = "ossModelIdentity";
    String inputProductData = "Dummy Input Product Data";
    String fragmentType = "ECIM_SwM";
    String swMNamespace = "Dummy Namespace";

    @Test
    public void testPerformPreCheckThrowsMoNotFoundException() {
        try {
            initializeVariables();

            initiateActivity();

            when(deleteUpgradePackageDataCollector.getUserProvidedData(jobEnvironment, nodeName, nodeType, platformType)).thenReturn(userInputData);
            when(userInputData.get(JobPropertyConstants.DELETE_UP_LIST)).thenReturn(inputProductData);
            when(deleteUpgradePackageDataCollector.getInputProductData(inputProductData)).thenReturn(inputProductDataSet);
            when(inputProductDataSet.isEmpty()).thenReturn(false);
            doThrow(MoNotFoundException.class).when(deleteUpgradePackageDataCollector).getSWMNameSpace(nodeName, fragmentType);

            final ActivityStepResult activityStepResult = objectUnderTest.performPreCheck(activityJobId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
        } catch (final Exception exception) {
            LOGGER.debug("Caught Exception {}", exception);//This Try catch block is placed to avoid below Sonarqube violation.
            //MAJOR SonarQube violation:
            //Refactor this method to throw at most one checked exception instead of: com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException, com.ericsson.oss.services.shm.common.exception.MoNotFoundException, com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
            //Read more: https://sonarqube.lmera.ericsson.se/coding_rules#rule_key=squid%3AS1160
        }
    }

    @Test
    public void testPerformPreCheckThrowsUnsupportedFragmentException() {
        try {
            initializeVariables();

            initiateActivity();

            when(deleteUpgradePackageDataCollector.getUserProvidedData(jobEnvironment, nodeName, nodeType, platformType)).thenReturn(userInputData);
            when(userInputData.get(JobPropertyConstants.DELETE_UP_LIST)).thenReturn(inputProductData);
            when(deleteUpgradePackageDataCollector.getInputProductData(inputProductData)).thenReturn(inputProductDataSet);
            when(inputProductDataSet.isEmpty()).thenReturn(false);
            doThrow(UnsupportedFragmentException.class).when(deleteUpgradePackageDataCollector).getSWMNameSpace(nodeName, fragmentType);

            final ActivityStepResult activityStepResult = objectUnderTest.performPreCheck(activityJobId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
        } catch (final Exception exception) {
            LOGGER.debug("Exception caught {}", exception);//This Try catch block is placed to avoid below Sonarqube violation.
            //MAJOR SonarQube violation:
            //Refactor this method to throw at most one checked exception instead of: com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException, com.ericsson.oss.services.shm.common.exception.MoNotFoundException, com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
            //Read more: https://sonarqube.lmera.ericsson.se/coding_rules#rule_key=squid%3AS1160
        }
    }

    @Test
    public void testPerformPreCheckThrowsEjbException() {
        try {
            initializeVariables();

            initiateActivity();

            when(deleteUpgradePackageDataCollector.getUserProvidedData(jobEnvironment, nodeName, nodeType, platformType)).thenReturn(userInputData);
            when(userInputData.get(JobPropertyConstants.DELETE_UP_LIST)).thenReturn(inputProductData);
            when(deleteUpgradePackageDataCollector.getInputProductData(inputProductData)).thenReturn(inputProductDataSet);
            when(inputProductDataSet.isEmpty()).thenReturn(false);
            when(deleteUpgradePackageDataCollector.getSWMNameSpace(nodeName, fragmentType)).thenReturn(swMNamespace);
            doThrow(EJBException.class).when(deleteUpgradePackageDataCollector).getUpData(nodeName, swMNamespace);

            final ActivityStepResult activityStepResult = objectUnderTest.performPreCheck(activityJobId);
            assertEquals(ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION, activityStepResult.getActivityResultEnum());
        } catch (final Exception exception) {
            LOGGER.debug("Exception thrown {}", exception);//This Try catch block is placed to avoid below Sonarqube violation.
            //MAJOR SonarQube violation:
            //Refactor this method to throw at most one checked exception instead of: com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException, com.ericsson.oss.services.shm.common.exception.MoNotFoundException, com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException
            //Read more: https://sonarqube.lmera.ericsson.se/coding_rules#rule_key=squid%3AS1160
        }
    }

    private void initiateActivity() throws JobDataNotFoundException, MoNotFoundException, UnsupportedFragmentException {
        when(activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ShmConstants.DELETEUPGRADEPKG_ACTIVITY)).thenReturn(true);
        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        when(networkElementData.getOssModelIdentity()).thenReturn(ossModelIdentity);
        deleteUpgradePackageDataCollector.checkFragmentAndUpdateLog(jobLogs, nodeType, ossModelIdentity);
    }

    /**
     * @throws JobDataNotFoundException
     * @throws MoNotFoundException
     */
    private void initializeVariables() throws JobDataNotFoundException, MoNotFoundException {
        when(neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.DELETE_UPGRADE_PACKAGE_JOB_CAPABILITY)).thenReturn(neJobStaticData);
        when(neJobStaticData.getMainJobId()).thenReturn(mainJobId);
        when(jobStaticDataProvider.getJobStaticData(mainJobId)).thenReturn(jobStaticData);
        when(neJobStaticData.getPlatformType()).thenReturn(platformType);
        when(neJobStaticData.getNodeName()).thenReturn(nodeName);
        when(networkElementRetrivalBean.getNetworkElementData(nodeName)).thenReturn(networkElementData);
        when(networkElementData.getNeType()).thenReturn(nodeType);
        when(activityUtils.getJobEnvironment(activityJobId)).thenReturn(jobEnvironment);
    }
}
