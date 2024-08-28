/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common;

import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.COMMA;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.EQUAL;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.INVENTORY_SUPERVISION;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.NETWORKELEMENT;
import static com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.common.Constants.ONE;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;

@Stateless
@Profiled
public class DPSUtils {

    @EServiceRef
    private DataPersistenceService dataPersistenceService;
    private static final Logger LOGGER = LoggerFactory.getLogger(DPSUtils.class);

    public String getNeType(final String nodeName) {
        final String networkElementFdn = NETWORKELEMENT + nodeName;
        final ManagedObject nodeMo = dataPersistenceService.getLiveBucket().findMoByFdn(networkElementFdn);
        LOGGER.debug("Network Element MO retrieved from DPS {}", nodeMo);
        return nodeMo.getAttribute("neType");
    }

    public boolean isInventorySupervisionEnabled(final String nodeName) {
        final String inventorySupervisionMoFdn = NETWORKELEMENT + nodeName + COMMA + INVENTORY_SUPERVISION + EQUAL + ONE;
        final ManagedObject inventorySupervisionMo = dataPersistenceService.getLiveBucket().findMoByFdn(inventorySupervisionMoFdn);
        return inventorySupervisionMo.getAttribute("active");
    }

}
