/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.impl.cpp.backup

import com.ericsson.oss.itpf.sdk.core.retry.*

/**
 * Stubs <code>RetryManager</code>.
 */
public class CppRetryManagerStub implements RetryManager {

    @Override
    public <T> T executeCommand(final RetryPolicy retryPolicy, final RetriableCommand<T> command) throws IllegalArgumentException,
    RetriableCommandException {
        try {
            return command.execute();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e); // NOPMD
        }
    }
}
