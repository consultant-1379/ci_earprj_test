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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand

/**
 * Stubs <code>RetryManager</code>.
 */
public class RetryManagerStub implements RetryManager {

    @Override
    public <T> T executeCommand(final RetryPolicy retryPolicy, final RetriableCommand<T> command) throws IllegalArgumentException,    RetriableCommandException {
        if(command instanceof ShmDpsRetriableCommand<T>){
            return command.execute();
        }else{
            int counter=1
            def retryContext = new RetryContextStub()
            while(counter <= retryPolicy.getAttempts()){
                try{
                    return command.execute(retryContext);
                }catch(Exception e){
                    if(retryPolicy.getExceptionsToRetry().contains(e.getClass())){
                        counter++
                    }   else{
                        throw e
                    }
                }
            }
        }
    }
}

