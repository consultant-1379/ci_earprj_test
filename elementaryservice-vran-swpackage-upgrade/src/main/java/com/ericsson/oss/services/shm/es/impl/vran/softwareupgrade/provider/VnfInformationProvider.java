/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ContainmentRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.SortDirection;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

/**
 * Service to retrieve VNF information
 */
@Stateless
public class VnfInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(VnfInformationProvider.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    static final String EXECUTION_RESOURCE_MO_TYPE = "ExecutionResource";

    public String getVnfId(final List<NetworkElement> networkElements) {
        final Iterator<ManagedObject> iterator = buildQuery(networkElements);
        return extractVnfId(networkElements, iterator);
    }

    public String getVnfId(final long activityJobId, final String inputVnfId, final String vnfIdKey, final String neName) {
        String vnfId = null;

        final Iterator<PersistenceObject> iterator = fetchJobProperties(neName);
        while (iterator.hasNext()) {
            LOGGER.debug("inside while loop = =  ");
            final PersistenceObject neJobPO = iterator.next();
            LOGGER.debug("ActivityJob ID - [{}] : next neJobPO :: {}", activityJobId, neJobPO);
            final Map<String, Object> neJobAttributes = neJobPO.getAllAttributes();
            final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) neJobAttributes.get(ShmConstants.JOBPROPERTIES);
            LOGGER.debug("ActivityJob ID - [{}] : next jobProperties :: {}", activityJobId, jobProperties);
            if (jobProperties != null && !jobProperties.isEmpty()) {
                vnfId = getVnfIdFromNeJobProperties(activityJobId, inputVnfId, vnfIdKey, jobProperties);
            }
            if (vnfId != null) {
                break;
            }
        }
        LOGGER.debug("ActivityJob ID - [{}] : retrieved  vnfid {} based on toVnfId :: {}", activityJobId, vnfId, inputVnfId);
        return vnfId;
    }

    public List<NetworkElement> fetchNetworkElements(final JobEnvironment jobContext) {
        final List<String> nodeNames = new ArrayList<>();
        nodeNames.add(jobContext.getNodeName());
        return fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(nodeNames, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
    }

    private Iterator<ManagedObject> buildQuery(final List<NetworkElement> networkElements) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final String nodeFdn = networkElements.get(0).getNodeRootFdn();
        final Query<ContainmentRestrictionBuilder> query = queryBuilder.createContainmentQuery(nodeFdn);
        query.setRestriction(query.getRestrictionBuilder().equalTo(ObjectField.TYPE, EXECUTION_RESOURCE_MO_TYPE));
        return dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
    }

    private String extractVnfId(final List<NetworkElement> networkElements, final Iterator<ManagedObject> iterator) {
        String vnfId = "";
        try {
            while (iterator.hasNext()) {
                final ManagedObject managedObject = iterator.next();
                vnfId = managedObject.getAttribute("vnfIdentity");
                LOGGER.debug("vnfId for node:{} is {}", networkElements.get(0).getNodeRootFdn(), vnfId);
            }
        } catch (final Exception ex) {
            LOGGER.error("Failed to get vnfId from {} for node:{}. Reason: {}", EXECUTION_RESOURCE_MO_TYPE, networkElements.get(0).getNodeRootFdn(), ex);
        }
        return vnfId;
    }

    private Iterator<PersistenceObject> fetchJobProperties(final String neName) {
        LOGGER.info("Fetching jobproperties = == = = == = = ={}", neName);
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NE_JOB);
        final Restriction queryRestriction = query.getRestrictionBuilder().equalTo(ShmConstants.NE_NAME, neName);
        query.setRestriction(queryRestriction);
        query.addSortingOrder(ShmConstants.ENDTIME, SortDirection.DESCENDING);
        return dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
    }

    private String getVnfIdFromNeJobProperties(final long activityJobId, final String inputVnfId, final String vnfIdKey, final List<Map<String, Object>> jobProperties) {
        String fromVnfId = null;
        String toVnfIdValue = null;
        for (final Map<String, Object> jobProperty : jobProperties) {
            final String toVnfIdKey = (String) jobProperty.get(ActivityConstants.JOB_PROP_KEY);
            if (vnfIdKey.equals(toVnfIdKey) && keyExists(jobProperty)) {
                toVnfIdValue = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                if ((toVnfIdValue != null && !toVnfIdValue.isEmpty()) && toVnfIdValue.equals(inputVnfId)) {
                    fromVnfId = getVnfIdFromVnfJobProperties(activityJobId, jobProperties);
                }
            }
            if (fromVnfId != null) {
                break;
            }
        }
        return fromVnfId;
    }

    private String getVnfIdFromVnfJobProperties(final long activityJobId, final List<Map<String, Object>> jobProperties) {
        String fromVnfId = null;
        for (final Map<String, Object> property : jobProperties) {
            if (isFromVnfIdKeyMatched((String) property.get(ActivityConstants.JOB_PROP_KEY)) && keyExists(property)) {
                fromVnfId = ((String) property.get(ActivityConstants.JOB_PROP_VALUE));
            }
            if (fromVnfId != null) {
                break;
            }
        }
        LOGGER.debug("ActivityJob ID - [{}] : Retrieved fromVnfId {} ", activityJobId, fromVnfId);
        return fromVnfId;
    }

    private boolean keyExists(final Map<String, Object> property) {
        return property.get(ActivityConstants.JOB_PROP_VALUE) != null && property.containsKey(ActivityConstants.JOB_PROP_VALUE);
    }

    private boolean isFromVnfIdKeyMatched(final String fromVnfIdKey) {
        return VranJobConstants.VNF_ID.equals(fromVnfIdKey);
    }

}
