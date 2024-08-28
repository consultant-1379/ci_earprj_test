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
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.tasks

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.Notification
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.mockito.Mockito.when

import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class PrecheckTaskSpec extends CdiSpecification {

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator;

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    JobStaticData jobStaticData;

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @ObjectUnderTest
    PrecheckTask precheckTask;

    @MockedImplementation
    Notification message;

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    ActivityJobTBACValidator activityJobTBACValidator

    private static final long activityJobId = 123l;
    private static final String nodeName = "nodeName";
    private static final String THIS_ACTIVITY = "anyActivity";
    List<Map<String, Object>> jobLogList = null;

    def setup() {
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNeJobId() >> 123l
        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData

        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        Map<String, Object> mainJobAttributes = new HashMap<>();
        activityUtils.getJobConfigurationDetails(activityJobId) >> mainJobAttributes
        activityUtils.getMainJobAttributes(activityJobId) >> mainJobAttributes
        jobEnvironment.getNodeName() >> nodeName
        jobLogList = new ArrayList<Map<String, Object>>();
    }

    def "testPreCheckSuccess"() {
        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY) >> true
        when: "invoke precheck"
        ActivityStepResult activityStepResult = precheckTask.activityPreCheck(activityJobId, THIS_ACTIVITY);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_SUCCESS_PROCEED_EXECUTION;
    }

    def "testPreCheckFailure"() {
        activityJobTBACValidator.validateTBAC(activityJobId, neJobStaticData, jobStaticData, THIS_ACTIVITY) >> false
        when: "invoke precheck"
        ActivityStepResult activityStepResult = precheckTask.activityPreCheck(activityJobId, THIS_ACTIVITY);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.PRECHECK_FAILED_SKIP_EXECUTION;
    }
}
