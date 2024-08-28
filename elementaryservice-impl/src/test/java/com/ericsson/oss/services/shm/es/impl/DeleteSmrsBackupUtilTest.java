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
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.ArgumentBuilderException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;

@RunWith(MockitoJUnitRunner.class)
public class DeleteSmrsBackupUtilTest {

    @InjectMocks
    DeleteSmrsBackupUtil objectUnderTest;

    @Mock
    private FdnServiceBean fdnServiceBeanMock;

    @Mock
    private NetworkElement networkElementMock;

    @Mock
    private SmrsFileStoreService smrsServiceUtil;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    SmrsRetryPolicies smrsRetryPolicies;

    @Mock
    FileResource fileResource;

    @Mock
    Resource resource;

    private final String nodeName = "Node Name";
    private final String backupName = "Backup Name";
    private final String neType = "neType";
    String smrsFilePath = "/";

    @Test
    public void verifyDeleteBackupWhenBackupAvaialableOnSmrs() throws ArgumentBuilderException, MoNotFoundException {

        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicyMock);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add(backupName);
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        when(fileResource.delete(Matchers.anyString())).thenReturn(true);
        final boolean isDeleted = objectUnderTest.deleteBackupOnSmrs(nodeName, backupName, neType);
        verify(fileResource).getFileNamesFromDirectory(Matchers.anyString());
        verify(fileResource).delete(Matchers.anyString());
        assertEquals(true, isDeleted);

    }

    @Test
    public void verifyDeleteBackupWhenBackupNotAvaialableOnSmrs() throws ArgumentBuilderException, MoNotFoundException {

        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicyMock);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add(backupName);
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        when(fileResource.delete(Matchers.anyString())).thenReturn(false);
        final boolean isDeleted = objectUnderTest.deleteBackupOnSmrs(nodeName, backupName, neType);
        assertEquals(false, isDeleted);

    }

    @Test
    public void testIsBackupExistsWhenBackupNameAvaialbleOnSmrs() {

        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicyMock);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add(backupName);
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        assertEquals(true, objectUnderTest.isBackupExistsOnSmrs(nodeName, backupName, neType));
    }

    @Test
    public void testIsBackupExistsWhenBackupNameNotAvaialbleOnSmrs() {

        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicyMock);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicyMock)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add("TestBackup");
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        final boolean isBackupExit = objectUnderTest.isBackupExistsOnSmrs(nodeName, backupName, neType);
        assertEquals(false, objectUnderTest.isBackupExistsOnSmrs(nodeName, backupName, neType));
    }
}
