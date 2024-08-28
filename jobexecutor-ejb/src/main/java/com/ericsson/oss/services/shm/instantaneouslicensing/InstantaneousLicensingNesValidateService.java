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
package com.ericsson.oss.services.shm.instantaneouslicensing;

import java.util.*;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
public class InstantaneousLicensingNesValidateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstantaneousLicensingNesValidateService.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public Map<NetworkElement, String> filterInstantaneousLicensingSupportedNes(final List<NetworkElement> supportedNes) {

        final List<String> networkElementFdns = new ArrayList<>();
        final List<String> instantaniousLicensingSupportedNes = new ArrayList<>();
        final List<String> instantaniousLicensingUnSupportedNes = new ArrayList<>();

        for (final NetworkElement networkElement : supportedNes) {
            networkElementFdns.add(networkElement.getNetworkElementFdn());
        }
        LOGGER.debug("NetworkElementFdns requested for Instantaneous Licensing are: {}", networkElementFdns);

        final List<Object[]> networkElements = getNetworkElementObjects(networkElementFdns);

        getSupportedAndUnsupportedNes(instantaniousLicensingSupportedNes, instantaniousLicensingUnSupportedNes, networkElements);

        return prepareNetworkElementResponse(instantaniousLicensingUnSupportedNes, supportedNes);
    }

    private List<Object[]> getNetworkElementObjects(final List<String> supportedNes) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery("OSS_NE_DEF", "NetworkElement");
        final Restriction fdnRestriction = query.getRestrictionBuilder().in(ObjectField.MO_FDN, supportedNes.toArray());
        query.setRestriction(fdnRestriction);
        return queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.MO_FDN), ProjectionBuilder.attribute(ShmConstants.TECHNOLOGY_DOMAIN));
    }

    private void getSupportedAndUnsupportedNes(final List<String> instantaniousLicensingSupportedNes, final List<String> instantaniousLicensingUnSupportedNes,
            final List<Object[]> networkElementObjects) {
        for (final Object[] networkElementObject : networkElementObjects) {
            if (networkElementObject != null) {
                final List<String> technologyDomains = (List<String>) networkElementObject[1];
                if (technologyDomains.size() == 1 && technologyDomains.contains(ShmConstants.TECHNOLOGY_DOMAIN_5G)) {
                    instantaniousLicensingSupportedNes.add((String) networkElementObject[0]);
                } else {
                    instantaniousLicensingUnSupportedNes.add((String) networkElementObject[0]);
                }
            }
        }
    }

    private Map<NetworkElement, String> prepareNetworkElementResponse(final List<String> instantaniousLicensingUnSupportedNes, final List<NetworkElement> supportedNes) {
        final Map<NetworkElement, String> unSupportedNes = new HashMap<>();
        if (!instantaniousLicensingUnSupportedNes.isEmpty()) {
            prepareUnsupportedNes(instantaniousLicensingUnSupportedNes, unSupportedNes, supportedNes);
        }
        return unSupportedNes;
    }

    private void prepareUnsupportedNes(final List<String> instantaniousLicensingUnSupportedNes, final Map<NetworkElement, String> unSupportedNes, final List<NetworkElement> supportedNes) {
        for (final String unSupportedNe : instantaniousLicensingUnSupportedNes) {
            for (final NetworkElement networkElement : supportedNes) {
                if (networkElement.getNetworkElementFdn().equals(unSupportedNe)) {
                    unSupportedNes.put(networkElement, "Unsupported technologyDomain");
                }
            }
        }
    }

}
