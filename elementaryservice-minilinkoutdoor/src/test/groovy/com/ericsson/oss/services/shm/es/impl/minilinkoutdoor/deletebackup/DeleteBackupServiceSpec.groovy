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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.deletebackup

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper
import com.ericsson.oss.services.shm.common.FdnServiceBean
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.es.api.JobUpdateService
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.DeleteSmrsBackupUtil
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.BackupActivityProperties
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.backup.MiniLinkOutdoorJobUtil
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.ActivityUtilsStub
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator

import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class DeleteBackupServiceSpec extends CdiSpecification {

    @ObjectUnderTest
    DeleteBackupService deleteBackupService;

    @ImplementationInstance
    ActivityUtils activityUtils = new ActivityUtilsStub(ACTIVITY_JOB_ID, NODE_NAME, BACKUP_NAME_TEST);

    @MockedImplementation
    JobUpdateService jobUpdateService;

    @MockedImplementation
    private MiniLinkOutdoorJobUtil miniLinkOutdoorJobUtil;

    @MockedImplementation
    private BackupActivityProperties backupActivityProperties;

    private static final long ACTIVITY_JOB_ID = 123;
    private static final String NODE_NAME = "ML-TN";
    private static final String BACKUP_NAME_TEST = "backup";
    private static final Double PERCENT_ZERO = 0.0;
    private boolean isExists = false;

    def "testCancel"() {
        when: "invoke cancel"
        ActivityStepResult activityStepResult = deleteBackupService.cancel(ACTIVITY_JOB_ID)
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.EXECUTION_FAILED
    }

    def "testPreCheck"() {
        given: "initialize"
        miniLinkOutdoorJobUtil.getBackupActivityProperties(ACTIVITY_JOB_ID, ActivityConstants.DELETE_BACKUP, DeleteBackupService.class) >> backupActivityProperties
        backupActivityProperties.getBackupName() >> "testBackup"
        ActivityStepResult activityStepResult = new ActivityStepResult();
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION);
        miniLinkOutdoorJobUtil.precheckSuccess(PERCENT_ZERO, backupActivityProperties) >> activityStepResult
        when: "invoke precheck"
        ActivityStepResult activityStepResultA = deleteBackupService.precheck(ACTIVITY_JOB_ID);
        then: "return value should not be null"
        Assert.notNull(activityStepResultA)
        activityStepResultA.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testExecute"() {
        when: "invoke execute"
        deleteBackupService.execute(ACTIVITY_JOB_ID)
        then : "expect nothing"
    }

    def "testCancelTimeout"() {
        when: "invoke cancel timeout"
        ActivityStepResult activityStepResult = deleteBackupService.cancelTimeout(ACTIVITY_JOB_ID, true)
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS
    }

    def "testHandleTimeoutSuccess"() {
        when: "invoke handle timeout"
        ActivityStepResult activityStepResult = deleteBackupService.handleTimeout(ACTIVITY_JOB_ID)
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_SUCCESS
    }

    def "testHandleTimeoutFailure"() {
        given: "initialize"
        isExists = true;
        when: "invoke handle timeout"
        ActivityStepResult activityStepResult = deleteBackupService.handleTimeout(ACTIVITY_JOB_ID)
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL
    }

    @ImplementationInstance
    final NetworkElement networkElement = new NetworkElement() {

        @Override
        public PlatformTypeEnum getPlatformType() {
            return PlatformTypeEnum.MINI_LINK_OUTDOOR;
        }

        @Override
        public String getNeType() {
            return PlatformTypeEnum.MINI_LINK_OUTDOOR.getName();
        }
    };

    @ImplementationInstance
    final FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper = new FdnServiceBeanRetryHelper() {

        @Override
        public List<NetworkElement> getNetworkElementsByNeNames(final List<String> neNames) {
            return Collections.singletonList(networkElement);
        }
    };

    @ImplementationInstance
    final FdnServiceBean fdnServiceBean = new FdnServiceBean() {

        @Override
        public List<NetworkElement> getNetworkElementsByNeNames(final List<String> neNames) {
            return Collections.singletonList(networkElement);
        }
    };

    @ImplementationInstance
    final DeleteSmrsBackupUtil deleteSmrsBackupUtil = new DeleteSmrsBackupUtil() {

        @Override
        public boolean deleteBackupOnSmrs(final String nodeName, final String backupName, final String neType) {
            return true;
        }

        @Override
        public boolean isBackupExistsOnSmrs(final String nodeName, final String backupName, final String neType) {
            return isExists;
        }

        @Override
        public NetworkElement getNetworkElement(final String nodeName) {
            return networkElement;
        }
    };

    @ImplementationInstance
    final JobPropertyUtils jobPropertyUtils = new JobPropertyUtils() {

        @Override
        public Map<String, String> getPropertyValue(final List<String> keyList, final Map<String, Object> jobConfigurationDetails,
                final String neName, final String neType, final String platformtype) {
            return Collections.<String, String> singletonMap("BACKUP_NAME", BACKUP_NAME_TEST);
        }
    };

    @ImplementationInstance
    final NeJobStaticDataProvider neJobStaticDataProvider = new NeJobStaticDataProvider() {

        @Override
        public NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) {
            return new NEJobStaticData(123L, 345L, "testFDN", "businessKey", PlatformTypeEnum.MINI_LINK_OUTDOOR.getName(), new Date().getTime(), null);
        }

        @Override
        public void updateNeJobStaticDataCache(long activityJobId, String platformCapbility, long activityStartTime) throws JobDataNotFoundException {
        }

        @Override
        public void clear(long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(long activityJobId, NEJobStaticData neJobStaticData) {
        }
        
        @Override
        public long getActivityStartTime(final long activityJobId) {
            return 0;
        }
    };

    @ImplementationInstance
    final JobStaticDataProvider jobStaticDataProvider = new JobStaticDataProvider() {

        @Override
        public JobStaticData getJobStaticData(final long mainJobId) {
            return new JobStaticData("", new HashMap<String, Object>(), "",JobType.DELETEBACKUP);
        }

        @Override
        public void clear(long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(long mainJobId, JobStaticData jobStaticData) {
        }
    };

    @ImplementationInstance
    final ActivityJobTBACValidator activityJobTBACValidator = new ActivityJobTBACValidator() {

        @Override
        public boolean validateTBAC(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) {
            return true;
        }
    };

    @ImplementationInstance
    final JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy = new JobConfigurationServiceRetryProxy() {

        @Override
        public Map<String, Object> getActivityJobAttributes(final long activityJobId) {
            return new HashMap<>();
        }

        @Override
        public Map<String, Object> getNeJobAttributes(final long neJobId) {
            return new HashMap<>();
        }
        
        @Override
		public Map<String, Object> getPOAttributes(long poId) {
			return new HashMap<>();
		}

        @Override
        public Map<String, Object> getMainJobAttributes(final long mainJobId) {
            return new HashMap<>();
        }

        @Override
        public List<Map<String, Object>> getProjectedAttributes(final String namespace, final String type, final Map<Object, Object> restrictions, final List<String> reqdAttributes) {
            return new ArrayList<>();
        }

        @Override
        public List<Long> getJobPoIdsFromParentJobId(final long neJobPoId, final String typeOfJob, final String restrictionAttribute){
            return new ArrayList();
        }
        
        @Override
        public List<Map<String, Object>> getActivityJobAttributesByNeJobId(long neJobId, Map<String, Object> restrictions) {
            // TODO Auto-generated method stub
            return new ArrayList<>();
        }
    };
}
