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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmDataDescriptorParser;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.ra.util.ArchiveExpandResponse;

@RunWith(MockitoJUnitRunner.class)
public class EcimOssBackupItemsReaderTest {

    @InjectMocks
    EcimOssBackupItemsReader objectUnderTest;

    @Mock
    NetworkElementData networkElement;

    @Mock
    BrmDataDescriptorParser ossDataXmlParser;

    @Mock
    SmrsRetryPolicies smrsRetryPolicies;

    @Mock
    RetryPolicy retryPolicy;

    @Mock
    SmrsFileStoreService smrsServiceUtil;

    @Mock
    FileResource fileResource;

    @Mock
    Resource resource;

    @Mock
    OssBackupInfoFileReader ossFileManager;

    @Mock
    ArchiveExpandResponse archiveExpandResponse;

    String backupName = "Dummy Backup";
    String neType = "RadioNode";
    String smrsFilePath = "smrsFilePath";
    String neName = "neName";
    String neFdn = "neFdn";

    @Test
    public void testGetBackupItems() {
        when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicy);
        when(networkElement.getNeType()).thenReturn(neType);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicy)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add(backupName);
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        when(resource.getName()).thenReturn(backupName);
        when(ossFileManager.extractBackupFile(Matchers.anyString(), Matchers.anyString())).thenReturn(archiveExpandResponse);
        final byte[] byteResource = "Dummy XML".getBytes();
        final Map<String, Object> ossAttributes = new HashMap<String, Object>();
        when(ossDataXmlParser.parse(byteResource, false)).thenReturn(ossAttributes);
        final Map<String, Object> actualOssAttributes = objectUnderTest.getBackupItems(networkElement, ossDataXmlParser, backupName, neName);
        assertEquals(ossAttributes, actualOssAttributes);
        verify(fileResource).getFileNamesFromDirectory(Matchers.anyString());
    }
}
