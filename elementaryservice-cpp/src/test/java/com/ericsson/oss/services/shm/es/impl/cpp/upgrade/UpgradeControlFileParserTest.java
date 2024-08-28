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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class UpgradeControlFileParserTest {

    UpgradeControlFileParser objectUnderTest = new UpgradeControlFileParser();

    @Test
    public void testParse() throws IOException, SAXException {
        objectUnderTest.parse("src/test/resources/CXP1020511_R4D73.xml");
    }

    @Test
    public void testParseWithInvalidFile() throws IOException {
        boolean saxExceptionOccured = false;
        try {
            objectUnderTest.parse("src/test/resources/CXP1020511_R4D73_Invalid.xml");
        } catch (final SAXException e) {
            saxExceptionOccured = true;
        }
        assertTrue(saxExceptionOccured);
    }

    @Test
    public void testGetProductNumber() {
        objectUnderTest.getProductNumber();
    }

    @Test
    public void testGetProductRevision() {
        objectUnderTest.getProductRevision();
    }
}
