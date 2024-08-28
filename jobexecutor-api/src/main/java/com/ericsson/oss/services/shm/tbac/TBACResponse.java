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
package com.ericsson.oss.services.shm.tbac;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;

public class TBACResponse {

    private List<NetworkElement> unAuthorizedNes = new ArrayList<>();
    private boolean isTBACValidationSuccess;
    private boolean isTbacValidationToBeDoneForAllNodesAsSingleTarget;

    /**
     * @return the unAuthorizedNes
     */
    public List<NetworkElement> getUnAuthorizedNes() {
        return unAuthorizedNes;
    }

    /**
     * @return the isTBACValidationSuccess
     */
    public boolean isTBACValidationSuccess() {
        return isTBACValidationSuccess;
    }

    /**
     * @param isTBACValidationSuccess
     *            the isTBACValidationSuccess to set
     */
    public void setTBACValidationSuccess(final boolean isTBACValidationSuccess) {
        this.isTBACValidationSuccess = isTBACValidationSuccess;
    }

    /**
     * @return the isTbacValidationToBeDoneForAllNodesAsSingleTarget
     */
    public boolean isTbacValidationToBeDoneForAllNodesAsSingleTarget() {
        return isTbacValidationToBeDoneForAllNodesAsSingleTarget;
    }

    /**
     * @param isTbacValidationToBeDoneForAllNodesAsSingleTarget
     *            the isTbacValidationToBeDoneForAllNodesAsSingleTarget to set
     */
    public void setTbacValidationToBeDoneForAllNodesAsSingleTarget(final boolean isTbacValidationToBeDoneForAllNodesAsSingleTarget) {
        this.isTbacValidationToBeDoneForAllNodesAsSingleTarget = isTbacValidationToBeDoneForAllNodesAsSingleTarget;
    }

    /**
     * @param unAuthorizedNes
     *            the unAuthorizedNes to set
     */
    public void setUnAuthorizedNes(final List<NetworkElement> unAuthorizedNes) {
        this.unAuthorizedNes = unAuthorizedNes;
    }

}
