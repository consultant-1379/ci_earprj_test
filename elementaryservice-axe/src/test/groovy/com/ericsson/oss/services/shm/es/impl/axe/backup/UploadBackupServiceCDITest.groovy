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
package com.ericsson.oss.services.shm.es.impl.axe.backup

import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*

import java.util.Map.Entry

import javax.ejb.EJBException

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

/**
 *
 * @author xrajeke
 *
 */
public class UploadBackupServiceCDITest extends AbstractAxeBackupServiceTest{

    @ObjectUnderTest
    private UploadBackupService objectUnderTest;


    private long activityJobId ;

    def setup() {
        addNetworkElementMOs(PARENT_NODE_NAME)
        platformProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.AXE
        cryptographyService.encrypt(_)>>"axePassword".getBytes(AxeConstants.CHAR_ENCODING);
        cryptographyService.decrypt(_)>>"axePassword".getBytes();
    }

    def long createAxeUploadJob(String backupNames,List activityProperties){
        Map<String, Object> jobTemplateData = new HashMap<>();
        Map<String, Object> mainJobData = new HashMap<>();
        Map<String, Object> neJobData = new HashMap<>();
        Map<String, Object> activityJobData = new HashMap<>();
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        Map<String, Object> neTypeProp = new HashMap<>();
        Map<String, Object> jobProp = new HashMap<>();
        Map<String, Object> neJobProp = new HashMap<>();

        jobTemplateData.put(ShmConstants.JOBCONFIGURATIONDETAILS, Collections.singletonMap("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE")));
        jobTemplateData.put("owner", "AXEUser")
        jobTemplateData.put("jobType","BACKUP")


        neTypeProp.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC)
        if(backupNames.contains(",")){
            jobProp.put(ShmConstants.KEY, UPLOAD_BACKUP_DETAILS)
            jobProp.put(ShmConstants.VALUE, backupNames)
            neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
            jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        }else{
            jobProp.put(ShmConstants.KEY, INPUT_BACKUP_NAMES)
            jobProp.put(ShmConstants.VALUE, backupNames)
        }
        jobConfigurationDetails.put("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE"))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);

        neJobProp.put(ShmConstants.KEY, ShmConstants.PARENT_NAME)
        neJobProp.put(ShmConstants.VALUE, PARENT_NODE_NAME)
        neJobData.put(ShmConstants.NE_NAME,NODE_NAME);
        neJobData.put(ShmConstants.JOBPROPERTIES,  Arrays.asList(neJobProp,jobProp));
        neJobData.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC);

        activityJobData.put(ShmConstants.ACTIVITY_NAME, "uploadbackup")
        activityJobData.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobData.put(ShmConstants.JOBPROPERTIES, activityProperties)
        activityJobData.put(ShmConstants.PROGRESSPERCENTAGE, 0d)


        return persistJobs(jobTemplateData, mainJobData, neJobData, activityJobData)
    }

    def "AXE Upload Job - Execute step - error cases - no invocation"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeUploadJob("MSC_BC_bkp1",Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> tbacResponse
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

        where:
        tbacResponse    |exception                                            |log
        false           |new JobDataNotFoundException("Test Groovy exception")|null
        true            |new JobDataNotFoundException("Test Groovy exception")|"[message:\"Upload\" activity initiated. , logLevel:INFO, message:Database service is not accessible., logLevel:ERROR]"
        true            |new MoNotFoundException("Test Groovy exception")     |"[message:\"Upload\" activity initiated. , logLevel:INFO, message:Unable to trigger \"Upload\" activity on the node. Failure reason: \"Test Groovy exception\", logLevel:ERROR]"
    }

    def "AXE Upload Job - Execute step - winfiol invocations - single backup"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeUploadJob("MSC_BC_bkp1",Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(_) >> getSessionIdResponse(actSessionId,actError);
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();

        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);

        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(jobResult==null){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("EXECUTE=0."))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBackups == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        "MSC_BC_bkp1" == extractJobProperty(CURRENT_BACKUP, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        processedCount == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        "1" == extractJobProperty(TOTAL_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        sessionId == extractJobProperty(SESSION_ID, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        hostName == extractJobProperty(AxeConstants.HOST_NAME, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        activityTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        interFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert("[message:\"Upload\" activity initiated. , logLevel:INFO, "+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        correlations * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7',[:]);

        where:
        actSessionId    |actError       |jobResult  |activityTriggered |failedBackups |sessionId      |hostName      |interFailure   |correlations   |processedCount   |log
        "b2ff92f1"      |null           |null       |"true"            |null          |"b2ff92f1"     |"svc-2-winfiol"|null          |0              |null             |"message:\"Upload\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO]"
        null            |"No Connection"|"FAILED"   |null              |"MSC_BC_bkp1" |null           |null          |"FAILED"       |1              |"1"              |"message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"No Connection\", logLevel:ERROR]"
    }

    def "AXE Upload Job - Execute step - winfiol invocations - repeated backup"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(PROCESSED_BACKUPS, processedCountOld)
        actProp.put(FAILED_BACKUPS, failedBackupsOld)
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, triggerCancel)
        activityJobId = createAxeUploadJob("MSC_BC_bkp0,MSC_BC_bkp1,MSC_BC_bkp2", generateActivityProp(actProp))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(_) >> getSessionIdResponse(actSessionId,actError);
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();
        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);

        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(actTriggered == "true"){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("EXECUTE=0."))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBackupsNew == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        currentBackup == extractJobProperty(CURRENT_BACKUP, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert (procsdCountNew == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES)))
        "3" == extractJobProperty(TOTAL_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intrmFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))

        sessionId == extractJobProperty(SESSION_ID, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        actTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        if(sessionId == null){
            assert("[message:\"Upload\" activity initiated. , logLevel:INFO, "
            +"message:\"Upload\" activity has failed for backup \""+currentBackup+"\". Failure reason: \"Failed to fetch credentials\", logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        }else{
            assert("[message:\"Upload\" activity initiated. , logLevel:INFO, "
            +"message:\"Upload\" activity is triggered (timeout = \"0\" minutes)., logLevel:INFO]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        }

        actCorr * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7', repeatVariables);
        canclCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        where:
        actError        |actSessionId           |processedCountOld       |failedBackupsOld       |triggerCancel  |currentBackup  |actCorr       |procsdCountNew    |failedBackupsNew                       |repeatVariables        |jobResult      |intrmFailure  |canclCorr      |sessionId      |actTriggered
        "Failed to fetch credentials"   |null   |"1"                     |null                   |null           |"MSC_BC_bkp1"  |1             |"2"               |"MSC_BC_bkp1"                          |['repeatExecute':true] |null           |"FAILED"      |0              |null           |null
        "Failed to fetch credentials"   |null   |"2"                     |"MSC_BC_bkp1"          |null           |"MSC_BC_bkp2"  |1             |"3"               |"MSC_BC_bkp1,MSC_BC_bkp2"              |[:]                    |"FAILED"       |"FAILED"      |0              |null           |null
        "Failed to fetch credentials"   |null   |"2"                     |"MSC_BC_bkp0,MSC_BC_bkp1"|null         |"MSC_BC_bkp2"  |1             |"3"               |"MSC_BC_bkp0,MSC_BC_bkp1,MSC_BC_bkp2"  |[:]                    |"FAILED"       |"FAILED"      |0              |null           |null
        "Failed to fetch credentials"   |null   |"1"                     |null                   |"true"         |"MSC_BC_bkp1"  |0             |"2"               |"MSC_BC_bkp1"                          |['noInvocation':0]     |"CANCELLED"    |"FAILED"      |1              |null           |null
        "Failed to fetch credentials"   |null   |"2"                     |"MSC_BC_bkp0,MSC_BC_bkp1"|"true"       |"MSC_BC_bkp2"  |0             |"3"               |"MSC_BC_bkp0,MSC_BC_bkp1,MSC_BC_bkp2"  |['noInvocation':0]     |"FAILED"       |"FAILED"      |1              |null           |null

        null    |"b2ff92f1"                     |"1"                     |null                   |null           |"MSC_BC_bkp1"  |0             |"1"               |null                                   |['noInvocation':0]     |null           |null          |0              |"b2ff92f1"     |"true"
        null    |"b2ff92f1"                     |"2"                     |null                   |null           |"MSC_BC_bkp2"  |0             |"2"               |null                                   |['noInvocation':0]     |null           |null          |0              |"b2ff92f1"     |"true"
        null    |"b2ff92f1"                     |"2"                     |"MSC_BC_bkp0"          |null           |"MSC_BC_bkp2"  |0             |"2"               |"MSC_BC_bkp0"                          |['noInvocation':0]     |null           |null          |0              |"b2ff92f1"     |"true"
        null    |"b2ff92f1"                     |"2"                     |null                   |"true"         |"MSC_BC_bkp2"  |0             |"2"               |null                                   |['noInvocation':0]     |null           |null          |0              |"b2ff92f1"     |"true"
    }


    def "AXE Upload Job - Timeout step - winfiol invocations - single backup"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(CURRENT_BACKUP, "MSC_BC_bkp1")
        actProp.put(AxeConstants.COOKIE_HEADER, "WINFIOL_SERVERID=s1; path=/")
        activityJobId = createAxeUploadJob("MSC_BC_bkp1",generateActivityProp(actProp))
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(_) >> getUploadPollingResponse(respCode, message,0d)
        when: "performing the result evalation during timeout"
        objectUnderTest.asyncHandleTimeout(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("HANDLE_TIMEOUT"))

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBackups == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intermFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert("["+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', [:],JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        1* pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "uploadbackup", NODE_NAME);
        0 * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        where:
        respCode        |message        |jobResult      |failedBackups  |intermFailure  |log
        1               |null           |"SUCCESS"      |null           |null           |"message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
        2               |"file not found"|"FAILED"      |"MSC_BC_bkp1"  |"FAILED"       |"message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"file not found\", logLevel:ERROR]"
    }

    def "AXE Upload Job - Timeout step - winfiol invocations - repeated backups"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(CURRENT_BACKUP, currBackup)
        actProp.put(PROCESSED_BACKUPS, procsdCountOld)
        actProp.put(FAILED_BACKUPS, failedBackupsOld)
        actProp.put(INTERMEDIATE_FAILURE, intermFailOld)
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, canclTriggered)
        actProp.put(AxeConstants.COOKIE_HEADER, "WINFIOL_SERVERID=s1; path=/")

        activityJobId = createAxeUploadJob("MSC_BC_bkp0,MSC_BC_bkp1,MSC_BC_bkp2",generateActivityProp(actProp))
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(UploadBackupPollResponse.class) >> getUploadPollingResponse(respCode, msg,0d)
        when: "performing the result evalation during timeout"
        objectUnderTest.asyncHandleTimeout(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(jobResult!=null){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("HANDLE_TIMEOUT"))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert (procsdCountNew == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES)))
        failedBackups == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intermFailureNew == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert("["+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        actCorr * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', repeatVariables,JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        1* pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "uploadbackup", NODE_NAME);
        cancelCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        where:
        respCode |msg   |procsdCountOld |procsdCountNew    |failedBackupsOld   |canclTriggered |intermFailOld   |currBackup     |actCorr|cancelCorr|jobResult      |failedBackups           |intermFailureNew|repeatVariables        |log
        2        |"msg2"|"1"            |"2"               |null               |null           |null            |"MSC_BC_bkp1"  |1      |0         |null           |"MSC_BC_bkp1"           |"FAILED"        |['repeatExecute':true] |"message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"msg2\", logLevel:ERROR]"
        3        |"msg3"|"2"            |"3"               |"MSC_BC_bkp1"      |null           |null            |"MSC_BC_bkp2"  |1      |0         |"FAILED"       |"MSC_BC_bkp1,MSC_BC_bkp2"|"FAILED"       |[:]                    |"message:\"Upload\" activity has failed for backup \"MSC_BC_bkp2\". Failure reason: \"msg3\", logLevel:ERROR]"
        4        |"msg4"|"1"            |"2"               |null               |"true"         |null            |"MSC_BC_bkp1"  |0      |1         |"CANCELLED"    |"MSC_BC_bkp1"           |"FAILED"        |[:]                    |"message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"msg4\", logLevel:ERROR]"
        1        |"msg1"|"1"            |"2"               |null               |null           |null            |"MSC_BC_bkp1"  |1      |0         |null           |null                    |null            |['repeatExecute':true] |"message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
        1        |"msg1"|"2"            |"3"               |null               |null           |null            |"MSC_BC_bkp2"  |1      |0         |"SUCCESS"      |null                    |null            |[:]                    |"message:\"Upload\" activity for the backup \"MSC_BC_bkp2\" is completed successfully., logLevel:INFO]"
        1        |"msg1"|"2"            |"3"               |"MSC_BC_bkp1"      |null           |"FAILED"        |"MSC_BC_bkp2"  |1      |0         |"FAILED"       |"MSC_BC_bkp1"           |"FAILED"        |[:]                    |"message:\"Upload\" activity for the backup \"MSC_BC_bkp2\" is completed successfully., logLevel:INFO]"
        1        |"msg1"|"1"            |"2"               |null               |"true"         |null            |"MSC_BC_bkp1"  |0      |1         |"CANCELLED"    |null                    |null            |[:]                    |"message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
    }

    def "AXE Upload Job - Timeout step - throws exception"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeUploadJob("MSC_BC_bkp0",Collections.emptyList())
        winFIOLRequestDispatcher.initiateRestCall(_) >> { throw new JobDataNotFoundException("Test Groovy exception") }

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
        assert("[message:\"Upload\" action has failed in handle timeout. Reason: \"Test Groovy exception\"., logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', [:],JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
    }

    def "AXE Upload Job - Timeout step Timedout"() {
        given : "AXE Backup upload job has been created"
        activityJobId = createAxeUploadJob("MSC_BC_bkp0",Collections.emptyList())

        when: "performing the timeout expiry"
        objectUnderTest.timeoutForAsyncHandleTimeout(activityJobId);

        then: "the job should get updated to failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == activityJob.getAttribute(ShmConstants.STEP_DURATIONS)
        assert("[message:Failed to get the \"Upload\" activity status from the node within \"0\" minutes. Failing the Activity., logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
    }


    def generateActivityProp(Map<String, String> jobProperties){
        List properties = new ArrayList<>()
        for(Entry<String, Object> prop:jobProperties.entrySet()){

            if(prop.getValue()!=null){
                Map<String, Object> activityProp = new HashMap<>();
                activityProp.put("key",prop.getKey())
                activityProp.put("value",prop.getValue())
                properties.add(activityProp)
            }
        }
        return properties
    }

    def "AXE Upload Job - SubscribeForPolling"() {
        given : "AXE Backup upload job has been created"
        if(createJob){
            activityJobId = createAxeUploadJob("MSC_BC_bkp0",Collections.emptyList())
        }else{
            activityJobId = 0
        }
        if(throwException){
            pollingActivityManager.subscribe(_,_,_,_,_) >> {throw new EJBException("test")}
        }

        when: "subscribing for polling"
        objectUnderTest.subscribeForPolling(activityJobId)

        then:""
        subscribes * pollingActivityManager.subscribe(_, _, null, _, Collections.<String> emptyList());
        dps * dpsStatusInfoProvider.isDatabaseAvailable()
        cache * pollingActivityManager.prepareAndAddPollingActivityDataToCache(_, _);

        where:
        createJob       |throwException |dpsAvaialable       |subscribes     |dps    |cache
        true            |false          |false               |1              |0      |0
        //        true            |true           |false               |0              |1      |0
        //        true            |true           |false               |0              |1      |0
        //        true            |true           |true                |0              |1      |1
        false           |false          |false               |0              |0      |0
    }

    def "AXE Upload Job - process polling response - single backup"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(CURRENT_BACKUP, "MSC_BC_bkp1")
        activityJobId = createAxeUploadJob("MSC_BC_bkp1",generateActivityProp(actProp))
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(UploadBackupPollResponse.class) >> getUploadPollingResponse(respCode, message, progress)
        when: "performing the result evalation during timeout"
        objectUnderTest.processPollingResponse(activityJobId, null);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(unsub>0){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("PROCESS_NOTIFICATION"))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBackups == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intermFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        actCorr * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7',[:]);
        unsub * pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "uploadbackup", NODE_NAME);
        canclCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        where:
        respCode |message |progress      |jobResult      |failedBackups  |intermFailure  |actCorr |canclCorr |unsub |log
        4        |null    |0d            |null           |null           |null           |0       |0         |0     |"[message:Upload progress for \"MSC_BC_bkp1\" is not available currently, logLevel:INFO]"
        3        |"failed"|25d           |"FAILED"       |"MSC_BC_bkp1"  |"FAILED"       |1       |0         |1     |"[message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"failed\", logLevel:ERROR]"
        2        |null    |30d           |null           |null           |null           |0       |0         |0     |"[message:Upload is ongoing for \"MSC_BC_bkp1\", logLevel:INFO]"
        1        |null    |100d          |"SUCCESS"      |null           |null           |1       |0         |1     |"[message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
    }

    def "AXE Upload Job - process polling response - repeated backups"() {
        given : "AXE Backup upload job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(CURRENT_BACKUP, currBackup)
        actProp.put(PROCESSED_BACKUPS, procsdCountOld)
        actProp.put(FAILED_BACKUPS, failedBackupsOld)
        actProp.put(INTERMEDIATE_FAILURE, intermFailOld)
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, canclTriggered)

        activityJobId = createAxeUploadJob("MSC_BC_bkp0,MSC_BC_bkp1,MSC_BC_bkp2",generateActivityProp(actProp))
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(UploadBackupPollResponse.class) >> getUploadPollingResponse(respCode, message, progress)
        when: "performing the result evalation during timeout"
        objectUnderTest.processPollingResponse(activityJobId, null);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(unsub>0){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("PROCESS_NOTIFICATION"))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBackups == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intermFailureNew == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        assert (procsdCountNew == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES)))
        actCorr * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7',repeatVariables);
        canclCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');
        unsub * pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "uploadbackup", NODE_NAME);

        where:
        respCode |progress |message |procsdCountOld |procsdCountNew |failedBackupsOld   |canclTriggered |intermFailOld   |currBackup     |jobResult      |failedBackups           |intermFailureNew|repeatVariables       |actCorr |canclCorr |unsub |log
        3        |10d      |"msg1"  |"1"            |"2"            |null               |null           |null            |"MSC_BC_bkp1"  |null           |"MSC_BC_bkp1"           |"FAILED"        |['repeatExecute':true]|1       |0         |1     |"[message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"msg1\", logLevel:ERROR]"
        3        |10d      |"msg2"  |"2"            |"3"            |"MSC_BC_bkp1"      |null           |null            |"MSC_BC_bkp2"  |"FAILED"       |"MSC_BC_bkp1,MSC_BC_bkp2"|"FAILED"       |[:]                   |1       |0         |1     |"[message:\"Upload\" activity has failed for backup \"MSC_BC_bkp2\". Failure reason: \"msg2\", logLevel:ERROR]"
        3        |10d      |"msg3"  |"1"            |"2"            |null               |"true"         |null            |"MSC_BC_bkp1"  |"CANCELLED"    |"MSC_BC_bkp1"           |"FAILED"        |[:]                   |0       |1         |1     |"[message:\"Upload\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"msg3\", logLevel:ERROR]"
        1        |100d     |null    |"1"            |"2"            |null               |null           |null            |"MSC_BC_bkp1"  |null           |null                    |null            |['repeatExecute':true]|1       |0         |1     |"[message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
        1        |100d     |null    |"2"            |"3"            |null               |null           |null            |"MSC_BC_bkp2"  |"SUCCESS"      |null                    |null            |[:]                   |1       |0         |1     |"[message:\"Upload\" activity for the backup \"MSC_BC_bkp2\" is completed successfully., logLevel:INFO]"
        1        |100d     |null    |"2"            |"3"            |"MSC_BC_bkp1"      |null           |"FAILED"        |"MSC_BC_bkp2"  |"FAILED"       |"MSC_BC_bkp1"           |"FAILED"        |[:]                   |1       |0         |1     |"[message:\"Upload\" activity for the backup \"MSC_BC_bkp2\" is completed successfully., logLevel:INFO]"
        1        |100d     |null    |"1"            |"2"            |null               |"true"         |null            |"MSC_BC_bkp1"  |"CANCELLED"    |null                    |null            |[:]                   |0       |1         |1     |"[message:\"Upload\" activity for the backup \"MSC_BC_bkp1\" is completed successfully., logLevel:INFO]"
        4        |0d       |null    |"1"            |"1"            |null               |null           |null            |"MSC_BC_bkp1"  |null           |null                    |null            |['no invocation':0]   |0       |0         |0     |"[message:Upload progress for \"MSC_BC_bkp1\" is not available currently, logLevel:INFO]"
        4        |0d       |null    |"1"            |"1"            |null               |"true"         |null            |"MSC_BC_bkp1"  |null           |null                    |null            |['no invocation':0]   |0       |0         |0     |"[message:Upload progress for \"MSC_BC_bkp1\" is not available currently, logLevel:INFO]"
        2        |20d      |null    |"1"            |"1"            |null               |null           |null            |"MSC_BC_bkp1"  |null           |null                    |null            |['no invocation':0]   |0       |0         |0     |"[message:Upload is ongoing for \"MSC_BC_bkp1\", logLevel:INFO]"
        2        |20d      |null    |"1"            |"1"            |null               |"true"         |null            |"MSC_BC_bkp1"  |null           |null                    |null            |['no invocation':0]   |0       |0         |0     |"[message:Upload is ongoing for \"MSC_BC_bkp1\", logLevel:INFO]"
    }


    def  UploadBackupPollResponse getUploadPollingResponse(final int status,final String statusMsg, final double progress){
        UploadBackupPollResponse uploadResponse = new UploadBackupPollResponse();
        uploadResponse.setStatus(status)
        uploadResponse.setStatusMsg(statusMsg)
        uploadResponse.setPercentageDone(progress)
        return uploadResponse;
    }
}
