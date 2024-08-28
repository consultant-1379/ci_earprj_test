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

package com.ericsson.oss.services.shm.es.impl.cpp.backup

import com.ericsson.oss.services.shm.es.api.JobUpdateService
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.shared.util.JobLogUtil

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.InventoryQueryConfigurationProvider
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.moaction.retry.ActionRetryPolicy
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.Notification
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.notifications.api.*
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.ActionResultInformation
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType

public class CppBackupTestDataProvider extends CdiSpecification {


    @MockedImplementation
    protected ActionRetryPolicy moActionRetryPolicy

    @MockedImplementation
    protected OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    protected OssModelInfo ossModelInfo

    @MockedImplementation
    protected PlatformTypeProviderImpl platformTypeProvider

    @MockedImplementation
    protected InventoryQueryConfigurationProvider inventoryQueryConfigurationListener

    @MockedImplementation
    SystemRecorder systemRecorder

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    protected Notification notification

    @MockedImplementation
    protected NotificationRegistry notificationRegistry

    @Inject
    protected ActivityUtils activityUtils

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected WorkflowInstanceNotifier workflowInstanceNotifier

    @MockedImplementation
    protected ConfigurationVersionService configurationVersionService;

    @MockedImplementation
    protected NetworkElementRetrievalBean networkElementRetrivalBean
  
    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

   


    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    def activityJobId = 3L;
    def SESSION_ID = "sessionId";
    def ACTION_TRIGGERED = "actionTriggered";
    def activityName = "createcv"
    def neJobId = 2L;
    def mainJobId = 1L;
    def nodeName = "LTE02ERBS00002";
    def businessKey = "Some Business Key"
    def templateJobId = 1L;
    def productNumber = "CXP102051_1"
    def productRevision = "R4D25"
    def EXECUTING = "EXECUTING"
    def SUCCESS = "SUCCESS"
    def PRODUCT_NUMBER = "productNumber"
    def PRODUCT_REVISION = "productRevision"
    def PERSISTED_ACTION_ID = "persistedActionId"

    def actionId = "1"
    def persistedActionId = "1"
    def JOB_NOT_FOUND_EXCEPTION = "JOB_NOT_FOUND_EXCEPTION"
    def EXCEPTION = "EXCEPTION"
    def ACTION_RESULT = "actionResult"
    def NULL = "NULL"
    def poId = 2L;
    def jobExecutionUser="TEST_USER"
   

    def Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
    def Map<String, Object> mainJobAttributes = new HashMap<String, Object>();
    def Map<String, Object> neJobAttributes = new HashMap<String, Object>();
    def NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, businessKey, String.valueOf(PlatformTypeEnum.CPP), (new Date()).getTime(), "LTE17");
    def cvMoFdn = "MeContext=LTE02ERBS00002,ManagedElement=1,SwManagement=1,ConfigurationVersion=1"

    def JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityName, JobTypeEnum.BACKUP, PlatformTypeEnum.CPP)

    def Map<String, Object> jobConfiguration = new HashMap<String, Object>();

    def FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(cvMoFdn, activityJobId, jobActivityInfo)
    def ActivityStepResult activityStepResult = new ActivityStepResult();
    def JobStaticData jobStaticData = new JobStaticData("admin", null, "MANUAL", JobType.getJobType("BACKUP"),"TEST_USER");

    def buildTestDataForBackupAction(final String activityName, final String scenario) {

        switch (scenario) {

            case JOB_NOT_FOUND_EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> {
                    throw new JobDataNotFoundException("JobDataNotFoundException occurred while executing...")
                }
                break
            case EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> {
                    throw new Exception("Exception occurred while executing...")
                }
                break
            case NULL:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> null
                break
            default:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY) >> neJobStaticData
                jobStaticDataProvider.getJobStaticData(neJobStaticData.mainJobId) >> jobStaticData


        }

        platformTypeProvider.getPlatformTypeBasedOnCapability(_ as String, _ as String) >> PlatformTypeEnum.CPP;
        notification.getNotificationSubject() >> fdnNotificationSubject
        notification.getNotificationEventType() >> NotificationEventTypeEnum.AVC
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 3
    }

    def long loadJobProperties(final String nodeName, final String activityName, final String cvMoFdn) {

        Map<String, Object> startableCvNameMap = new HashMap<>();
        Map<String, Object> startableRollbackCvNameMap = new HashMap<>();
        Map<String, Object> mainjobAttributesDetail = new HashMap<>();
        Map<String, Object> mainJobAttributesMap = new HashMap<>()
        Map<String, Object> mainJobAttrCvName = new HashMap<>()
        Map<String, Object> mainJobAttrCvRollBackMap = new HashMap<>()

        final Map<String, Object> mainJobAttrGenerateBackupName = new HashMap<>()
        final Map<String, Object> mainJobAttrCvIdentity = new HashMap<>()
        final Map<String, Object> mainJobAttrCvType = new HashMap<>()
        final Map<String, Object> mainJobAttrCvComment = new HashMap<>()
        final Map<String, Object> mainJobAttrBackupName = new HashMap<>()
        final List<Map<String, Object>> mainJobAttributesList = new ArrayList<>()

        mainjobAttributesDetail.put(ShmJobConstants.KEY, "CREATE_CV_DETAILS");
        mainjobAttributesDetail.put(ShmJobConstants.VALUE, "shm_testbackup1/System/Systemdata");
        mainJobAttrCvName.put(ShmJobConstants.KEY, "CV_NAME");
        mainJobAttrCvName.put(ShmJobConstants.VALUE, "TestCvBackup1");

        mainJobAttrGenerateBackupName.put(ShmJobConstants.KEY, "GENERATE_BACKUP_NAME");
        mainJobAttrGenerateBackupName.put(ShmJobConstants.VALUE, "false");
        mainJobAttrCvIdentity.put(ShmJobConstants.KEY, "CV_IDENTITY");
        mainJobAttrCvIdentity.put(ShmJobConstants.VALUE, "testcvidentity");

        mainJobAttrCvType.put(ShmJobConstants.KEY, "CV_TYPE");
        mainJobAttrCvType.put(ShmJobConstants.VALUE, "STANDARD");
        mainJobAttrCvComment.put(ShmJobConstants.KEY, "CV_COMMENT");
        mainJobAttrCvComment.put(ShmJobConstants.VALUE, "testcvcomment");
        startableCvNameMap.put(ShmJobConstants.KEY, "STARTABLE_CV_NAME");
        startableCvNameMap.put(ShmJobConstants.VALUE, "TestCvBackup1");

        startableRollbackCvNameMap.put(ShmJobConstants.KEY, "ROLLBACK_CV_NAME")
        startableRollbackCvNameMap.put(ShmJobConstants.VALUE, "TestCvBackup1")


        mainJobAttrBackupName.put(ShmJobConstants.KEY, "GENERATE_BACKUP_NAME")
        mainJobAttrBackupName.put(ShmJobConstants.VALUE, "true")



        mainJobAttributesList.add(mainjobAttributesDetail)
        mainJobAttributesList.add(mainJobAttrCvName)
        mainJobAttributesList.add(mainJobAttrGenerateBackupName)
        mainJobAttributesList.add(mainJobAttrCvIdentity)
        mainJobAttributesList.add(mainJobAttrCvType)
        mainJobAttributesList.add(mainJobAttrCvComment)
        mainJobAttributesList.add(startableCvNameMap)
        mainJobAttributesList.add(mainJobAttrBackupName)
        mainJobAttributesList.add(startableRollbackCvNameMap)
        mainJobAttributesList.add(mainJobAttrCvRollBackMap)



        final List<Map<String, Object>> schedulePropertiesList = new ArrayList<>()
        final Map<String, Object> schedulePropRptTypeMap = new HashMap<>()
        final Map<String, Object> mainScheduleMap = new HashMap<>()

        schedulePropRptTypeMap.put(ShmConstants.KEY, JobPropertyConstants.REPEAT_TYPE)
        schedulePropertiesList.add(schedulePropRptTypeMap)
        mainScheduleMap.put(ShmConstants.SCHEDULINGPROPERTIES, schedulePropertiesList)
        jobConfiguration.put(ShmConstants.MAIN_SCHEDULE, mainScheduleMap)


        jobConfiguration.put(ShmJobConstants.JOBPROPERTIES, mainJobAttributesList)


        mainJobAttributesMap.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration)
        mainJobAttributesMap.put("owner", "CPPUser")
        mainJobAttributesMap.put(ShmConstants.JOBTEMPLATEID, templateJobId)
        PersistenceObject mainJob = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("MainJob").addAttributes(mainJobAttributesMap).build();
        def mainJobId = mainJob.getPoId()
        loadNEJobAttributes(nodeName, cvMoFdn);
        return loadActivityJobProperties(activityName);
    }

    private long loadActivityJobProperties(final String activityName) {

        final Map<String, Object> activityjobAttributes = new HashMap<>()
        final Map<String, Object> activityjobAttributeAid = new HashMap<>()
        final Map<String, Object> activityjobAttributesPid = new HashMap<>()
        final Map<String, Object> activityJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()

        activityjobAttributes.put(ShmJobConstants.KEY, ACTION_TRIGGERED);
        activityjobAttributes.put(ShmJobConstants.VALUE, activityName);

        activityjobAttributeAid.put(ShmJobConstants.KEY, ActivityConstants.ACTION_ID);
        activityjobAttributeAid.put(ShmJobConstants.VALUE, actionId);
        activityjobAttributesPid.put(ShmJobConstants.KEY, PERSISTED_ACTION_ID)
        activityjobAttributesPid.put(ShmJobConstants.VALUE, persistedActionId)

        activityJobAttributesList.add(activityjobAttributes);
        activityJobAttributesList.add(activityjobAttributeAid);
        activityJobAttributesList.add(activityjobAttributesPid);

        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NAME, activityName)
        activityJobAttributesMap.put(ShmConstants.PLATEFORM_TYPE, "CPP")
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobAttributesMap.put(ShmConstants.PROGRESSPERCENTAGE, 0d)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId)

        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList);

        PersistenceObject activityJob = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
        return activityJob.getPoId()
    }

    private loadNEJobAttributes(final String nodeName, final String cvMoFdn) {

        final Map<String, Object> neJobAttributesMap = new HashMap<>()
        final Map<String, Object> neJobPropertyUserInput = new HashMap<>()
        final Map<String, Object> neJobPropertyCancelInformation = new HashMap<>()
        final List<Map<String, Object>> neSpecificPropertyList = new ArrayList<>()

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN)
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, cvMoFdn)
        neSpecificPropertyList.add(neJobPropertyUserInput);
        neSpecificPropertyList.add(neJobPropertyCancelInformation)

        neJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList)

        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID, mainJobId)
        neJobAttributesMap.put(ShmConstants.NE_NAME, nodeName)
        neJobAttributesMap.put(ShmConstants.JOBTEMPLATEID, templateJobId)
        PersistenceObject neJob = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("NeJob").addAttributes(neJobAttributesMap).build()
        def neJobId = neJob.getPoId()
    }

    def String getJobProperty(final String propertyName, final List<Map<String, String>> jobProperties) {

        for (Map<String, String> jobProperty : jobProperties) {
            if (propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                return jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null;
    }

    def Map<String, Object> buildMoAttributeMap() {
        final Map<String, Object> csvMoAttributeMap = new HashMap<String, Object>()
        final Map<String, Object> moAttributeMap = new HashMap<String, Object>()
        final Map<String, Object> csvVersionNameMap = new HashMap<String, Object>()
        final List<Map<String, Object>> storedConfigurationVersionList = new ArrayList<Map<String, Object>>()
        final List<String> rollbackList = new ArrayList<>()
        rollbackList.add("TestCvBackup1")
        csvVersionNameMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "TestCvBackup1")
        storedConfigurationVersionList.add(csvVersionNameMap)
        moAttributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList)
        moAttributeMap.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "TestCvBackup1")
        moAttributeMap.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollbackList)

        csvMoAttributeMap.put(ShmConstants.MO_ATTRIBUTES, moAttributeMap)
        csvMoAttributeMap.put(ShmConstants.FDN, cvMoFdn)
        return csvMoAttributeMap

    }

    def Map<String, Object> buildMoAttributeMapForFailure() {
        final Map<String, Object> csvMoAttributeMap = new HashMap<String, Object>()
        final Map<String, Object> moAttributeMap = new HashMap<String, Object>()
        final Map<String, Object> csvVersionNameMap = new HashMap<String, Object>()
        final List<Map<String, Object>> storedConfigurationVersionList = new ArrayList<Map<String, Object>>()
        final List<String> rollbackList = new ArrayList<>()
        rollbackList.add("TestCvBackup12345")
        csvVersionNameMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "TestCvBackup1")
        storedConfigurationVersionList.add(csvVersionNameMap)
        moAttributeMap.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION, storedConfigurationVersionList)
        moAttributeMap.put(ConfigurationVersionMoConstants.STARTABLE_CONFIGURATION_VERSION, "TestCvBackup1")
        moAttributeMap.put(ConfigurationVersionMoConstants.ROLLBACK_LIST, rollbackList)

        csvMoAttributeMap.put(ShmConstants.MO_ATTRIBUTES, moAttributeMap)
        csvMoAttributeMap.put(ShmConstants.FDN, cvMoFdn)
        return csvMoAttributeMap

    }

    def buildDpsAttributeChangedEvent(String actionName) {
        AttributeChangeData attributeChangeDataProgHdr;
        AttributeChangeData attributeChangeDataState;
        List<Map<String, Object>> upActionList = new ArrayList<Map<String, Object>>()
        Map<String, Object> actionaResultMap = new HashMap<String, Object>();
        actionaResultMap.put("notifiableAttributeValue", ActionResultInformation.EXECUTED.toString());
        actionaResultMap.put("previousNotifiableAttributeValue", EXECUTING);

        upActionList.add(actionaResultMap);
        AttributeChangeData attributeChangeDataResult = new AttributeChangeData(ACTION_RESULT, upActionList, upActionList, new Object(), new Object())
        Set<AttributeChangeData> attributeSet = new HashSet<>();
        attributeSet.add(attributeChangeDataProgHdr)
        attributeSet.add(attributeChangeDataState)
        attributeSet.add(attributeChangeDataResult)
        DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("RcswM", "Create Configuration Version", "2.0.0", 3L, cvMoFdn, "liveBucket", attributeSet)
        return dpsAttributeChangedEvent
    }

}
