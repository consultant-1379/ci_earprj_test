/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public enum JobCategory implements Serializable {
    /**
     * For SHM jobs created from CLI
     */
    CLI("CLI"),

    /**
     * For SHM jobs created from SHM UI
     */
    UI("UI"),

    /**
     * For SHM jobs created from External Services
     */
    REMOTE("REMOTE"),

    /**
     * For NHC reports created from NHC UI
     */
    NHC_UI("NHC_UI"),

    /**
     * For SHM jobs created from Flow Automation
     */
    FA("FA"),

    /**
     * For NHC reports created from Flow Automation
     */
    NHC_FA("NHC_FA");

    private String category;

    private JobCategory(final String attribute) {
        this.category = attribute;
    }

    public String getAttribute() {
        return category;
    }

    public static JobCategory getJobCategory(final String jobCategory) {

        for (final JobCategory category : JobCategory.values()) {
            if (category.name().equalsIgnoreCase(jobCategory)) {
                return category;
            }
        }
        return null;
    }

    public static List<String> getShmJobCategories() {
        return Arrays.asList(UI.getAttribute(), CLI.getAttribute(), REMOTE.getAttribute(), FA.getAttribute());
    }

    public static List<String> getNhcJobCategories() {
        return Arrays.asList(NHC_UI.getAttribute(), NHC_FA.getAttribute());
    }
}