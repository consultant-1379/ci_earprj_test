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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import java.util.Map

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants

class AxeJobConfigurationServiceImplTest extends CdiSpecification{

    @ObjectUnderTest
    private AxeJobConfigurationServiceImpl axeJobConfigurationServiceImpl;
    
    def 'Validating progress percentage attribute from each OPS notification before persisting in DB'(){

        given:"Providing activityProgress percentage for validation"
        def Map<String, Object> validatedAttributes = new HashMap<>();
        def Map<String, Object> activityJobAttr = new HashMap<>();

        when:"preparing Latest Activity Progress to Persist In DB"
        axeJobConfigurationServiceImpl.prepareLatestActivityProgresstoPersistInDB(activityProgressPercentage,validatedAttributes,activityJobAttr);

        then: "negative progress should not be updated in attributes which are going to be perisisted"
        validatedAttributes.get(ShmConstants.PROGRESSPERCENTAGE)==expected
        
        where :
        activityProgressPercentage  | expected
        -1.0                        | null
        10.0                        | 10.0
        0.0                         | 0.0
        150.0                       |100.0
    }
    
    
}
