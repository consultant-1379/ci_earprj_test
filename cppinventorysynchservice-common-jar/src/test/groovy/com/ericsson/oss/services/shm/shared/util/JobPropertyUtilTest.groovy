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
package com.ericsson.oss.services.shm.shared.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest

class JobPropertyUtilTest extends CdiSpecification{

    @ObjectUnderTest
    private JobPropertyUtil jobPropertyUtil
    
    def "read And update property with new value if existis in the given job properties"(){
        given : "given job properties"
        List<Map<String, String>> jobProperties = new ArrayList<>()
        Map<String,String> passwordProp = new HashMap<>()
        passwordProp.put("key", "Password")
        passwordProp.put("value", "12345")
        jobProperties.add(passwordProp)
        when: "Reading and update password property if exists"
        jobPropertyUtil.updateJobProperty(jobProperties,"Password","EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84=");
        then: "Verify is password property updated with new string"
        for(Map<String,String> eachJobProp : jobProperties){
            for(Map.Entry map: eachJobProp.entrySet()){
                if(map.getKey().equals("Password")){
                    assert(map.getValue().equals("EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84="))
                }
            }
        }
    }
}
