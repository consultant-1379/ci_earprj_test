package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.JOBPROPERTIES;
import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.NETYPEJOBPROPERTIES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NotificationSubject;
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
        propertyMap.put(ShmConstants.KEY, MiniLinkConstants.BACKUP_NAME);
        propertyMap.put(ShmConstants.VALUE, backupName);
        jobPropertyList.add(propertyMap);

        final Map<String, Object> jobPropertiesMap = Collections.<String, Object>singletonMap(JOBPROPERTIES, jobPropertyList);
        final List<Object> neTypeJobProperties = Collections.<Object>singletonList(jobPropertiesMap);
        final Map<String, Object> jobConfigurationDetails = Collections.<String, Object>singletonMap(NETYPEJOBPROPERTIES, neTypeJobProperties);
        return jobConfigurationDetails;
    }

    @Override
    public void sendNotificationToWFS(final JobEnvironment jobEnvironment, final long activityJobId, final String activity, final Map<String, Object> processVariables) {
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
