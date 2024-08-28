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
package com.ericsson.oss.services.shm.tbac;

import java.util.List;

/**
 * @author tcssbop This interface is used to validate TBAC for cancel actions.
 * 
 */
public interface JobAdministratorTBACValidator {

    /**
     * This method is used to validate TBAC for Main Job.
     * 
     * @param mainJobId
     * @param mainJobTemplateId
     * @param cancelledBy
     * @return true/false
     */
    boolean validateTBACForMainJob(final long mainJobId, final long mainJobTemplateId, final String cancelledBy);

    /**
     * This method is used to validate TBAC for Ne Job.
     * 
     * @param nodeName
     * @param cancelledBy
     * @return true/false
     */
    boolean validateTBACForNEJob(final String nodeName, final String cancelledBy);

    /**
     * This method is used to validate TBAC for NeJobs.
     * 
     * @param nodeNames
     * @param cancelledBy
     * @return true/false
     */
    boolean validateTBACAtJobLevel(final List<String> nodeNames, final String cancelledBy);

}
