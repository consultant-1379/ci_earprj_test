/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.es.dps.events;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;

/**
 * This class will throw DatabaseNotAvailableException if database is down.
 * 
 * @author xprapav
 * 
 */

@ApplicationScoped
public class DpsStatusInfoProvider {

    private boolean isDpsAvailable = true;

    @Inject
    private PollingActivityManager pollingActivityManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsStatusInfoProvider.class);

    public void isDatabaseAvailable(final boolean isDpsAvailable) {
        this.isDpsAvailable = isDpsAvailable;
        if (isDpsAvailable) {
            pollingActivityManager.processCachePollingEntries();
        }

    }

    /**
     * Checks whether the database is down, if it is down then throws the {@link DatabaseNotAvailableException}
     * 
     * @param ex
     */
    public void checkDatabaseAvailability(final RuntimeException ex) {

        if (!isDpsAvailable) {
            LOGGER.error("In DpsStatusInfoProvider: Database service not available.");
            throw new DatabaseNotAvailableException(ShmCommonConstants.DATABASE_SERVICE_NOT_AVAILABE, ex);
        }
    }

    /**
     * @return isDatabaseAvailable
     */
    public boolean isDatabaseAvailable() {
        return isDpsAvailable;
    }

}
