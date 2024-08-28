/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoGenerateNameValidator {

    /**
     * 
     * This method replaces all special characters in BackupName which are in product number and revision with an underscore
     * 
     * @return
     */
    public String getValidatedAutoGenerateBackupName(final String autoGenerateBackupName) {
        String validatedBackupName;
        final Pattern pattern = Pattern.compile("[.%&^!]");
        final Matcher regexMatcher = pattern.matcher(autoGenerateBackupName.trim());
        validatedBackupName = regexMatcher.replaceAll("_");
        return validatedBackupName;
    }

}
