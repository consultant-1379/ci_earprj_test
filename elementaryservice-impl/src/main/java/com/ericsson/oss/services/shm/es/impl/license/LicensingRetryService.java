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
package com.ericsson.oss.services.shm.es.impl.license;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

/**
 * To provide the method calls to {@link LicensingService} with retry mechanism
 * 
 * @author xrajeke
 * 
 */
@Traceable
@Profiled
public class LicensingRetryService {
    @Inject
    private RetryManager retryManager;

    @Inject
    private LicensingService licensingService;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    public Map<String, Object> getLicenseMoAttributes(final long activityJobId) {
        return retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return licensingService.getLicenseMoAttributes(activityJobId);
            }
        });
    }

    public String getLicenseMoFdn(final long activityJobId) {

        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return licensingService.getLicenseMoFdn(activityJobId);
            }
        });
    }

    public boolean updateLicenseInstalledTime(final Map<String, Object> restrictionAttributes) {

        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {
            @Override
            public Boolean execute() {
                return licensingService.updateLicenseInstalledTime(restrictionAttributes);
            }
        });
    }

    public List<Map<String, Object>> getAttributesListOfLicensePOs(final Map<String, Object> restrictions) {

        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {
            @Override
            public List<Map<String, Object>> execute() {
                return licensingService.getAttributesListOfLicensePOs(restrictions);
            }
        });
    }

    public Map<String, Object> getRestrictedAttributesOfNode(final NEJobStaticData neJobStaticData, final String neType, final String fingerPrint, final Map<String, Object> mainJobAttributes) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Map<String, Object>>() {
            @Override
            public Map<String, Object> execute() {
                return licensingService.getRestrictedParameters(neJobStaticData, neType, fingerPrint, mainJobAttributes);
            }
        });

    }
    
    public String getNodeSequenceNumber(final String fingerprint) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return licensingService.getNodeSequenceNumber(fingerprint);
            }
        });

    }

    public String getLicenseKeyFilePathFromNeJob(final long neJobId) {
        return retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return licensingService.getLicenseKeyFilePathFromNeJob(neJobId);
            }
        });
    }
}
