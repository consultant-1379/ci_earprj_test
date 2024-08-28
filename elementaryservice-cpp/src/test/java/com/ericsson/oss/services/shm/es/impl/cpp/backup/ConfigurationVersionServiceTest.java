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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.es.impl.cpp.common.*;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationVersionServiceTest {

    @Mock
    @Inject
    CommonCvOperations commonCvOperations;

    @InjectMocks
    ConfigurationVersionService objectUnderTest;

    @Mock
    ManagedObject cvMo;

    @Mock
    UpgradePackageMO upMo;

    @Mock
    ManagedObject moMock;

    @Mock
    ConfigurationVersionUtils cvUtilMock;

    @Mock
    Map<String, Object> cvMoAttrMapMock;

    @Mock
    ConfigurationVersionMO cvMoMock;

    private static final String nodeName = "Some Node name";
    String fdn = "Some FDN";
    Map<String, Object> moAttributes = new HashMap<String, Object>();

    @Before
    public void setup() {
        when(cvMoMock.getFdn()).thenReturn(fdn);
        when(cvMoMock.getAllAttributes()).thenReturn(moAttributes);
        when(upMo.getAllAttributes()).thenReturn(moAttributes);
    }

    @Test
    public void testGetCvMoFdn() {
        when(commonCvOperations.getCVMo(nodeName)).thenReturn(cvMoMock);
        final Map<String, Object> cvMoAttr = objectUnderTest.getCVMoAttr(nodeName);
        assertTrue(cvMoAttr.containsKey(ShmConstants.FDN));
        assertTrue(cvMoAttr.containsKey(ShmConstants.MO_ATTRIBUTES));
    }

    @Test
    public void testGetUpMoAttributes() {
        final String searchValue = "Some Search Value";
        when(commonCvOperations.getUPMo(nodeName, searchValue)).thenReturn(upMo);
        final Map<String, Object> moAttributesMap = new HashMap<String, Object>();
        moAttributesMap.put(ShmConstants.FDN, null);
        moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, moAttributes);
        assertEquals(moAttributesMap, objectUnderTest.getUpgradePackageMo(nodeName, searchValue));
    }

    @Test
    public void testGetCvMOFromNode() {
        Mockito.when(commonCvOperations.getCVMo(nodeName)).thenReturn(cvMoMock);
        Mockito.when(cvMoMock.getFdn()).thenReturn(fdn);
        Mockito.when(cvMoMock.getAllAttributes()).thenReturn(cvMoAttrMapMock);
        assertEquals(objectUnderTest.getCvMOFromNode(nodeName).getFdn(), fdn);
        org.junit.Assert.assertTrue(objectUnderTest.getCvMOFromNode(nodeName) instanceof ConfigurationVersionMO);
    }
}
