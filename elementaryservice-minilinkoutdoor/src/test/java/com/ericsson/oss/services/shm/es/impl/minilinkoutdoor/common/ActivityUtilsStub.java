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

import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.JOBPROPERTIES;
import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.NETYPEJOBPROPERTIES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;

public class ActivityUtilsStub extends ActivityUtils {
    private final long activityJobId;
    private final String nodeName;
    private final String backupName;

    private int numOfSentNotifications = 0;
    private final Set<String> subscribedFdns = new HashSet<>();

    public ActivityUtilsStub(final long activityJobId, final String nodeName, final String backupName) {
        this.activityJobId = activityJobId;
        this.nodeName = nodeName;
        this.backupName = backupName;
    }

    @Override
    public JobEnvironment getJobEnvironment(final long activityJobId) {
        JobEnvironment jobEnvironmentMock = mock(JobEnvironment.class);
        when(jobEnvironmentMock.getNodeName()).thenReturn(nodeName);
        return jobEnvironmentMock;
    }

    @Override
    public JobActivityInfo getActivityInfo(final long activityJobId, final Class<?> clazz) {
        return mock(JobActivityInfo.class);
    }

    @Override
    public long getActivityJobId(final NotificationSubject subject) {
        return activityJobId;
    }

    @Override
    public Map<String, Object> getJobConfigurationDetails(final long activityJobId) {
        final List<Map<String, String>> jobPropertyList = new ArrayList<>();
        final Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ShmConstants.KEY, Constants.BACKUP_NAME);
        propertyMap.put(ShmConstants.VALUE, backupName);
        jobPropertyList.add(propertyMap);

        final Map<String, Object> jobPropertiesMap = Collections.<String, Object>singletonMap(JOBPROPERTIES, jobPropertyList);
        final List<Object> neTypeJobProperties = Collections.<Object>singletonList(jobPropertiesMap);
        return Collections.<String, Object>singletonMap(NETYPEJOBPROPERTIES, neTypeJobProperties);
    }

    @Override
    public void sendNotificationToWFS(final NEJobStaticData neJobStaticData, final long activityJobId, final String activity, final Map<String, Object> processVariables) {
        ++numOfSentNotifications;
    }

    @Override
    public FdnNotificationSubject subscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        subscribedFdns.add(moFdn);
        return null;
    }

    @Override
    public boolean unSubscribeToMoNotifications(final String moFdn, final long activityJobId, final JobActivityInfo jobActivityInfo) {
        subscribedFdns.remove(moFdn);
        return true;
    }

    @Override
    public void logCancelledByUser(final List<Map<String, Object>> jobLogList, final NEJobStaticData neJobStaticData, final String activityName) {
        numOfSentNotifications = 0;
    }

    public void subscribeToMoNotifications(final String moFdn) {
        subscribedFdns.add(moFdn);
    }

    public void clearUp() {
        subscribedFdns.clear();
        numOfSentNotifications = 0;
    }

    public boolean subscribedToFdn(final String fdn) {
        return subscribedFdns.contains(fdn);
    }

    public int getNumOfSentNotifications() {
        return numOfSentNotifications;
    }

}
