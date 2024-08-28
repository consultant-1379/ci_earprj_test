/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.polling.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.model.ShmPollingActivityData;

/**
 * This class provides the required attributes to read the data for Polling
 * 
 * @author xprapav
 */

public class PollingActivityAttributes implements ShmPollingActivityData, Serializable {

    private static final long serialVersionUID = 1L;

    private long activityJobId;

    private String moFdn;

    private String namespace;

    private String mimVersion;

    private List<String> moAttributes;

    private Map<String, String> additionalInformation;

    private String pollCycleStatus;

    public PollingActivityAttributes(final long activityJobId, final String moFdn, final String namespace, final String mimVersion, final List<String> moAttributes,
            final Map<String, String> additionalInformation, final String pollCycleStatus) {
        this.activityJobId = activityJobId;
        this.moFdn = moFdn;
        this.namespace = namespace;
        this.mimVersion = mimVersion;
        this.moAttributes = moAttributes;
        this.additionalInformation = additionalInformation;
        this.pollCycleStatus = pollCycleStatus;
    }

    @Override
    public long getActivityJobId() {
        return activityJobId;
    }

    @Override
    public String getMoFdn() {
        return moFdn;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getMimVersion() {
        return mimVersion;
    }

    @Override
    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    @Override
    public String getPollCycleStatus() {
        return pollCycleStatus;
    }

    @Override
    public void setActivityJobId(final long activityJobId) {
        this.activityJobId = activityJobId;
    }

    @Override
    public void setMoFdn(final String moFdn) {
        this.moFdn = moFdn;
    }

    @Override
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    @Override
    public void setMimVersion(final String mimVersion) {
        this.mimVersion = mimVersion;
    }

    @Override
    public void setAdditionalInformation(final Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @Override
    public void setPollCycleStatus(final String pollCycleStatus) {
        this.pollCycleStatus = pollCycleStatus;
    }

    @Override
    public List<String> moAttributes() {
        return moAttributes;
    }

    @Override
    public void setMoAttributes(final List<String> moAttributes) {
        this.moAttributes = moAttributes;
    }
}
