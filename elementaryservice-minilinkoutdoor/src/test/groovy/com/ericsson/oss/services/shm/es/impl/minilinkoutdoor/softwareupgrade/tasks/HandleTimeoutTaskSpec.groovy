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
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.es.api.ActivityStepResult

import static org.mockito.Mockito.when

import org.spockframework.util.Assert

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum

class HandleTimeoutTaskSpec extends CdiSpecification {

    @MockedImplementation
    NEJobStaticData neJobStaticData;

    @MockedImplementation
    NetworkElementRetrievalBean networkElementRetrivalBean;

    @MockedImplementation
    NetworkElementData networkElement;

    @MockedImplementation
    ActivityUtils activityUtils;

    @ObjectUnderTest
    HandleTimeoutTask handleTimeoutTask;

    @MockedImplementation
    JobActivityInfo jobActivityInfo;

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    private static final long activityJobId = 123l;
    private static final String nodeName = "nodeName";

    def setup() {
        jobActivityInfo.getActivityName() >> "activity"
        jobActivityInfo.getActivityJobId() >> activityJobId
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY) >> neJobStaticData
        neJobStaticData.getNodeName() >> nodeName
        networkElementRetrivalBean.getNetworkElementData(nodeName) >> networkElement
        networkElement.getNeFdn() >> "nodeFdn"
    }

    def "testHandleTimeout"() {
        when: "invoke handle timeout"
        ActivityStepResult activityStepResult = handleTimeoutTask.handleTimeout(jobActivityInfo);
        then: "return value should not be null"
        Assert.notNull(activityStepResult)
        activityStepResult.getActivityResultEnum() ==  ActivityStepResultEnum.TIMEOUT_RESULT_FAIL;
    }
}
