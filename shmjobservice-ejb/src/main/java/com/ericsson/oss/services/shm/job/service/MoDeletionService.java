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
package com.ericsson.oss.services.shm.job.service;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

@Stateless
public class MoDeletionService {
    
    @EServiceRef
    private DataPersistenceService dataPersistenceService;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int deleteMoByFDN(final String hcJobFdn) {
        final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
        final PersistenceObject persistenceObject = liveBucket.findMoByFdn(hcJobFdn);
        return liveBucket.deletePo(persistenceObject);
    }

}
