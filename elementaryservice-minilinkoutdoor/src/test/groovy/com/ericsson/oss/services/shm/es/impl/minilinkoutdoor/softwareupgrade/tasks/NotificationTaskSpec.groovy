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
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.es.impl.ActivityAndNEJobProgressCalculator
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.model.notification.SHMCommonCallbackNotification
import com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.service.ConfirmService
import com.ericsson.oss.services.shm.generic.notification.SHMCommonCallBackNotificationJobProgressBean

import static org.mockito.Mockito.when
import spock.lang.Unroll

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.impl.JobEnvironment

class NotificationTaskSpec extends CdiSpecification {

    @MockedImplementation
    ActivityAndNEJobProgressCalculator activityAndNEJobProgressPercentageCalculator

    @MockedImplementation
    NEJobStaticData neJobStaticData

    @MockedImplementation
    JobStaticData jobStaticData

    @MockedImplementation
    ActivityUtils activityUtils

    @MockedImplementation
    JobEnvironment jobEnvironment

    @ObjectUnderTest
    NotificationTask notificationTask

    @MockedImplementation
    JobActivityInfo jobActivityInfo

    @MockedImplementation
    SHMCommonCallBackNotificationJobProgressBean message

    @MockedImplementation
    SHMCommonCallbackNotification commonNotification

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider


    private static final long activityJobId = 123l;
    private static final long neJobId = 123l;
    private static final String nodeName = "CORE42ML6351";
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

        message.getCommonNotification() >> commonNotification
        commonNotification.getFdn() >> "fdn"
        Map<String, Object> additionalAttributes = new HashMap<>()
        additionalAttributes.put(Constants.ACTIVITY_JOB_ID, activityJobId)
        commonNotification.getAdditionalAttributes() >> additionalAttributes
        commonNotification.getProgressPercentage() >> "0.0"
    }

    @Unroll("state=#state, result=#result")
    def 'testprocessNotification'() {
        given: "initialize"
        activityUtils.getActivityInfo(activityJobId, ConfirmService.class) >> jobActivityInfo
        jobActivityInfo.getActivityJobId() >> activityJobId
        when: "invoke process notification"
        commonNotification.getState() >> state
        notificationTask.processRecivedNotification(message, "activity", jobActivityInfo)
        then: "return value should not be null"
        if(state == "DOWNLOADED") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "ACTIVATING") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "IDLE") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        } else if(state == "AWAITING_DOWNLOAD") {
            1 * activityAndNEJobProgressPercentageCalculator.updateNEJobProgressPercentage(neJobId);
        }

        where: "params for state"
        state                 |   result
        "DOWNLOADED"          |  "pass"
        "ACTIVATING"          |  "pass"
        "IDLE"                |  "pass"
        "AWAITING_DOWNLOAD"   |  "pass"
        "NONE"                |  "fail"
    }
}
