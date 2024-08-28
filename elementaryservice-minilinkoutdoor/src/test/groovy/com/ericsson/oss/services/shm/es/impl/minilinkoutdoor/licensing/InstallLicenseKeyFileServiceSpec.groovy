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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.licensing

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.JobUpdateService
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.impl.license.LicenseUtil
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.mockito.Mockito.when

import org.spockframework.util.Assert
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class InstallLicenseKeyFileServiceSpec extends CdiSpecification {

    protected RuntimeConfigurableDps runtimeDps;

    @MockedImplementation
    LicenseUtil licenseUtil;

    @MockedImplementation
    MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

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
    ManagedObject mo;
    @MockedImplementation
    JobActivityInfo jobActivityInfo;
    @MockedImplementation
    SHMCommonCallBackNotificationJobProgressBean message;
    @MockedImplementation
    SHMCommonCallbackNotification commonNotification
    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider
    @MockedImplementation
    JobStaticData jobStaticData;
    @MockedImplementation
    ActivityJobTBACValidator activityJobTBACValidator
    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    private static final long activityJobId = 123l;
    private static final long mainJobId = 124l;
    private static final String nodeName = "nodeName";
    private static final long neJobid = 1234l;
    private static final String LICENSE_FILEPATH = "LICENSE_FILEPATH";
    final String licenseFilePath = "Some License File Path";
    final String fingerPrint = "Some FingerPrint";
    final String ProductType = "LTE";
    List<Map<String, Object>> jobLogList = null;

    def setup() {
        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps.class);

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        neJobStaticData.getMainJobId() >> 123l
        jobStaticDataProvider.getJobStaticData(123l) >> jobStaticData

        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, ActivityConstants.INSTALL_LICENSE_ACTIVITY) >> true

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
        licenseFile.put(LICENSE_FILEPATH, "LICENSE_FILEPATH");
        jobPropertyUtils.getPropertyValue(Collections.singletonList(LICENSE_FILEPATH),
                activityUtils.getJobConfigurationDetails(activityJobId), nodeName) >> licenseFile
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>();
        activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class) >> jobActivityInfo;

        message.getCommonNotification() >> commonNotification
        commonNotification.getFdn() >> "fdn"
        Map<String, Object> additionalAttributes = new HashMap<>()
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, activityJobId)
        additionalAttributes.put(Constants.ACTIVITY_NAME, "LICENCE")
        commonNotification.getAdditionalAttributes() >> additionalAttributes
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
        1 * miniLinkOutdoorJobUtil.getSubscriptionKey(nodeName, "LICENCE")
    }

    def "testCancelTimeout"() {
        when: "invoke cancel"
        ActivityStepResult activityStepResult = installLicenseKeyFileService.cancelTimeout(activityJobId, true);
        then: "retun value shoud not be null"
        Assert.notNull(activityStepResult)
    }

    def "testHandleTimeout"() {
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        activityUtils.getActivityStepResult(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL) >> activityStepResult
        when: "invoke cancel"
        ActivityStepResult activityStepResultA = installLicenseKeyFileService.handleTimeout(activityJobId);
        then: "retun value shoud not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }

    @Unroll("installState=#installState, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, InstallLicenseKeyFileService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        when: "invoke cancel"
        commonNotification.getState() >> installState
        installLicenseKeyFileService.processNotification(message)
        then: "retun value shoud not be null"
        if(installState == "NOOP") {
            1 * miniLinkOutdoorJobUtil.finishActivity(jobActivityInfo, null, JobResult.SUCCESS, [], ActivityConstants.INSTALL_LICENSE);
        } else if (installState == "LKF_CONFIGURING") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, [], [], 30.0)
        } else if (installState == "LKF_DOWNLOADING") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, [], [], 40.0)
        } else if (installState == "LKF_VALIDATING") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, [], [], 50.0)
        } else if (installState == "LKF_INSTALLING") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, [], [], 70.0)
        } else if (installState == "LKF_ENABLING") {
            1 * jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, [], [], 90.0)
        } else {
            1 * miniLinkOutdoorJobUtil.finishActivity(jobActivityInfo, null, JobResult.FAILED, [], ActivityConstants.INSTALL_LICENSE);
        }

        where: "params for installState"
        installState          |   result
        "NOOP"  |  "pass"
        "LKF_CONFIGURING"  |  "pass"
        "LKF_DOWNLOADING"  |  "pass"
        "LKF_VALIDATING"  |  "pass"
        "LKF_INSTALLING"  |  "pass"
        "LKF_ENABLING"  |  "pass"
        "ERROR_NO_STORAGE"  |  "pass"
        "ERROR_NO_SPACE"  |  "pass"
        "UNKNOWN_VERSION"  |  "pass"
        "UNKNOWN_SIGNATURE_TYPE" | "pass"
        "UNKNOWN_FINGERPRINT_METHOD" | "pass"
        "UNKNOWN_FINGERPRINT" | "pass"
        "ERROR_CORRUPT_SIGNATURE" | "pass"
        "ERROR_TRANSFER_FAILED" | "pass"
        "ERROR_SEQUENCE_NUMBER" | "pass"
        "ERROR_XML_SYNTAX" | "pass"
        "ERROR_SYSTEM_ERROR" | "pass"
        "NONE" | "fail"
    }
}
