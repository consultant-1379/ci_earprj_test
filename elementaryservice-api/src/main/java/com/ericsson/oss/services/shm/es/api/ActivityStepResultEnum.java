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
package com.ericsson.oss.services.shm.es.api;

/**
 * To be used in Activity step result instead of boolean value.
 */
public enum ActivityStepResultEnum {
    PRECHECK_FAILED_SKIP_EXECUTION, PRECHECK_SUCCESS_PROCEED_EXECUTION, PRECHECK_SUCCESS_SKIP_EXECUTION, EXECUTION_SUCESS, EXECUTION_FAILED, EXECUTION_TRIGGERED_EVALUATE_RESULT, TIMEOUT_RESULT_SUCCESS, TIMEOUT_RESULT_FAIL, TIMEOUT_RETRY_ACTIVITY, REPEAT_EXECUTE, TIMEOUT_REPEAT_EXECUTE_MANUAL, TIMEOUT_ACTIVITY_ONGOING, REPEAT_PRECHECK;

}
