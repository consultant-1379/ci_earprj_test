/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.instantaneouslicensing;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;

/**
 * Class is to handle InstantaneousLicensing MO
 * 
 * @author Team Royals
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class InstantaneousLicensingMOHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstantaneousLicensingMOHandler.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    /**
     * method to fetch InstantaneousLicensing MO's attributes
     * 
     * @param fdn
     * @return
     */
    public Map<String, Object> getInstantaneousLicensingMOAttributes(final String fdn) {

        try {
            final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
            final ManagedObject instantaneousLicensingMO = liveBucket.findMoByFdn(fdn);
            if (instantaneousLicensingMO != null) {
                LOGGER.debug("Successfully found instantaneousLicensing MO for fdn : {}", fdn);
                return instantaneousLicensingMO.getAllAttributes();
            }
            LOGGER.error("Unable to get instantaneousLicensingIterator MO for fdn : {}", fdn);
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving instantaneousLicensing Mo for fdn {} () .Reason ", fdn, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }

        return new HashMap<>();
    }
}
