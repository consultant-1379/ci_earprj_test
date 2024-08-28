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
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.security.cryptography.CryptographyService;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.axe.common.AbstractAxeService;
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants;
import com.ericsson.oss.services.shm.es.axe.common.DpsUtil;
import com.ericsson.oss.services.shm.es.axe.common.NeworkElementSecurityMO;
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider;
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter;

public abstract class AbstractAxeBackupService extends AbstractAxeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAxeBackupService.class);

    @Inject
    protected PollingActivityManager pollingActivityManager;

    @Inject
    protected DpsStatusInfoProvider dpsStatusInfoProvider;

    @Inject
    protected DpsUtil dpsUtil;

    @Inject
    protected CryptographyService cryptographyService;

    @Inject
    EncryptAndDecryptConverter encryptAndDecryptConverter;

    protected void subscribe(final long activityJobId, final ActivityInfo activityAnnotation) {

        final JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, activityAnnotation.activityName(), activityAnnotation.jobType(), activityAnnotation.platform());
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.BACKUP_JOB_CAPABILITY);
            final NetworkElementData networkElementData = networkElementRetrivalBean.getNetworkElementData(neJobStaticData.getParentNodeName());

            pollingActivityManager.subscribe(jobActivityInfo, networkElementData, null, ShmCommonConstants.MO_TYPE_NETWORK_ELEMENT + "=" + neJobStaticData.getParentNodeName(),
                    Collections.<String> emptyList());

            LOGGER.debug("Polling subscribed for activityJobId:{}", activityJobId);
        } catch (final RuntimeException ex) {
            LOGGER.error("Polling subscription for activityJobId:{} failed. reason:  ", activityJobId, ex);
            if (dpsStatusInfoProvider.isDatabaseAvailable()) {
                pollingActivityManager.prepareAndAddPollingActivityDataToCache(activityJobId, jobActivityInfo);
            }
        } catch (final Exception ex) {
            LOGGER.error("Polling subscription failed for activityJobId:{}. reason:  ", activityJobId, ex);
        }
    }

    protected Map<String, Object> prepareHeadersInformation(final String nodeName, final String componentName, final Map<String, String> encryptBackupWithPwdDetails, final String activityName)
            throws UnsupportedEncodingException {
        final StringBuilder securityInfoheader = new StringBuilder();
        final Map<String, Object> headerMap = new HashMap<>();
        final NeworkElementSecurityMO nodeSecurityInfo = dpsUtil.getSecurityAttributesfromNEChild(nodeName);
        final byte[] passwordInBase64 = DatatypeConverter.parseBase64Binary(nodeSecurityInfo.getPassword());
        final byte[] resultPrivDec = cryptographyService.decrypt(passwordInBase64);
        final String decryptedPwd = new String(resultPrivDec, StandardCharsets.UTF_8);
        securityInfoheader.append(nodeSecurityInfo.getUserName()).append(":").append(decryptedPwd);
        headerMap.put("Authorization", "Basic " + DatatypeConverter.printBase64Binary(securityInfoheader.toString().getBytes("UTF-8")));
        if (isApgSupportSecuredBackup(activityName, componentName, encryptBackupWithPwdDetails)) {
            final StringBuilder backupAuthHeader = new StringBuilder();
            final String decryptedBkpPwd = encryptAndDecryptConverter.getDecryptedPassword(encryptBackupWithPwdDetails.get(JobPropertyConstants.SECURE_BACKUP_KEY));
            backupAuthHeader.append(nodeSecurityInfo.getUserName()).append(":").append(decryptedBkpPwd);
            headerMap.put(AxeConstants.ENCRYPTED_BACKUP_HEADER, DatatypeConverter.printBase64Binary(backupAuthHeader.toString().getBytes("UTF-8")));
            String userLabel = encryptBackupWithPwdDetails.get(JobPropertyConstants.USER_LABEL);
            if (userLabel == null || userLabel.isEmpty()) {
                userLabel = JobPropertyConstants.DEFAULT_USER_LABEL_FROM_ENM;
            }
            headerMap.put(JobPropertyConstants.WINFIOL_REQUEST_LABEL_HEADER, userLabel);

        }
        return headerMap;
    }

    /**
     * fetch the componentType from nodeName.
     * <p>
     * eg:- input: MSC17__CP1 return: CP1
     *
     * @param nodeName
     * @return componentType
     */
    protected static String getComponentType(final String nodeName) {
        String componentType = "";
        if (nodeName.contains(AxeConstants.CLUSTER_SUFFIX)) {
            componentType = AxeConstants.CLUSTER_BACKUP;
        } else if (nodeName.contains(AxeConstants.NODE_COMPONENT_SEPERATOR)) {
            final String[] nodeAndClusterNames = nodeName.split(AxeConstants.NODE_COMPONENT_SEPERATOR);
            componentType = nodeAndClusterNames[nodeAndClusterNames.length - 1];
        } else {
            LOGGER.warn("ComponentType Not Found for {}", nodeName);
        }
        return componentType;
    }

    private boolean isApgSupportSecuredBackup(final String activityName, final String componentName, final Map<String, String> encryptBackupWithPwdDetails) {
        return (activityName.equals(ActivityConstants.CREATE_BACKUP) && componentName.contains(AxeConstants.APG_COMPONENT)
                && encryptBackupWithPwdDetails.containsKey(JobPropertyConstants.SECURE_BACKUP_KEY) && encryptBackupWithPwdDetails.get(JobPropertyConstants.SECURE_BACKUP_KEY) != null
                && !encryptBackupWithPwdDetails.get(JobPropertyConstants.SECURE_BACKUP_KEY).isEmpty());
    }
}
