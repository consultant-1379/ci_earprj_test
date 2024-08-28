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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest

class AxeTimeOutHandlerTest extends AxeAbstractUpgradeTest {

    @ObjectUnderTest
    private AxeUpgradeActivitiesService axeUpgradeActivitiesService


    def 'Upgrade package TimeOut Handler Tests when success'(){

        given: "Preparing ActivityJobId"
        buildPo();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");

        when:"Updating Job attribute Data"
        axeUpgradeActivitiesService.asyncHandleTimeout(activityJobId)

        then: "Verify  the Job attribute Data"
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Notification not received with in")
    }

    def 'Upgrade package TimeOut Handler Tests when failed'(){

        given: "Preparing ActivityJobId"
        buildPo();
        buildActivityjobs("true","true","Success","Success","","","COMPLETED","COMPLETED","CREATED","CREATED","");
        def activityJobIdForFailure = 0

        when:"Updating Job attribute Data"
        axeUpgradeActivitiesService.asyncHandleTimeout(activityJobIdForFailure)

        then: "Verify  the Job attribute Data"
        runtimeDps.stubbedDps.liveBucket.findPoById(1).attributes.get("lastLogMessage")==null
    }
}
