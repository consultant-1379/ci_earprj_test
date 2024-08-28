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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.instrumentation.NeJobInstrumentation;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NeJobInstrumentaionService implements NeJobInstrumentation {
    @Inject
    private NeJobInstrumentaionBean neJobInstrumentaionBean;

    @Override
    public void preStart(final String platformType, final String jobType) {
        neJobInstrumentaionBean.actvityStart(platformType, jobType);
    }

    @Override
    public void postFinish(final String platformType, final String jobType) {
        neJobInstrumentaionBean.activityEnd(platformType, jobType);

    }
}
