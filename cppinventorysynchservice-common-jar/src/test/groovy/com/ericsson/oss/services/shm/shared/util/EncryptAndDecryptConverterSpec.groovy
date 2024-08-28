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
package com.ericsson.oss.services.shm.shared.util

import java.nio.charset.StandardCharsets

import javax.xml.bind.DatatypeConverter

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.security.cryptography.CryptographyService

public class EncryptAndDecryptConverterSpec extends CdiSpecification{

    @ObjectUnderTest
    EncryptAndDecryptConverter encryptAndDecryptConverter
    @MockedImplementation
    protected CryptographyService cryptographyService;


    def "Get Encrypt Password."(){
        given : "preparing encrypted password"
        byte[] encodepwd = [17, -29, 49, 59, 126, 65, 103, 87, -60, -5, 65, -97, -83, -14, -74, -88, 13, 122, 56, 31, 124, -124, -93, 74, -10, 62, -61, 63, 56, -10, 111, -50]  ;
        cryptographyService.encrypt(_) >> encodepwd;
        when : "preparing encrypted password"
        String actualEncryptedPassword = encryptAndDecryptConverter.getEncryptedPassword(password)
        then : "verify encrypted password"
        actualEncryptedPassword.equals(expectedEncryptedPassword)
        where :
        password      | expectedEncryptedPassword
        "12345"       | "EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84="
        null          | null
    }

    def "Get Decrypt Password."(){
        given : "preparing decrypted password"
        final String encryptedPassword = "EeMxO35BZ1fE+0GfrfK2qA16OB98hKNK9j7DPzj2b84="
        String password = "12345"
        cryptographyService.decrypt(_) >> password.getBytes();
        when : "preparing decrypt password"
        String decryptedPwd = encryptAndDecryptConverter.getDecryptedPassword(encryptedPassword)
        then : "verify decrypted password"
        decryptedPwd.equals(password)
    }
}
