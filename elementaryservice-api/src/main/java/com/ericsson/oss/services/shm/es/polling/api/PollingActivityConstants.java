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
package com.ericsson.oss.services.shm.es.polling.api;

/**
 * This class contains all the constants used for Polling.
 * 
 * @author xsrabop
 */
public class PollingActivityConstants {

    public static final String ACTIVITY_JOB_ID = "activityJobId";
    public static final String MO_FDN = "moFdn";
    public static final String MO_ATTRIBUTES = "moAttributes";
    public static final String POLL_INITIATED_TIME = "pollInitiatedTime";
    public static final String MAX_WAIT_TIME_TO_READ = "maxWaitTimeToRead";
    public static final String MIM_VERSION = "mimVersion";
    public static final String NAMESPACE = "namespace";
    public static final String POLL_CYCLE_STATUS = "pollCycleStatus";
    public static final String POLLING_CALLBACK_QUEUE = "callbackQueue";
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    public static final String NE_OSS_PREFIX = "ossPrefix";
    public static final String POLLING_TYPE = "type";
    public static final String LOG_MESSAGE = "MoFdn:\"%s\"  MO Attributes: \"%s\" ActivityjobId: \"%s\" Namespace: \"%s\" MIM Version: \"%s\" Additional Information:\"%s\" ";

    //For MoAction
    public static final String ACTION_NAME = "actionName";
    public static final String MO_ACTION_ATTRIBUTES = "moActionAttributes";
    public static final String MO_NAME = "moName";
    public static final String IS_ACTION_ALREADY_RUNNING = "isActionAlreadyRunning";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String RETRY_COUNT = "retryCount";
    public static final String OPERATION_TYPE = "operationType";

}
