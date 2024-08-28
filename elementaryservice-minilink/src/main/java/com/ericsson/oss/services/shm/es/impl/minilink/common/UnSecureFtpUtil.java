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

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CONFIG_DOWNLOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CONFIG_UPLOADING;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.FTP_RETRY_ATTEMPTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.FTP_RETRY_INTERVAL;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SMRS_NODE_TYPE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.TRANSFER_ACTIVE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP_ADDRESS_1;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP_CONFIG_PWD;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP_USERNAME_1;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_NE_PM;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_PM_FILE_STATUS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_DCN_FTP_ACTIVE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XFDCNFTPACTIVE_VALUE;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.DEFAULT_PWD;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.DEFAULT_ADDRESS;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.DEFAULT_USERNAME;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.SET_FTP_FAILED;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;

@Stateless
public class UnSecureFtpUtil {
    @Inject
    private SmrsFileStoreService smrsFileStore;

    @Inject
    private ManagedObjectUtil managedObjectUtil;

    @Inject
    private RetryManager retryManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(UnSecureFtpUtil.class);

    public void setupFtp(final String nodeName, final String smrsServiceType) {
        setDefaultFTPValues(nodeName);
        final RetryPolicy policy = RetryPolicy.builder().attempts(FTP_RETRY_ATTEMPTS).waitInterval(FTP_RETRY_INTERVAL, TimeUnit.SECONDS)
                .exponentialBackoff(1.0).retryOn(FTPRetryException.class).build();
        try {
            retryManager.executeCommand(policy, new RetriableCommand<Void>() {
                @Override
                public Void execute(final RetryContext retryContext) throws FTPRetryException {
                    findFtpEntry(nodeName, smrsServiceType);
                    return null;
                }
            });
        } catch (RetriableCommandException e) {
            LOGGER.error("Failed to set FTP details: {}", e);
        } catch (Exception e) {
            LOGGER.warn("Failed to set FTP details: {}", e);
        }
    }

    public void findFtpEntry(final String nodeName, final String smrsServiceType) throws FTPRetryException {
        final ManagedObject configLoadObjectsMo = managedObjectUtil.getManagedObject(nodeName, XF_CONFIG_LOAD_OBJECTS);
        final String configStatus = configLoadObjectsMo.getAttribute(XF_CONFIG_STATUS);
        final ManagedObject pmMo = managedObjectUtil.getManagedObject(nodeName, XF_NE_PM);
        final String pmStatus = pmMo.getAttribute(XF_PM_FILE_STATUS);
        if (configStatus.equals(CONFIG_UPLOADING) || pmStatus.equals(TRANSFER_ACTIVE) || configStatus.equals(CONFIG_DOWNLOADING)) {
            LOGGER.debug("configStatus:{} and pmStatus:{}", configStatus, pmStatus);
            throw new FTPRetryException(SET_FTP_FAILED);
        } else {
            final Map<String, Object> ftpAttributes = new HashMap<>();
            final ManagedObject xfDcnFtpMo = managedObjectUtil.getManagedObject(nodeName, XF_DCN_FTP);
            final SmrsAccountInfo smrsInfo = smrsFileStore.getSmrsDetails(smrsServiceType, SMRS_NODE_TYPE, nodeName);
            ftpAttributes.put(XF_DCN_FTP_CONFIG_PWD, String.valueOf(smrsInfo.getPassword()));
            ftpAttributes.put(XF_DCN_FTP_ADDRESS_1, smrsInfo.getServerIpAddress());
            ftpAttributes.put(XF_DCN_FTP_USERNAME_1, smrsInfo.getUser());
            ftpAttributes.put(XF_DCN_FTP_ACTIVE, XFDCNFTPACTIVE_VALUE);
            xfDcnFtpMo.setAttributes(ftpAttributes);
        }
    }

    private void setDefaultFTPValues(final String nodeName) {
        final Map<String, Object> ftpAttributes = new HashMap<>();
        final ManagedObject xfDcnFtpMo = managedObjectUtil.getManagedObject(nodeName, XF_DCN_FTP);
        ftpAttributes.put(XF_DCN_FTP_CONFIG_PWD, DEFAULT_PWD);
        ftpAttributes.put(XF_DCN_FTP_ADDRESS_1, DEFAULT_ADDRESS);
        ftpAttributes.put(XF_DCN_FTP_USERNAME_1, DEFAULT_USERNAME);
        ftpAttributes.put(XF_DCN_FTP_ACTIVE, XFDCNFTPACTIVE_VALUE);
        xfDcnFtpMo.setAttributes(ftpAttributes);
    }
}
