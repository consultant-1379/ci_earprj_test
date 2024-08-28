/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.job.activity.JobType

class JobDeletionServiceExceptionTest extends AbstractJobDeletionServiceTest{

    @ObjectUnderTest
    private SHMJobServiceHelper objectUnderTest;
    
    @MockedImplementation
    protected MoDeletionService moDeletionService;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }
    
    def "Test deleteJobs() when Node is not Syncronized" () {

        given: "when job is persisted in DB and Available for Deletion"
        buildJobData(true,false,JobType.NODE_HEALTH_CHECK.toString(),true);
        moDeletionService.deleteMoByFDN(hcJobFdn1) >> {throw  new RetriableCommandException() }
        
        when: "NHC job and HcjonMO is available"
        objectUnderTest.deleteJobs(Arrays.asList(mainJobId));

        then:"All objects related to job should get deleted from database successfully other than HCJobMO"
        assert(runtimeDps.stubbedDps.liveBucket.findPoById(mainJobId) == null)
        assert(runtimeDps.stubbedDps.liveBucket.findMoByFdn(hcJobFdn1) != null)
        neandActivityJoblst.each {
            assert(runtimeDps.stubbedDps.liveBucket.findPoById(it) == null);
        }
    }
            
}
