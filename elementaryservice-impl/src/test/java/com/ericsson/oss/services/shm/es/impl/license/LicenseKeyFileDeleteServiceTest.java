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
package com.ericsson.oss.services.shm.es.impl.license;

import static com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBContext;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService;

/**
 * This class tests the delete feature of the license key files Elementary Services.
 * 
 * @author xmanush
 */
@RunWith(MockitoJUnitRunner.class)
public class LicenseKeyFileDeleteServiceTest {

    @InjectMocks
    LicenseKeyFileDeleteService licenseKeyFileDeleteServiceMock;

    @Mock
    @Inject
    SystemRecorder systemRecorderMock;

    @Mock
    @Inject
    DpsWriter dpsWriterMock;

    @Mock
    @Inject
    DpsReader dpsReaderMock;

    @Mock
    private FileResource fileResourceMock;

    @Mock
    PersistenceObject matchingPO;

    @Mock
    EJBContext ejbContextMock;

    final String licenseMOFdn = "MeContext=ERBS00006,ManagedElement=1,SystemFunctions=1, Licensing=1";
    final long mainJobId = 281475014606250L;

    /**
     * This method deletes the historic license key files from the DPS and also from SMRS location.
     * 
     */

    @Test
    public void testDeleteHistoricLicensePOs() {
        final String fingerPrint = "CPPREFRXI";
        final String sequenceNumber = "1001";

        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<String, Map<String, List<String>>>();
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<String, List<String>>();
        final List<String> sequenceNumberValueList = new ArrayList<String>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<String, List<String>>();
        final List<String> fingerPrintValueList = new ArrayList<String>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);
        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, "/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(matchingPO.getAllAttributes()).thenReturn(poAttributes);
        when(matchingPO.getPoId()).thenReturn(123456L);
        final List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();
        matchingPOList.add(matchingPO);

        when(dpsReaderMock.findPOS(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, attributeRestrictionMap)).thenReturn(matchingPOList);
        final int deleteLicenseData = 1;
        when(dpsWriterMock.deletePoByPoId(123456)).thenReturn(deleteLicenseData);

        //Mockito.doNothing().when(fileResourceMock).init("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(fileResourceMock.exists("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml")).thenReturn(true);
        when(fileResourceMock.delete("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml")).thenReturn(true);
        verify(ejbContextMock, times(0)).setRollbackOnly();
        Mockito.doNothing().when(systemRecorderMock).recordCommand("DELETE_LICENSEKEY_FILES", CommandPhase.STARTED, "ERBS00006", licenseMOFdn, Long.toString(mainJobId));

        licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
    }

    @Test
    public void testDeleteHistoricLicensePOsShouldFail() {
        final String fingerPrint = "CPPREFRXI";
        final String sequenceNumber = "1001";

        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<String, Map<String, List<String>>>();
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<String, List<String>>();
        final List<String> sequenceNumberValueList = new ArrayList<String>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<String, List<String>>();
        final List<String> fingerPrintValueList = new ArrayList<String>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);

        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, "/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(matchingPO.getAllAttributes()).thenReturn(poAttributes);
        when(matchingPO.getPoId()).thenReturn(123456L);
        final List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();
        matchingPOList.add(matchingPO);

        when(dpsReaderMock.findPOS(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, attributeRestrictionMap)).thenReturn(matchingPOList);

        Mockito.doNothing().when(systemRecorderMock).recordCommand("DELETE_LICENSEKEY_FILES", CommandPhase.STARTED, "ERBS00006", licenseMOFdn, Long.toString(mainJobId));

        licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
    }

    // Unable to delete DB entry for 
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteShouldFailAsNoDBEntryIsFound() {
        final String fingerPrint = "CPPREFRXI";
        final String sequenceNumber = "1001";

        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<String, Map<String, List<String>>>();
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<String, List<String>>();
        final List<String> sequenceNumberValueList = new ArrayList<String>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<String, List<String>>();
        final List<String> fingerPrintValueList = new ArrayList<String>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);

        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, "/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(matchingPO.getAllAttributes()).thenReturn(poAttributes);
        when(matchingPO.getPoId()).thenReturn(123456L);
        final List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();
        matchingPOList.add(matchingPO);

        when(dpsReaderMock.findPOS(Matchers.any(String.class), Matchers.any(String.class), Matchers.any(Map.class))).thenReturn(matchingPOList);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("DELETE_LICENSEKEY_FILES", CommandPhase.STARTED, "ERBS00006", licenseMOFdn, Long.toString(mainJobId));

        licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
    }

    @Test
    public void testDeleteFileShouldFailAsNoFileExists() {
        final String fingerPrint = "CPPREFRXI";
        final String sequenceNumber = "1001";

        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<String, Map<String, List<String>>>();
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<String, List<String>>();
        final List<String> sequenceNumberValueList = new ArrayList<String>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<String, List<String>>();
        final List<String> fingerPrintValueList = new ArrayList<String>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);

        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, "/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(matchingPO.getAllAttributes()).thenReturn(poAttributes);
        when(matchingPO.getPoId()).thenReturn(123456L);
        final List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();
        matchingPOList.add(matchingPO);

        when(dpsReaderMock.findPOS(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, attributeRestrictionMap)).thenReturn(matchingPOList);
        final int deleteLicenseData = 1;
        when(dpsWriterMock.deletePoByPoId(123456)).thenReturn(deleteLicenseData);

        //Mockito.doNothing().when(fileResourceMock).init("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(fileResourceMock.exists("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml")).thenReturn(false);
        Mockito.doNothing().when(systemRecorderMock).recordCommand("DELETE_LICENSEKEY_FILES", CommandPhase.STARTED, "ERBS00006", licenseMOFdn, Long.toString(mainJobId));

        licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
    }

    @Test
    public void testDeleteShouldFailAsLKFileNotDeleted() {
        final String fingerPrint = "CPPREFRXI";
        final String sequenceNumber = "1001";

        final Map<String, Map<String, List<String>>> attributeRestrictionMap = new HashMap<String, Map<String, List<String>>>();
        final Map<String, List<String>> sequenceNumberValueMap = new HashMap<String, List<String>>();
        final List<String> sequenceNumberValueList = new ArrayList<String>();
        sequenceNumberValueList.add(sequenceNumber);
        sequenceNumberValueMap.put("LESS_THAN", sequenceNumberValueList);
        final Map<String, List<String>> fingerPrintValueMap = new HashMap<String, List<String>>();
        final List<String> fingerPrintValueList = new ArrayList<String>();
        fingerPrintValueList.add(fingerPrint);
        fingerPrintValueMap.put("EQUALS", fingerPrintValueList);

        attributeRestrictionMap.put(LICENSE_DATA_FINGERPRINT, fingerPrintValueMap);
        attributeRestrictionMap.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumberValueMap);

        final Map<String, Object> poAttributes = new HashMap<String, Object>();
        poAttributes.put(LICENSE_DATA_SEQUENCE_NUMBER, sequenceNumber);
        poAttributes.put(LICENSE_DATA_FINGERPRINT, fingerPrint);
        poAttributes.put(LICENSE_DATA_LICENSE_KEYFILE_PATH, "/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(matchingPO.getAllAttributes()).thenReturn(poAttributes);
        when(matchingPO.getPoId()).thenReturn(123456L);
        final List<PersistenceObject> matchingPOList = new ArrayList<PersistenceObject>();
        matchingPOList.add(matchingPO);

        when(dpsReaderMock.findPOS(LICENSE_DATA_PO_NAMESPACE, LICENSE_DATA_PO, attributeRestrictionMap)).thenReturn(matchingPOList);
        final int deleteLicenseData = 1;
        when(dpsWriterMock.deletePoByPoId(123456)).thenReturn(deleteLicenseData);

        // Mockito.doNothing().when(fileResourceMock).init("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        when(fileResourceMock.exists("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml")).thenReturn(true);
        when(fileResourceMock.delete("/ericsson/tor/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml")).thenReturn(false);
        verify(ejbContextMock, times(0)).setRollbackOnly();
        Mockito.doNothing().when(systemRecorderMock).recordCommand("DELETE_LICENSEKEY_FILES", CommandPhase.STARTED, "ERBS00006", licenseMOFdn, Long.toString(mainJobId));

        licenseKeyFileDeleteServiceMock.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
    }
}