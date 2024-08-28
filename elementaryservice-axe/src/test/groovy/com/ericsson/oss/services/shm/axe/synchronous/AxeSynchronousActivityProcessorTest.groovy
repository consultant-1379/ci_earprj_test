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
package com.ericsson.oss.services.shm.axe.synchronous
import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.es.impl.axe.upgrade.AxeAbstractUpgradeTest
class AxeSynchronousActivityProcessorTest extends AxeAbstractUpgradeTest {

    @ObjectUnderTest
    private AxeSynchronousActivityProcessor axeSynchronousActivityProcessor

    @Inject
    private AXEUpgradeSynchronousActivityDataCache axeAcivityStaticDataCache;


    def 'Check for sync activities and notify the same if previous activity completed on all nejobs'(){
        given: "List of Sync Activites"
        buildPo();
        buildActivityjobs("true","false","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");

        when:"perform upgrade package Timeout for allocated Job"
        axeSynchronousActivityProcessor.checkAndNotifySynchronousActivities(neJobStaticData,1,"MSC-BC-BSP")
        then: "check the cache status"
        axeAcivityStaticDataCache.getSyncCompletionStatus(mainJobId+"_MSC-BC-BSP"+"_SyncCompleted")
    }

    def 'Check for sync activities and donot notify synchronous activity if previous activity not completed on all neJobs'(){
        given: "List of Sync Activites"
        buildPo();
        buildActivityjobs("true","false","Success","","","","COMPLETED","RUNNING","CREATED","CREATED","");
        when:"perform upgrade package Timeout for allocated Job"
        axeSynchronousActivityProcessor.checkAndNotifySynchronousActivities(neJobStaticData,1,"MSC-BC-BSP")
        then: "check the cache status"
        axeAcivityStaticDataCache.get(neJobStaticData.getMainJobId()+"_MSC-BC-BSP").size()>0 && !axeAcivityStaticDataCache.getSyncCompletionStatus(mainJobId+"_MSC-BC-BSP"+"_SyncCompleted")
    }

    def 'Check for sync activities for FINISHED notification , if any activity failed donot procceed for next activity,and cancel running neJobs'(){
        given: "List of Sync Activites"
        buildPo();
        buildActivityjobs("true","false","Success","FAILED","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        when:"perform upgrade package Timeout for allocated Job"
        axeSynchronousActivityProcessor.checkAndNotifySynchronousActivities(neJobStaticData,1,"MSC-BC-BSP")
        then: "check the cache status"
        axeAcivityStaticDataCache.getSyncCompletionStatus(mainJobId+"_MSC-BC-BSP"+"_SyncCompleted")
    }

    def 'Check for sync activities if NeJob got failed  donot procceed for next activity,and cancel running neJobs'(){
        given: "List of Sync Activites"
        buildPo();
        buildActivityjobs("true","false","Success","","","","COMPLETED","","CREATED","","FAILED");
        when:"perform upgrade package Timeout for allocated Job"
        axeSynchronousActivityProcessor.checkAndNotifySynchronousActivities(neJobStaticData,1,"MSC-BC-BSP")
        then: "check the cache status"
        axeAcivityStaticDataCache.getSyncCompletionStatus(mainJobId+"_MSC-BC-BSP"+"_SyncCompleted")
    }

    def 'Check for sync activities for FAILED notification , donot procceed for next activity,and cancel running neJobs'(){
        given: "List of Sync Activites"
        buildPo();
        buildActivityjobs("true","false","Success","FAILED","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        when:"perform upgrade package Timeout for allocated Job"
        axeSynchronousActivityProcessor.failOtherNeJobsIfActvitityisSync(neJobStaticData.getMainJobId(),neJobStaticData.getNeJobId(),"MSC-BC-BSP",1)
        then: "check the cache status"
        axeAcivityStaticDataCache.getSyncCompletionStatus(mainJobId+"_MSC-BC-BSP"+"_SyncCompleted")
    }
}
