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
package com.ericsson.oss.services.shm.job.service

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.job.utils.CreateJobAdditionalDataProvider
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter

class CreateJobAdditionalDataProviderTest extends CdiSpecification{

    @ObjectUnderTest
    private CreateJobAdditionalDataProvider createJobAdditionalDataProvider

    @MockedImplementation
    private EncryptAndDecryptConverter encryptAndDecryptConverter;

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def "read And update password property if existis in Ne Type job properties"(){
        given : "given job properties for an neType"
        List<Map<String, String>> jobProperties = new ArrayList<>()
        Map<String,String> passwordProp = new HashMap<>()
        passwordProp.put("key", "Password")
        passwordProp.put("value", "12345")
        jobProperties.add(passwordProp)
        encryptAndDecryptConverter.getEncryptedPassword("12345") >> "EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84=";
        when: "Reading and update password property after encryption"
        createJobAdditionalDataProvider.readAndEncryptPasswordProperty(jobProperties,"12345");
        then: "Verify is password property updated with encrypted string"
        for(Map<String,String> eachJobProp : jobProperties){
            for(Map.Entry map: eachJobProp.entrySet()){
                if(map.getKey().equals("Password")){
                    assert(map.getValue().equals("EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84="))
                }
            }
        }
    }
}
    
