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
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.nms.security.smrs.api.NodeType;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.common.DpsReader;

@RunWith(MockitoJUnitRunner.class)
public class NodeTypeProviderTest {

    @Mock
    @Inject
    DpsReader dpsReader;

    @Mock
    ManagedObject networkMo;

    @InjectMocks
    NodeTypeProvider objectUnderTest;

    private static final String VALID_NODENAME = "Valid node name";
    private static final String INVALID_NODENAME = "Invalid node name";
    private static final String moFdn = "NetworkElement=" + VALID_NODENAME;
    private static final String NE_TYPE = "neType";
    private static final String VALID_NETYPE = "ERBS";
    private static final String INVALID_NETYPE = "ERBS123";
    private static final String INVALID_NETYPE_NULL = null;

    @Test
    public void testWithValidNodeName() {

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(NE_TYPE, VALID_NETYPE);
        when(networkMo.getAllAttributes()).thenReturn(attributesMap);
        when(dpsReader.findMoByFdn(moFdn)).thenReturn(networkMo);

        assertEquals(NodeType.ERBS, objectUnderTest.getNodeType(VALID_NODENAME));
    }

    @Test
    public void testWithInValidNodeName() {

        when(dpsReader.findMoByFdn(moFdn)).thenReturn(null);
        try {
            objectUnderTest.getNodeType(INVALID_NODENAME);
        } catch (final Exception e) {
            assertEquals("the path on OSS could not be determined.", e.getMessage());
        }
    }

    @Test
    public void testWithValidNodeNameHavingNeTypeAsNull() {

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(NE_TYPE, INVALID_NETYPE_NULL);
        when(networkMo.getAllAttributes()).thenReturn(attributesMap);
        when(dpsReader.findMoByFdn(moFdn)).thenReturn(networkMo);
        try {
            objectUnderTest.getNodeType(VALID_NODENAME);
        } catch (final Exception e) {
            assertEquals("the path on OSS could not be determined.", e.getMessage());
        }
    }

    @Test
    public void testWithValidNodeNameHavingInvalidNeType() {

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put(NE_TYPE, INVALID_NETYPE);
        when(networkMo.getAllAttributes()).thenReturn(attributesMap);
        when(dpsReader.findMoByFdn(moFdn)).thenReturn(networkMo);
        try {
            objectUnderTest.getNodeType(VALID_NODENAME);
        } catch (final Exception e) {
            assertEquals("there is no support in OSS filestore for the given node type ERBS123 .", e.getMessage());
        }
    }
}
