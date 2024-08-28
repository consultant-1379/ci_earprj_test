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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.notifications.impl.AbstractNotificationListener;

@Singleton
@Startup
public class CppBackupNotificationQueueObserver extends AbstractNotificationListener {

    @Override
    protected String getFilter() {
        return ShmCommonConstants.SHM_BACKUP_NOTFICATION_FILTER;
    }

}
