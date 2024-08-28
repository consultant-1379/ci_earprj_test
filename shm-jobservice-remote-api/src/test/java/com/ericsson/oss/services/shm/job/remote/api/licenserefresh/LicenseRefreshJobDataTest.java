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
package com.ericsson.oss.services.shm.job.remote.api.licenserefresh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LicenseRefreshJobDataTest {

    @InjectMocks
    private LicenseRefreshJobData licenseRefreshJobData;

    @Test
    public void testLicenseRefreshJobData() {
        Map<String, Object> activitySchedulesMap = new HashMap<>();
        List<Map<String, Object>> activitySchedules = new ArrayList<>();
        Map<String, Object> configuration = new HashMap<>();
        List<Map<String, Object>> configurations = new ArrayList<>();
        Map<String, Object> mainSchedule = new HashMap<>();
        activitySchedulesMap.put("activity1", "refresh");
        activitySchedulesMap.put("activity2", "request");
        activitySchedulesMap.put("activity3", "install");
        activitySchedules.add(activitySchedulesMap);
        configuration.put("config", "configuration1");
        configurations.add(configuration);
        mainSchedule.put("schedule", "immediate");
        licenseRefreshJobData.setActivitySchedules(activitySchedules);
        licenseRefreshJobData.setConfigurations(configurations);
        licenseRefreshJobData.setMainSchedule(mainSchedule);
        licenseRefreshJobData.toString();
        Assert.assertNotNull(licenseRefreshJobData.getActivitySchedules());
        Assert.assertNotNull(licenseRefreshJobData.getConfigurations());
        Assert.assertNotNull(licenseRefreshJobData.getMainSchedule());
        Assert.assertSame(activitySchedules, licenseRefreshJobData.getActivitySchedules());
        Assert.assertSame(configurations, licenseRefreshJobData.getConfigurations());
        Assert.assertSame(mainSchedule, licenseRefreshJobData.getMainSchedule());
    }

}
