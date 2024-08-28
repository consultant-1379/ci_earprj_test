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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.common;

import java.util.*;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;

/**
 * Class to hold contextual or job information for ENM software packages to be deleted.
 * 
 * @author xsripod
 *
 */
@SuppressWarnings("unused")
public class DeletePackageContextForEnm extends DeletePackageContext {

    private static final long serialVersionUID = -7304620663689753521L;

    private JobEnvironment jobContext;
    private List<Map<String, Object>> neJobProperties = null;
    private Map<String, Object> mainJobProperties = null;

    private int totalCount;
    private int currentIndex;
    private int noOfFailures;
    private int successCount;
    private String failedPackages;
    private String[] packages;
    private String currentPackage;

    public JobEnvironment getJobContext() {
        return jobContext;
    }

    public void setJobContext(final JobEnvironment jobContext) {
        this.jobContext = jobContext;
    }

    public void setNeJobProperties(final List<Map<String, Object>> neJobProperties) {
        this.neJobProperties = neJobProperties;
    }

    public void setMainJobProperties(final Map<String, Object> mainJobProperties) {
        this.mainJobProperties = mainJobProperties;
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

    public void setFailedPackages(final String failedPackages) {
        this.failedPackages = failedPackages;
    }

    public void setPackages(final String[] packages) {
        this.packages = packages;
    }

    public void setCurrentPackage(final String currentPackage) {
        this.currentPackage = currentPackage;
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
    public String getFailedPackages() {
        return failedPackages;
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

    @Override
    public String toString() {
        return "DeletePackageContextForEnm [neJobProperties=" + neJobProperties + ", mainJobProperties=" + mainJobProperties + ", totalCount=" + totalCount + ", currentIndex=" + currentIndex
                + ", noOfFailures=" + noOfFailures + ", successCount=" + successCount + ", failedPackages=" + failedPackages + ", packages=" + Arrays.toString(packages) + ", currentPackage="
                + currentPackage + "]";
    }

}
