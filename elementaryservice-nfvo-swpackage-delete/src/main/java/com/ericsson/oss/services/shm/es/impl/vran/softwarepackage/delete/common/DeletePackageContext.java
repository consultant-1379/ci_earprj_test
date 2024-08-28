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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;

@SuppressWarnings("unused")
public abstract class DeletePackageContext {

    private static final long serialVersionUID = 945685039800597094L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePackageContext.class);

    public abstract JobEnvironment getContext();

    public abstract List<Map<String, Object>> getNeJobProperties();

    public abstract Map<String, Object> getMainJobProperties();

    public abstract int getTotalCount();

    public abstract int getCurrentIndex();

    public abstract int getNoOfFailures();

    public abstract int getSuccessCount();

    public abstract String getFailedPackages();

    public abstract String[] getPackages();

    public abstract String getCurrentPackage();

    public boolean isComplete() {
        LOGGER.debug("ActivityJob ID - [{}] : Verifying deletion of packages is completed, Number of failures: {}, success: {}", getContext().getActivityJobId(), getNoOfFailures(), getSuccessCount());
        return getTotalCount() == getNoOfFailures() + getSuccessCount();
    }

    public boolean areThereAnyPackagesToBeDeleted() {
        return getTotalCount() > 0;
    }

    public boolean areAllPackagesFailedToDelete() {
        return getTotalCount() == getNoOfFailures();
    }

}
