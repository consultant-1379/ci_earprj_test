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
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies
import com.ericsson.oss.services.shm.job.activity.JobType

import javassist.bytecode.stackmap.BasicBlock.Catch

class HcJobRetryProxyExceptionTest extends CdiSpecification{

    @ObjectUnderTest
    private HcJobRetryProxy objectUnderTest;
    
    @MockedImplementation
    private DpsRetryPolicies dpsRetryPolicies;
    
    @MockedImplementation
    private DataBucket liveBucket;
    
    @MockedImplementation
    private PersistenceObject jobPo;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }
    
    def "Test deleteJobs() when Node is not Syncronized" () {

        given: "DPS retry policy is throwing exception"
        dpsRetryPolicies.getOptimisticLockRetryPolicy() >> {throw  new RetriableCommandException() } 
        
        when: "Delete method of persistance object is invoked"
        String message;
        try {
            objectUnderTest.deletePo(jobPo, liveBucket)
        }catch(Exception exception){
            message = exception.getMessage()
        }
        
        then:"since exception is thrown method should not be invoked"
        assert(message == null);
        
    }
            
}
