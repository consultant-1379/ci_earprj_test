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
package com.ericsson.oss.services.shm.es.impl.axe.licensing

import static com.ericsson.oss.services.shm.es.axe.common.AxeConstants.*

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.shm.common.FileResource
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException
import com.ericsson.oss.services.shm.es.api.ActivityStepResult
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse
import com.ericsson.oss.services.shm.es.impl.axe.backup.AbstractAxeBackupServiceTest
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants
import com.ericsson.oss.services.shm.jobs.common.constants.JobVariables
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.constants.WorkFlowConstants
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities

/**
 *
 * @author xrajeke
 *
 */
public class InstallLicenseCancelTest extends AbstractAxeBackupServiceTest{

    @ObjectUnderTest
    private InstallLicenseKeyFileService objectUnderTest;

    @MockedImplementation
    private FileResource fileResource;

    @MockedImplementation
    private LicenseKeyFileDeleteService licenseKeyFileDeleteService;

    private long activityJobId ;
    
    
    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    def setup() {
        addNetworkElementMOs(NODE_NAME)
        platformProvider.getPlatformTypeBasedOnCapability(_, _) >> PlatformTypeEnum.AXE
    }

    def long createAxeLicenseJob(String lkfPath, List activityProperties){
        Map<String, Object> jobTemplateData = new HashMap<>();
        Map<String, Object> mainJobData = new HashMap<>();
        Map<String, Object> neJobData = new HashMap<>();
        Map<String, Object> activityJobData = new HashMap<>();
        Map<String, Object> jobConfigurationDetails = new HashMap<>();
        Map<String, Object> neTypeProp = new HashMap<>();
        Map<String, Object> jobProp = new HashMap<>();

        jobTemplateData.put(ShmConstants.JOBCONFIGURATIONDETAILS, Collections.singletonMap("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE")));
        jobTemplateData.put("owner", "AXEUser")
        jobTemplateData.put("jobType","LICENSE")

        jobProp.put(ShmConstants.KEY, CommonLicensingActivityConstants.LICENSE_FILE_PATH)
        jobProp.put(ShmConstants.VALUE, lkfPath)
        neTypeProp.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC)
        neTypeProp.put(ShmConstants.JOBPROPERTIES, Arrays.asList(jobProp))
        jobConfigurationDetails.put(ShmConstants.NETYPEJOBPROPERTIES, Arrays.asList(neTypeProp))
        jobConfigurationDetails.put("mainSchedule", Collections.singletonMap("execMode", "IMMEDIATE"))
        mainJobData.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobConfigurationDetails);

        neJobData.put(ShmConstants.NE_NAME,NODE_NAME);
        neJobData.put(ShmConstants.NETYPE, NE_TYPE_MSC_BC);

        activityJobData.put(ShmConstants.ACTIVITY_NAME, "install")
        activityJobData.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobData.put(ShmConstants.JOBPROPERTIES, activityProperties)
        activityJobData.put(ShmConstants.PROGRESSPERCENTAGE, 0d)


        return persistJobs(jobTemplateData, mainJobData, neJobData, activityJobData)
    }

       
    def 'AXE License Job - cancel verification when JobDataNotFound Exception occured'(){

        given : "AXE License job has been created"
        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt",Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >>  true;
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        fileResource.exists(_) >> true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)>> {throw new Exception("")};

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=objectUnderTest.cancel(activityJobId)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Failure reason:")
    }
    
    def 'AXE License Job - cancel verification when exception occured'(){

        given : "AXE License job has been created"
        activityJobId = createAxeLicenseJob("/home/smrs/smrsroot/license/fp/l.txt",Collections.emptyList())
        activityJobTBACValidator.validateTBAC(_, _, _, _) >>  true;
        long licPoId  = createLicenseDataPo("/home/smrs/smrsroot/license/fp/l.txt")
        fileResource.exists(_) >> true
        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_JOB_CAPABILITY)>> {throw new JobDataNotFoundException("Database service is not accessible")};

        when :"Perform cancel job for activity job id"
        ActivityStepResult activityStepResult=objectUnderTest.cancel(activityJobId)

        then :"Check cancel job is triggered for activity job id"
        activityStepResult.getActivityResultEnum().equals(ActivityStepResultEnum.EXECUTION_FAILED)
        runtimeDps.stubbedDps.liveBucket.findPoById(activityJobId).attributes.get("lastLogMessage").contains("Database service is not accessible")
        
    }
        
    def getLicenseResponse(Integer status,String reason){
        if(status == null){
            return null;
        }else{
            final LicenseInstallResponse licensePollResponse = new LicenseInstallResponse()
            licensePollResponse.setLicense(reason)
            licensePollResponse.setStatus(status)
            return licensePollResponse;
        }
    }

    def long createLicenseDataPo(String lkfPath){
        Map<String, Object> licenseData = new HashMap<>();
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_FINGERPRINT, "fp123")
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER, "1")
        licenseData.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, lkfPath)
        PersistenceObject licensePO   = runtimeDps.addPersistenceObject().namespace(LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE).type(LicensingActivityConstants.LICENSE_DATA_PO).addAttributes(licenseData).build();
        return licensePO.getPoId()
    }

    def Object getInstalledOn(long poId){
        def licensePo = runtimeDps.stubbedDps.liveBucket.findPoById(poId)
        return licensePo.getAttribute(LicensingActivityConstants.INSTALLED_ON)
    }
}
