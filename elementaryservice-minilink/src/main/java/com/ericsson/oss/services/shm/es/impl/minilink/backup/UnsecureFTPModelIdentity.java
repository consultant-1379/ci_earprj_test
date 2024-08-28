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

package com.ericsson.oss.services.shm.es.impl.minilink.backup;

public enum UnsecureFTPModelIdentity {
    TN11B {
        public String getOssModelIdentity() {
        return "M11B-TN-4.4FP.7";
        }
    },
    CN210 {
        public String getOssModelIdentity() {
        return "M12A-CN210-1.2";
        }
    },
    CN510R1 {
        public String getOssModelIdentity() {
        return "M12A-CN510R1-1.2";
        }
    };
    /**
     * abstract method to get the ossModelIdentity
     * @return String
     */
    public abstract String getOssModelIdentity();
}