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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
/**
 * This class holds the information for all STN related jobs
 * 
 * @author xgowbom
 */
public class StnJobInformation {

    private String nodeFdn;
    private String nodeName;
    private String stnPackageId;
    private JobEnvironment jobContext;
    private String filename;
    private String packageLocation;
    private String softwarePackageName;
    private String softwarePackageId;
    private long neJobId;

    public StnJobInformation(final JobEnvironment jobContext, final String nodeFdn, final String nodeName, final String softwarePackageName ) {
        this.jobContext = jobContext;
        this.nodeFdn = nodeFdn;
        this.nodeName = nodeName;
        this.softwarePackageName = softwarePackageName;
    }

    /**
     * @return the jobContext
     */
    public JobEnvironment getJobEnvironment() {
        return jobContext;
    }

    /**
     * @param jobContext
     * 
     */
    public void setJobEnvironment(final JobEnvironment jobContext) {
        this.jobContext = jobContext;
    }

    /**
     * @return the softwarePackageName
     */
    public String getSoftwarePackageName() {
        return softwarePackageName;
    }

    /**
     * @param softwarePackageName
     * 
     */
    public void setFileName(final String file) {
        this.filename = file;
    }
    
    public String getFileName() {
        return filename;
    }

    /**
     * @param filename
     * 
     */
    public void setSoftwarePackageName(final String softwarePackageName) {
        this.softwarePackageName = softwarePackageName;
    }

    /**
     * @return the softwarePackageId
     */
    public String getSoftwarePackageId() {
        return softwarePackageId;
    }

    /**
     * @param softwarePackageId
     * 
     */
    public void setSoftwarePackageId(final String softwarePackageId) {
        this.softwarePackageId = softwarePackageId;
    }

    /**
     * @return the packageLocation
     */
    public String getPackageLocation() {
        return packageLocation;
    }

    /**
     * @param packageLocation
     * 
     */
    public void setPackageLocation(final String packageLocation) {
        this.packageLocation = packageLocation;
    }

    /**
     * @return the nodeFdn
     */
    public String getNodeFdn() {
        return nodeFdn;
    }

    /**
     * @param nodeFdn
     * 
     */
    public void setNodeFdn(final String nodeFdn) {
        this.nodeFdn = nodeFdn;
    }

    /**
     * @return the neName
     */
    public String getNeName() {
        return nodeName;
    }

    /**
     * @param neName
     * 
     */
    public void setNeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the stnPackageId
     */
    public String getStnPackageId() {
        return stnPackageId;
    }

    /**
     * @param stnPackageId
     * 
     */
    public void setStnPackageId(final String stnPackageId) {
        this.stnPackageId = stnPackageId;
    }
    
    /**
     * @return the neJobId
     */
    public long getNeJobId() {
    	return neJobId;
    }

    /**
     * @param neJobId the neJobId to set
     */
    public void setNeJobId(final long neJobId) {
    	this.neJobId = neJobId;
    }

    @Override
    public String toString() {
    	return "StnJobInformation [nodeFdn=" + nodeFdn + ", nodeName=" + nodeName + ", stnPackageId=" + stnPackageId + 
    			", jobContext=" + jobContext + ", packageLocation=" + packageLocation+ ", softwarePackageName=" + softwarePackageName + 
    			", softwarePackageId=" + softwarePackageId + ", neJobId= "+ neJobId +"]";
    }

}
