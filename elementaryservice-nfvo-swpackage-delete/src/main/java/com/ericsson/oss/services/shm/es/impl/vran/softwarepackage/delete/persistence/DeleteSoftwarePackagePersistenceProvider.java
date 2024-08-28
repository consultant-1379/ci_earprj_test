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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.enums.JobStateEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.filestore.softwarepackage.exceptions.SwPkgDelFailureInDbException;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;

@Stateless
public class DeleteSoftwarePackagePersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSoftwarePackagePersistenceProvider.class);

    @Inject
    private DpsReader dpsReader;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInformationProvider;

    @Inject
    private JobPropertyUtils jobPropertyUtils;


    public boolean isSoftwarePackageInUse(final String softwarePackage) {

        final Iterator<PersistenceObject> jobEntities = getJobEntities();
        final String jobsUsingSwPackage = getJobNamesBySoftwarePackage(softwarePackage, jobEntities, VranJobConstants.VRAN_SOFTWAREPACKAGE_NAME);

        return jobsUsingSwPackage != null && jobsUsingSwPackage.trim().length() > 0;
    }

    private Iterator<PersistenceObject> getJobEntities() {

        try {
            final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
            final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();
            query.setRestriction(restrictionBuilder.not(restrictionBuilder.in(ShmConstants.STATE, (Object[]) JobStateEnum.getInactiveJobstates())));
            return dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
        } catch (final RuntimeException ex) {
            LOGGER.error("Failed to check if any job is using the current software package. Reason: {} ", ex.getMessage(), ex);
            dpsAvailabilityInformationProvider.checkDatabaseAvailability(ex);
            throw new SwPkgDelFailureInDbException("Failed to fetch Job data");
        }
    }

    private String getJobNamesBySoftwarePackage(final String softwarePackage, final Iterator<PersistenceObject> jobEntities, final String softwarePackageIdentifier) {
        final StringBuilder jobsUsingSwPkg = new StringBuilder();
        final List<String> requiredJobPropertyKeys = new ArrayList<String>();
        requiredJobPropertyKeys.add(softwarePackageIdentifier);
        while (jobEntities.hasNext()) {
            final PersistenceObject jobEntity = jobEntities.next();
            final Map<String, Object> jobConfigurationDetails = jobEntity.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS);
            if (jobConfigurationDetails != null) {
                final Map<String, String> jobProperties = jobPropertyUtils.getPropertyValue(requiredJobPropertyKeys, jobConfigurationDetails);
                final String softwarePackageName = jobProperties.get(softwarePackageIdentifier);
                LOGGER.debug("Input softwarePackage {} and softwarePackageName form DB {} ", softwarePackage, softwarePackageName);
                if (softwarePackageName != null && softwarePackage.equalsIgnoreCase(softwarePackageName)) {
                    jobsUsingSwPkg.append(getJobNameFromJobTemplateEntity(jobEntity));
                }
            }
        }
        return jobsUsingSwPkg.toString();
    }

    private String getJobNameFromJobTemplateEntity(final PersistenceObject jobEntity) {
        String jobName = null;
        final long jobTemplateId = jobEntity.getAttribute(ShmConstants.JOBTEMPLATEID);
        if (jobTemplateId > 0) {
            final PersistenceObject jobTemplateEntity = dpsReader.findPOByPoId(jobTemplateId);
            if (jobTemplateEntity != null && jobTemplateEntity.getAttribute(ShmConstants.NAME) != null) {
                jobName = jobTemplateEntity.getAttribute(ShmConstants.NAME);
                LOGGER.debug("Softwarepackage is currently in use in the job: {}", jobName);
            }
        }
        return jobName;
    }
}
