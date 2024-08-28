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
package com.ericsson.oss.services.shm.shared.util;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;

import com.ericsson.oss.itpf.security.cryptography.CryptographyService;

/**
 * Class that implements 2 levels of encrypt and decrypt of the given string using CryptographyService and Base64
 */

public class EncryptAndDecryptConverter {

    @Inject
    protected CryptographyService cryptographyService;

    /**
     * Encrypts the password using the cryptography service and Base64
     *
     * @param userPassword
     * @return base64 encoded encrypted string
     */
    public String getEncryptedPassword(final String password) {
        final String encryptedPassword = null;
        if (password != null) {
            final byte[] encodedPwd = cryptographyService.encrypt(password.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printBase64Binary(encodedPwd);
        } else {
            return encryptedPassword;
        }
    }

    /**
     * Decrypts the password using the cryptography service and Base64
     *
     * @param encryptedPassword
     * @return decrypted string
     */
    public String getDecryptedPassword(final String encryptedPassword) {
        final byte[] decryptedBytes = cryptographyService.decrypt(DatatypeConverter.parseBase64Binary(encryptedPassword));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
