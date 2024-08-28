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
package com.ericsson.oss.services.shm.es.impl.minilink.upgrade;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.es.api.NeJobValidator;

/**
 * 
 * @author xnagvar
 */
@EServiceQualifier("MINI_LINK_INDOOR.UPGRADE")
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UpgradeJobValidator implements NeJobValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeJobValidator.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.NeJobLevelValidation#validate(long)
     */
    @Override
    public boolean validate(final long neJobId) {
        LOGGER.info("Validate result is false.");
        return false;
    }



}
