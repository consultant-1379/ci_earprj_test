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
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum

class RefreshServiceActivityTest extends EcimLicenseRefreshDataProviderTest {

    @ObjectUnderTest
    RefreshService refreshService

    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def 'When refresh activity is triggered and precheck is successful'() {
        given:"Data for precheck Action"
        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        when:"precheck action is triggered and triggering is successful"
        ActivityStepResult activityStepResult = refreshService.precheck(activityJobId)

        then:"Check if action is triggered"
        assert (activityStepResult.getActivityResultEnum().toString()== ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION.toString())
    }

    def 'When refresh activity is triggered and TBAC is failed'() {
        given:"Data for precheck Action"

        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> false

        when:"precheck action is triggered and TBAC is failed"
        ActivityStepResult activityStepResult = refreshService.precheck(activityJobId)

        then:"Check if ActivityStepResult is skipped execution"
        assert (activityStepResult.getActivityResultEnum().toString()== ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION.toString())
    }

    def 'When precheck action is triggered and Exception is thrown'() {
        given:"Data for precheck Action"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw new Exception() }

        when:"precheck action is triggered and Exception is thrown"
        ActivityStepResult activityStepResult = refreshService.precheck(activityJobId)

        then:"Check if Exception is thrown"
        assert (activityStepResult.getActivityResultEnum().toString()== ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION.toString())
    }

    def 'When refresh activity is triggered and ShmNodeLicenseRefreshRequestDataPOs are Available'() {
        given:"Data for precheck Action"

        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true

        PersistenceObject po = buildShmNodeLicenseRefreshRequestDataPO(nodeName)

        when:"precheck action is triggered and ShmNodeLicenseRefreshRequestDataPOs are Available"
        ActivityStepResult activityStepResult = refreshService.precheck(activityJobId)

        then:"Check if refresh activity is skipped"
        assert (activityStepResult.getActivityResultEnum()).equals(ActivityStepResultEnum.PRECHECK_SUCCESS_SKIP_EXECUTION)
    }

    def 'When refresh activity is triggered and execute is successful'() {
        given:"Data for execute Action"

        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)

        buildInstantaneousLicensingMO(nodeName)
        buildShmNodeLicenseRefreshRequestDataPO(nodeName)

        when:"execute action is triggered and triggering is successful"
        refreshService.execute(activityJobId)

        then:"Check if action is triggered"
        assert (actionId == licenseRefreshServiceProvider.performMOAction(ilMoFdn))
        2 * activityUtils.subscribeToMoNotifications(_, _, _)
    }

    def 'When refresh activity is triggered and performAction is failed'() {
        given:"Data for execute Action"

        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)

        when:"execute action is triggered and performAction is failed"
        refreshService.execute(activityJobId)

        then:"Check if execute action is triggered"
        1 * activityUtils.failActivity(activityJobId, _, _, _)
    }

    def 'When refresh activity is triggered and processnotification is successful'() {
        given:"Data for processNotification Action"

        loadJobProperties(nodeName,"refresh")

        buildInstantaneousLicensingMO(nodeName)

        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        activityUtils.getActivityJobId(_) >> activityJobId >> activityJobId
        activityUtils.getActivityInfo(activityJobId, _) >> jobActivityInfo
        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
        activityUtils.getModifiedAttributes(dpsAttributeChangedEvent) >> modifiedAttributes

        buildDataForRefreshActivity(nodeName)

        when:"processnotification action is triggered and triggering is successful"
        refreshService.processNotification(notification)

        then:"Check if processnotification is successful"

        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        assert ("SUCCESS" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES)))
        2 * activityUtils.unSubscribeToMoNotifications(_, _, _)
    }



    def 'When job went into handle timeout and it is failed'(){

        given:"Data for handleTimeout Action"

        loadJobProperties(nodeName,"refresh")
        buildDataForRefreshActivity(nodeName)
        buildShmNodeLicenseRefreshRequestDataPO(nodeName)

        activityUtils.getActivityInfo(activityJobId, RefreshService.class) >> jobActivityInfo
        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment
        jobEnvironment.getNodeName() >> nodeName

        when:"handletimeout action is triggered"
        ActivityStepResult activityStepResult = refreshService.handleTimeout(activityJobId)
        then:"Check if handleTimeout is triggered"
        assert (activityStepResult.getActivityResultEnum().toString()== ActivityStepResultEnum.TIMEOUT_RESULT_FAIL.toString())
    }

    def 'When cancel action is triggered'(){

        given:"Data for cancel Action"
        activityJobId = 4569L

        when:"cancel action is triggered"
        ActivityStepResult activityStepResult =refreshService.cancel(activityJobId)

        then:"Check if cancel is triggered"
        assert (activityStepResult == null)
    }

    def 'When cancelTimeout is triggered'(){

        given:"Data for cancelTimeout Action"
        def finalizeResult = true
        def activityJobId = 4569L

        when:"cancelTimeout action is triggered"
        ActivityStepResult activityStepResult =refreshService.cancelTimeout(activityJobId,finalizeResult)

        then:"Check if cancel is triggered"
        assert (activityStepResult == null)
    }

    def 'When execute action is triggered and JobDataNotFoundException is thrown'() {
        given:"Data for execute Action"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw new JobDataNotFoundException() }

        when:"execute action is triggered and JobDataNotFoundException is thrown"
        refreshService.execute(activityJobId)

        then:"Check if failactivity is triggered"
        1 * activityUtils.failActivity(activityJobId, _, _,_)
    }

    def 'When execute action is triggered and Exception is thrown'() {
        given:"Data for execute Action"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw new Exception() }

        when:"execute action is triggered and Exception is thrown"
        refreshService.execute(activityJobId)

        then:"Check if failActivity is triggered"
        1 * activityUtils.failActivity(activityJobId, _, _,_)
    }

    def 'When processNotification action is triggered and Exception is thrown'() {
        given:"Data for processNotification Action"

        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        activityUtils.getActivityJobId(_) >> activityJobId >> activityJobId
        activityUtils.getActivityInfo(activityJobId, _) >> jobActivityInfo
        notification.getDpsDataChangedEvent() >> dpsAttributeChangedEvent
        activityUtils.getModifiedAttributes(dpsAttributeChangedEvent) >> modifiedAttributes
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw exception }

        when:"processNotification action is triggered and Exception is thrown"
        refreshService.processNotification(notification)

        then:"Check if failActivity is triggered"
        1 * activityUtils.failActivity(activityJobId, _, _,_)
        where:
        exception << [new JobDataNotFoundException(), new Exception()]
    }

    def 'When job went into handle timeout and Exception is thrown'(){

        given:"Data for handleTimeout Action"

        loadJobProperties(nodeName,"refresh")
        buildShmNodeLicenseRefreshRequestDataPO(nodeName)
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> {throw exception }

        when:"handletimeout action is triggered"
        ActivityStepResult activityStepResult = refreshService.handleTimeout(activityJobId)
        then:"Check if handleTimeout is triggered"
        assert activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL)

        where:
        exception << [new JobDataNotFoundException(), new Exception()]
    }
}
