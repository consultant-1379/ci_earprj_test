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
package com.ericsson.oss.services.shm.job.utils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.shared.util.EncryptAndDecryptConverter;
import com.ericsson.oss.services.shm.shared.util.JobPropertyUtil;

/**
 * This class updates the password property in Job Configuration details for backup job created on AXE components with encryption option.
 * 
 */
public class CreateJobAdditionalDataProvider {

    @Inject
    EncryptAndDecryptConverter encryptAndDecryptConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateJobAdditionalDataProvider.class);

    /**
     * @param jobProperties
     * @param password
     */
    public void readAndEncryptPasswordProperty(final List<Map<String, String>> jobProperties, final String password) {
        final String encryptedPwd = encryptAndDecryptConverter.getEncryptedPassword(password);
        JobPropertyUtil.updateJobProperty(jobProperties, ShmConstants.PASSWORD, encryptedPwd);
        LOGGER.debug("jobProperties after encryption {}", jobProperties);
    }
}
