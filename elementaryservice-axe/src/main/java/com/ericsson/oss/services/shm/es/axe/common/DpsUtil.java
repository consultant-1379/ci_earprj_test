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
package com.ericsson.oss.services.shm.es.axe.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.topology.axe.impl.exceptions.SecurityInfoMONotFoundException;

@Stateless
@Profiled
public class DpsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsUtil.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private static final String EQUAL_SEPARATOR = "=";

    public NeworkElementSecurityMO getSecurityAttributesfromNEChild(final String nodeName) {
        final String networkElementFdn = ShmCommonConstants.MO_TYPE_NETWORK_ELEMENT + EQUAL_SEPARATOR + nodeName;
        LOGGER.info("Start of fetching NetworkElementSecurityMO attributes for Fdn: {}", networkElementFdn);
        try {
            dataPersistenceService.setWriteAccess(false);
            final Query<TypeContainmentRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(SecurityFunctionMOEnum.NE_SEQURITY_MO_NAME_SPACE.getName(),
                    SecurityFunctionMOEnum.NE_SEQURITY_MO.getName(), networkElementFdn);
            final List<Object[]> projectionResults = getLiveBucket().getQueryExecutor().executeProjection(query, ProjectionBuilder.attribute(SecurityFunctionMOEnum.SECURE_USER_NAME.getName()),
                    ProjectionBuilder.attribute(SecurityFunctionMOEnum.SECURE_USER_PASSWORD.getName()));
            return getNodeSecurityInfo(projectionResults);
        } catch (NotDefinedInModelException e) {
            LOGGER.error("Errror while fetching NodeSecurityInfo for nodeName : {}", nodeName);
            throw new SecurityInfoMONotFoundException(e.getMessage(), e.getCause());
        }

    }

    private NeworkElementSecurityMO getNodeSecurityInfo(final List<Object[]> projectionResults) {
        if (projectionResults == null || projectionResults.isEmpty()) {
            return null;
        } else {
            NeworkElementSecurityMO nodeSecurityInfo = null;
            for (final Object[] projectionResult : projectionResults) {
                final String username = (String) projectionResult[0];
                final String password = (String) projectionResult[1];
                nodeSecurityInfo = new NeworkElementSecurityMO(username, password);
            }
            return nodeSecurityInfo;
        }
    }

    private DataBucket getLiveBucket() {
        return dataPersistenceService.getLiveBucket();
    }

    public Map<String, Object> getConnectivityInformation(final String nodeName, final String componentType) {
        final String networkElementFdn = ShmCommonConstants.MO_TYPE_NETWORK_ELEMENT + EQUAL_SEPARATOR + nodeName;
        final ManagedObject networkElementMo = getLiveBucket().findMoByFdn(networkElementFdn);
        final PersistenceObject persistenceObject = networkElementMo.getTarget();
        return getAttributeFromAssociation(persistenceObject, nodeName, componentType);
    }

    private Map<String, Object> getAttributeFromAssociation(final PersistenceObject persistenceObject, final String nodeName, final String componentType) {
        LOGGER.debug("Started fetching of ConnectivityInformation attributes for nodeName{}", nodeName);
        final Collection<PersistenceObject> associations = persistenceObject.getAssociations("ciRef");
        if (!associations.isEmpty()) {
            if (associations.size() > 1) {
                LOGGER.warn("There is more than one ConnectivityInformation MO associated with this node");
            }
            final PersistenceObject ciPo = associations.iterator().next();
            LOGGER.info("fetched ConnectivityInformation attributes for nodeName{}, and Attributes are {}", nodeName, ciPo.getAllAttributes());
            final Map<String, Object> connectivityInfoMap = new HashMap<>();
            if (AxeConstants.APG2_COMPONENT.equals(componentType)) {
                connectivityInfoMap.put(AxeConstants.AP2_CLUSTER_IP_ADDRESS, ciPo.getAttribute(AxeConstants.AP2_CLUSTER_IP_ADDRESS));
            } else {
                connectivityInfoMap.put(AxeConstants.IP_ADDRESS, ciPo.getAttribute(AxeConstants.IP_ADDRESS));
            }
            return connectivityInfoMap;
        }
        return null;
    }
}
