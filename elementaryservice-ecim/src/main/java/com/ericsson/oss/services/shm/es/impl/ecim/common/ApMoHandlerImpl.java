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
package com.ericsson.oss.services.shm.es.impl.ecim.common;

import static com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimInventoryConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;


import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;

/**
 * Responsible for executing the Actions or Read attributes on the AutoProvisioning MO.
 * @author xmadupp
 *
 */
@Stateless
public class ApMoHandlerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApMoHandlerImpl.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private ActivityUtils activityUtils;

    private DataBucket getLiveBucket() {
        try {
            return dataPersistenceService.getLiveBucket();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving live bucket. Reason : {}", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException(INV_INTERNAL_ERROR);
        }
    }

    /**
     * Update AutoProvisioning MO Attributes.
     * @param fdn AP MO fdn.
     * @param moArguments attributes to set.
     * @throws MoNotFoundException
     */
    public void updateApMoAttributes(final String moFdn, final Map<String, Object> moArguments) {
        LOGGER.debug("Going to set AP MO attributes {} on MO with fdn: {}", moArguments.toString(), moFdn);
        final ManagedObject managedObject = getLiveBucket().findMoByFdn(moFdn);
        if (managedObject != null) {
            managedObject.setAttributes(moArguments);
        } else {
            LOGGER.error("AutoProvisioning MO found null with fdn {}", moFdn);
        }
    }

    /**
     * Gets the AP MO attributes.
     * @param nodeName NodeName.
     * @return
     * @throws MoNotFoundException
     */
    public Map<String, Object> getApMoAttritues(final String nodeName) throws MoNotFoundException {
        try {
            final NetworkElement networkElement = activityUtils.getNetworkElement(nodeName, SHMCapabilities.NO_CAPABILITY);
            final List<String> projectedAttributes = new ArrayList<>();
            projectedAttributes.add(EcimCommonConstants.AutoProvisioningMoConstants.RBS_CONFIG_LEVEL_KEY);
            return getApMOProjectionAttributes(networkElement, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO, null, EcimCommonConstants.AutoProvisioningMoConstants.AP_MO_NAMESPACE, projectedAttributes);
        } catch (MoNotFoundException exception) {
            throw new MoNotFoundException(exception.toString());
        }
    }

    private final Map<String, Object> getApMOProjectionAttributes(final NetworkElement networkElement, final String type, final Map<String, Object> restrictions, String nameSpace, final List<String> projectedAttributes) {
        final Map<String, Object> projectedAttributesResponse = new HashMap<String, Object>();
        final String meContextFdn = networkElement.getNodeRootFdn();
        if (meContextFdn != null) {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final Query<TypeContainmentRestrictionBuilder> query = queryBuilder.createTypeQuery(nameSpace, type, meContextFdn);

            final Projection[] attributesArray = constructProjection(projectedAttributes);
            final List<Object[]> databaseEntries = getLiveBucket().getQueryExecutor().executeProjection(query, ProjectionBuilder.field(ObjectField.MO_FDN), attributesArray);
            for (Object[] poObject: databaseEntries) {
                projectedAttributesResponse.put(PollingActivityConstants.MO_FDN, poObject[0]);
                for (int attributeIndex = 1; attributeIndex < poObject.length; attributeIndex++) {
                    projectedAttributesResponse.put(attributesArray[attributeIndex - 1].getProjectionValue(), poObject[attributeIndex]);
                }
            }
        }
        LOGGER.debug("AP MO Attributes retreived: {}", projectedAttributesResponse.toString());
        return projectedAttributesResponse;
    }

    private Projection[] constructProjection(final List<String> projectedAttributes) {
        final Projection[] attributesArray = new Projection[projectedAttributes.size()];
        int projectionIndex = 0;
        for (final String attribute : projectedAttributes) {
            attributesArray[projectionIndex] = ProjectionBuilder.attribute(attribute);
            projectionIndex++;
        }
        return attributesArray;
    }
}
