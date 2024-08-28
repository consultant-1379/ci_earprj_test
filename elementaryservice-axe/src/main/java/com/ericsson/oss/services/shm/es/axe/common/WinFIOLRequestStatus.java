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
package com.ericsson.oss.services.shm.es.axe.common;

public enum WinFIOLRequestStatus {

    OK(1, "Completed Successfully"), BUSY(2, "In Progress"), FAILED(3, "Failed"), NOT_FOUND(4, "result not available");

    private int code;
    private String message;

    WinFIOLRequestStatus(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public static WinFIOLRequestStatus getEnum(final int code) {
        for (WinFIOLRequestStatus statusEnum : WinFIOLRequestStatus.values()) {
            if (code == statusEnum.getCode()) {
                return statusEnum;
            }
        }
        return null;
    }

}
