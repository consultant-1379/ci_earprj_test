/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;

@SuppressWarnings("unused")
public class OnboardSoftwarePackageContextForNfvo extends OnboardSoftwarePackageContext {

    private static final long serialVersionUID = -73046206636897535L;

    private JobEnvironment jobContext;
    private List<Map<String, Object>> neJobProperties = null;
    private Map<String, Object> mainJobProperties = null;

    private String businessKey;
    private String nodeFdn;

    private int totalCount;
    private int currentIndex;
    private int noOfFailures;
    private int successCount;
    private String failedPackages;
    private String[] packages;
    private String currentPackage;

    private String nfvoJobId;

    public void setJobContext(final JobEnvironment jobContext) {
        this.jobContext = jobContext;
    }

    public void setNeJobProperties(final List<Map<String, Object>> neJobProperties) {
        this.neJobProperties = neJobProperties;
    }

    public void setMainJobProperties(final Map<String, Object> mainJobProperties) {
        this.mainJobProperties = mainJobProperties;
    }

    public void setBusinessKey(final String businessKey) {
        this.businessKey = businessKey;
    }

    public void setNodeFdn(final String nodeFdn) {
        this.nodeFdn = nodeFdn;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    public void setCurrentIndex(final int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void setNoOfFailures(final int noOfFailures) {
        this.noOfFailures = noOfFailures;
    }

    public void setSuccessCount(final int successCount) {
        this.successCount = successCount;
    }

    public void setPackages(final String[] packages) {
        this.packages = packages;
    }

    public void setCurrentPackage(final String currentPackage) {
        this.currentPackage = currentPackage;
    }

    public void setNfvoJobId(final String nfvoJobId) {
        this.nfvoJobId = nfvoJobId;
    }

    @Override
    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public int getCurrentIndex() {
        return currentIndex;
    }

    @Override
    public int getNoOfFailures() {
        return noOfFailures;
    }

    @Override
    public int getSuccessCount() {
        return successCount;
    }

    @Override
    public String[] getPackages() {
        return packages;
    }

    @Override
    public String getCurrentPackage() {
        return currentPackage;
    }

    @Override
    public List<Map<String, Object>> getNeJobProperties() {
        return neJobProperties;
    }

    @Override
    public Map<String, Object> getMainJobProperties() {
        return mainJobProperties;
    }

    @Override
    public JobEnvironment getContext() {
        return jobContext;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getNodeFdn() {
        return nodeFdn;
    }

    public String getNfvoJobId() {
        return nfvoJobId;
    }

    @Override
    public String toString() {
        return "OnboardPackageContextForNfvo [neJobProperties=" + neJobProperties + ", mainJobProperties=" + mainJobProperties + ", businessKey=" + businessKey + ", nodeFdn=" + nodeFdn
                + ", totalCount=" + totalCount + ", currentIndex=" + currentIndex + ", noOfFailures=" + noOfFailures + ", successCount=" + successCount + ", failedPackages=" + failedPackages
                + ", packages=" + Arrays.toString(packages) + ", currentPackage=" + currentPackage + ", nfvoJobId=" + nfvoJobId + "]";
    }
}
