/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

@RunWith(MockitoJUnitRunner.class)
public class DPSUtilsTest {

    @InjectMocks
    private DPSUtils objectUnderTest;

    @Mock
    private DataPersistenceService dataPersistenceService;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private ManagedObject nodeMo;

    @Test
    public void testGetNeType() {
        String neName = "CORE82MLTN01";
        final String networkElementFdn = "NetworkElement=" + neName;
        when(dataPersistenceService.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(networkElementFdn)).thenReturn(nodeMo);
        when(nodeMo.getAttribute("neType")).thenReturn("MINI_LINK_Outdoor");
        assertEquals("MINI_LINK_Outdoor", objectUnderTest.getNeType(neName));
    }

}
