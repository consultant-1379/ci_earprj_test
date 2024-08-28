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

import com.ericsson.oss.services.shm.es.instrumentation.ActivityInstrumentation;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ActvityInstrumentationService implements ActivityInstrumentation {

    @Inject
    private ActivityInstrumentationBean activityInstrumentationBean;

    @Override
    public void preStart(final String platformType, final String jobType, final String name) {
        activityInstrumentationBean.actvityStart(platformType, jobType, name);
    }

    @Override
    public void postFinish(final String platformType, final String jobType, final String name) {
        activityInstrumentationBean.activityEnd(platformType, jobType, name);

    }

}
