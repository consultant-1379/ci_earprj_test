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

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@RunWith(MockitoJUnitRunner.class)
public class MainJobProgressCalculatorTest {

    @InjectMocks
    private MainJobProgressCalculator objectUnderTest;

    @Mock
    private DpsReader dpsReaderMock;

    @Mock
    private Map<String, Object> mapMock;
    
    @Mock
    private Map<String, Object> neJobProgressmapMock;

    @Test
    public void test_calculateMainJobProgressPercentage_noNEJobs() {
        when(mapMock.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(0d);
        double mainJobProgress = objectUnderTest.calculateMainJobProgressPercentage(1234l, 5,Arrays.asList(mapMock));
        Assert.assertEquals(0, mainJobProgress, 0);
    }

    @Test
    public void test_calculateMainJobProgressPercentage_forOneNEJob() {
        final Map<Object, Object> restrictionAttributes = new HashMap<Object, Object>();
        restrictionAttributes.put(ShmConstants.MAINJOBID, 1234l);
        when(dpsReaderMock.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictionAttributes, Arrays.asList(ShmConstants.PROGRESSPERCENTAGE))).thenReturn(
                Arrays.asList(mapMock));
        when(mapMock.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(10d);

        double mainJobProgress = objectUnderTest.calculateMainJobProgressPercentage(1234l, 1,Arrays.asList(mapMock));
        Assert.assertEquals(10.0, mainJobProgress, 0);
    }

    @Test
    public void test_calculateMainJobProgressPercentage() {
        final Map<Object, Object> restrictionAttributes = new HashMap<Object, Object>();
        restrictionAttributes.put(ShmConstants.MAINJOBID, 1234l);
        when(dpsReaderMock.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.NE_JOB, restrictionAttributes, Arrays.asList(ShmConstants.PROGRESSPERCENTAGE))).thenReturn(
                Arrays.asList(mapMock, mapMock, mapMock));
        //neJobProgressmapMock
        when(mapMock.get(ShmConstants.PROGRESSPERCENTAGE)).thenReturn(10d, 2d, 47d);

        double mainJobProgress = objectUnderTest.calculateMainJobProgressPercentage(1234l, 3,Arrays.asList(mapMock,mapMock,mapMock));
        Assert.assertEquals(19.67, mainJobProgress, 0);
       // Assert.assertEquals(32.0, mainJobProgress, 0);
    }
}
