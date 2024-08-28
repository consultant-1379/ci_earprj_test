/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class MainJobsDetailsReader {

    @EServiceRef
    private DataPersistenceService dps;

    private static final List<String> requiredJobTemplateAttributes = Arrays.asList(ShmConstants.NAME, ShmConstants.OWNER, ShmConstants.JOB_TYPE, ShmConstants.CREATION_TIME,
            ShmConstants.JOBCONFIGURATIONDETAILS);
    private static final List<String> requiredMainJobAttributes = Arrays.asList(ShmConstants.PROGRESSPERCENTAGE, ShmConstants.RESULT, ShmConstants.STARTTIME, ShmConstants.ENDTIME, ShmConstants.STATE,
            ShmConstants.NO_OF_NETWORK_ELEMENTS, ShmConstants.JOBTEMPLATEID);

    public Map<Long, Map<String, Object>> getSHMMainJobTemplates(final JobInput jobInput) {

        final Map<Long, Map<String, Object>> shmJobTemplates = new HashMap<>();
        dps.setWriteAccess(Boolean.FALSE);

        final Query<TypeRestrictionBuilder> query = dps.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE);

        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction shmCategoryRestriction = restrictionBuilder.in(ShmConstants.JOB_CATEGORY, jobInput.getJobCategory().toArray());
        query.setRestriction(shmCategoryRestriction);

        final QueryExecutor queryExecutor = dps.getLiveBucket().getQueryExecutor();
        final Iterator<PersistenceObject> jobTemplates = queryExecutor.execute(query);

        while (jobTemplates.hasNext()) {
            final PersistenceObject jobTemplate = jobTemplates.next();
            shmJobTemplates.put(jobTemplate.getPoId(), jobTemplate.getAttributes(requiredJobTemplateAttributes));
        }
        return shmJobTemplates;
    }

    public Map<Long, Map<String, Object>> getMainJobs(final List<Long> jobTemplatePoIds) {

        dps.setWriteAccess(Boolean.FALSE);

        final Query<TypeRestrictionBuilder> query = dps.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);

        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
        final Restriction templateIdsRestriction = restrictionBuilder.in(ShmConstants.JOB_TEMPLATE_ID, jobTemplatePoIds.toArray());
        final Restriction deletingJobRestriction = restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, JobState.DELETING.name()));
        query.setRestriction(restrictionBuilder.allOf(templateIdsRestriction, deletingJobRestriction));

        return executeProjection(query, requiredMainJobAttributes);
    }

    private Map<Long, Map<String, Object>> executeProjection(final Query<TypeRestrictionBuilder> query, final List<String> requiredAttributes) {
        final QueryExecutor queryExecutor = dps.getLiveBucket().getQueryExecutor();
        final Projection[] attributesArray = prepareProjection(requiredAttributes);
        final List<Object[]> poObjects = queryExecutor.executeProjection(query, ProjectionBuilder.field(ObjectField.PO_ID), attributesArray);

        final Map<Long, Map<String, Object>> response = new HashMap<>();
        for (final Object[] poObject : poObjects) {
            final Map<String, Object> projectedAttributesResponse = new HashMap<>();
            projectedAttributesResponse.put(ShmConstants.PO_ID, poObject[0]);
            for (int attributeIndex = 1; attributeIndex < poObject.length; attributeIndex++) {
                projectedAttributesResponse.put(attributesArray[attributeIndex - 1].getProjectionValue(), poObject[attributeIndex]);
            }
            response.put((Long) poObject[0], projectedAttributesResponse);
        }
        return response;
    }

    private Projection[] prepareProjection(final List<String> requiredAttributes) {
        final Projection[] attributesArray = new Projection[requiredAttributes.size()];
        int projectionIndex = 0;
        for (final String attribute : requiredAttributes) {
            attributesArray[projectionIndex] = ProjectionBuilder.attribute(attribute);
            projectionIndex++;
        }
        return attributesArray;
    }

    public Map<String, Object> retrieveJob(final long poId) {
        dps.setWriteAccess(Boolean.FALSE);
        final PersistenceObject po = dps.getLiveBucket().findPoById(poId);

        final Map<String, Object> mainJob = new HashMap<>();
        if (po != null) {
            mainJob.putAll(po.getAllAttributes());
            mainJob.put(ShmJobConstants.MAINJOBID, po.getPoId());
        }
        return mainJob;

    }

}
