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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActionMainAndAdditionalResultHolder;
import com.ericsson.oss.services.shm.es.impl.cpp.common.CvActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.shm.inventory.backup.entities.AdminProductData;

@Traceable
@Profiled
public class ConfigurationVersionService {

    @Inject
    CommonCvOperations commonCvOperations;

    @Inject
    ConfigurationVersionUtils cvUtil;

    /**
     * This method retrieves the Configuration Version MO attributes.
     * 
     * @param neName
     * @return Map<String, Object>
     */
    public Map<String, Object> getCVMoAttr(final String nodeName) {
        final Map<String, Object> moAttributesMap = new HashMap<String, Object>();
        final ConfigurationVersionMO cvMo = commonCvOperations.getCVMo(nodeName);
        if (cvMo != null) {
            moAttributesMap.put(ShmConstants.FDN, cvMo.getFdn());
            moAttributesMap.put(ShmConstants.MO_ATTRIBUTES, cvMo.getAllAttributes());
        }
        return moAttributesMap;
    }

    /**
     * This method retrieves the Configuration Version MO attributes.
     * 
     * @param neName
     * @return Map<String, Object>
     */
    public String getCVMoFdn(final String nodeName) {
        return commonCvOperations.getCVMoFdn(nodeName);
    }

    /**
     * This method retrieves the Upgrade Package MO attributes.
     * 
     * @param neName
     *            , searchValue
     * @return Map<String, Object>
     */
    public Map<String, Object> getUpgradePackageMo(final String nodeName, final String searchValue) {
        final Map<String, Object> upMoAttributesMap = new HashMap<String, Object>();
        final UpgradePackageMO upMo = commonCvOperations.getUPMo(nodeName, searchValue);
        if (upMo != null) {
            final Map<String, Object> upMoAttributes = upMo.getAllAttributes();
            upMoAttributesMap.put(ShmConstants.FDN, upMo.getFdn());
            upMoAttributesMap.put(ShmConstants.MO_ATTRIBUTES, upMoAttributes);
            return upMoAttributesMap;
        }
        return null;
    }

    /**
     * This method retrieves the Configuration Version MO POJO. see {@link ConfigurationVersionMO}
     * 
     * @param neName
     * @return {@link ConfigurationVersionMO}
     */
    public ConfigurationVersionMO getCvMOFromNode(final String nodeName) {
        ConfigurationVersionMO cvMO = null;
        CvActivity cvActivity = null;
        CvActionMainAndAdditionalResultHolder cvMainActionHolder = null;
        String fdn = null;
        final ConfigurationVersionMO cvMOBean = commonCvOperations.getCVMo(nodeName);
        if (cvMOBean != null) {
            fdn = cvMOBean.getFdn();
            final Map<String, Object> cvMOattr = cvMOBean.getAllAttributes();
            cvActivity = cvUtil.getCvActivity(cvMOattr);
            cvMainActionHolder = cvUtil.retrieveActionResultDataWithAddlInfo(cvMOattr);
            final List<AdminProductData> missingUps = cvUtil.getMissingUps(cvMOattr);
            final List<AdminProductData> corruptedUps = cvUtil.getCorrputedUps(cvMOattr);
            cvMO = new ConfigurationVersionMO(fdn, cvActivity, cvMainActionHolder, missingUps, corruptedUps);
        }

        return cvMO;
    }

}
