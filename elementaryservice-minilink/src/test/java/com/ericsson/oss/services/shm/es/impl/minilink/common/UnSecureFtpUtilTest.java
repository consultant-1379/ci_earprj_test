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
package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CONFIG_UPLOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SMRS_NODE_TYPE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.TRANSFER_ACTIVE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_NE_PM;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_PM_FILE_STATUS;
import static com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants.BACKUP_ACCOUNT;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;

@RunWith(MockitoJUnitRunner.class)
public class UnSecureFtpUtilTest extends BackupRestoreTestBase {

    @ObjectUnderTest
    private UnSecureFtpUtil unSecureFtpUtil;

    @MockedImplementation
    private ManagedObject managedObject;

    @MockedImplementation
    private SmrsFileStoreService smrsFileStore;

    @ImplementationInstance
    private RetryManager retryManager = new RetryManagerNonCDIImpl();

    @Test
    public void testSetupFtp() {
        when(managedObjectUtil.getManagedObject(NODE_NAME, XF_CONFIG_LOAD_OBJECTS)).thenReturn(managedObject);
        when(managedObjectUtil.getManagedObject(NODE_NAME, XF_NE_PM)).thenReturn(managedObject);
        when(managedObjectUtil.getManagedObject(NODE_NAME, XF_DCN_FTP)).thenReturn(managedObject);
        when(managedObject.getAttribute(XF_CONFIG_STATUS)).thenReturn("2");
        when(managedObject.getAttribute(XF_PM_FILE_STATUS)).thenReturn("2");
        when(managedObject.getName()).thenReturn("1");
        when(smrsFileStore.getSmrsDetails(BACKUP_ACCOUNT, SMRS_NODE_TYPE, NODE_NAME)).thenReturn(setSmrsDetails());
        unSecureFtpUtil.setupFtp(NODE_NAME, BACKUP_ACCOUNT);
    }

    private SmrsAccountInfo setSmrsDetails() {
        String pswd = "password";
        SmrsAccountInfo smrsAccount = new SmrsAccountInfo();
        char[] pwd = pswd.toCharArray();
        smrsAccount.setServerIpAddress("");
        smrsAccount.setPassword(pwd);
        smrsAccount.setUser("mm_cli_backup");
        return smrsAccount;
    }
}
