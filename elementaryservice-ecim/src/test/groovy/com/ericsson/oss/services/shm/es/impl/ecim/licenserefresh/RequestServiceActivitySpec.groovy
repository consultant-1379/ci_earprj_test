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

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.FdnServiceBean
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification.LkfImportProcessNotificationImpl
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException

import spock.lang.Unroll

class RequestServiceActivitySpec extends RequestServiceActivityDataProviderSpec{

    @ObjectUnderTest
    private RequestService requestService

    @MockedImplementation
    private InventoryQueryConfigurationProvider inventoryQueryConfigurationProvider

    @MockedImplementation
    private FdnServiceBean fdnServiceBean


    List<NetworkElement> nodeFdnList = new ArrayList();

    NetworkElement nodeFdn = new NetworkElement()

    def 'Test precheck for request Activity when precheck is Success'(){

        given: 'NetworkElement and the request details'
        loadLicenseRefreshJobProperties(nodeName,"request", SUCCESS,"")
        buildDataForRequestActivity(nodeName,SUCCESS)

        when:"performing the precheck step"
        ActivityStepResult activityStepResult = requestService.precheck(activityJobId)

        then:'Check if action is triggered and result is as expected'
        activityJobId !=null
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        log << ["Precheck for \"request\" activity is successful."]
    }

    @Unroll
    def 'When execute action is triggered for request activity and is #result for scenario #scenario and licRefreshType #licRefreshType' () {

        given:"Data for execute Action of request activity"
        loadLicenseRefreshJobProperties(nodeName,"request",result,licRefreshType)
        buildDataForRequestActivity(nodeName,scenario)
        def licenseRefreshRequestDataPoId = buildLicenseRefreshRequestPO()
        activityUtils.getPoAttributes(mainJobId) >> mainJobAttributes
        activityUtils.getPoAttributes(templateJobId) >> templateJobAttributes
        inventoryQueryConfigurationProvider.getNeFdnBatchSize() >> 10
        when:"execute is triggered for request activity"
        requestService.execute(activityJobId)

        then:"Check if execute action is triggered and result is as expected"
        def requestDataPoId = runtimeDps.stubbedDps.liveBucket.findPoById(licenseRefreshRequestDataPoId)
        assert requestDataPoId != null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        final List<Map<String, Object>> jobProperties = activityJob.getAllAttributes().get("jobProperties")
        result == getLicenseRefreshJobProperty(ShmConstants.RESULT, jobProperties)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        result          |scenario                               | licRefreshType         |  log
        "SUCCESS"       |"SUCCESS"                              |       ""               |  "Execute for \"request\" activity is successful."
        "FAILED"        |"EXCEPTION"                            |       ""               |  "\"request\" activity has failed. Failure reason: \"Exception Occurred\""
        "FAILED"        |"JOB_DATA_NOT_FOUND_EXCEPTION"         |       ""               |  "\"request\" activity has failed. Failure reason: \"JobDataNotFoundException Occurred\""
        "SUCCESS"       |"SUCCESS"                              | "UpgradeLicensekeys"   |  "Execute for \"request\" activity is successful."
    }

    def 'Test handleTimeout for request Activity when Notification is not received from ELIS'(){

        given: 'NetworkElement and the request details'
        loadLicenseRefreshJobProperties(nodeName,"request","FAILED","")
        def licenseRefreshRequestDataPoId = buildLicenseRefreshRequestPO()
        def lkfRequestDataPoId = prepareLkfRequestDataPo()
        jobUpdateService.retrieveJobWithRetry(activityJobId) >>  getRequestActivityJobAttributesMap("request","FAILED")
        buildDataForRequestActivity(nodeName, scenario)
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> neJobStaticData
        activityUtils.getActivityInfo(_, _) >> jobActivityInfo
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList

        when:"performing the handle timeout step"
        ActivityStepResult activityStepResult = requestService.handleTimeout(activityJobId)

        then:'Check if action is triggered and result is as expected'
        activityStepResult.getActivityResultEnum().equals(requestActivityResult)
        def requestDataPo = runtimeDps.stubbedDps.liveBucket.findPoById(licenseRefreshRequestDataPoId)
        assert requestDataPo == null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        scenario                      | requestActivityResult                                 | log
        "FAILED"                      | ActivityStepResultEnum.TIMEOUT_RESULT_FAIL         | "Notifications not received for the \"request\" activity. Retrieving status from node."
    }

    @Unroll
    def 'Test handleTimeout for request Activity for #scenario'(){

        given: 'NetworkElement and the request details'
        loadLicenseRefreshJobProperties(nodeName,"request","FAILED","")
        fingerprintMap.put("fingerprint", radioNodeFingerPrint)
        jobUpdateService.retrieveJobWithRetry(activityJobId) >> fingerprintMap
        buildDataForRequestActivity(nodeName, scenario)
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> neJobStaticData
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList
        when:"performing the handle timeout step"
        ActivityStepResult activityStepResult = requestService.handleTimeout(activityJobId)

        then:'Check if action is triggered and result is as expected'
        activityStepResult.getActivityResultEnum().equals(requestActivityResult)
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        scenario                      | requestActivityResult                                 | log
        "EXCEPTION"                   | ActivityStepResultEnum.TIMEOUT_RESULT_FAIL            | "\"request\" action has failed in handle timeout. Reason: \"Exception Occurred\"."
        "JOB_DATA_NOT_FOUND_EXCEPTION"| ActivityStepResultEnum.TIMEOUT_RESULT_FAIL            | "\"request\" action has failed in handle timeout. Reason: \"JobDataNotFoundException Occurred\"."
    }

    @Unroll
    def 'Test processNotification received from Elis for #state'(){

        given: 'prepare Notification received from Elis'
        loadLicenseRefreshJobProperties(nodeName,"request",status,"")
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, additionalInfo)
        LkfImportProcessNotificationImpl progressNotificationImpl = new LkfImportProcessNotificationImpl()
        def licenseRefreshRequestDataPoId = buildLicenseRefreshRequestPO()
        lkfImportResponse.getActivityJobId() >> activityJobId
        lkfImportResponse.getState() >> state
        lkfImportResponse.getStatus() >> status
        lkfImportResponse.getFingerprint() >> radioNodeFingerPrint
        lkfImportResponse.getAdditionalInfo() >> additionalInfo
        lkfImportResponse.getNeJobId() >> neJobId
        progressNotificationImpl.setLkfImportResponse(lkfImportResponse)
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> neJobStaticData
        activityUtils.getActivityInfo(_, _) >> jobActivityInfo
        licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(radioNodeFingerPrint) >> "home/smrs/smrsroot/license/ilPath"
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList
        when: 'Elis notification is received and is processed'
        requestService.processNotification(progressNotificationImpl)

        then:
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }
        def requestDataPo = runtimeDps.stubbedDps.liveBucket.findPoById(licenseRefreshRequestDataPoId)
        assert requestDataPo == null

        where:
        status          |  state                               | log                                                                                                                             | additionalInfo
        "SUCCESS"       |  "LKF_IMPORT_COMPLETED"              | "License Key File is imported successfully."                                                                                    | "Lkf import is success"
        "FAILED"        |  "LKF_REQUEST_FAILED"                | "\"Request activity\" is failed. Failure Reason : Error information is not avaiable from Elis."                                 | "Error information is not avaiable from Elis"
        "FAILED"        |  "LKF_PARSING_COMPLETED"             | "\"Request activity\" is failed. Failure Reason : License Keys Files Parsing Failed."                                           | "License Keys Files Parsing Failed"
        "FAILED"        |  "LKF_INVALIDFILES_FILTER_FAILED"    | "\"Request activity\" is failed. Failure Reason : Invalid files received from Elis."                                            | "Invalid files received from Elis"
    }

    def 'Test processNotification received from Elis when Lkf is imported'(){

        given: 'prepare Notification received from Elis'
        loadLicenseRefreshJobProperties(nodeName,"request",status,"")
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, additionalInfo)
        LkfImportProcessNotificationImpl progressNotificationImpl = new LkfImportProcessNotificationImpl()
        def licenseRefreshRequestDataPoId = buildLicenseRefreshRequestPO()
        lkfImportResponse.getActivityJobId() >> activityJobId
        lkfImportResponse.getState() >> state
        lkfImportResponse.getStatus() >> status
        lkfImportResponse.getFingerprint() >> radioNodeFingerPrint
        lkfImportResponse.getAdditionalInfo() >> additionalInfo
        lkfImportResponse.getNeJobId() >> neJobId
        progressNotificationImpl.setLkfImportResponse(lkfImportResponse)
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> neJobStaticData
        activityUtils.getActivityInfo(_, _) >> jobActivityInfo
        licenseMoServiceRetryProxy.getLicenseKeyFileNameFromFingerPrint(radioNodeFingerPrint) >> "home/smrs/smrsroot/license/ilPath"
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList
        when: 'Elis notification is received and is processed'
        requestService.processNotification(progressNotificationImpl)

        then:
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }
        def requestDataPo = runtimeDps.stubbedDps.liveBucket.findPoById(licenseRefreshRequestDataPoId)
        assert requestDataPo != null

        where:
        status          |  state                               | log                                                                                                                             | additionalInfo
        "SUCCESS"       |  "LKF_IMPORT_INITIATED"              | "License Key File is received from ELIS with fingerprint \"NR01gNodeBRadio00001_fp\" and License Key File import is initiated." | "Lkf import is initiated"
    }

    @Unroll
    def 'Exceptions are occured while processing Elis notification'(){
        given: 'prepare Notification received from Elis'
        loadLicenseRefreshJobProperties(nodeName,"request",status,"")
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, null)
        LkfImportProcessNotificationImpl progressNotificationImpl = new LkfImportProcessNotificationImpl()
        lkfImportResponse.getActivityJobId() >> activityJobId
        lkfImportResponse.getFingerprint() >> radioNodeFingerPrint
        lkfImportResponse.getState() >> state
        lkfImportResponse.getStatus() >> status
        progressNotificationImpl.setLkfImportResponse(lkfImportResponse)
        activityUtils.getActivityInfo(_, _) >> jobActivityInfo
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> { throw exception }
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList
        when: 'Elis notification is received and is processed'
        requestService.processNotification(progressNotificationImpl)

        then:
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        status          |  state                                |  exception                                                            | log
        "FAILED"        |  "LKF_REQUEST_FAILED"                 |  new JobDataNotFoundException("job data is unavailable")              | "Database service is not accessible"
        "FAILED"        |  "INVALID_FILES_FILTER_COMPLETED"     |  new Exception("Error occured while processing Elis notification")    | "Error occured while processing Elis notification"
    }

    @Unroll
    def 'Exception occured while processing Elis notification'(){
        given: 'prepare Notification received from Elis'
        loadLicenseRefreshJobProperties(nodeName,"request",status,"")
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, null)
        LkfImportProcessNotificationImpl progressNotificationImpl = new LkfImportProcessNotificationImpl()
        lkfImportResponse.getActivityJobId() >> activityJobId
        lkfImportResponse.getFingerprint() >> radioNodeFingerPrint
        lkfImportResponse.getState() >> state
        lkfImportResponse.getStatus() >> status
        progressNotificationImpl.setLkfImportResponse(lkfImportResponse)
        activityUtils.getActivityInfo(_, _) >> jobActivityInfo
        neJobStaticDataProvider.getNeJobStaticData(_ , _) >> { throw exception}
        nodeFdn.setNodeRootFdn("ManagedElement=NR01gNodeBRadio00001")
        nodeFdnList.add(nodeFdn)
        fdnServiceBean.getNetworkElementsByNeNames(_) >> nodeFdnList
        when: 'Elis notification is received and is processed'
        requestService.processNotification(progressNotificationImpl)

        then:
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        def lastLog=activityJob.getAttribute("lastLogMessage")
        if(null != lastLog) {
            assert(lastLog.contains(log))
        }

        where:
        status          |  state                                |  exception
        "FAILED"        |  "LKF_REQUEST_FAILED"                 |  new JobDataNotFoundException("job data is unavailable")
    }

    def 'When cancel action is triggered for request activity'(){

        given:"Data to perform cancel action"
        activityJobId = 456922L

        when:"cancel action is triggered"
        ActivityStepResult activityStepResult = requestService.cancel(activityJobId)

        then:"assert if cancel is triggered"
        assert activityStepResult == null
    }

    def 'When cancelTimeout is triggered for request activity'(){

        given:"Data to perform cancelTimeout Action"
        def result = true
        def activityJobId = 456922L

        when:"cancelTimeout action is triggered"
        ActivityStepResult activityStepResult =requestService.cancelTimeout(activityJobId,result)

        then:"assert if cancelTimeout is triggered"
        assert activityStepResult == null
    }
}
