/*
 * ------------------------------------------------------------------------------
 *  ********************************************************************************
 *  * COPYRIGHT Ericsson  2017
 *  *
 *  * The copyright to the computer program(s) herein is the property of
 *  * Ericsson Inc. The programs may be used and/or copied only with written
 *  * permission from Ericsson Inc. or in accordance with the terms and
 *  * conditions stipulated in the agreement/contract under which the
 *  * program(s) have been supplied.
 *  *******************************************************************************
 *  *----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.shm;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;

/**
 * @author erubarg
 * @since 02/2017
 *
 */
public class MoPopulator {

    private RuntimeConfigurableDps configurableDps;

    public MoPopulator(final RuntimeConfigurableDps configurableDps) {
        this.configurableDps = configurableDps;
    }

    public void createMultipleNfvos(final String fdnMask, final String nfvoType, final int numberOfNfvos) {
        if (numberOfNfvos < 1) {
            return;
        }
        for (int nfvoNumber = 1; nfvoNumber <= numberOfNfvos; nfvoNumber++) {
            createNfvoMo(fdnMask + nfvoNumber, nfvoType);
        }
    }

    public void createNfvoMo(final String fdn, final String nfvoType) {
        final Map<String, Object> nfvoAttributes = new HashMap<>();
        nfvoAttributes.put("nfvoType", nfvoType);
        configurableDps.addManagedObject().withFdn(fdn).namespace("OSS_NE_DEF").addAttributes(nfvoAttributes).build();
    }

    public void deleteMoByFdn(final String fdn) {
        final PersistenceObject poToDelete = configurableDps.build().getLiveBucket().findMoByFdn(fdn);
        if (poToDelete != null) {
            configurableDps.build().getLiveBucket().deletePo(poToDelete);
        }
    }
}
