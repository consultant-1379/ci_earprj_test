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
package com.ericsson.oss.services.shm.es.impl.license;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

@RunWith(MockitoJUnitRunner.class)
public class LicensingRetryServiceTest {

    @Mock
    private RetryManager retryManager;

    @Mock
    LicensingService licensingService;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    private NEJobStaticData neJobStaticData;

    @InjectMocks
    LicensingRetryService licensingRetryService;

    @Mock
    RetryPolicy retryPolicy;

    @Mock
    ShmDpsRetriableCommand<Map<String, Object>> shmDpsRetriableCommandasMap;

    @Mock
    ShmDpsRetriableCommand<String> shmDpsRetriableCommandasString;

    @Mock
    ShmDpsRetriableCommand<Boolean> shmDpsRetriableCommandasBoolean;

    @Test
    public void testGetLicenseMoAttributes() {
        long activityJobId = 1111l;
        when(dpsRetryPolicies.getReadAttributesRetryPolicy()).thenReturn(retryPolicy);
        when(licensingService.getLicenseMoAttributes(activityJobId)).thenReturn(new HashMap<String, Object>());
        licensingRetryService.getLicenseMoAttributes(activityJobId);
    }

    @Test
    public void testGetLicenseMoFdn() {
        long activityJobId = 1111l;
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicy);
        when(licensingService.getLicenseMoFdn(activityJobId)).thenReturn("fdn");
        licensingRetryService.getLicenseMoFdn(activityJobId);
    }

    @Test
    public void testUpdateLicenseInstalledTime() {
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicy);
        when(licensingService.updateLicenseInstalledTime(anyMap())).thenReturn(true);
        Map<String, Object> map = new HashMap<String, Object>();
        when(retryManager.executeCommand((RetryPolicy) anyObject(), (ShmDpsRetriableCommand<Boolean>) anyObject())).thenReturn(true);
        licensingRetryService.updateLicenseInstalledTime(map);
    }

    @Test
    public void testGetAttributesListOfLicensePOs() {
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicy);
        when(licensingService.updateLicenseInstalledTime(anyMap())).thenReturn(true);
        Map<String, Object> map = new HashMap<String, Object>();
        when(retryManager.executeCommand((RetryPolicy) anyObject(), (ShmDpsRetriableCommand<List<Map<String, Object>>>) anyObject())).thenReturn(new ArrayList<Map<String, Object>>());
        licensingRetryService.getAttributesListOfLicensePOs(map);
    }

    @Test
    public void testGetLicenseKeyFilePathFromNeJob() {
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicy);
        when(licensingService.getLicenseKeyFilePathFromNeJob(Matchers.anyLong())).thenReturn("licensepath");
        when(retryManager.executeCommand((RetryPolicy) anyObject(), (ShmDpsRetriableCommand<String>) anyObject())).thenReturn("licensepath");
        licensingRetryService.getLicenseKeyFilePathFromNeJob(12345);
    }

    @Test
    public void testgetRestrictedAttributesOfNode() {
        String neType = "ERBS";
        long activityJobId = 1111l;
        String fingerPrint = "fingerprint";
        final Map<String, Object> mainJobAttributes = new HashMap();
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicy);
        Map<String, Object> restriction = new HashMap<String, Object>();
        when(licensingService.getRestrictedParameters(neJobStaticData, neType, fingerPrint, mainJobAttributes)).thenReturn(restriction);
        Map<String, Object> map = new HashMap<String, Object>();
        when(retryManager.executeCommand((RetryPolicy) anyObject(), (ShmDpsRetriableCommand<Map<String, Object>>) anyObject())).thenReturn((Map<String, Object>) new HashMap<String, Object>());
        licensingRetryService.getRestrictedAttributesOfNode(neJobStaticData, neType, fingerPrint, mainJobAttributes);
    }

}
