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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.resource.ResourceException;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.FileDeleteUtilBean;
import com.ericsson.oss.services.shm.ra.FileConnection;
import com.ericsson.oss.services.shm.ra.FileConnectionFactory;
import com.ericsson.oss.services.shm.ra.exception.ArchiveEntryNotFoundException;
import com.ericsson.oss.services.shm.ra.exception.InvalidFileFormatException;
import com.ericsson.oss.services.shm.ra.util.ArchiveExpandResponse;

@RunWith(MockitoJUnitRunner.class)
public class OssBackupInfoFileReaderTest {

    @InjectMocks
    OssBackupInfoFileReader ossBackupInfoFileReader;

    @Mock
    ArchiveExpandResponse archiveExpandResponse;

    @Mock
    private FileConnectionFactory fileConnectionFactory;

    @Mock
    private FileConnection fileConnection;

    @Mock
    private FileDeleteUtilBean fileDeleteUtilBean;

    @Test
    public void extractBackupFileTest() throws ResourceException, FileNotFoundException, IOException, InvalidFileFormatException, ArchiveEntryNotFoundException {

        final Set<String> fileTobeExtracted = new HashSet<String>();
        fileTobeExtracted.add("abc");

        when(fileConnectionFactory.getConnection()).thenReturn(fileConnection);
        when(fileConnection.extractFileFromArchive("xyz", FilenameUtils.getName("xyz"), fileTobeExtracted)).thenReturn(archiveExpandResponse);

        ArchiveExpandResponse archiveExpandResponse = ossBackupInfoFileReader.extractBackupFile("xyz", "abc");
        assertNotNull(archiveExpandResponse);

    }

}
