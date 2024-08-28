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
package com.ericsson.oss.services.shm.loadcontrol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public final class ChannelURIBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelURIBuilder.class);
    private static final String FORMATTED_NHC_JOB_TYPE = "Node_Health_Check";
    private static final String SHM = "Shm";
    private static final String NHC = "Nhc";
    private static final String JMS_CHANNEL_URI_BASE = "jms:/queue/";
    private static final String JMS_CHANNEL_EXPORTED_URI_BASE = "java:jboss/exported/jms/queue/";
    private static final String JMS_URI_SUB_STRING = "Job";
    private static final String JMS_URI_SUFFIX = "ActvityQueue";

    private ChannelURIBuilder() {

    }

    /**
     * Method to build the channel URI dynamically based on platform type, job Type and activity name
     * 
     * @param platformType
     * @param jobType
     * @param activityName
     * @return
     */
    public static String buildChannelName(final String platformType, final String jobType, final String activityName) {
        LOGGER.trace("In buildChannelName.  platformType : {},JobType: {}, ActivityName : {}", platformType, jobType, activityName);
        final String platformTypeFortmated = makeFirstLetterCapital(platformType);
        final String jobTypeFormated = makeFirstLetterCapital(jobType);
        final String activityNameFormated = makeFirstLetterCapital(activityName);
        if (JobTypeEnum.NODE_HEALTH_CHECK.getAttribute().equalsIgnoreCase(jobType)) {
            return JMS_CHANNEL_URI_BASE + NHC + platformTypeFortmated + FORMATTED_NHC_JOB_TYPE + JMS_URI_SUB_STRING + activityNameFormated + JMS_URI_SUFFIX;
        } else {
            return JMS_CHANNEL_URI_BASE + SHM + platformTypeFortmated + jobTypeFormated + JMS_URI_SUB_STRING + activityNameFormated + JMS_URI_SUFFIX;
        }
    }

    /**
     * Method to build the channel URI dynamically based on platform type, job Type and activity name
     * 
     * @param platformType
     * @param jobType
     * 
     * @param activityName
     * @return
     */
    public static String buildChannelJNDIName(final String platformType, final String jobType, final String activityName) {
        final String platformTypeFortmated = makeFirstLetterCapital(platformType);
        final String jobTypeFormated = makeFirstLetterCapital(jobType);
        final String activityNameFormated = makeFirstLetterCapital(activityName);
        if (JobTypeEnum.NODE_HEALTH_CHECK.getAttribute().equalsIgnoreCase(jobType)) {
            return JMS_CHANNEL_EXPORTED_URI_BASE + NHC + platformTypeFortmated + FORMATTED_NHC_JOB_TYPE + JMS_URI_SUB_STRING + activityNameFormated + JMS_URI_SUFFIX;
        } else {
            return JMS_CHANNEL_EXPORTED_URI_BASE + SHM + platformTypeFortmated + jobTypeFormated + JMS_URI_SUB_STRING + activityNameFormated + JMS_URI_SUFFIX;
        }
    }

    /**
     * Method to make first letter of the string as upper-case and remaining as lower-case
     * 
     * @param string
     * @return
     */
    private static String makeFirstLetterCapital(final String string) {
        final String lowerCaseString = string.toLowerCase();
        return lowerCaseString.substring(0, 1).toUpperCase() + lowerCaseString.substring(1);
    }
}
