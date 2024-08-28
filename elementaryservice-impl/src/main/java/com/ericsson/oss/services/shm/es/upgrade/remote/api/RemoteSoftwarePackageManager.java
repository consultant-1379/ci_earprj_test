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
package com.ericsson.oss.services.shm.es.upgrade.remote.api;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.filestore.swpackage.es.remote.api.RemoteSoftwarePackageValidationService;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class RemoteSoftwarePackageManager {

    @EServiceRef
    private RemoteSoftwarePackageValidationService remoteSoftwarePackageValidationService;

    @EServiceRef
    private RemoteSoftwarePackageService remoteSoftwarePackageService;

    public Map<String, Object> validateUPMoState(final Map<String, String> nodeSwPkgDetailsMap) {
        return remoteSoftwarePackageValidationService.validateUPMoState(nodeSwPkgDetailsMap);
    }

    public List<String> getSoftwarePackageDetails(final String swPkgName) {
        return remoteSoftwarePackageService.getSoftwarePackageDetails(swPkgName);
    }

    public List<String> getUpgradePackageDetails(final String swPkgName) {
        return remoteSoftwarePackageService.getUpgradePackageDetails(swPkgName);
    }

    public String getSoftwarPackagePath(final String swPkgName) {
        return remoteSoftwarePackageService.getSmoInfoFilePathFromSmrs(swPkgName);
    }

    public String getSoftwarPackageReleaseVersion(final String swPkgName) {
        return remoteSoftwarePackageService.getSoftwarPackageReleaseVersion(swPkgName);
    }
}
