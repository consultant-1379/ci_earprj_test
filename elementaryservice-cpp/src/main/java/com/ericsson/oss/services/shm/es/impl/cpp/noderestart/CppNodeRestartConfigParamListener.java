/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.services.shm.common.enums.NodeType;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ActivityTimeoutConstants;

/**
 * Listening the respective Configuration parameter change in the Models for node restart activity.
 * 
 */
@ApplicationScoped
public class CppNodeRestartConfigParamListener {

    private static final Map<String, Integer> RESTOREJOB_ACTIVITY_TIMEOUTS = new ConcurrentHashMap<>();

    private static final String DELIMETER_UNDERSCORE = "_";

    private static final Logger LOGGER = LoggerFactory.getLogger(CppNodeRestartConfigParamListener.class);

    @Inject
    @Configured(propertyName = "cppNodeRestartRetryWaitInterval_ms")
    private int cppNodeRestartRetryWaitInterval;


    @Inject
    @Configured(propertyName = "cppNodeRestartSleepTime_ms")
    private int cppNodeRestartSleepTime;

    @Inject
    @Configured(propertyName = "erbsNodeRestartSleepTime_ms")
    private int erbsNodeRestartSleepTime;

    @Inject
    @Configured(propertyName = "mgwNodeRestartSleepTime_ms")
    private int mgwNodeRestartSleepTime;

    @Inject
    @Configured(propertyName = "mrsNodeRestartSleepTime_ms")
    private int mrsNodeRestartSleepTime;

    @Inject
    private SystemRecorder systemRecorder;

    void listenForCppNodeRestartRetryIntervalAttribute(@Observes @ConfigurationChangeNotification(propertyName = "cppNodeRestartRetryWaitInterval_ms") final int cppNodeRestartRetryWaitInterval) {
        this.cppNodeRestartRetryWaitInterval = cppNodeRestartRetryWaitInterval;
    }

    public int getCppNodeRestartRetryInterval() {
        return cppNodeRestartRetryWaitInterval;
    }


    public void listenForCppNodeRestartSleepTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "cppNodeRestartSleepTime_ms") final int cppNodeRestartSleepTime) {
        this.cppNodeRestartSleepTime = cppNodeRestartSleepTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, cppNodeRestartSleepTime);
        LOGGER.info("Changed Sleep Time value for cpp Node Restart activity is : {} ms", cppNodeRestartSleepTime);
    }

    public void listenForErbsNodeRestartSleepTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "erbsNodeRestartSleepTime_ms") final int erbsNodeRestartSleepTime) {
        this.erbsNodeRestartSleepTime = erbsNodeRestartSleepTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, erbsNodeRestartSleepTime);
        LOGGER.info("Changed Sleep Time value for erbs Node Restart activity is : {} ms", erbsNodeRestartSleepTime);
    }

    public void listenForMgwNodeRestartSleepTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "mgwNodeRestartSleepTime_ms") final int mgwNodeRestartSleepTime) {
        this.mgwNodeRestartSleepTime = mgwNodeRestartSleepTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, mgwNodeRestartSleepTime);
        LOGGER.info("Changed Sleep Time value for mgw Node Restart activity is : {} ms", mgwNodeRestartSleepTime);
    }

    public void listenForMrsNodeRestartSleepTimeAttribute(@Observes @ConfigurationChangeNotification(propertyName = "mrsNodeRestartSleepTime_ms") final int mrsNodeRestartSleepTime) {
        this.mrsNodeRestartSleepTime = mrsNodeRestartSleepTime;
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, mrsNodeRestartSleepTime);
        LOGGER.info("Changed Sleep Time value for mrs Node Restart activity is : {} ms", mrsNodeRestartSleepTime);
    }

    @PostConstruct
    public void constructTimeOutsMap() {
        final long postConstructStarted = System.currentTimeMillis();
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.ERBS + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, erbsNodeRestartSleepTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MGW + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, mgwNodeRestartSleepTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(NodeType.MRS + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, mrsNodeRestartSleepTime);
        RESTOREJOB_ACTIVITY_TIMEOUTS.put(PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME, cppNodeRestartSleepTime);
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);

    }

    public int getNodeRestartSleepTime(final String neType) {
        final String key = neType + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME;
        final String platformkey = PlatformTypeEnum.CPP + DELIMETER_UNDERSCORE + ActivityTimeoutConstants.SLEEP_TIME;
        if (RESTOREJOB_ACTIVITY_TIMEOUTS.containsKey(key)) {
            return RESTOREJOB_ACTIVITY_TIMEOUTS.get(key);
        }
        return RESTOREJOB_ACTIVITY_TIMEOUTS.get(platformkey);
    }
}