/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/


package com.ericsson.oss.services.shm.jobexecutor.fa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.fa.FaBuildingBlocksResponseSender
import com.ericsson.oss.services.shm.job.api.JobConfigurationService
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

class FaBuildingBlockResponseSenderTest  extends CdiSpecification {

    @ObjectUnderTest
    private FaBuildingBlocksResponseSender faBuildingBlocksResponseSender

    @MockedImplementation
    private JobConfigurationService jobConfigurationService

    @MockedImplementation
    private Channel channel

    @MockedImplementation
    private ChannelLocator channelLocator

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FaBuildingBlockResponseSenderTest.class);

    long mainJobId = 1L
    long templateId = 13l
    int updatedActivityTimeout = 20
    Map<String, Object> poAttributes = new HashMap<>()
    List<Map<String, Object>> activityJobProperties = new ArrayList<>()
    Map<String, Object> jobProperties = new HashMap<>()
    def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 1L, "LTE02dg2ERBS0001", "", "ECIM", 1L, "")

    def 'Check for sending updated timeout to FA from SHM'(jobCategory,activityJobId,fbbType){

        given : "Activity details for sending updated timeout as intermittent response to FA"
        buildActivityDetails(jobCategory, activityJobId, fbbType)
        channelLocator.lookupChannel(_ as Object) >> channel
        when : "Delete Backup job is created from FA "
        faBuildingBlocksResponseSender.sendUpdatedActivityTimeout(activityJobId, neJobStaticData, poAttributes, "deletebackup", updatedActivityTimeout);
        then : "Verify that activity timeout is sent to FA"
        1 * channel.send((Serializable) _ as Object)
        where:
        jobCategory | activityJobId  | fbbType
        "FA"        | 1L             | "com.ericsson.oss.shm.fbb.CreateBackupJobFbb"
        "FA"        | 0              | "com.ericsson.oss.shm.fbb.CreateBackupJobFbb"
        "FA"        | 1L             | "Invalid"
    }


    def 'Check with invalid inputs for sending updated timeout to FA from SHM'(jobCategory, activityJobId, fbbType){

        given : "Activity details for sending updated timeout as intermittent response to FA"
        buildActivityDetails(jobCategory, activityJobId, fbbType)
        when : "Delete Backup job is created from FA "
        faBuildingBlocksResponseSender.sendUpdatedActivityTimeout(activityJobId, neJobStaticData, poAttributes, "deletebackup", updatedActivityTimeout);
        then : "Verify that activity timeout is sent to FA"
        0 * faBuildingBlocksResponseSender.send( _ as Object, jobCategory)
        where:
        jobCategory | activityJobId | fbbType
        "invalid"   | 1L            | "com.ericsson.oss.shm.fbb.CreateBackupJobFbb"
    }


    def 'check for exception case while sending updated activity timeout to SHM BB'(){

        given : "No Activity details for sending updated timeout as intermittent response to FA"
        when : "Delete Backup job is created from FA "
        faBuildingBlocksResponseSender.sendUpdatedActivityTimeout(1L, neJobStaticData, null, "deletebackup", updatedActivityTimeout);
        then : "Verify that exception is logged"
        LOGGER.error("Exception occurred while sending updated activityTimeout to FA for ActivityJobId:{} due to {}:", 1L, _ as Object);
    }

    def buildActivityDetails(jobCategory, activityJobId, fbbType){
        jobProperties.put(ShmJobConstants.KEY, ShmJobConstants.FaCommonConstants.FA_REQUEST_ID)
        jobProperties.put(ShmJobConstants.VALUE, "1234")
        activityJobProperties.add(jobProperties)
        jobProperties = new HashMap<>()
        jobProperties.put(ShmJobConstants.KEY, ShmJobConstants.FaCommonConstants.FLOW_EXECUTION_NAME)
        jobProperties.put(ShmJobConstants.VALUE, "ASU-Flow")
        activityJobProperties.add(jobProperties);
        jobProperties = new HashMap<>();
        jobProperties.put(ShmJobConstants.KEY, ShmJobConstants.USERNAME);
        jobProperties.put(ShmJobConstants.VALUE, "administrator")
        activityJobProperties.add(jobProperties)
        jobProperties = new HashMap<>()
        jobProperties.put(ShmJobConstants.KEY, ShmJobConstants.FaCommonConstants.FBB_TYPE)
        jobProperties.put(ShmJobConstants.VALUE, fbbType)
        activityJobProperties.add(jobProperties)
        poAttributes.put(ActivityConstants.JOB_PROPERTIES, activityJobProperties)
        jobConfigurationService.getJobCategory(neJobStaticData.getMainJobId()) >> jobCategory
    }
}
