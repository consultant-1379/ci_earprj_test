/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.testng.Assert;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * Test class for NFVOResovler.
 * 
 * @author xeswpot
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class NFVOResolverTest {

    @InjectMocks
    private NFVOResolver nfvoResolver;

    @Test
    public void testGetNetworkElementResponse() {

        final String[] networkElements = { "node1" };
        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = Arrays.asList(networkElements);
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final NetworkElementResponse response = nfvoResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, null, true);
        Assert.assertEquals(response.getSupportedNes().size(), 1);

    }

    @Test
    public void testGetNetworkElementResponse_withCapability() {

        final String[] networkElements = { "node1" };
        final long mainJobId = 123456L;
        final long templateJobId = 456787L;
        final List<String> neNames = Arrays.asList(networkElements);
        final Map<String, Object> attributeMap = new HashMap<String, Object>();

        final NetworkElementResponse response = nfvoResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, JobTypeEnum.BACKUP, true);
        Assert.assertEquals(response.getSupportedNes().size(), 1);

    }
}
