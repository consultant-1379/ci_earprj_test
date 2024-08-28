/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;

@RunWith(PowerMockRunner.class)
public class MainJobsDetailsReaderTest {

    @InjectMocks
    MainJobsDetailsReader objectUnderTest;

    @Mock
    private PersistenceObject persistenceObjectMock;

    @Mock
    DataPersistenceService dpsMock;

    @Mock
    DataBucket liveBucket;

    long poId = 1;

    @Test
    public void testRetrieveJob() {
        Map<String, Object> poAttr = new HashMap<>();
        when(dpsMock.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(poId)).thenReturn(persistenceObjectMock);
        when(persistenceObjectMock.getAllAttributes()).thenReturn(poAttr);
        poAttr = objectUnderTest.retrieveJob(poId);
        assertEquals(poAttr.get(ShmJobConstants.MAINJOBID), 0L);
        verify(dpsMock).setWriteAccess(false);
    }

}
