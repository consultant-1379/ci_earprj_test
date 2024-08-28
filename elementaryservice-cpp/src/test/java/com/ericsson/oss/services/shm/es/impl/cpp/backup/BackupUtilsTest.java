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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;

@RunWith(MockitoJUnitRunner.class)
public class BackupUtilsTest {

    @InjectMocks
    private BackupUtils backupUtils;

    @Mock
    ActivityUtils activityUtils;

    private static final String ACTION_RESULT = "actionResult";
    private static final String RESULT = "success";
    private static final String NOTIFIABLE_ATTRIBUTE = "notifiableAttributeValue";
    private static final Map<String, AttributeChangeData> MODIFIED_ATTRIBUTE = new HashMap<>();
    private static final String PREVIOUSNOTIFIABLE_ATTRIBUTE_VALUES = "previousNotifiableAttributeValue";

    @Test
    public void testGetActionResultData() {
        when(activityUtils.getNotifiableAttribute(MODIFIED_ATTRIBUTE, ACTION_RESULT)).thenReturn(getNotifiableAttributeMap());
        final Map<String, Object> actionResult = backupUtils.getActionResultData(MODIFIED_ATTRIBUTE);
        assertNotNull(actionResult);
        assertEquals(RESULT, (String) actionResult.get(ACTION_RESULT));
    }

    @Test
    public void testGetActionResultDataWhenActionResultIsNotAvailableInNotification() {
        when(activityUtils.getNotifiableAttribute(MODIFIED_ATTRIBUTE, ACTION_RESULT)).thenReturn(getNotifiableAttributeMapWithNotifiableAttributeAsNull());
        final Map<String, Object> actionResult = backupUtils.getActionResultData(MODIFIED_ATTRIBUTE);
        assertNotNull(actionResult);
        assertEquals(0, actionResult.size());
    }

    private Map<String, Object> getNotifiableAttributeMap() {
        final Map<String, Object> modifiedAttr = new HashMap<>();

        final Map<String, Object> actionResultMap = new HashMap<>();
        actionResultMap.put(ACTION_RESULT, RESULT);

        modifiedAttr.put(NOTIFIABLE_ATTRIBUTE, actionResultMap);
        return modifiedAttr;
    }

    private Map<String, Object> getNotifiableAttributeMapWithNotifiableAttributeAsNull() {
        final Map<String, Object> notifiableAttributeMap = new HashMap<>();
        notifiableAttributeMap.put(NOTIFIABLE_ATTRIBUTE, null);
        notifiableAttributeMap.put(PREVIOUSNOTIFIABLE_ATTRIBUTE_VALUES, null);

        return notifiableAttributeMap;
    }

}
