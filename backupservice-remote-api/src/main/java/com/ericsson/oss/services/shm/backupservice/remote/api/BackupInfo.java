/*------------------------------------------------------------------------------
 *******************************************************************************
 /* * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.backupservice.remote.api;

import java.io.Serializable;

/**
 * Stores the backup information required for performing backup management operations on a network element.
 */
public class BackupInfo implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8018986181829233897L;

    /**
     * name of the backup.
     */
    private final String name;

    /**
     * Applicable only for ECIM based NEs (createBackup,uploadBackup)- contains the domain of the BrmManager which will manages this backup.
     */
    private final String domain;

    /**
     * Applicable only for ECIM based NEs (createBackup,uploadBackup) - contains the type of the BrmManager which will manage this backup.
     */
    private final String type;
    /**
     * Applicable only for CPP based NEs(createBackup) - contains the identity of the CV product
     */
    private final String identity;
    /**
     * Applicable only for CPP based NEs(createBackup) - contains additional text to be set by the user.
     */
    private final String comment;


    /**
     * @param name
     * @param domain
     * @param type
     * @param identity
     * @param comment
     */
    public BackupInfo(final String name, final String domain, final String type, final String identity, final String comment) {
        this.name = name;
        this.domain = domain;
        this.type = type;
        this.identity = identity;
        this.comment = comment;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return the identity
     */
    public String getIdentity() {
        return identity;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

   

}
