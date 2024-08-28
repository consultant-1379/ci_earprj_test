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
package com.ericsson.oss.services.shm.es.impl.minilink.licensing

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.JobUpdateService
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult
import com.ericsson.oss.services.shm.model.NotificationSubject
import com.ericsson.oss.services.shm.nejob.cache.*
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants
import com.ericsson.oss.services.shm.notifications.api.Notification
import com.ericsson.oss.services.shm.es.impl.minilink.upgrade.MiniLinkActivityUtil
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.shared.enums.JobLogType
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import org.spockframework.util.Assert
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil

class InstallLicenseKeyFileServiceSpec extends CdiSpecification {

    protected RuntimeConfigurableDps runtimeDps;

    @MockedImplementation
    LicenseUtil licenseUtil;

    @MockedImplementation
    MiniLinkActivityUtil miniLinkActivityUtil;

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressCalculator;

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @MockedImplementation
    JobUpdateService jobUpdateService;

    @ObjectUnderTest
    InstallLicenseKeyFileService installLicenseKeyFileService;
    @MockedImplementation
    JobPropertyUtils jobPropertyUtils;
    @MockedImplementation
    ManagedObjectUtil managedObjectUtil;
    @MockedImplementation
    ManagedObject mo;
    @MockedImplementation
    JobActivityInfo jobActivityInfo;
    @MockedImplementation
    Notification message;
    @MockedImplementation
    NotificationSubject notificationSubject;
    @MockedImplementation
    DpsAttributeChangedEvent event;
    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider


    private static final long activityJobId = 123l;
    private static final long mainJobId = 124l;
    private static final String nodeName = "nodeName";
    private static final long neJobid = 1234l;
    private static final String INVALID_LKF = "The node considers the LKF file invalid. Error code is: %s";
    final String licenseFilePath = "Some License File Path";
    final String fingerPrint = "Some FingerPrint";
    final String ProductType = "LTE";
    List<Map<String, Object>> jobLogList = null;
    AttributeChangeData acd = null;

    def setup() {
        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps.class);

        licenseUtil.getLicenseInstallOperStatus(activityJobId) >> "licenseInstallState"
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l

        HashMap<String, Object> neAttributes = new HashMap<String, Object>();
        neAttributes.put(ShmConstants.MAIN_JOB_ID, mainJobId);
        neAttributes.put(ShmConstants.NE_JOB_ID, neJobid);
        neAttributes.put(ShmConstants.NE_NAME, nodeName);
        activityUtils.getNeJobAttributes(activityJobId) >> (neAttributes);
        activityUtils.getPoAttributes(activityJobId) >> (neAttributes);
        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        Map<String, Object> mainJobAttributes = new HashMap<>();
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        Map<String, String> licenseFile = new HashMap<>();
        licenseFile.put(MiniLinkConstants.LICENSE_FILEPATH, "LICENSE_FILEPATH");
        jobPropertyUtils.getPropertyValue(Collections.singletonList(MiniLinkConstants.LICENSE_FILEPATH),
                activityUtils.getJobConfigurationDetails(activityJobId), nodeName) >> licenseFile
        managedObjectUtil.getManagedObject(nodeName, MiniLinkConstants.XF_LICENSE_INSTALL_OBJECTS) >> mo;
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>();


        activityUtils.getActivityInfo(activityJobId, getClass()) >> jobActivityInfo;
        message.getNotificationEventType() >> "AVC"
        message.getNotificationSubject() >> notificationSubject
        activityUtils.getActivityJobId(notificationSubject) >> activityJobId
        message.getDpsDataChangedEvent() >> event
        Set<AttributeChangeData> changedAttr = new HashSet<>();
        acd = new AttributeChangeData();
        acd.setName(MiniLinkConstants.XF_LICENSE_INSTALL_OPER_STATUS);

        changedAttr.add(acd);
        event.getChangedAttributes() >> changedAttr
    }

    def "testCancel" () {
        when: "invoke cancel"
        installLicenseKeyFileService.cancel(activityJobId);
        then : "expect nothing"
    }

    def "testPreCheck"() {
        /*given: "initialize"*/
        when: "invoke cancel"
        ActivityStepResult activityStepResult = installLicenseKeyFileService.precheck(activityJobId);
        then: "retun value shoud not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testExecute"() {
        when: "invoke execute"
        installLicenseKeyFileService.execute(activityJobId)
        then : "return"
        1 * activityAndNEJobProgressCalculator.updateNEJobProgressPercentage(123l);
    }

    def "testCancelTimeout"() {
        when: "invoke cancel"
        ActivityStepResult activityStepResult = installLicenseKeyFileService.cancelTimeout(activityJobId, true);
        then: "retun value shoud not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    def "testhandleTimeout"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) >> activityStepResult
        when: "invoke cancel"
        ActivityStepResult activityStepResultA = installLicenseKeyFileService.handleTimeout(activityJobId);
        then: "retun value shoud not be null"
        Assert.notNull(activityStepResultA)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    @Unroll("installState=#installState, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        acd.setNewValue(installState);
        when: "invoke cancel"
        installLicenseKeyFileService.processNotification(message);
        then: "retun value shoud not be null"
        if(installState == "lkfInstallFinished") {
            1 * miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, null, JobResult.SUCCESS, [], ActivityConstants.INSTALL_LICENSE);
            1 * miniLinkActivityUtil.sendNotification(jobActivityInfo, ActivityConstants.INSTALL_LICENSE);
        } else if (installState == "lkfDownloadStarted") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(123, [], [], 40.0)
        } else if (installState == "errorRMMUnavailable" || installState == "errorNoSpaceOnRMM") {
            1 * miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, null, JobResult.FAILED, [], ActivityConstants.INSTALL_LICENSE);
            1 * miniLinkActivityUtil.sendNotification(jobActivityInfo, ActivityConstants.INSTALL_LICENSE);
        } else if (installState == "unKnownVersion" || installState == "unknownFingerprint" || installState == "unknownFingerprintMethod" || installState == "unKnownSignatureType" || installState == "errorCorruptSignature") {
            1 *  activityUtils.addJobLog(
                    String.format(INVALID_LKF, installState),
                    JobLogType.SYSTEM.getLogType(), jobLogList, JobLogLevel.ERROR.getLogLevel());
        } else if (installState == "lkfValidationStarted") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(123, [], [], 30.0)
        } else if (installState == "lkfEnabling") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(123, [], [], 90.0)
        }  else if (installState == "lkfInstallingOnRMM") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(123, [], [], 60.0)
        } else {
            1 * miniLinkActivityUtil.finishInstallActivity(jobActivityInfo, null, JobResult.FAILED, [], ActivityConstants.INSTALL_LICENSE);
            1 * miniLinkActivityUtil.sendNotification(jobActivityInfo, ActivityConstants.INSTALL_LICENSE);
        }

        where: "params for installState"
        installState          |   result
        "lkfDownloadStarted"  |  "pass"
        "lkfInstallFinished"  |  "pass"
        "lkfValidationStarted"|  "pass"
        "unKnownVersion"      |  "pass"
        "abcc"                |  "pass"
        "lkfEnabling"         |  "pass"
        "errorRMMUnavailable" |  "pass"
        "errorNoSpaceOnRMM"   |  "pass"
        "unKnownSignatureType"|  "pass"
        "unknownFingerprintMethod"|  "pass"
        "lkfInstallingOnRMM"  |  "pass"
        "unknownFingerprint"  |  "pass"
        "errorCorruptSignature"  |  "pass"
    }
}
