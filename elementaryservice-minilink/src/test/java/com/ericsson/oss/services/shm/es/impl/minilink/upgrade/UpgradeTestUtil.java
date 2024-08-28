/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.GenericNotification;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;


public class UpgradeTestUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeTestUtil.class);

    private final RuntimeConfigurableDps configurableDps;
    private Map<Long, ManagedObject> managedObjects;
    private Map<Long, Boolean> isRAUPackages;
    private Map<Long, List<Map<String, Object>>> jobLogListMap;
    private Map<Long, List<Map<String, Object>>> jobPropertyListMap;

    public UpgradeTestUtil() {
        configurableDps = new RuntimeConfigurableDps();
        managedObjects = new HashMap<Long, ManagedObject>();
        isRAUPackages = new HashMap<Long, Boolean>();
        jobLogListMap = new HashMap<Long, List<Map<String, Object>>>();
        jobPropertyListMap = new HashMap<Long, List<Map<String, Object>>>();
    }

    public void clear() {
        managedObjects = new HashMap<Long, ManagedObject>();
        isRAUPackages = new HashMap<Long, Boolean>();
        jobLogListMap = new HashMap<Long, List<Map<String, Object>>>();
        jobPropertyListMap = new HashMap<Long, List<Map<String, Object>>>();
    }

    boolean isRAUPackage(final long activityJobId) {
        return isRAUPackages.get(activityJobId);
    }

    public void addManagedObjectToMap(final Long id, final String globalStateAttribute, final boolean isRAU, final String... arguments) {
        assert arguments.length % 2 == 0;
        final Map<String, Object> attributes = new HashMap<String, Object>();
        for (int i = 0; i < arguments.length / 2; i++) {
            attributes.put(arguments[2 * i], arguments[2 * i + 1]);
        }
        isRAUPackages.put(id, isRAU);
        final List<String> globalAttributes = new ArrayList<String>();
        globalAttributes.add(globalStateAttribute);
        final ManagedObject mo = configurableDps.addManagedObject().name("ML-TN-" + id)
                .addAttribute(MiniLinkConstants.XF_SW_GLOBAL_STATE, globalAttributes).addAttributes(attributes).build();
        managedObjects.put(id, mo);
    }
    public ManagedObject getManagedObject(final Long id) {
        return managedObjects.get(id);
    }

    public void updateManageObject(final Long poId, final Map<String, Object> arguments) {
        for (final Entry<Long, ManagedObject> entry : managedObjects.entrySet()) {
            if (entry.getValue().getPoId() == poId) {
                entry.getValue().getAllAttributes().putAll(arguments);
                break;
            }
        }
    }

    public DpsAttributeChangedEvent createDpsAttributeChangedEvent(final String stateOld, final String stateNew) {
        final DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent();
        dpsAttributeChangedEvent.setFdn("testFDN");
        final Set<AttributeChangeData> changedAttributes = new HashSet<>();
        final List<String> globalStateOld = new ArrayList<>();
        globalStateOld.add(stateOld);
        final List<String> globalStateNew = new ArrayList<>();
        globalStateNew.add(stateNew);
        changedAttributes.add(new AttributeChangeData("xfSwGlobalState", globalStateOld, globalStateNew, null, null));
        dpsAttributeChangedEvent.setChangedAttributes(changedAttributes);
        return dpsAttributeChangedEvent;
    }

    public DpsObjectCreatedEvent createDpsObjectCreatedEvent(final String stateNew) {
        final DpsObjectCreatedEvent dpsObjectCreatedEvent = new DpsObjectCreatedEvent();
        dpsObjectCreatedEvent.setFdn("testFDN");
        final List<String> globalStateNew = new ArrayList<>();
        globalStateNew.add(stateNew);
        dpsObjectCreatedEvent.setType("xfSwObjects");
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("xfSwGlobalState", globalStateNew);
        dpsObjectCreatedEvent.setAttributeValues(attributes);
        return dpsObjectCreatedEvent;
    }

    public Notification createNotification(final DpsDataChangedEvent dpsDataChangedEvent, final Long jobId,
            final NotificationEventTypeEnum notificationEventTypeEnum) {
        final JobActivityInfo jobActivityInfo = new JobActivityInfo(jobId, "", JobTypeEnum.UPGRADE, PlatformTypeEnum.MINI_LINK_INDOOR);

        final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject("testFDN", jobId, jobActivityInfo);
        final Notification notification = new GenericNotification(dpsDataChangedEvent, fdnNotificationSubject, notificationEventTypeEnum);
        return notification;
    }

    public void addJobLogList(final long jobId, final List<Map<String, Object>> jobLogList) {
        jobLogListMap.put(jobId, jobLogList);
    }

    public void addJobPropertyList(final long jobId, final List<Map<String, Object>> jobPropertyList) {
        jobPropertyListMap.put(jobId, jobPropertyList);
    }

    public boolean logContainsMessage(final Long id, final String message) {
        boolean containsMessage = false;
        final List<Map<String, Object>> jobLogList = jobLogListMap.get(id);
        for (final Map<String, Object> jobLog : jobLogList) {
            LOGGER.info("JOBLOG: " + jobLog.get(ActivityConstants.JOB_LOG_MESSAGE));
            if (!containsMessage && jobLog.containsKey(ActivityConstants.JOB_LOG_MESSAGE)) {
                final String logMessage = (String) jobLog.get(ActivityConstants.JOB_LOG_MESSAGE);
                containsMessage = logMessage.contains(message);
            }
        }
        return containsMessage;
    }

    public String getPropertyValue(final Long id, final String key) {
        String propertyValue = "";
        final List<Map<String, Object>> jobPropertyList = jobPropertyListMap.get(id);
        if (jobPropertyList != null) {
            for (final Map<String, Object> jobProperty : jobPropertyList) {
                if (propertyValue.length() == 0 && jobProperty.get(ActivityConstants.JOB_PROP_KEY).equals(key)) {
                    propertyValue = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                }
            }
        }
        return propertyValue;
    }
}
