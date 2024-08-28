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
package com.ericsson.oss.services.shm.es.impl.axe.licensing

import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.shm.common.FileResource
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse
import com.ericsson.oss.services.shm.es.impl.axe.backup.AbstractAxeBackupServiceTest
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

/**
 *
 * @author xrajeke
 *
 */
public class InstallLicenseKeyFileServiceCDITest extends AbstractAxeBackupServiceTest{

    @ObjectUnderTest
    private InstallLicenseKeyFileService objectUnderTest;

    @MockedImplementation
    private FileResource fileResource;

    @MockedImplementation
    private LicenseKeyFileDeleteService licenseKeyFileDeleteService;

    private long activityJobId ;

    def setup() {
        addNetworkElementMOs(NODE_NAME)
        platformProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.AXE
    }

    def long createAxeLicenseJob(String lkfPath, List activityProperties){
        Map<String, Object> jobTemplateData = new HashMap<>();
        Map<String, Object> mainJobData = new HashMap<>();
        Map<String, Object> neJobData = new HashMap<>();
        Map<String, Object> activityJobData = new HashMap<>();
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        Map<String, Object> neTypeProp = new HashMap<>();
        Map<String, Object> jobProp = new HashMap<>();

        jobTemplateData.put(ShmConstants.JOBCONFIGURATIONDETAILS, Collections.singletonMap("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE")));
        jobTemplateData.put("owner", "AXEUser")
        jobTemplateData.put("jobType","LICENSE")

        jobProp.put(ShmConstants.KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH)
        jobProp.put(ShmConstants.VALUE, lkfPath)
        neTypeProp.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC)
        neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
        jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        jobConfigurationDetails.put("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE"))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);

        neJobData.put(ShmConstants.NE_NAME,NODE_NAME);
        neJobData.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC);

        activityJobData.put(ShmConstants.ACTIVITY_NAME, "install")
        activityJobData.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobData.put(ShmConstants.JOBPROPERTIES, activityProperties)
        activityJobData.put(ShmConstants.PROGRESSPERCENTAGE, 0d)


        return persistJobs(jobTemplateData, mainJobData, neJobData, activityJobData)
    }

    def "AXE License Job - Execute step - error cases - no invocation"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeLicenseJob(lkfPath, Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> tbacResponse
        long licPoId  = createLicenseDataPo(lkfPathInPO)
        fileResource.exists("x3.txt") >> true
        winFIOLRequestDispatcher.initiateRestCall(_) >> { throw exception }


        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);

        then:"job should get failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        null == activityJob.getAttribute(ShmConstants.STEP_DURATIONS)

        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        1 * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7', [:]);

        null == getInstalledOn(licPoId)
        0 * licenseKeyFileDeleteService.deleteHistoricLicensePOs(_, _);

        where:
        tbacResponse    |lkfPathInPO    |lkfPath    |exception                                            |log
        false           |null           |null       |new Exception("Unused exception")                    |"[message:\"Licence install\" activity has failed. Failure reason: \"LicensekeyFile not provided\", logLevel:ERROR]"
        false           |null           |""         |new Exception("Unused exception")                    |"[message:\"Licence install\" activity has failed. Failure reason: \"LicensekeyFile not provided\", logLevel:ERROR]"
        false           |null           |"x.txt"    |new Exception("Unused exception")                    |"[message:\"Licence install\" activity has failed. Failure reason: \"Unauthorized request\", logLevel:ERROR]"
        true            |"x0.txt"       |"x1.txt"   |new Exception("Unused exception")                    |"[message:\"Licence install\" activity has failed. Failure reason: \"LicensekeyFile not available in Database\", logLevel:ERROR]"
        true            |"x2.txt"       |"x2.txt"   |new Exception("Unused exception")                    |"[message:\"Licence install\" activity has failed. Failure reason: \"LicensekeyFile not available in ENM/SMRS\", logLevel:ERROR]"
        true            |"x3.txt"       |"x3.txt"   |new JobDataNotFoundException("Test Groovy exception")|"[message:\"Licence install\" activity initiated. , logLevel:INFO, message:\"Licence install\" activity has failed. Failure reason: \"Database service is not accessible.\", logLevel:ERROR]"
        true            |"x3.txt"       |"x3.txt"   |new MoNotFoundException("Test Groovy exception")     |"[message:\"Licence install\" activity initiated. , logLevel:INFO, message:\"Licence install\" activity has failed. Failure reason: \"Test Groovy exception\", logLevel:ERROR]"
    }

    def "AXE License Job - Execute step - winfiol invocations"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt",Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        fileResource.exists(_) >> true
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(SessionIdResponse.class) >> getSessionIdResponse(actSessionId,actError)

        clientResponse.getMetadata() >> setCookieData();

        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();

        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(LicenseInstallResponse.class) >> getLicenseResponse(status, reason)

        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)

        if(actSessionId!=null){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("EXECUTE=0."))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        sessionId == extractJobProperty(SESSION_ID, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        hostName == extractJobProperty(AxeConstants.HOST_NAME, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))

        assert("[message:\"Licence install\" activity initiated. , logLevel:INFO, "+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        actCorr * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7',[:])
        cancelCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');
        delete * licenseKeyFileDeleteService.deleteHistoricLicensePOs(_, _);
        null == getInstalledOn(licPoId)
        //     enabling this line resulting Mocking inconsistencies -->   statusRetries * winFIOLRequestDispatcher.initiateRestCall(_,_)

        where:
        actSessionId    |actError |status |reason |jobResult |progress |sessionId |hostName       |actCorr |cancelCorr |delete  |log
        null          |"XXX"    |null   |null   |"FAILED"  |0.0      |null      |null           |1       |0          |0       |"message:\"Licence install\" activity has failed. Failure reason: \"XXX\", logLevel:ERROR]"
        "b2ff92f1"      |null     |null   |null   |null      |0.0      |"b2ff92f1"|"svc-2-winfiol"|0       |0          |0       |"message:\"Licence install\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO, message:\"Licence install\" is still running or its result is not yet available, waiting untill activity is timedout., logLevel:INFO]"
        "b2ff92f1"    |null     |1      |null   |"SUCCESS" |100.00   |"b2ff92f1"|"svc-2-winfiol"|1       |0          |1       |"message:\"Licence install\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO, message:\"Licence install\" activity is completed successfully., logLevel:INFO]"
        "b2ff92f1"    |null     |3      |null   |"FAILED"  |0.00     |null      |null           |1       |0          |0       |"message:\"Licence install\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO, message:\"Licence install\" activity has failed. Failure reason: \"null\", logLevel:ERROR]"
        "b2ff92f1"      |null     |2      |null   |null      |0.0      |"b2ff92f1"|"svc-2-winfiol"|0       |0          |0       |"message:\"Licence install\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO, message:\"Licence install\" is still running or its result is not yet available, waiting untill activity is timedout., logLevel:INFO]"
        "b2ff92f1"      |null     |4      |null   |null      |0.0      |"b2ff92f1"|"svc-2-winfiol"|0       |0          |0       |"message:\"Licence install\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO, message:\"Licence install\" is still running or its result is not yet available, waiting untill activity is timedout., logLevel:INFO]"
    }



    def "AXE License Job - Timeout step - winfiol invocations"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(HOST_NAME, "svc-2-wifiol")
        actProp.put(AxeConstants.COOKIE_HEADER, "WINFIOL_SERVERID=s1; path=/")

        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt",generateActivityProp(actProp))
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(LicenseInstallResponse.class) >> getLicenseResponse(status, reason)

        when: "performing the result evalation during timeout"
        objectUnderTest.asyncHandleTimeout(activityJobId);

        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("HANDLE_TIMEOUT"))

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert("[message:Activity \"Licence install\" timed out. Retrieving status from node., logLevel:INFO, "+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', null,JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        0 * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');
        delete * licenseKeyFileDeleteService.deleteHistoricLicensePOs(_, _);
        isInstalldOnUpdated == (null != getInstalledOn(licPoId))

        where:
        status  |reason |progress |delete |isInstalldOnUpdated       |jobResult     |log
        null    |"YYY"  |0.0      |0      |false                     |"FAILED"      |"message:\"Licence install\" action has failed in handle timeout. Reason: \"Invalid Response from WinFiol\"., logLevel:ERROR]"
        3       |"YYY"  |0.0      |0      |false                     |"FAILED"      |"message:\"Licence install\" activity has failed. Failure reason: \"YYY\", logLevel:ERROR]"
        1       |null   |100.0    |1      |true                      |"SUCCESS"     |"message:\"Licence install\" activity is completed successfully., logLevel:INFO]"
        2       |null   |0.0      |0      |false                     |"FAILED"      |"message:Failing \"Licence install\" Activity as it is taking more than expected time., logLevel:ERROR"+", message:\"Licence install\" activity has failed., logLevel:ERROR]"
        4       |null   |0.0      |0      |false                     |"FAILED"      |"message:\"Licence install\" activity has failed. Failure reason: \"null\", logLevel:ERROR]"
    }

    def "AXE License Job - Timeout step - error cases - no invocation"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt", Collections.emptyList())
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        winFIOLRequestDispatcher.initiateRestCall(_) >> { throw exception }


        when: "performing the execute step"
        objectUnderTest.asyncHandleTimeout(activityJobId);

        then:"job should get failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("HANDLE_TIMEOUT"))

        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', null,JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        0 * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        null == getInstalledOn(licPoId)
        0 * licenseKeyFileDeleteService.deleteHistoricLicensePOs(_, _);

        where:
        exception                       |log
        new Exception("Internal error") |"[message:Activity \"Licence install\" timed out. Retrieving status from node., logLevel:INFO, message:\"Licence install\" action has failed in handle timeout. Reason: \"Internal error\"., logLevel:ERROR]"
    }

    def "AXE License Job - Timeout step Timedout"() {
        given : "AXE License job has been created"
        activityJobId = createAxeLicenseJob("",Collections.emptyList())

        when: "performing the timeout expiry"
        objectUnderTest.timeoutForAsyncHandleTimeout(activityJobId);

        then: "the job should get updated to failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == activityJob.getAttribute(ShmConstants.STEP_DURATIONS)
        assert("[message:Failed to get the \"Licence install\" activity status from the node within \"0\" minutes. Failing the Activity., logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
    }
    
    def 'AXE License Job - cancel is triggered for activity job id'(){

        given : "AXE License job has been created"
        Map<String, Object> actProp = new HashMap<>();
        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt",generateActivityProp(actProp))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        fileResource.exists(_) >> true
       

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=objectUnderTest.cancel(activityJobId)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_SUCESS)
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        "true" == extractJobProperty(ActivityConstants.IS_CANCEL_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Node does not support cancellation of")
    }

    def getLicenseResponse(Integer status,String reason){
        if(status == null){
            final LicenseInstallResponse licensePollResponse = new LicenseInstallResponse()
            return licensePollResponse;
        }else{
            final LicenseInstallResponse licensePollResponse = new LicenseInstallResponse()
            licensePollResponse.setLicense(reason)
            licensePollResponse.setStatus(status)
            return licensePollResponse;
        }
    }

    def long createLicenseDataPo(String lkfPath){
        Map<String, Object> licenseData = new HashMap<>();
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_FINGERPRINT, "fp123")
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER, "1")
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, lkfPath)
        PersistenceObject licensePO   = runtimeDps.addPersistenceObject().namespace(LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE).type(LicensingActivityConstants.LICENSE_DATA_PO).addAttributes(licenseData).build();
        return licensePO.getPoId()
    }

    def Object getInstalledOn(long poId){
        def licensePo = runtimeDps.stubbedDps.liveBucket.findPoById(poId)
        return licensePo.getAttribute(LicensingActivityConstants.INSTALLED_ON)
    }
}
