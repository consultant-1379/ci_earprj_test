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
package com.ericsson.oss.services.shm.tbac

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.retry.TBACEvaluationRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.common.retry.TBACEvaluationRetryPolicy
import com.ericsson.oss.services.shm.common.tbac.api.SHMTBACHandler
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider

class ActivityJobTBACValidatorTest extends CdiSpecification{

    @ObjectUnderTest
    private ActivityJobTBACValidatorImpl activityJobTBACValidatorImpl;

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider;

    @MockedImplementation
    protected ActivityJobTBACHelper activityJobTBACHelper;

    @MockedImplementation
    protected RetryPolicy retryPolicy;

    @MockedImplementation
    private TBACEvaluationRetryPolicy tbacRetryPolicies;

    @MockedImplementation
    private TBACEvaluationRetryConfigurationParamProvider tbacEvaluationRetryConfigurationParamProvider;

    @MockedImplementation
    private SHMTBACHandler shmTbacHandler;


    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.tbac")
    }

    def isUserAuthorized;
    def activityJobId = 1L;
    def String suppliedProductRevision="6";
    def nodeName="LTE02ERBS00001";
    def owner = "administrator";
    def executionMode = "Immediate";
    def activitySchedules = new HashMap<String, Object>();
    def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 2L, nodeName, "1234", "CPP", 5L, null);
    def JobStaticData jobStaticData = new JobStaticData(owner, activitySchedules, executionMode,JobType.UPGRADE);
    def activityName = "activate";

    def setup() {
        activitySchedules.put("2L_ERBS_activate", "Immediate");
    }

    protected mockRetries() {
        retryPolicy.getAttempts() >> 3;
        retryPolicy.waitTimeInMillis >> 1000;
        tbacRetryPolicies.getTbacEvaluationRetryPolicy >> retryPolicy;
        tbacEvaluationRetryConfigurationParamProvider.getTbacEvaluationRetryCount >>1;
        tbacEvaluationRetryConfigurationParamProvider.tbacEvaluationRetryInterval_ms >> 1;
    }

    def 'validate TBAC' () {
        given:
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData;
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData;
        shmTbacHandler.isAuthorized(jobStaticData.getOwner(), neJobStaticData.getNodeName()) >> true;
        when:
        isUserAuthorized=activityJobTBACValidatorImpl.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityName);
        then: "validating TBAC"
        isUserAuthorized == true;
    }

    def 'validate TBAC Failure' () {
        given:
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData;
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData;
        shmTbacHandler.isAuthorized(jobStaticData.getOwner(), neJobStaticData.getNodeName()) >> false;
        when:
        isUserAuthorized=activityJobTBACValidatorImpl.validateTBAC(activityJobId, neJobStaticData, jobStaticData, activityName);
        then: "validating TBAC Failure"
        isUserAuthorized == false;
    }
}
