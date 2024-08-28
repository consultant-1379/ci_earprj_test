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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

public class NeInfoQuery {
    private String neType;
    private List<String> neFdns;
    private List<NeParams> params;

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }

    /**
     * @return the neNames
     */
    public List<String> getNeFdns() {
        return neFdns;
    }

    /**
     * @param neNames
     *            the neNames to set
     */
    public void setNeFdns(final List<String> neNames) {
        this.neFdns = neNames;
    }

    /**
     * @return the params
     */
    public List<NeParams> getParams() {
        return params;
    }

    /**
     * @param params
     *            the params to set
     */
    public void setParams(final List<NeParams> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        final StringBuilder paramBuilder = new StringBuilder();
        final List<NeParams> paramList = this.getParams();
        if (paramList != null && (!paramList.isEmpty())) {
            for (int index = 0; index < paramList.size(); index++) {
                paramBuilder.append(paramList.get(index) + ";");
            }

        }
        final String neParams = paramBuilder.toString();
        return "neType : " + this.getNeType() + "; neFdns : " + this.getNeFdns().toString() + "; params : " + neParams;

    }
}
