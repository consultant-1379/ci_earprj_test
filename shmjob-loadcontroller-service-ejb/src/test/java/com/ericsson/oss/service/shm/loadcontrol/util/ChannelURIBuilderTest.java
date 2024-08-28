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
package com.ericsson.oss.service.shm.loadcontrol.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.loadcontrol.util.ChannelURIBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ChannelURIBuilderTest {

    @Test
    public void testBuildChannelNameMethod() {
        String obtainedChannel = ChannelURIBuilder.buildChannelName("CPP", "UPGRADE", "Install");
        Assert.assertEquals("jms:/queue/ShmCppUpgradeJobInstallActvityQueue", obtainedChannel);

    }

    @Test
    public void testBuildNhcChannelNameMethod() {
        String obtainedChannel = ChannelURIBuilder.buildChannelName("ECIM", JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), "nodehealthcheck");
        Assert.assertEquals("jms:/queue/NhcEcimNode_Health_CheckJobNodehealthcheckActvityQueue", obtainedChannel);

    }

    @Test
    public void testBuildChannelJNDINameMethod() {
        String obtainedChannel = ChannelURIBuilder.buildChannelJNDIName("CPP", "UPGRADE", "Install");
        Assert.assertEquals("java:jboss/exported/jms/queue/ShmCppUpgradeJobInstallActvityQueue", obtainedChannel);

    }

    @Test
    public void testBuildNhcChannelJNDINameMethod() {
        String obtainedChannel = ChannelURIBuilder.buildChannelJNDIName("ECIM", JobTypeEnum.NODE_HEALTH_CHECK.getAttribute(), "nodehealthcheck");
        Assert.assertEquals("java:jboss/exported/jms/queue/NhcEcimNode_Health_CheckJobNodehealthcheckActvityQueue", obtainedChannel);

    }

}
