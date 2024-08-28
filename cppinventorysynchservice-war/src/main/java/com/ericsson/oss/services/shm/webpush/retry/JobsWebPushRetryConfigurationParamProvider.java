package com.ericsson.oss.services.shm.webpush.retry;

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

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobsWebPushRetryConfigurationParamProvider {


    public int getWebPushRetryCount() {
        return 3;
    }

    public int getWebPushWaitInterval_ms() {
        return 3000;
    }

 

}
