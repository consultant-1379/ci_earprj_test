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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DeleteUpMO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUpMO.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    public int deleteUP(final String upMoFdn) throws EJBException {
        final PersistenceObject po = findMoByFdn(upMoFdn);
        final long deleteStartTime = System.currentTimeMillis();
        if (po != null) {
            try {
                LOGGER.info("Trying to delete Upgrade Package with FDN: {}", upMoFdn);
                return dataPersistenceService.getLiveBucket().deletePo(po);
            } catch (Exception exception) {
                LOGGER.error("exception in deleteUP: ", exception);
                final long deleteFailedtime = System.currentTimeMillis();
                int count = 1;
                Throwable exceptionCause = exception.getCause();
                LOGGER.error("exceptionCause: ", exceptionCause);
                while (exceptionCause != null && count <= 5) {
                    if (exceptionCause instanceof EJBException) {
                        count++;
                        if ((deleteFailedtime - deleteStartTime) > 1 * 60 * 1000) {
                            LOGGER.debug("Do not retry, since the node is taking more time to delete. Then wait for the DELETE notification");

                            //Do not retry, since the node is taking more time to delete. Then wait for the DELETE notification
                            return -1;
                        } else {
                            //Retry this case, since it could be OptimisticLockException
                            throw (EJBException) exceptionCause;
                        }
                    } else {
                        exceptionCause = exceptionCause.getCause();
                    }
                }

                //Its not an EJB Exception, then fail the job
                throw exception;
            }
        } else {
            LOGGER.error("Either There is no UpMo to Delete Or UpMo was Already Deleted . upMoFdn {}", upMoFdn);
            return 0;
        }
    }

    public ManagedObject findMoByFdn(final String upMoFdn) {

        return dataPersistenceService.getLiveBucket().findMoByFdn(upMoFdn);
    }

}