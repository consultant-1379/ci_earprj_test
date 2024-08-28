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

import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.EQUALS;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;

public class DeletePackageContextBuilder {

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    public DeletePackageContextForEnm buildDeletePackageContextForEnm(final JobEnvironment jobContext) {

        final DeletePackageContextForEnm deletePackageContextForEnm = new DeletePackageContextForEnm();
        final Map<String, Object> mainJobProperties = vranJobActivityService.getMainJobAttributes(jobContext);
        final List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);

        deletePackageContextForEnm.setJobContext(jobContext);

        deletePackageContextForEnm.setMainJobProperties(mainJobProperties);
        deletePackageContextForEnm.setNeJobProperties(neJobProperties);

        deletePackageContextForEnm.setTotalCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_ENM, neJobProperties));
        deletePackageContextForEnm.setCurrentIndex(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM, neJobProperties));
        deletePackageContextForEnm.setNoOfFailures(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM, neJobProperties));
        deletePackageContextForEnm.setSuccessCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM, neJobProperties));

        deletePackageContextForEnm.setFailedPackages(vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.FAILED_PACKAGES_FROM_ENM, neJobProperties));

        final String packagesSelected = vranJobActivityService.getPropertyFromNEJobPropeties(VranJobConstants.DELETE_VNF_PACKAGES_FROM_ENM, mainJobProperties, jobContext.getNodeName());
        final String[] packages = vranJobActivityUtil.splitSoftwarePackages(packagesSelected);

        if (packages != null && packages.length > deletePackageContextForEnm.getCurrentIndex()) {
            final String currentPackage = packages[deletePackageContextForEnm.getCurrentIndex()];
            deletePackageContextForEnm.setCurrentPackage(currentPackage);
        }

        return deletePackageContextForEnm;
    }

    public DeletePackageContextForNfvo buildDeletePackageContextForNfvo(final JobEnvironment jobContext) {

        final DeletePackageContextForNfvo deletePackageContextForNfvo = new DeletePackageContextForNfvo();
        final Map<String, Object> mainJobProperties = vranJobActivityService.getMainJobAttributes(jobContext);
        final List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);

        deletePackageContextForNfvo.setJobContext(jobContext);

        deletePackageContextForNfvo.setMainJobProperties(mainJobProperties);
        deletePackageContextForNfvo.setNeJobProperties(neJobProperties);

        deletePackageContextForNfvo.setTotalCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_NFVO, neJobProperties));
        deletePackageContextForNfvo.setCurrentIndex(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO, neJobProperties));
        deletePackageContextForNfvo.setNoOfFailures(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO, neJobProperties));
        deletePackageContextForNfvo.setSuccessCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO, neJobProperties));

        deletePackageContextForNfvo.setFailedPackages(vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.FAILED_PACKAGES_FROM_NFVO, neJobProperties));

        final String packagesSelected = vranJobActivityService.getPropertyFromNEJobPropeties(VranJobConstants.DELETE_VNF_PACKAGES_FROM_NFVO, mainJobProperties, jobContext.getNodeName());
        final String[] packages = vranJobActivityUtil.splitSoftwarePackages(packagesSelected);

        if (packages != null && packages.length > deletePackageContextForNfvo.getCurrentIndex()) {
            final String currentPackage = packages[deletePackageContextForNfvo.getCurrentIndex()];
            deletePackageContextForNfvo.setCurrentPackage(currentPackage);
        }

        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        deletePackageContextForNfvo.setBusinessKey((String) neJobAttributes.get(ShmConstants.BUSINESS_KEY));

        deletePackageContextForNfvo.setNodeFdn(VranJobConstants.NFVO_MO + EQUALS + jobContext.getNodeName());
        deletePackageContextForNfvo.setNfvoJobId(vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.VNF_JOB_ID, neJobProperties));

        return deletePackageContextForNfvo;
    }

}
