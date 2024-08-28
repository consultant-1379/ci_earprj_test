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

import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.EQUALS;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;

public class OnboardPackageContextBuilder {

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    public OnboardSoftwarePackageContextForNfvo buildOnboardPackageContextForNfvo(final JobEnvironment jobContext) {

        final OnboardSoftwarePackageContextForNfvo onboardSoftwarePackageContextForNfvo = new OnboardSoftwarePackageContextForNfvo();
        final Map<String, Object> mainJobProperties = vranJobActivityService.getMainJobAttributes(jobContext);
        final List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);

        onboardSoftwarePackageContextForNfvo.setJobContext(jobContext);

        onboardSoftwarePackageContextForNfvo.setMainJobProperties(mainJobProperties);
        onboardSoftwarePackageContextForNfvo.setNeJobProperties(neJobProperties);

        onboardSoftwarePackageContextForNfvo.setTotalCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_TO_BE_ONBOARDED, neJobProperties));
        onboardSoftwarePackageContextForNfvo.setCurrentIndex(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED, neJobProperties));
        onboardSoftwarePackageContextForNfvo.setNoOfFailures(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.ONBOARD_FAILURE_PACKAGES_COUNT, neJobProperties));
        onboardSoftwarePackageContextForNfvo.setSuccessCount(vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.ONBOARD_SUCCESS_PACKAGES_COUNT, neJobProperties));

        final String packagesSelected = vranJobActivityService.getPropertyFromNEJobPropeties(VranJobConstants.VNF_PACKAGES_TO_ONBOARD, mainJobProperties, jobContext.getNodeName());
        final String[] packages = vranJobActivityUtil.splitSoftwarePackages(packagesSelected);
        final String currentPackage = packages[onboardSoftwarePackageContextForNfvo.getCurrentIndex()];

        onboardSoftwarePackageContextForNfvo.setCurrentPackage(currentPackage);

        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        onboardSoftwarePackageContextForNfvo.setBusinessKey((String) neJobAttributes.get(ShmConstants.BUSINESS_KEY));

        onboardSoftwarePackageContextForNfvo.setNodeFdn(VranJobConstants.NFVO_MO + EQUALS + jobContext.getNodeName());

        return onboardSoftwarePackageContextForNfvo;
    }

}
