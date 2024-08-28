/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.noderestart;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.DescendantsScope;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.NodeModelNameSpaceProvider;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

/**
 * 
 * This Utility is to get the Managed Element MO
 * 
 */
@Stateless
public class CppNodeRestartMOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CppNodeRestartMOUtil.class);

    @Inject
    private NodeModelNameSpaceProvider nodeModelNameSpaceProvider;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    /**
     * This method is used to get the Managed Element MO.
     * 
     * @param nodeName
     * @return managedObject
     * 
     */
    public String getManagedElementMOFdnByNodeName(final String nodeName) {
        String moFdn = "";
        ManagedObject managedObject = null;
        try {

            final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            if (networkElementList != null && !networkElementList.isEmpty()) {
                final String rootFdn = networkElementList.get(0).getNodeRootFdn();
                final String nameSpace = nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName);
                if (nameSpace != null && !ActivityConstants.EMPTY.equals(nameSpace) && !RestartActivityConstants.NAMESPACE_NOT_FOUND.equals(nameSpace)) {
                    final Query<TypeContainmentRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(nameSpace, RestartActivityConstants.MANAGED_ELEMENT, rootFdn);
                    final Restriction queryRestriction = query.getRestrictionBuilder().descendants(RestartActivityConstants.DISTANCE, DescendantsScope.DESCENDANTS_AT_DISTANCE);
                    query.setRestriction(queryRestriction);
                    final Iterator<Object> managedObjIterator = getLiveBucket().getQueryExecutor().execute(query);
                    if (managedObjIterator != null) {
                        while (managedObjIterator.hasNext()) {
                            managedObject = (ManagedObject) managedObjIterator.next();
                            moFdn = managedObject.getFdn();
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception while retrieving the managed object for : " + RestartActivityConstants.RESTART_NODE + " and Node Name" + nodeName + " Exception : ", ex);
        }
        return moFdn;
    }

    public ManagedObject getManagedElementMOByFdn(final String managedElementFdn) {
        return getLiveBucket().findMoByFdn(managedElementFdn);
    }

    public ManagedObject getManagedElementMOByNodename(final String nodeName) {
        ManagedObject managedObject = null;
        try {

            final List<NetworkElement> networkElementList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(nodeName));
            if (networkElementList != null && !networkElementList.isEmpty()) {
                final String rootFdn = networkElementList.get(0).getNodeRootFdn();
                final String nameSpace = nodeModelNameSpaceProvider.getNamespaceByNodeName(nodeName);
                if (nameSpace != null && !ActivityConstants.EMPTY.equals(nameSpace) && !RestartActivityConstants.NAMESPACE_NOT_FOUND.equals(nameSpace)) {
                    final Query<TypeContainmentRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(nameSpace, RestartActivityConstants.MANAGED_ELEMENT, rootFdn);
                    final Restriction queryRestriction = query.getRestrictionBuilder().descendants(RestartActivityConstants.DISTANCE, DescendantsScope.DESCENDANTS_AT_DISTANCE);
                    query.setRestriction(queryRestriction);
                    final Iterator<Object> managedObjIterator = getLiveBucket().getQueryExecutor().execute(query);
                    if (managedObjIterator != null) {
                        while (managedObjIterator.hasNext()) {
                            managedObject = (ManagedObject) managedObjIterator.next();
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception while retrieving the managed object for : " + RestartActivityConstants.RESTART_NODE + " and Node Name" + nodeName + " Exception : ", ex);
        }
        return managedObject;
    }

    private DataBucket getLiveBucket() {
        return dataPersistenceService.getLiveBucket();
    }

}
