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
package com.ericsson.oss.services.shm.es.impl.minilink.licensing;

import java.util.Collections;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants;
import com.ericsson.oss.services.shm.es.impl.minilink.upgrade.MiniLinkActivityUtil;
import com.ericsson.oss.services.shm.minilinkindoor.common.FtpUtil;
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps;
import com.ericsson.oss.services.shm.es.impl.minilink.common.UnSecureFtpUtil;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.MINILINK_INDOOR_MODEL_IDENTITY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CN210_MODEL_IDENTITY;
import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.CN510R1_MODEL_IDENTITY;

/**
 * This class facilitates the common methods or implementations required for all activities of License Use Case.
 */
@Stateless
@Traceable
@Profiled
public class LicenseUtil {

    @Inject
    private MiniLinkActivityUtil miniLinkActivityUtil;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private FtpUtil ftpUtil;

    @Inject
    private MiniLinkDps miniLinkDps;

    @Inject
    private UnSecureFtpUtil unSecureFtpUtil;

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseUtil.class);

    public String getXfLicenseInstallObjectsFdn(final Long activityJobId) {
        return miniLinkActivityUtil.getManagedObject(activityJobId, MiniLinkConstants.XF_LICENSE_INSTALL_OBJECTS).getFdn();
    }

    public String getLicenseInstallOperStatus(final long activityJobId) {
        final ManagedObject licenseObjectsMo = miniLinkActivityUtil.getManagedObject(activityJobId, MiniLinkConstants.XF_LICENSE_INSTALL_OBJECTS);
        final String licenseInstallOperStatus = licenseObjectsMo.getAttribute(MiniLinkConstants.XF_LICENSE_INSTALL_OPER_STATUS);
        final String nodeName = activityUtils.getJobEnvironment(activityJobId).getNodeName();
        LOGGER.info("xfLicenseInstallOperStatus value: {} on node: {} for acvtivityJobId: {}", licenseInstallOperStatus, nodeName, activityJobId);
        return licenseInstallOperStatus;
    }

    public void setSmrsFtpOnNode(final long activityJobId, final String nodeName) {
        final String ossModelIdentity=miniLinkDps.getOssModelIdentity(nodeName);
        LOGGER.debug("OssModelIdentity for node {} is: {}",nodeName,ossModelIdentity);
        if (MINILINK_INDOOR_MODEL_IDENTITY.equals(ossModelIdentity) || CN210_MODEL_IDENTITY.equals(ossModelIdentity) || CN510R1_MODEL_IDENTITY.equals(ossModelIdentity)) {
            unSecureFtpUtil.setupFtp(nodeName, SmrsServiceConstants.LICENCE_ACCOUNT);
        } else {
            final Integer tableEntry = ftpUtil.setupFtp(nodeName, SmrsServiceConstants.LICENCE_ACCOUNT);
            miniLinkActivityUtil.updateManagedObject(activityJobId, MiniLinkConstants.XF_DCN_FTP,
                    Collections.<String, Object> singletonMap(MiniLinkConstants.XF_LICENSE_INSTALL_ACTIVE_FTP, tableEntry));
        }
    }

}
