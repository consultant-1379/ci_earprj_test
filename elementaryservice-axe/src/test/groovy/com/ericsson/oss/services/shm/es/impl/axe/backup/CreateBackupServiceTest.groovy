/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup
import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*

import javax.ejb.EJBException

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
public class CreateBackupServiceTest extends AbstractAxeBackupServiceTest{

    @ObjectUnderTest
    private CreateBackupService objectUnderTest;

    private long activityJobId ;

    def setup() {
        addNetworkElementMOs(PARENT_NODE_NAME)
        platformProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.AXE
        cryptographyService.encrypt(_)>>"axePassword".getBytes(AxeConstants.CHAR_ENCODING);
        cryptographyService.decrypt(_)>>"axePassword".getBytes();
    }

    def long createAxeCreateBackupJob(String backupNames,List activityProperties,String nodeName,List<Map<String,Object>> neTypeJobProperties){
        Map<String, Object> jobTemplateData = new HashMap<>();
        Map<String, Object> mainJobData = new HashMap<>();
        Map<String, Object> neJobData = new HashMap<>();
        Map<String, Object> activityJobData = new HashMap<>();
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        Map<String, Object> neTypeProp = new HashMap<>();
        Map<String, Object> jobProp = new HashMap<>();

        Map<String, Object> neJobAttributesMap = new HashMap<>();
        List<Map<String, Object>> jobProperties = new ArrayList<>();

        jobProp.put(ShmConstants.KEY, "backupNames")
        jobProp.put(ShmConstants.VALUE, backupNames)
        neTypeProp.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC)
        if(!neTypeJobProperties.isEmpty()){
            neTypeJobProperties.add(jobProp)
            neTypeProp.put(ShmConstants.JOBPROPERTIES, neTypeJobProperties)
        }else{
            neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
        }
        jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        neJobAttributesMap.put(ShmConstants.NE_NAME,nodeName)
        Map<String, Object> jobParameters1= new HashMap<>();
        Map<String, Object> jobParameters2= new HashMap<>();
        Map<String, Object> jobParameters3= new HashMap<>();
        jobParameters1.put(ShmConstants.KEY, ShmConstants.IS_COMPONENT_JOB)
        jobParameters1.put(ShmConstants.VALUE, "true")
        jobParameters2.put(ShmConstants.KEY, ShmConstants.PARENT_NAME)
        jobParameters2.put(ShmConstants.VALUE, PARENT_NODE_NAME)
        jobProperties.add(jobParameters1)
        jobProperties.add(jobParameters2)
        neJobAttributesMap.put(ShmConstants.JOBPROPERTIES, jobProperties)
        jobConfigurationDetails.put(ShmConstants.NEJOB_PROPERTIES, Arrays.asList(neJobAttributesMap))
        final Map<String, Object> mainJobPropertyMap = new HashMap<String, Object>()
        mainJobPropertyMap.put(ShmConstants.KEY, "jobName")
        mainJobPropertyMap.put(ShmConstants.VALUE, "BackupJob_administrator_08012019123456")
        mainJobData.put(ShmConstants.JOBPROPERTIES, Arrays.asList(mainJobPropertyMap))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);
        neJobData.put(ShmConstants.NE_NAME,nodeName);
        neJobData.put(AxeConstants.INPUT_BACKUP_NAMES,"MSC_BC_bkp1");
        neJobData.put(ShmConstants.JOBPROPERTIES, jobProperties)
        activityJobData.put(ShmConstants.ACTIVITY_NAME, "createbackup")
        activityJobData.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobData.put(ShmConstants.JOBPROPERTIES, activityProperties)
        activityJobData.put(ShmConstants.PROGRESSPERCENTAGE, 0d)
        jobTemplateData.put("owner", "AXEUser")
        jobTemplateData.put("jobType","BACKUP")
        jobConfigurationDetails.put("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE"))
        jobTemplateData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);
        return persistJobs(jobTemplateData, mainJobData, neJobData, activityJobData)
    }

    def "AXE CreateBackup Job - Execute step - error cases - no invocation"() {
        given : "AXE Create Backup job has been created"
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp1",Collections.emptyList(),NODE_NAME,Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> tbacResponse
        winFIOLRequestDispatcher.initiateRestCall(_) >> { throw exception }
        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);
        then:"job should get failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        0.0 == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
        1 * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7', [:]);
        where:
        tbacResponse    |exception                                            |log
        false           |new JobDataNotFoundException("Test Groovy exception")|null
    }

    def "AXE Create Backup Job - Execute step - winfiol invocations"() {
        given : "AXE Backup job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, cancelTriggered)
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp1",generateActivityProp(actProp),NODE_NAME,Collections.emptyList())
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
        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        sessionId == extractJobProperty(SESSION_ID, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        activityTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)).contains(log))
        correlations * workflowInstanceNotifier.sendActivate(_, _);
        cancelCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');
        where:
        actSessionId    |actError                       |jobResult  |activityTriggered |cancelTriggered|sessionId  |correlations|cancelCorr |log
        "b2ff92f1"      |null                           |null       |"true"            |     null      |"b2ff92f1" |0           | 0         |"message:\"Createbackup\" activity is triggered with option \"Create new backup\" (timeout = \"0\" minutes)."
        null            |"Failed to fetch credentials"  |"FAILED"   |null              |     null      |null       |1           | 0         |"message:\"Createbackup\" activity has failed. Failure reason: \"Failed to fetch credentials\""
        null            |"Failed to fetch credentials"  |"CANCELLED"|null              |   "true"      |null       |0           | 1         |"message:\"Createbackup\" activity has failed. Failure reason: \"Failed to fetch credentials\""
    }

    def "AXE Create Backup Job - Timeout step after winfiol invocations"() {
        given : "AXE Backup cretae job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(HOST_NAME, "winfiol-svc-1")
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, cancelTriggered)
        actProp.put(COOKIE_HEADER, "WINFIOL_SERVERID=s1; path=/")
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp1",generateActivityProp(actProp),nodeName,generateNeTypeJobProperties(rotate,overwrite, null))
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse
        clientResponse.getEntity(_) >> getPollReponse(backupName,backupStatus,percentageDone,componentName)
        when: "performing the result evalation during timeout"
        objectUnderTest.asyncHandleTimeout(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        def neJob = runtimeDps.stubbedDps.liveBucket.findPoById(neJob.getPoId())
        null != neJob.getAttribute(ShmConstants.JOBPROPERTIES)
        backupFolderAfterRotate == extractJobProperty(AxeConstants.INPUT_BACKUP_NAMES, neJob.getAttribute(ShmConstants.JOBPROPERTIES))
        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)).contains(log))
        correlations * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance(_, [:],JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        cancelCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, _);
        unsubscribe * pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "createbackup", _);
        where:
        backupStatus            | nodeName           |componentName|percentageDone |jobResult  |backupName   |rotate|overwrite|backupFolderAfterRotate|correlations |cancelTriggered |cancelCorr|unsubscribe|log
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|false |false    |"MSC_BC_bkp1"          |   1         |null            |0         |  1        |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|null  |null     |"MSC_BC_bkp1"          |   1         |null            |0         |  1        |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|false |false    |"MSC_BC_bkp1"          |   1         |null            |0         |  1        |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|false |true     |"MSC_BC_bkp1"          |   1         |null            |0         |  1        |"message:Overwrite backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|true  |false    |"RELFSW0"              |   1         |null            |0         |  1        |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" and Rotate backup to RELFSW0 is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|true  |true     |"RELFSW0"              |   1         |null            |0         |  1        |"message:Overwrite backup with BackupName = \"MSC_BC_bkp1\" and Rotate backup to RELFSW0 is completed succesfully"
        "BRM_BACKUP_COMPLETE"   |"MSC-BC-BSP-01__APG"|"ap"         | 100            |"SUCCESS"  |"MSC_BC_bkp1"|true  |false    |"MSC_BC_bkp1"          |   1         |null            |0         |  1        |"message:\"Createbackup\" activity for the backup \"MSC_BC_bkp1\" is completed successfully"
        "Backup ongoing"        |"MSC-BC-BSP-01__CP1"|"cp"         | 25             |"FAILED"   |"MSC_BC_bkp1"|false |false    |null                   |   1         |null            |0         |  1        |"message:Failing \"Createbackup\" Activity as it is taking more than expected time."
        "BRM_BACKUP_INCOMPLETE" |"MSC-BC-BSP-01__CP1"|"cp"         | 100            |"FAILED"   |"MSC_BC_bkp1"|false |false    |null                   |   1         |null            |0         |  1        |"message:\"Createbackup\" activity has failed."
        "BACKUP_CORRUPT"        |"MSC-BC-BSP-01__CP1"|"cp"         | 10             |"CANCELLED"|"MSC_BC_bkp1"|false |false    |null                   |   0         |"true"          |1         |  1        |"message:\"Createbackup\" activity is cancelled successfully."
    }

    def "AXE Create Backup Job - Process Polling step after winfiol invocations"() {
        given : "AXE Backup create job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(SESSION_ID, "abc")
        actProp.put(HOST_NAME, "winfiol-svc-1")
        actProp.put(COOKIE_HEADER, "WINFIOL_SERVERID=s1; path=/")
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, cancelTriggered)
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp1",generateActivityProp(actProp),nodeName ,generateNeTypeJobProperties(rotate,overwrite,null))

        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(_) >> getPollReponse(backupName,backupStatus,percentageDone,componentName);
        when: "performing the result evalation during Polling"
        objectUnderTest.processPollingResponse(activityJobId,Collections.emptyMap());
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        def neJob = runtimeDps.stubbedDps.liveBucket.findPoById(neJob.getPoId())
        null != neJob.getAttribute(ShmConstants.JOBPROPERTIES)
        backupFolderAfterRotate == extractJobProperty(AxeConstants.INPUT_BACKUP_NAMES, neJob.getAttribute(ShmConstants.JOBPROPERTIES))
        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        assert(extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)).contains(log))
        correlations * workflowInstanceNotifier.sendActivate(_, _);
        cancelCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, _);
        unsubscribe * pollingActivityManager.unsubscribeByActivityJobId(activityJobId, "createbackup", _);
        where:
        backupStatus           | nodeName           |componentName|percentageDone  |jobResult   |backupName   |rotate|overwrite|backupFolderAfterRotate|cancelTriggered|correlations  |cancelCorr|unsubscribe|log
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|false |false    |"MSC_BC_bkp1"          |  null         |1             | 0      |1          |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|false |false    |"MSC_BC_bkp1"          |  null         |1             | 0      |1          |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|false |true     |"MSC_BC_bkp1"          |  null         |1             | 0      |1          |"message:Overwrite backup with BackupName = \"MSC_BC_bkp1\" is completed succesfully"
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|true  |false    |"RELFSW0"              |  null         |1             | 0      |1          |"message:Create new backup with BackupName = \"MSC_BC_bkp1\" and Rotate backup to RELFSW0 is completed succesfully"
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|true  |true     |"RELFSW0"              |  null         |1             | 0      |1          |"message:Overwrite backup with BackupName = \"MSC_BC_bkp1\" and Rotate backup to RELFSW0 is completed succesfully"
        "Backup ongoing"       |"MSC-BC-BSP-01__CP1"|"cp"         | 25             | null       |"MSC_BC_bkp1"|false |false    |null                   |  null         |0             | 0      |0          |"message:ProgressInfo : BackupName=MSC_BC_bkp1, Status=Backup ongoing"
        "BRM_BACKUP_CORRUPT"   |"MSC-BC-BSP-01__CP1"|"cp"         | 100            | "FAILED"   |"MSC_BC_bkp1"|false |false    |null                   |  null         |1             | 0      |1          |"message:\"Createbackup\" activity has failed."
        "BACKUP_INCOMPLETE"    |"MSC-BC-BSP-01__CP1"|"cp"         | 70             |"CANCELLED" |"MSC_BC_bkp1"|false |false    |null                   |  "true"       |0             | 1      |1          |"message:\"Createbackup\" activity is cancelled successfully."
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__APG"|"ap"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|true  |true     |"MSC_BC_bkp1"          |  null         |1             | 0      |1          |"message:\"Createbackup\" activity for the backup \"MSC_BC_bkp1\" is completed successfully"
        "BRM_BACKUP_COMPLETE"  |"MSC-BC-BSP-01__APG"|"ap"         | 100            | "SUCCESS"  |"MSC_BC_bkp1"|true  |false    |"MSC_BC_bkp1"          |  null         |1             | 0      |1          |"message:\"Createbackup\" activity for the backup \"MSC_BC_bkp1\" is completed successfully"
    }

    def "AXE Create Job - HandleTimeout step Timedout"() {
        given : "AXE Backup create job has been created"
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp0",Collections.emptyList(),NODE_NAME,Collections.emptyList())
        when: "performing the timeout expiry"
        objectUnderTest.timeoutForAsyncHandleTimeout(activityJobId);
        then: "the job should get updated to failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == activityJob.getAttribute(ShmConstants.STEP_DURATIONS)
        assert("[message:Failed to get the \"Createbackup\" activity status from the node within \"0\" minutes. Failing the Activity., logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
    }

    def "AXE Create Job - SubscribeForPolling"() {
        given : "AXE Backup create job has been created"
        if(createJob){
            activityJobId = createAxeCreateBackupJob("MSC_BC_bkp0",Collections.emptyList(),NODE_NAME,Collections.emptyList())
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
        false           |false          |false               |0              |0      |0
    }

    def  Map<String, Object> getPollReponse(final String backupName,final String backupStatus,final int percentageDone,final String componentName){
        Map<String, Object> createBackupPollResponseMap =new HashMap<>();
        Map<String, Object> backupPollResponseMap =new LinkedHashMap<>();
        backupPollResponseMap.put("backupName", backupName);
        backupPollResponseMap.put("status", backupStatus);
        backupPollResponseMap.put("percentageDone", percentageDone);
        createBackupPollResponseMap.put(componentName, backupPollResponseMap);
        return createBackupPollResponseMap;
    }

    def "AXE Create Backup Job - Execute step - winfiol invocations with Rotate and Overwrite options"() {
        given : "AXE Backup job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, null)
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp1",generateActivityProp(actProp),nodeName,generateNeTypeJobProperties(rotate,overwrite, password))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(_) >> getSessionIdResponse(actSessionId,actError);
        Map<String,Object> cookieMap = new HashMap();
        cookieMap.put("Set-Cookie",["WINFIOL_SERVERID=s1; path=/"]);
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();
        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        assert(extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)).contains(log))
        where:
        actSessionId    |nodeName                    |rotate |overwrite |password  | actError  |log
        "b2ff92f1"      |"MSC-BC-BSP-01__CP1"        |true   |false     |null      |  null     |"message:\"Createbackup\" activity is triggered with option \"Create new backup and Rotate backup\" (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__CP1"        |true   |true      | null     |  null     |"message:\"Createbackup\" activity is triggered with option \"Overwrite backup and Rotate backup\" (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01-AXE_CLUSTER" |false  |false     | null     |  null     |"message:\"Createbackup\" activity is triggered with option \"Create new backup\" (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01-AXE_CLUSTER" |false  |true      | null     |  null     |"message:\"Createbackup\" activity is triggered with option \"Overwrite backup\" (timeout = \"0\" minutes)."
    }
 
    
    def "AXE Create Backup Job - Execute step - winfiol invocations with Password options"()     {
        given : "AXE Backup job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, null)
        activityJobId = createAxeCreateBackupJob("MSC_BC_bkp2",generateActivityProp(actProp),nodeName,generateNeTypeJobProperties(rotate,overwrite, password))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(_) >> getSessionIdResponse(actSessionId,actError);
        axeApgProductRevisionProvider.getApgComponentsProductRevision(_) >> getNeApgVersionData(nodeName,apgVersion);
        Map<String,Object> cookieMap = new HashMap();
        cookieMap.put("Set-Cookie",["WINFIOL_SERVERID=s1; path=/"]);
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();
        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        assert(extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)).contains(log))
        where:
        actSessionId    |nodeName                   |rotate  |overwrite |password  |apgVersion  |actError |log
        "b2ff92f1"      |"MSC-BC-BSP-01__CP1"       |false   |false     | null     | null       |null     |"message:\"Createbackup\" activity is triggered with option \"Create new backup\" (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG37"     |false   |false     | "netsim" | "3.7.0"    |null     |"message:\"Createbackup\" activity is triggered to create secure backup (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01-AXE_CLUSTER"|false   |false     | null     | null       |null     |"message:\"Createbackup\" activity is triggered with option \"Create new backup\" (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG_2"     |false   |false     | "netsim" | null       |null     |"message:Failed to read APG Version from inventory, make sure inventory data is in sync."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG35"     |false   |false     | "netsim" | "3.6.0"    |null     |"message:\"Createbackup\" activity is triggered to create regular backup (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG"       |false   |false     | null     | "3.7.0"    |null     |"message:\"Createbackup\" activity is triggered to create regular backup (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG"       |false   |false     | "netsim" | "3.7.0"    |null     |"message:\"Createbackup\" activity is triggered to create secure backup (timeout = \"0\" minutes)."
        "b2ff92f1"      |"MSC-BC-BSP-01__APG1"      |false   |false     | "netsim" | "R8B"      |null     |"message:Unexpected APG version received: \"R8B\""
        }
}