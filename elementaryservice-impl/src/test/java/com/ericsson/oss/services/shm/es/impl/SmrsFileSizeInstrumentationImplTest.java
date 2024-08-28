/**
 * 
 */
package com.ericsson.oss.services.shm.es.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
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

import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.common.smrs.retry.SmrsRetryPolicies;
import com.ericsson.oss.services.shm.es.instrumentation.impl.SmrsFileSizeInstrumentationService;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.BrmDataDescriptorParser;
import com.ericsson.oss.services.shm.model.NetworkElementData;

/**
 * @author 1117725
 * @param <OssBackupInfoFileReader>
 * @param <ArchiveExpandResponse>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SmrsFileSizeInstrumentationImplTest {
	
    @InjectMocks
    SmrsFileSizeInstrumentationService smrsFileSizeInstrumentationService;
	
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
    
    String backupName = "Dummy Backup";
    String backupLocation = "Dummy Backup";
    String neType = "RadioNode";
    String smrsFilePath = "smrsFilePath";
    String neName = "neName";
    String neFdn = "neFdn";

    @Test
    public void testgetBackupFilePath(){
    	when(smrsRetryPolicies.getSmrsImportRetryPolicy()).thenReturn(retryPolicy);
        when(networkElement.getNeType()).thenReturn(neType);
        when(smrsServiceUtil.getSmrsPath(neType, SmrsServiceConstants.BACKUP_ACCOUNT, retryPolicy)).thenReturn(smrsFilePath);
        final List<String> resourceList = new ArrayList<>();
        resourceList.add(backupName);
        resourceList.add(backupLocation);
        when(fileResource.getFileNamesFromDirectory(Matchers.anyString())).thenReturn(resourceList);
        when(resource.getName()).thenReturn(backupName);
        when(resource.getName()).thenReturn(backupLocation);
        final byte[] byteResource = "Dummy XML".getBytes();
        final Map<String, Object> ossAttributes = new HashMap<String, Object>();
        when(ossDataXmlParser.parse(byteResource, false)).thenReturn(ossAttributes);
        smrsFileSizeInstrumentationService.getBackupFilePath(backupLocation, backupName);
        verify(fileResource).getFileNamesFromDirectory(Matchers.anyString());
        assertEquals(backupName,backupLocation);
    }

}
