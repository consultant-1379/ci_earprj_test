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
package com.ericsson.oss.services.shm.es.instrumentation.impl;

import javax.ejb.*;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.instrumentation.WaitingJobsInstrumentation;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class WaitingJobsInstrumentationService implements WaitingJobsInstrumentation {

    @Inject
    private WaitingJobsInstrumentationBean waitingJobsInstrumentationBean;

    @Override
    public void preStart(final String jobType) {

        waitingJobsInstrumentationBean.waitingJobStart(jobType);
    }

    @Override
    public void postFinish(final String jobType) {

        waitingJobsInstrumentationBean.waitingJobEnd(jobType);
    }

    @Override
    public void preStart(final String jobType, final String neType) {

        waitingJobsInstrumentationBean.waitingJobStart(jobType, neType);
    }

    @Override
    public void postFinish(final String jobType, final String neType) {

        waitingJobsInstrumentationBean.waitingJobEnd(jobType, neType);
    }
}
