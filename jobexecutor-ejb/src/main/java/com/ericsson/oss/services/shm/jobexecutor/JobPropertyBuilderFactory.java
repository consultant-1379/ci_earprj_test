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
package com.ericsson.oss.services.shm.jobexecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.jobexecutorlocal.JobPropertyProvider;
import com.ericsson.oss.services.shm.jobs.common.annotations.JobTypeQualifier;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;

@ApplicationScoped
public class JobPropertyBuilderFactory {

    @Inject
    @Any
    private Instance<JobPropertyProvider> prepareJobPropertiesForBackup;

    public JobPropertyProvider getProvider(final JobType jobtype) {
        return prepareJobPropertiesForBackup.select(new JobTypeQualifier(jobtype)).get();
    }

}
