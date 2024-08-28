/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.moaction.api;

import java.io.Serializable;
import java.util.Map;

import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;

/**
 * This class is used to for preparing MoAction data to add to cache.
 * 
 * @author xpavdeb
 */

public class ShmEcimMoActionAttributes implements ShmEBMCMoActionData, Serializable {

    private static final long serialVersionUID = 1L;

    private long activityJobId;

    private String moFdn;

    private String namespace;

    private String actionName;

    private String mimVersion;

    private String moName;

    private long maxWaitTime;

    private Map<String, Object> additionalInformation;

    private Map<String, Object> moActionAttributes;

    public ShmEcimMoActionAttributes(final long activityJobId, final String moFdn, final String actionName, final String namespace, final String mimVersion,
            final Map<String, Object> moActionAttributes, final Map<String, Object> additionalInformation, final String moName, final long maxWaitTime) {
        this.activityJobId = activityJobId;
        this.moFdn = moFdn;
        this.namespace = namespace;
        this.actionName = actionName;
        this.mimVersion = mimVersion;
        this.moName = moName;
        this.additionalInformation = additionalInformation;
        this.moActionAttributes = moActionAttributes;
        this.maxWaitTime = maxWaitTime;
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
    public String getActionName() {
        return actionName;
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
    public Map<String, Object> getMoActionAttributes() {
        return moActionAttributes;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
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
    public void setActionName(final String actionName) {
        this.actionName = actionName;
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
    public void setMoActionAttributes(final Map<String, Object> moActionAttributes) {
        this.moActionAttributes = moActionAttributes;
    }

    @Override
    public void setAdditionalInformation(final Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    @Override
    public String getMoName() {
        return moName;
    }

    @Override
    public void setMoName(final String moName) {
        this.moName = moName;
    }

    @Override
    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    @Override
    public void setMaxWaitTime(final long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

}
