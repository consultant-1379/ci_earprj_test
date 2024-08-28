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
package com.ericsson.oss.services.shm.job.service

import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEJobProperty
import com.ericsson.oss.services.shm.jobservice.common.*
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter

class SHMJobServiceImplSpec extends AbstractSHMJobServiceImplSpec  {

    @ObjectUnderTest
    private SHMJobServiceImpl shmJobServiceImpl
    
    @MockedImplementation
    EncryptAndDecryptConverter encryptAndDecryptConverter;
    
    def "Prepare supported AXE netypes"(){
        given : "preparing AXE neTypes Information"
        final NeTypesInfo neTypesInfo = getAxeNeTypesInfo()
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability(_,_) >> PlatformTypeEnum.AXE;
        when: "Getting the supported and unsupported netypes by passing the NeTypesInfo"
        final NeTypesPlatformData neTypesPlatformData = shmJobServiceImpl.getNeTypesPlatforms(neTypesInfo);
        then: " neTypesPlatformData matches the result"
        neTypesPlatformData.getSupportedNeTypesByPlatforms().get("AXE").equals(neTypes)
    }

    def "varify in valid netypes"(){
        given : "preparing in valid neTypes Information"
        final NeTypesInfo neTypesInfo = getInValidNeTypesInfo()
        platformTypeProviderImpl.getPlatformTypeBasedOnCapability(_,_) >>{ throw new UnsupportedPlatformException("Exception in retrieving platform type.") }
        when: "Getting the supported and unsupported netypes by passing the NeTypesInfo"
        final NeTypesPlatformData neTypesPlatformData = shmJobServiceImpl.getNeTypesPlatforms(neTypesInfo);
        then: " neTypesPlatformData matches the result"
        neTypesPlatformData.getUnsupportedNeTypes().equals(neTypes)
    }

    public NeTypesInfo getInValidNeTypesInfo(){
        final NeTypesInfo neTypesInfo = new NeTypesInfo()
        neTypes.add("abc")
        neTypes.add("SGSE")
        neTypes.add("123")
        neTypesInfo.setJobType(JobTypeEnum.UPGRADE)
        neTypesInfo.setNeTypes(neTypes)
        return neTypesInfo
    }

    def "Successful Backup job creation for AXE platform nodes"(){
        given : "Prepare Job Info to create back job for AXE platform nodes"
        long poId = createTemplatePO()
        final JobInfo jobInfo = getJobInfo()

        when: "call create job with provided job info data"
        final Map<String, Object> response = shmJobServiceImpl.createShmJob(jobInfo) ;

        then: "success response from create backup"
        response.get("errorCode").toString().equals(JobHandlerErrorCodes.SUCCESS.getResponseDescription())
    }
    def "Successful Backup job creation for AXE platform nodes with encrypted backup support"(){
        given : "Prepare Job Info to create back job for AXE platform nodes"
        long poId = createTemplatePO()
        final JobInfo jobInfo = getJobInfo()
        buildNeTypeJobProperties(jobInfo)
        List<String> onlyApgs = new ArrayList<String>()
        onlyApgs.add("MSC18_APG1")
        onlyApgs.add("MSC18_APG2")
        Map<String, String> neComponetWithApgVersion = new ConcurrentHashMap()
        neComponetWithApgVersion.put("MSC18_APG1", "3.6.0")
        neComponetWithApgVersion.put("MSC18_APG2", "3.7.0")
        axeApgProductRevisionProvider.getApgComponentsProductRevision(onlyApgs) >> neComponetWithApgVersion;
        encryptAndDecryptConverter.getEncryptedPassword("12345") >> "EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84=";

        when: "call create job with provided job info data"
        final Map<String, Object> response = shmJobServiceImpl.createShmJob(jobInfo) ;

        then: "success response from create backup"
        for(NEJobProperty nejobProp: jobInfo.getNeJobProperties()){
            if(nejobProp.getNeName().equals("MSC18__APG2")){
                for(JobProperty jobPro: nejobProp.getJobProperties()){
                    if(jobPro.name.contains(ShmConstants.NE_PRODUCT_REVISION)){
                        assert(jobPro.value.contains("TRUE"))
                    }
                }
            }
        }
        assert(jobInfo.getNeJobProperties().size() >0)
        response.get("errorCode").toString().equals(JobHandlerErrorCodes.SUCCESS.getResponseDescription())
    }

    def "Failure Backup job creation for AXE platform nodes when job configuration is not provided"(){
        given : "No Job Info to create back job for AXE platform nodes"
        final JobInfo jobInfo = null

        when: "call create job without providing job info data"
        String message;
        try{
            shmJobServiceImpl.createShmJob(jobInfo)
        }catch(Exception exp){
            message = exp.getMessage()
        }

        then:"throws the exceptions"
        assert(message.contains("Job Configuration is not provided"))
    }

    def "Failure Backup job creation for AXE platform nodes when networkelement is not populated"(){
        given : "Prepare Job Info to create back job for AXE platform nodes"
        long poId = createTemplatePO()
        final JobInfo jobInfo = getJobInfo()
        jobInfo.setNeNames(Collections.emptyList())

        when: "call create job without nes"
        String message;
        try{
            shmJobServiceImpl.createShmJob(jobInfo)
        }catch(Exception exp){
            message = exp.getMessage()
        }

        then:"throws the exceptions"
        assert(message.contains("No meFdns specified"))
    }
    def"nodes available in DPS  network element poids based on node names "(){

        given : "Provide node names"

        final List<String> neNames=new ArrayList<String>();
        neNames.add("LTE06dg2ERBS00003");
        neNames.add("LTE06dg2ERBS00006");
        inventoryQueryConfigurationListener.getNeFdnBatchSize()  >> 50
        final NetworkElementIdResponse poids=getNetworkElementPoidsInDps(neNames)

        when :"call getPoids method to get Poids"

        final NetworkElementIdResponse poIdsList = shmJobServiceImpl.getNetworkElementPoIds(neNames);

        then : "responce should be ok and get details"

        assert(poIdsList.getPoIdList() == [4, 5]);
        assert(poIdsList.getPoIdList().size() == 2);
    }

    def"nodes not available in DPS  network element poids based on node names "(){

        given : "Provide node names"

        final List<String> neNames=new ArrayList<String>();
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        inventoryQueryConfigurationListener.getNeFdnBatchSize()  >> 50
        final NetworkElementIdResponse poids=getNetworkElementPoidsNotInDps(neNames)

        when :"call getPoids method to get Poids"

        final NetworkElementIdResponse poIdsList = shmJobServiceImpl.getNetworkElementPoIds(neNames);

        then : "responce should be ok and get details"

        assert(poIdsList.getPoIdList() == []);
        assert(poIdsList.getPoIdList().size() == 0);
    }
}
