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

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.instrumentation.MainJobInstrumentation;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MainJobInstrumentationService implements MainJobInstrumentation {
    @Inject
    private MainJobInstrumentationBean mainJobInstrumentationBean;

    @Override
    @Deprecated
    public void preStart(final String jobType) {
        mainJobInstrumentationBean.actvityStart(jobType);
    }

    @Override
    @Deprecated
    public void postFinish(final String jobType) {
        mainJobInstrumentationBean.activityEnd(jobType);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.instrumentation.MainJobInstrumentation#updateRunningMainJobCount()
     */
    @Override
    public void updateRunningMainJobCount(final int mainJobsCount, final List<String> jobTypes) {
        mainJobInstrumentationBean.updateRunningMainJobCount(mainJobsCount, jobTypes);

    }

}
