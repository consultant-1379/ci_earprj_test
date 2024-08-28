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

package com.ericsson.oss.services.shm.es.impl.minilink.backup;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.impl.minilink.common.BackupActivityProperties;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_COMMAND;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigStatus.CONFIG_UP_LOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.XfConfigLoadCommand.CONFIG_UPLOAD;

/**
 * This bean gives an API for the DPS related operations necessary for creating a MINI-LINK backup.
 */

@Stateless
@Traceable
@Profiled
public class BackupDps {

    @Inject
    private MiniLinkDps miniLinkDps;

    /**
     * Initiates the backup upload by setting xfConfigLoadCommand to configUpload in the mirror model. Additionally, it sets the
     * xfConfigStatus attribute to configUpLoading. This is necessary since this attribute remains configUpLoadOK after a successful backup upload
     * and it may happen that no attribute change notification would be triggered otherwise.
     * @param backupActivityProperties
     */
    public void initBackup(final BackupActivityProperties backupActivityProperties) {
        final String nodeName = backupActivityProperties.getNodeName();
        miniLinkDps.setXfConfigStatusWithoutMediation(nodeName, CONFIG_UP_LOADING.getStatusValue());
        miniLinkDps.updateManagedObjectAttribute(nodeName, XF_CONFIG_LOAD_OBJECTS, XF_CONFIG_LOAD_COMMAND, CONFIG_UPLOAD.getStatusValue());
    }

}
