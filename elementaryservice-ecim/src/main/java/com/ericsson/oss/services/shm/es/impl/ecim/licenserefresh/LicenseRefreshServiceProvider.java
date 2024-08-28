/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.licenserefresh.LicenseRefreshSource;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Stateless
public class LicenseRefreshServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseRefreshServiceProvider.class);

    @Inject
    private DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    public List<PersistenceObject> findShmNodeLicenseRefreshRequestDataPOs(final long neJobId) {

        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(ShmConstants.NE_JOB_ID, String.valueOf(neJobId));
        return dpsReader.findPOs(ShmConstants.NAMESPACE, SHM_NODE_LICENSE_REFRESH_REQUEST_DATA, restrictions);
    }

    public void createShmNodeLicenseRefreshRequestDataPO(final int actionId, final String nodeName, final long neJobId) {

        final Map<String, Object> licenseRefreshRequestDataAttributes = new HashMap<>();

        licenseRefreshRequestDataAttributes.put(NETWORK_ELEMENT_NAME, nodeName);
        licenseRefreshRequestDataAttributes.put(ShmConstants.NE_JOB_ID, String.valueOf(neJobId));
        licenseRefreshRequestDataAttributes.put(ShmConstants.STATUS, NODE_REFRESH_REQUESTED);
        licenseRefreshRequestDataAttributes.put(LICENSE_REFRESH_SOURCE, LicenseRefreshSource.SHM.name());
        licenseRefreshRequestDataAttributes.put(CORRELATION_ID, String.valueOf(actionId));

        final Map<String, Object> licenseRefreshRequestInfo = new HashMap<>();
        licenseRefreshRequestInfo.put(ActivityConstants.ACTION_ID, actionId);
        licenseRefreshRequestDataAttributes.put(LICENSE_REFRESH_REQUEST_INFO, licenseRefreshRequestInfo);

        dpsWriter.createPO(ShmConstants.NAMESPACE, SHM_NODE_LICENSE_REFRESH_REQUEST_DATA, ShmConstants.VERSION, licenseRefreshRequestDataAttributes);

    }

    public void deleteShmNodeLicenseRefreshRequestDataPO(final String nodeName) {

        final List<PersistenceObject> shmNodeLicenseRefreshRequestDataPOs = findShmNodeLicenseRefreshRequestDataPOs(nodeName);

        if (shmNodeLicenseRefreshRequestDataPOs != null && !shmNodeLicenseRefreshRequestDataPOs.isEmpty()) {
            shmNodeLicenseRefreshRequestDataPOs.forEach(shmNodeLicenseRefreshRequestDataPo -> {
                LOGGER.debug("LicenseRefreshJob: Deleting shmNodeLicenseRefreshRequestDataPo [{}] having nodeName [{}]", shmNodeLicenseRefreshRequestDataPo.getPoId(), nodeName);
                dpsWriter.deletePoByPoId(shmNodeLicenseRefreshRequestDataPo.getPoId());
            });
        }
    }

    public Map<String, Object> prepareShmLicenseRefreshElisRequestEvent(final String nodeName) {
        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(NODE_NAME, nodeName);
        final List<PersistenceObject> shmNodeLicenseRefreshRequestDataPos = dpsReader.findPOs(ShmConstants.NAMESPACE, SHM_NODE_LICENSE_REFRESH_REQUEST_DATA, restrictions);
        if (shmNodeLicenseRefreshRequestDataPos != null && !shmNodeLicenseRefreshRequestDataPos.isEmpty()) {

            final Map<String, Object> shmNodeLicenseRefreshRequestDataAttributes = shmNodeLicenseRefreshRequestDataPos.get(0).getAllAttributes();
            LOGGER.debug("LicenseRefreshJob:Request activity : shmNodeLicenseRefreshRequestDataAttributes : {}", shmNodeLicenseRefreshRequestDataAttributes);
            return (Map<String, Object>) shmNodeLicenseRefreshRequestDataAttributes.get(LICENSE_REFRESH_REQUEST_INFO);
        }
        return null;
    }

    public void deleteLkfRequestDataPo(final long neJobId, final String fingerprint) {
        int deletedPosCount = 0;
        final List<PersistenceObject> lkfRequestDataPos = getLkfRequestDataPos();
        for (final PersistenceObject lkfRequestDataPo : lkfRequestDataPos) {
            Map<String, Object> neJobIds = lkfRequestDataPo.getAttribute(JOB_IDS);
            if (neJobId == Long.valueOf((String) neJobIds.get(fingerprint))) {
                LOGGER.debug("LicenseRefreshJob:Request activity deleting LkfRequestDataPo [{}] having jobId [{}] and fingerPrint [{}]", lkfRequestDataPo.getPoId(), neJobId, fingerprint);
                deletedPosCount = dpsWriter.deletePoByPoId(lkfRequestDataPo.getPoId());
                LOGGER.debug("LicenseRefreshJob:Request activity deleted [{}] number of LkfRequestDataPos", deletedPosCount);
                break;
            }
        }
    }

    public String findInstantaneousLicensingMOFdn(final String nodeName) {

        final Map<String, Object> restrictions = new HashMap<>();
        final List<ManagedObject> instantaneousLicensingMOs = dpsReader.getManagedObjects(INSTANTANEOUS_LICENSING_NAMESPACE, INSTANTANEOUS_LICENSING_MO, restrictions, nodeName);
        String instantaneousLicensingMOFdn = "";
        if (instantaneousLicensingMOs != null) {
            instantaneousLicensingMOFdn = instantaneousLicensingMOs.get(0).getFdn();
        }
        return instantaneousLicensingMOFdn;
    }

    public int performMOAction(final String instantaneousLicensingMOFdn) {
        int actionId = -1;
        try {
            actionId = dpsWriter.performAction(instantaneousLicensingMOFdn, MO_ACTION_REFRESH_KEY_FILE, new HashMap<String, Object>());
            LOGGER.debug("LicenseRefreshJob:Refresh activity performed MO Action on instantaneousLicensingMOFdn [{}] with actionId as [{}]", instantaneousLicensingMOFdn, actionId);
        } catch (Exception ex) {
            LOGGER.error("Exception occured while performAction for moFdn:{} with Exception:{}", instantaneousLicensingMOFdn, ex);
        }
        return actionId;
    }

    public List<PersistenceObject> findShmNodeLicenseRefreshRequestDataPOs(final String nodeName) {

        final Map<String, Object> restrictions = new HashMap<>();
        restrictions.put(NODE_NAME, nodeName);
        return dpsReader.findPOs(ShmConstants.NAMESPACE, SHM_NODE_LICENSE_REFRESH_REQUEST_DATA, restrictions);
    }

    private List<PersistenceObject> getLkfRequestDataPos() {
        final Map<String, Object> restrictions = new HashMap<>();
        return dpsReader.findPOs(ShmConstants.NAMESPACE, LKF_REQUEST_DATA, restrictions);
    }

}
