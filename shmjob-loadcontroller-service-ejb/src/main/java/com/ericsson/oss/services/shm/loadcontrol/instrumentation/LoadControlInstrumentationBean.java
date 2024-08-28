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
package com.ericsson.oss.services.shm.loadcontrol.instrumentation;

import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.sdk.instrument.annotation.*;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;

@ApplicationScoped
@InstrumentedBean(displayName = "LoadControlInstrumentation", description = "current counter values")
public class LoadControlInstrumentationBean {

    private String currentThresholdCounts = "";

    private final ConcurrentHashMap<String, Long> currentCounts = new ConcurrentHashMap<String, Long>();

    public void updateCurrentCountJmx(final String counterName, final Long counterValue) {
        currentCounts.put(counterName, counterValue);
    }

    public void setCurrentThresholdCounts(final String currentThresholds) {
        this.currentThresholdCounts = currentThresholds;
    }

    @MonitoredAttribute(visibility = Visibility.INTERNAL, units = Units.NONE, collectionType = CollectionType.DYNAMIC, category = Category.THROUGHPUT, displayName = "current load counter values", interval = 60)
    public String getCurrentCounts() {
        return this.currentCounts.toString();
    }

    @MonitoredAttribute(visibility = Visibility.INTERNAL, units = Units.NONE, collectionType = CollectionType.DYNAMIC, category = Category.THROUGHPUT, displayName = "current Load control threshold values", interval = 60)
    public String getCurrentThresholdCounts() {
        return this.currentThresholdCounts;
    }

}
