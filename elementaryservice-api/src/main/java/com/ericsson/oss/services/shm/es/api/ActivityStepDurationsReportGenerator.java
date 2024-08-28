/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.api;

/**
 * This interface is used for each main Job report generation at the specified location.
 * 
 * @author xarirud, tcschat
 */
public interface ActivityStepDurationsReportGenerator {

    void generateJobReportAndUpdateMainJob(final long mainJobPoId);

    void triggerJobReportGenerationThroughPibScript(final String jobName);

}
