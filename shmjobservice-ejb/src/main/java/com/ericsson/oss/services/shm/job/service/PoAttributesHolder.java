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
package com.ericsson.oss.services.shm.job.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

/**
 * This class retrieves the Persistence Object attribute and creates a attribute holder map which is returned back to the caller.
 *
 * @deprecated All the logic done here to get the job templates and Main jobs have been moved to MainJobDetailsReader class
 * 
 * @author xvishsr
 */

@Stateless
@Profiled
@Deprecated
public class PoAttributesHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoAttributesHolder.class);

    private static final List<String> requiredMainJobAttributes = Arrays.asList(ShmConstants.PROGRESSPERCENTAGE, ShmConstants.RESULT, ShmConstants.STARTTIME, ShmConstants.ENDTIME, ShmConstants.STATE,
            ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.JOBTEMPLATEID);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private DataBucket getLiveBucket() {
        return dataPersistenceService.getLiveBucket();
    }

    public PersistenceObject findPOByPoId(final long poId) {
        return getLiveBucket().findPoById(poId);
    }

    /**
     * This method queries Template job Persistence Objects with specified restrictions and prepares a map having all attributes of the PO.
     * 
     * @param nameSpace
     * @param type
     * @param restrictions
     * @return List<Map<String, Object>> attributesHolder
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<Long, Map<String, Object>> getTemplateJobDetails(final String nameSpace, final String type, final JobInput jobInput) {
        LOGGER.debug("jobInput in getTemplateJobDetails in the retrieval of SHM jobs {}", jobInput);
        final Date queryStartTime = new Date();
        final Map<Long, Map<String, Object>> attributesHolder = new HashMap<Long, Map<String, Object>>();
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(nameSpace, type);
        final Restriction categoryRestriction = query.getRestrictionBuilder().in(ShmConstants.JOB_CATEGORY, jobInput.getJobCategory().toArray());
        query.setRestriction(categoryRestriction);
        final List<PersistenceObject> persistenceObjectList = queryExecutor.getResultList(query);

        for (final PersistenceObject persistenceObject : persistenceObjectList) {
            attributesHolder.put(persistenceObject.getPoId(), persistenceObject.getAllAttributes());
        }
        LOGGER.info("Time taken to retrieve {} template job details is : {} milliseconds", attributesHolder.size(), (new Date().getTime() - queryStartTime.getTime()));
        return attributesHolder;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Map<String, Object>> getMainJobDetails(final String nameSpace, final String type, final List<Long> jobTemplatePoIds) {

        LOGGER.debug("{} TemplateIds  passed to get main job details", jobTemplatePoIds.size());
        int projectionIndex = 0;
        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(nameSpace, type);
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction templateIdsRestriction = restrictionBuilder.in(ShmConstants.JOB_TEMPLATE_ID, jobTemplatePoIds.toArray());
        final Restriction deletingJobRestriction = restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, JobState.DELETING.name()));
        final Restriction res = restrictionBuilder.allOf(templateIdsRestriction, deletingJobRestriction);
        query.setRestriction(res);
        final com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection attributesArray[] = new Projection[requiredMainJobAttributes.size()];

        for (final String attribute : requiredMainJobAttributes) {
            attributesArray[projectionIndex] = ProjectionBuilder.attribute(attribute);
            projectionIndex++;
        }

        final Date queryStartTime = new Date();
        final List<Object[]> datbaseEntries = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID), attributesArray);
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        for (final Object[] poObject : datbaseEntries) {
            final Map<String, Object> projectedAttributesResponse = new HashMap<String, Object>();
            projectedAttributesResponse.put(ShmConstants.PO_ID, poObject[0]);
            for (int attributeIndex = 1; attributeIndex < poObject.length; attributeIndex++) {
                projectedAttributesResponse.put(attributesArray[attributeIndex - 1].getProjectionValue(), poObject[attributeIndex]);
            }
            response.add(projectedAttributesResponse);
        }

        LOGGER.info("Time taken to retrieve {} main job details is : {} milliseconds", response.size(), (new Date().getTime() - queryStartTime.getTime()));
        return response;
    }

    /**
     * This method queries Persistence Objects with the specified PO Ids and prepares a map with all Persistence Object attributes.
     * 
     * @param poIds
     * @return Map<Long, Map<String, Object>> attributesHolder
     */
    public Map<Long, Map<String, Object>> findPOsByPoIds(final List<Long> poIds) {

        final Map<Long, Map<String, Object>> attributesHolder = new HashMap<Long, Map<String, Object>>();

        final List<PersistenceObject> persistenceObjectList = getLiveBucket().findPosByIds(poIds);

        for (final PersistenceObject persistenceObject : persistenceObjectList) {
            attributesHolder.put(persistenceObject.getPoId(), persistenceObject.getAllAttributes());
        }

        LOGGER.trace("Number of main job template retrieved : {}", attributesHolder.keySet().size());
        return attributesHolder;
    }
}
