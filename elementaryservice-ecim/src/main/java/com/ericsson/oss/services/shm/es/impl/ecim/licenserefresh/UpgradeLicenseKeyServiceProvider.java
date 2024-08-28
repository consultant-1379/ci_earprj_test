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

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.EUFT;
import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.INSTANTANEOUS_LICENSING_MO;
import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.INSTANTANEOUS_LICENSING_NAMESPACE;
import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.SWLT_ID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.events.instlicense.ShmLicenseRefreshElisRequest;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;

@Stateless
public class UpgradeLicenseKeyServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeLicenseKeyServiceProvider.class);

    @Inject
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private LicenseMoService licenseMoService;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private DpsReader dpsReader;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public ShmLicenseRefreshElisRequest prepareShmLicenseRefreshElisRequestEvent(final NEJobStaticData neJobStaticData, final Map<String, Object> mainJobAttributes)
            throws MoNotFoundException, UnsupportedFragmentException {
        LOGGER.debug("Entered  prepareShmLicenseRefreshElisRequestEvent of UpgradeLicenseKeyService for the NeJob {} ", neJobStaticData.getNeJobId());
        String euft = null;
        String swltId = null;
        final String neType = getNeType(neJobStaticData);
        final List<ManagedObject> instantaneousLicensingMOs = findInstantaneousLicensingMOs(neJobStaticData.getNodeName());
        if (instantaneousLicensingMOs != null && !instantaneousLicensingMOs.isEmpty()) {
            euft = instantaneousLicensingMOs.get(0).getAttribute(EUFT);
            swltId = instantaneousLicensingMOs.get(0).getAttribute(SWLT_ID);
        }
        final ShmLicenseRefreshElisRequest shmLicenseRefreshElisRequest = new ShmLicenseRefreshElisRequest();
        shmLicenseRefreshElisRequest.setJobId(String.valueOf(neJobStaticData.getNeJobId()));
        shmLicenseRefreshElisRequest.setFingerprint(getFingerPrintOnNode(neJobStaticData.getNodeName()));
        shmLicenseRefreshElisRequest.setRequestType(LicenseRefreshConstants.REFRESH);
        shmLicenseRefreshElisRequest.setEuft(euft);
        shmLicenseRefreshElisRequest.setSwltId(swltId);
        shmLicenseRefreshElisRequest.setSwRelease(getSoftwarePackageReleaseVersion(neJobStaticData, mainJobAttributes, neType));
        final String networkElementFdn = getNodeFdn(neJobStaticData.getNodeName());
        shmLicenseRefreshElisRequest.setNodeType(getTechnologyDomain(networkElementFdn));
        LOGGER.info("ShmLicenseRefreshElisRequest {} prepared for NE  Job {} ", shmLicenseRefreshElisRequest, neJobStaticData.getNeJobId());
        return shmLicenseRefreshElisRequest;
    }

    @SuppressWarnings("unchecked")
    public String getLkfRequestTypeInitiatedByNodeOrSoftwarePackage(final Map<String, Object> mainJobAttributes) {
        if (mainJobAttributes != null) {
            final long templateJobId = (long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID);
            final Map<String, Object> templateJobAttributes = activityUtils.getPoAttributes(templateJobId);
            final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) templateJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Map<String, String>> jobPropertyList = (List<Map<String, String>>) jobConfigurationDetails.get(ShmConstants.JOBPROPERTIES);
            return JobPropertyUtil.getProperty(jobPropertyList, LicenseRefreshConstants.LICENSE_REFRESH_TYPE);
        }
        return null;
    }

    /**
     * @param neJobStaticData
     * @return
     * @throws MoNotFoundException
     */
    private String getNeType(final NEJobStaticData neJobStaticData) throws MoNotFoundException {
        return networkElementRetrievalBean.getNeType(neJobStaticData.getNodeName());
    }

    private String getFingerPrintOnNode(final String nodeName) throws MoNotFoundException, UnsupportedFragmentException {
        final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return licenseMoService.getFingerPrintFromNode(networkElement);
    }

    private String getSoftwarePackageReleaseVersion(final NEJobStaticData neJobStaticData, final Map<String, Object> mainJobAttributes, final String neType) {
        final String swPkgName = getSoftwarePackageName(neJobStaticData, mainJobAttributes, neType);
        return remoteSoftwarePackageManager.getSoftwarPackageReleaseVersion(swPkgName);
    }

    /**
     * @param neType
     * @param neJobStaticData
     * @param mainJobAttributes
     * @throws MoNotFoundException
     */
    @SuppressWarnings("unchecked")
    private String getSoftwarePackageName(final NEJobStaticData neJobStaticData, final Map<String, Object> mainJobAttributes, final String neType) {

        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(Arrays.asList(UpgradeActivityConstants.SWP_NAME), jobConfigurationDetails, neJobStaticData.getNodeName(), neType,
                neJobStaticData.getPlatformType());
        return String.valueOf(keyValueMap.get(UpgradeActivityConstants.SWP_NAME));
    }

    private List<ManagedObject> findInstantaneousLicensingMOs(final String nodeName) {
        final Map<String, Object> restrictions = new HashMap<>();
        return dpsReader.getManagedObjects(INSTANTANEOUS_LICENSING_NAMESPACE, INSTANTANEOUS_LICENSING_MO, restrictions, nodeName);
    }

    private String getNodeFdn(final String nodeName) throws MoNotFoundException {
        final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
        return networkElement.getNeFdn();
    }

    private String getTechnologyDomain(final String networkElementFdn) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery(ShmCommonConstants.NAMESPACE_NETWORK_ELEMENT, ShmCommonConstants.MO_TYPE_NETWORK_ELEMENT);
        final Restriction fdnRestriction = query.getRestrictionBuilder().in(ObjectField.MO_FDN, networkElementFdn);
        final Restriction techDomainRestriction = query.getRestrictionBuilder().equalTo("technologyDomain", Arrays.asList("5GS"));
        query.setRestriction(query.getRestrictionBuilder().allOf(fdnRestriction, techDomainRestriction));
        Iterator<ManagedObject> networkElements = queryExecutor.execute(query);

        if (networkElements != null && networkElements.hasNext()) {
            return "5G";
        } else {
            LOGGER.debug("Technology domain for NetworkElement having FDN : {} is not available", networkElementFdn);
        }
        return null;
    }

}
