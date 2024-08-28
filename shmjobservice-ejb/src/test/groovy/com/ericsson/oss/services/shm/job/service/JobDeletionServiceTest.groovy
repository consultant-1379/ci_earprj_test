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
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.job.activity.JobType
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class JobDeletionServiceTest extends AbstractJobDeletionServiceTest{
    
    @ObjectUnderTest
    private SHMJobServiceHelper objectUnderTest;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def "Test deleteJobs() when there is NHC job available in Database" () {

        given: "when job is persisted in DB and Available for Deletion"
        buildJobData(isHcJobMoRequired,isAlarmDataRequired,JobType.NODE_HEALTH_CHECK.toString(),isActivityJobNeeded);

        when: "NHC job,HcjonMO and alarms are persisted as per multiple scenarios"
        objectUnderTest.deleteJobs(Arrays.asList(mainJobId));

        then:"All objects related to job should get deleted from database successfully"
        assert(runtimeDps.stubbedDps.liveBucket.findPoById(mainJobId) == null)
        assert(runtimeDps.stubbedDps.liveBucket.findMoByFdn(hcJobFdn1) == null)
        neandActivityJoblst.each {
            assert(runtimeDps.stubbedDps.liveBucket.findPoById(it) == null);
        }
        
        where:
        isHcJobMoRequired | isAlarmDataRequired | isActivityJobNeeded
        true              | true                | true
        false             | false               | false
        true              | false               | false
        true              | false               | true
        false             | true                | false
        false             | true                | true
        true              | true                | false
        true              | false               | true
    }
    
    
    def "Test deleteJobs() when there is Upgrade job available in Database" () {

        given: "when job is persisted in DB and Available for Deletion"
        buildJobData(false,false,JobType.UPGRADE.toString().toString(),true);

        when: "UPGRADE job is available and No Alarm,HcjonMO is not available"
        objectUnderTest.deleteJobs(Arrays.asList(mainJobId));

        then:"All objects related to job should get deleted from database successfully"
        assert(runtimeDps.stubbedDps.liveBucket.findPoById(mainJobId) == null)
        neandActivityJoblst.each {
            assert(runtimeDps.stubbedDps.liveBucket.findPoById(it) == null);
        }
    }
    
}
