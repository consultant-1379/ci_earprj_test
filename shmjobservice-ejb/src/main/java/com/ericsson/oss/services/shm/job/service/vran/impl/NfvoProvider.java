/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.job.service.vran.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

/**
 * Provides available NFVOs
 *
 * @author xeswpot
 */
@Stateless
public class NfvoProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NfvoProvider.class);

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private static final String NFVO_MO = "NetworkFunctionVirtualizationOrchestrator";
    private static final String NFVO_NAMESPACE = "OSS_NE_DEF";
    public static final String NFVO_TYPE = "nfvoType";
    public static final String NFVO_TYPE_ECM = "ECM";

    /**
     * Returns list of the NFVO names from the database
     *
     * @return list of the NFVO names
     */
    public List<String> getNfvoNames() {

        final List<String> nfvoNames = new ArrayList<>();
        try {

            final List<PersistenceObject> nfvoMOs = getNfvoMOs();
            for (PersistenceObject nfvoMO : nfvoMOs) {
                final String nfvoName = ((ManagedObject) nfvoMO).getName();
                nfvoNames.add(nfvoName);
            }
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while getting the NFVO details from the database : ", ex);
        }
        LOGGER.info("Resolved NFVOs : {} ", nfvoNames);
        return nfvoNames;
    }

    /**
     * Returns List of the PersistenceObject of NFVO from the database
     *
     * @return List of the PersistenceObject of NFVO
     */
    private List<PersistenceObject> getNfvoMOs() {

        final Query<TypeRestrictionBuilder> query = dataPersistenceService.getQueryBuilder().createTypeQuery(NFVO_NAMESPACE, NFVO_MO);
        final TypeRestrictionBuilder restrictionBuilder = query.getRestrictionBuilder();

        query.setRestriction(restrictionBuilder.not(restrictionBuilder.equalTo(NFVO_TYPE, NFVO_TYPE_ECM)));

        return dataPersistenceService.getLiveBucket().getQueryExecutor().getResultList(query);
    }

}
