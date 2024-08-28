/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.remote.impl.backup

import static org.junit.Assert.*

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.backupservice.remote.api.BackupInfo
import com.ericsson.oss.services.shm.backupservice.remote.impl.BackupManagementService
import com.ericsson.oss.services.shm.common.FdnServiceBean
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.es.ecim.backup.common.BrmMoService
import com.ericsson.oss.services.shm.job.service.SHMJobService
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobservice.common.JobHandlerErrorCodes

public class BackupManagementServiceTest  extends CdiSpecification {

    @ObjectUnderTest
    private BackupManagementService backupManagementService

    @MockedImplementation
    BrmMoService brmMoService

    @MockedImplementation
    FdnServiceBean fdnServiceBean

    @MockedImplementation
    SHMJobService shmJobService

    def nodeName="LTE02dg2ERBS00001"
    def jobName="BackupJob_adminstator"
    def BackupInfo cppBackupInfo = new BackupInfo("backuptest", "", "", "Identity", "Comments")
    def BackupInfo ecimBackupInfo = new BackupInfo("backuptest", "System", "Systemdata", "Identity", "Comments")
    def Map<String, Object> successResponse = new HashMap<String, Object>()
    def List<NetworkElement> networkElementList = new ArrayList()
    def Map<String, Object>  configPersistFailResponse = new HashMap<String, Object>()
    def Map<String, Object>  jobPersistFailResponse = new HashMap<String, Object>()

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.inject(brmMoService)
    }

    def getNetworkList(){
        def NetworkElement networkElement = new NetworkElement()
        networkElement.setName(nodeName)
        networkElement.setNeType("RadioNode")
        networkElement.setPlatformType(PlatformTypeEnum.ECIM)
        networkElementList.add(networkElement)
    }

    def getSuccessResponse(){
        successResponse.put("errorCode", JobHandlerErrorCodes.SUCCESS.getResponseDescription())
        successResponse.put(ShmConstants.JOBCONFIGID, "123")
        successResponse.put(ShmConstants.JOBNAME, jobName)
    }


    def getConfigPersistenceFailureResponse(){
        configPersistFailResponse.put("errorCode", JobHandlerErrorCodes.JOB_CFGN_PERSISTENCE_FAILED.getResponseDescription())
        configPersistFailResponse.put(ShmConstants.JOBCONFIGID, "123")
        configPersistFailResponse.put(ShmConstants.JOBNAME, jobName)
    }

    def getJobPersistenceFailureResponse(){
        jobPersistFailResponse.put("errorCode", JobHandlerErrorCodes.JOB_PERSISTENCE_FAILED.getResponseDescription())
        jobPersistFailResponse.put(ShmConstants.JOBCONFIGID, "123")
        jobPersistFailResponse.put(ShmConstants.JOBNAME, jobName)
    }

    def setup(){
        getNetworkList()
        getSuccessResponse()
        getConfigPersistenceFailureResponse()
        getJobPersistenceFailureResponse()
        fdnServiceBean.getNetworkElementsByNeNames(_ as Object) >> networkElementList
    }

    def 'ECIM create backup with valid input' () {
        given: "giving response as success on calling createShmJob method with valid input"
        shmJobService.createShmJob(_ as Object) >> successResponse
        when: "when creating backup job"
        String result = backupManagementService.create(nodeName, ecimBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'ECIM create backup with invalid configuration' () {
        given:  "giving response as failed on calling createShmJob method with invalid job configuration"
        shmJobService.createShmJob(_ as Object) >> configPersistFailResponse
        when: "when creating node backup"
        String result = backupManagementService.create(nodeName, cppBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'ECIM create backup with invalid input' () {
        given: "giving response as failed on calling createShmJob method with invalid job input"
        shmJobService.createShmJob(_ as Object) >> jobPersistFailResponse
        when: "when creating node backup"
        String result = backupManagementService.create(nodeName, ecimBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'ECIM export backup with valid input' () {
        given:  "giving response as success on calling createShmJob method with valid input"
        shmJobService.createShmJob(_ as Object) >> successResponse
        when: "when exporting node backup"
        String result = backupManagementService.export(nodeName, ecimBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'ECIM export backup with invalid configuation' () {
        given: "giving response as failed on calling createShmJob method with invalid job configuration"
        shmJobService.createShmJob(_ as Object) >> configPersistFailResponse
        when: "when exporting node backup"
        String result = backupManagementService.export(nodeName, ecimBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'ECIM export backup with invalid input' () {
        given: "giving response as failed on calling createShmJob method with invalid job input"
        shmJobService.createShmJob(_ as Object) >> jobPersistFailResponse
        when: "when exporting node backup"
        String result = backupManagementService.export(nodeName, ecimBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'CPP create backup with valid input' () {
        given: "giving response as success on calling createShmJob method with valid input"
        shmJobService.createShmJob(_ as Object) >> successResponse
        when: "when creating node backup "
        String result = backupManagementService.create(nodeName, cppBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'CPP create backup with invalid configuration' () {
        given: "giving response as failed on calling createShmJob method with invalid job configuration"
        shmJobService.createShmJob(_ as Object) >> configPersistFailResponse
        when: "when exporting node backup"
        String result = backupManagementService.create(nodeName, cppBackupInfo)
        then: "returning the result for job creation"
        result
    }

    def 'CPP create backup with invalid input' () {
        given: "giving response as failed on calling createShmJob method with invalid job input"
        shmJobService.createShmJob(_ as Object) >> jobPersistFailResponse
        when: "when exporting node backup"
        String result = backupManagementService.create(nodeName, cppBackupInfo)
        then: "returning the result for job creation"
        result
    }
}
