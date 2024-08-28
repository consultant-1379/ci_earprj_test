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

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.resources.Resource
import com.ericsson.oss.services.shm.common.FileResource
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse
import com.ericsson.oss.services.shm.jobs.common.constants.*
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
/**
 * 
 * @author xrajeke
 *
 */
public class DeleteBackupServiceCDITest extends AbstractAxeBackupServiceTest{

    @ObjectUnderTest
    private DeleteBackupService objectUnderTest;

    @MockedImplementation
    private FileResource fileResource;

    @MockedImplementation
    private SmrsFileStoreService smrsServiceUtil;

    @MockedImplementation
    private Resource resource;


    private long activityJobId ;

    def setup() {
        addNetworkElementMOs(PARENT_NODE_NAME)
        platformProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.AXE
        cryptographyService.encrypt(_)>>"axePassword".getBytes(AxeConstants.CHAR_ENCODING);
        cryptographyService.decrypt(_)>>"axePassword".getBytes();
    }

    def long createAxeDeleteBackupJob(String backupNames,List activityProperties){
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

        jobProp.put(ShmConstants.KEY, BACKUP_NAME)
        jobProp.put(ShmConstants.VALUE, backupNames)
        neTypeProp.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC)
        neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
        jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        jobConfigurationDetails.put("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE"))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);

        neJobProp.put(ShmConstants.KEY, ShmConstants.PARENT_NAME)
        neJobProp.put(ShmConstants.VALUE, PARENT_NODE_NAME)
        neJobData.put(ShmConstants.NE_NAME,NODE_NAME);
        neJobData.put(ShmConstants.JOBPROPERTIES,  Arrays.asList(neJobProp));
        neJobData.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC);

        activityJobData.put(ShmConstants.ACTIVITY_NAME, "deletebackup")
        activityJobData.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobData.put(ShmConstants.JOBPROPERTIES, activityProperties)
        activityJobData.put(ShmConstants.PROGRESSPERCENTAGE, 0d)


        return persistJobs(jobTemplateData, mainJobData, neJobData, activityJobData)
    }

    def "AXE DeleteBackup Job - Execute step - error cases - no invocation"() {
        given : "AXE Delete Backup job has been created"
        activityJobId = createAxeDeleteBackupJob(backupNames,Collections.emptyList())
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
        tbacResponse    |backupNames        |exception                                            |log
        false           |null               |new Exception("unused exception")                    |null
        true            |"MSC_BC_bkp1|ENM"  |new Exception("unused exception")                    |"[message:\"Delete Backup\" activity initiated. , logLevel:INFO, message:Executing \"Delete Backup\" activity on backup file = \"MSC_BC_bkp1\"., logLevel:INFO, message:\"Delete Backup\" activity has failed for backup \"MSC_BC_bkp1\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        true            |"MSC_BC_bkp1|NODE" |new JobDataNotFoundException("Test Groovy exception")|"[message:\"Delete Backup\" activity initiated. , logLevel:INFO, message:Executing \"Delete Backup\" activity on backup file = \"MSC_BC_bkp1\"., logLevel:INFO, message:Database service is not accessible., logLevel:ERROR]"
        true            |"MSC_BC_bkp1|NODE" |new MoNotFoundException("Test Groovy exception")     |"[message:\"Delete Backup\" activity initiated. , logLevel:INFO, message:Executing \"Delete Backup\" activity on backup file = \"MSC_BC_bkp1\"., logLevel:INFO, message:Unable to trigger \"Delete Backup\" activity on the node. Failure reason: \"Test Groovy exception\", logLevel:ERROR]"
    }

    def "AXE DeleteBackup Job - Execute step"() {
        given : "AXE Delete Backup  job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(PROCESSED_BACKUPS, processedCountOld)
        actProp.put(FAILED_BACKUPS, failedBackupsOld)
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, triggerCancel)
        actProp.put(ActivityConstants.IS_ACTIVITY_TRIGGERED, actTriggeredOld)
        activityJobId = createAxeDeleteBackupJob(backupNames, generateActivityProp(actProp))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(SessionIdResponse.class) >> getSessionIdResponse(null,actError)
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString();
        smrsServiceUtil.getSmrsPath(_,_,_) >> "/home/smrs/smrsroot/backup/"
        fileResource.getFileNamesFromDirectory(_ as String) >> Arrays.asList(curBkp)
        fileResource.delete(_) >> fileDeleted
        when: "performing the execute step"
        objectUnderTest.execute(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(actTriggered == "true"){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("EXECUTE=0."))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBkpsNew == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        curBkp == extractJobProperty(CURRENT_BACKUP, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        procsdNew == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        totalBkps == extractJobProperty(TOTAL_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intrmFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        actTriggered == extractJobProperty(ActivityConstants.IS_ACTIVITY_TRIGGERED, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))

        def preLog = actTriggeredOld == null ? "[message:\"Delete Backup\" activity initiated. , logLevel:INFO, ":"["
        assert(preLog + "message:Executing \"Delete Backup\" activity on backup file = \""+curBkp+"\"., logLevel:INFO, " + log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        actCorr * workflowInstanceNotifier.sendActivate('MSC-BC-BSP-01__CP1@7', repeatVariables);
        canclCorr * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        //these calls can not be verified (groovy might have a limitation on mocks) 1 * fileResource.delete(_) && 1 * winFIOLRequestDispatcher.initiateRestCall(_,SessionIdResponse.class)

        where:
        loc   |backupNames |actTriggeredOld  |actError |processedCountOld |fileDeleted |failedBackupsOld |triggerCancel |curBkp          |actCorr |procsdNew |totalBkps |failedBkpsNew |repeatVariables |jobResult |intrmFailure |canclCorr |actTriggered |progress |log
        "ENM" |"bkp0|ENM"  |null             |"unUsed" |null              |true        |null             |null          |"bkp0"          |1       |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"
        "ENM" |"bkp0|ENM"  |null             |"unUsed" |null              |false       |null             |null          |"bkp0"          |1       |"1"       |"1"       |"bkp0|ENM"    |[:]             |"FAILED"  |"FAILED"     |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"bkp0\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"bkp0|ENM"  |"true"           |"unUsed" |null              |true        |null             |null          |"bkp0"          |1       |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"

        "NODE"|"bkp0|NODE" |null             |"XXX"    |null              |true        |null             |null          |"bkp0"          |1       |"1"       |"1"       |"bkp0|NODE"   |[:]             |"FAILED"  |"FAILED"     |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"bkp0\". Failure reason: \"XXX\", logLevel:ERROR]"
        "NODE"|"bkp0|NODE" |null             |null     |null              |true        |null             |null          |"bkp0"          |1       |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"
        "NODE"|"bkp0|NODE" |null             |"File was not found"|null   |true        |null             |null          |"bkp0"          |1       |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"

        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |null  |"unUsed" |null              |true        |null             |null          |"b0"            |1       |"1"       |"3"       |null          |['repeatExecute':true]|null|null         |0         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |null  |"unUsed" |"1"               |false       |"b0|ENM"         |null          |"b1"            |1       |"2"       |"3"       |"b0|ENM,b1|ENM"|['repeatExecute':true]|null|"FAILED"    |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b1\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |null  |"unUsed" |"2"               |false       |"b0|ENM"         |null          |"b2"            |1       |"3"       |"3"       |"b0|ENM,b2|ENM"|[:]            |"FAILED"  |"FAILED"     |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |null  |"unUsed" |"2"               |true        |null             |null          |"b2"            |1       |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"true"|"unUsed" |"2"               |true        |null             |"true"        |"b2"            |0       |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |1         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |null  |"unUsed" |null              |true        |null             |"true"        |"b0"            |0       |"1"       |"3"       |null          |[:]             |"CANCELLED"|null        |1         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"

        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|"XXX"    |null              |false       |null             |null          |"b0"            |1       |"1"       |"3"       |"b0|NODE"     |['repeatExecute':true]|null|"FAILED"     |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b0\". Failure reason: \"XXX\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|"YYY"    |"1"               |false       |"b0|NODE"        |null          |"b1"            |1       |"2"       |"3"       |"b0|NODE,b1|NODE"|['repeatExecute':true]|null|"FAILED"  |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b1\". Failure reason: \"YYY\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|""       |"2"               |false       |"b0|NODE"        |null          |"b2"            |1       |"3"       |"3"       |"b0|NODE,b2|NODE"|[:]            |"FAILED"  |"FAILED"   |0         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|null     |"2"               |true        |null             |null          |"b2"            |1       |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |0         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|"unreach"|"2"               |true        |null             |"true"        |"b2"            |0       |"3"       |"3"       |"b2|NODE"     |[:]             |"FAILED"  |"FAILED"     |1         |null         |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"unreach\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|null|"File was not found" |null  |true        |null             |"true"        |"b0"            |0       |"1"       |"3"       |null          |[:]             |"CANCELLED"|null        |1         |"true"       |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"
    }

    def "AXE DeleteBackup Job - Timeout step - winfiol invocations"() {
        given : "AXE Delete Backup  job has been created"
        Map<String, Object> actProp = new HashMap<>();
        actProp.put(PROCESSED_BACKUPS, processedCountOld)
        actProp.put(FAILED_BACKUPS, failedBackupsOld)
        actProp.put(CURRENT_BACKUP, curBkp+"|"+loc)
        actProp.put(TOTAL_BACKUPS, totalBkps)
        actProp.put(ActivityConstants.IS_CANCEL_TRIGGERED, triggerCancel)
        activityJobId = createAxeDeleteBackupJob(backupNames, generateActivityProp(actProp))
        activityJobTBACValidator.validateTBAC(_, _, _, _) >> true;
        winFIOLRequestDispatcher.initiateRestCall(_) >> clientResponse;
        clientResponse.getEntity(SessionIdResponse.class) >> getSessionIdResponse(null,actError)
        clientResponse.getMetadata() >> setCookieData();
        clientResponse.getMetadata().get("Set-Cookie") >> setCookieData().get("Set-Cookie");
        clientResponse.getMetadata().get("Set-Cookie").get(0).toString() >> setCookieData().get("Set-Cookie").get(0).toString() ;
        smrsServiceUtil.getSmrsPath(_,_,_) >> "/home/smrs/smrsroot/backup/"
        fileResource.getFileNamesFromDirectory(_ as String) >> Arrays.asList(matchingBakp)
        when: "performing the timeout step"
        objectUnderTest.asyncHandleTimeout(activityJobId);
        then:"results should be as expected"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)

        null != activityJob.getAttribute(ShmConstants.LOG)
        null != activityJob.getAttribute(ShmConstants.JOBPROPERTIES)
        progress == activityJob.getAttribute(ShmConstants.PROGRESSPERCENTAGE)
        if(jobResult!=null){
            assert(activityJob.getAttribute(ShmConstants.STEP_DURATIONS).toString().contains("HANDLE_TIMEOUT"))
        }

        jobResult == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        failedBkpsNew == extractJobProperty(FAILED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        procsdNew == extractJobProperty(PROCESSED_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        totalBkps == extractJobProperty(TOTAL_BACKUPS, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        intrmFailure == extractJobProperty(INTERMEDIATE_FAILURE, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))

        assert("["+log == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))

        1 * workflowInstanceNotifier.sendPrecheckOrTimeoutMsgToWfsInstance('MSC-BC-BSP-01__CP1@7', repeatVariables,JobVariables.TIMEOUT_FOR_HANDLE_TIMEOUT_COMPLETED);
        0 * workflowInstanceNotifier.sendAsynchronousMsgToWaitingWFSInstance(WorkFlowConstants.CANCEL_MOACTION_DONE, 'MSC-BC-BSP-01__CP1@7');

        //these calls can not be verified (groovy might have a limitation on mocks) 1 * fileResource.delete(_) && 1 * winFIOLRequestDispatcher.initiateRestCall(_,SessionIdResponse.class)

        where:
        loc   |backupNames         |matchingBakp |actError |processedCountOld |failedBackupsOld |triggerCancel |curBkp          |procsdNew |totalBkps |failedBkpsNew |repeatVariables |jobResult |intrmFailure |progress |log
        "ENM" |"bkp0|ENM"          |"bkp0"       |"unUsed" |null              |null             |null          |"bkp0"          |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"
        "ENM" |"bkp0|ENM"          |"X"          |"unUsed" |null              |null             |null          |"bkp0"          |"1"       |"1"       |"bkp0|ENM"    |[:]             |"FAILED"  |"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"bkp0\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"bkp0|ENM"          |"bkp0"       |"unUsed" |null              |null             |null          |"bkp0"          |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"

        "NODE"|"bkp0|NODE"         |"unused"     |"XXX"    |null              |null             |null          |"bkp0"          |"1"       |"1"       |"bkp0|NODE"   |[:]             |"FAILED"  |"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"bkp0\". Failure reason: \"XXX\", logLevel:ERROR]"
        "NODE"|"bkp0|NODE"         |"unused"     |null     |null              |null             |null          |"bkp0"          |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"
        "NODE"|"bkp0|NODE"         |"unused"     |"File was not found"|null   |null             |null          |"bkp0"          |"1"       |"1"       |null          |[:]             |"SUCCESS" |null         |100.0    |"message:\"Delete Backup\" activity for the backup \"bkp0\" is completed successfully., logLevel:INFO]"

        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"b0"      |"unUsed" |null              |null             |null          |"b0"            |"1"       |"3"       |null          |['repeatExecute':true]|null|null         |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"X"       |"unUsed" |"1"               |"b0|ENM"         |null          |"b1"            |"2"       |"3"       |"b0|ENM,b1|ENM"|['repeatExecute':true]|null|"FAILED"    |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b1\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"X"       |"unUsed" |"2"               |"b0|ENM"         |null          |"b2"            |"3"       |"3"       |"b0|ENM,b2|ENM"|[:]            |"FAILED"  |"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"Unable to delete Backup from ENM\", logLevel:ERROR]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"b2"      |"unUsed" |"2"               |null             |null          |"b2"            |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"b2"      |"unUsed" |"2"               |null             |"true"        |"b2"            |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "ENM" |"b0|ENM,b1|ENM,b2|ENM" |"b0"      |"unUsed" |null              |null             |"true"        |"b0"            |"1"       |"3"       |null          |[:]             |"CANCELLED"|null        |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"

        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|"XXX"    |null              |null             |null          |"b0"            |"1"       |"3"       |"b0|NODE"     |['repeatExecute':true]|null|"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b0\". Failure reason: \"XXX\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|"YYY"    |"1"               |"b0|NODE"        |null          |"b1"            |"2"       |"3"       |"b0|NODE,b1|NODE"|['repeatExecute':true]|null|"FAILED"  |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b1\". Failure reason: \"YYY\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|""       |"2"               |"b0|NODE"        |null          |"b2"            |"3"       |"3"       |"b0|NODE,b2|NODE"|[:]          |"FAILED"  |"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|null     |"2"               |null             |null          |"b2"            |"3"       |"3"       |null          |[:]             |"SUCCESS" |null         |33.0     |"message:\"Delete Backup\" activity for the backup \"b2\" is completed successfully., logLevel:INFO]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|"unreach"|"2"               |null             |"true"        |"b2"            |"3"       |"3"       |"b2|NODE"     |[:]             |"FAILED"  |"FAILED"     |0.0      |"message:\"Delete Backup\" activity has failed for backup \"b2\". Failure reason: \"unreach\", logLevel:ERROR]"
        "NODE"|"b0|NODE,b1|NODE,b2|NODE"|"unused"|"File was not found" |null  |null             |"true"        |"b0"            |"1"       |"3"       |null          |[:]             |"CANCELLED"|null        |33.0     |"message:\"Delete Backup\" activity for the backup \"b0\" is completed successfully., logLevel:INFO]"
    }

    def "AXE DeleteBackup Job - Timeout step - error cases - no invocation"() {
        given : "AXE Delete Backup  job has been created"
        activityJobId = createAxeDeleteBackupJob("b|ENM", Collections.emptyList())
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

        where:
        exception                       |log
        new Exception("Internal error") |"[message:\"Delete Backup\" action has failed in handle timeout. Reason: \"1\"., logLevel:ERROR]"
    }

    def "AXE DeleteBackup Job - Timeout step Timedout"() {
        given : "AXE License job has been created"
        activityJobId = createAxeDeleteBackupJob("",Collections.emptyList())

        when: "performing the timeout expiry"
        objectUnderTest.timeoutForAsyncHandleTimeout(activityJobId);

        then: "the job should get updated to failed"
        activityJobId !=null
        def activityJob = runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId)
        "FAILED" == extractJobProperty(ShmConstants.RESULT, activityJob.getAttribute(ShmConstants.JOBPROPERTIES))
        null == activityJob.getAttribute(ShmConstants.STEP_DURATIONS)
        assert("[message:Failed to get the \"Delete Backup\" activity status from the node within \"0\" minutes. Failing the Activity., logLevel:ERROR]" == extractMessage((List) activityJob.getAttribute(ShmConstants.LOG)))
    }
}
