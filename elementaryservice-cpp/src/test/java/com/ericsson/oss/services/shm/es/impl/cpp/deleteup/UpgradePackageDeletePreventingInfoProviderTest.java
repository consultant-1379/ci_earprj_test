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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;
import com.ericsson.oss.services.shm.es.impl.cpp.deleteup.persistjobdata.PersistJobData;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageService;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePackageDeletePreventingInfoProviderTest {

    private static final String DELETE_PREVENTING_U_PS = "deletePreventingUPs";
    private static final String DELETE_PREVENTING_C_VS = "deletePreventingCVs";
    private static final String PNO_CXP102051_1 = "CXP102051/1";
    private static final String PRODUCT_REVISION = "productRevision";
    private static final String PRODUCT_NUMBER = "productNumber";

    @InjectMocks
    private UpgradePackageDeletePreventingInfoProvider objectUnderTest;

    @Mock
    private ActivityUtils activityUtils;

    @Mock
    private PersistJobData persistJobData;

    @Mock
    private UpgradePackageService upgradePackageService;

    @Mock
    private JobUpdateService jobUpdateService;
    @Mock
    private DeleteUpgradePackageActionInfo info;
    @Mock
    private ProductDataBean productDataBean;
    @Mock
    private JobLogUtil jobLogUtil;

    @Test
    public void testGetAndRemoveDeltaUPs_withFullTree() {

        Map<String, HashMap<String, Object>> preventingUpsAndCvs = new HashMap<String, HashMap<String, Object>>();

        HashMap<String, Object> map71 = new HashMap<String, Object>();
        HashMap<String, Object> map80 = new HashMap<String, Object>();
        HashMap<String, Object> map81 = new HashMap<String, Object>();
        HashMap<String, Object> map72 = new HashMap<String, Object>();
        HashMap<String, Object> map73 = new HashMap<String, Object>();
        HashMap<String, Object> map75 = new HashMap<String, Object>();

        HashMap<String, Object> map72dpups = new HashMap<String, Object>();
        map72dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map72dpups.put(PRODUCT_REVISION, "R4D72");

        HashMap<String, Object> map73dpups = new HashMap<String, Object>();
        map73dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73dpups.put(PRODUCT_REVISION, "R4D73");

        HashMap<String, Object> map75dpups = new HashMap<String, Object>();
        map75dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map75dpups.put(PRODUCT_REVISION, "R4D75");

        map71.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map71.put(PRODUCT_REVISION, "R4D71");
        map71.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017111434"));
        List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        list.add(map72dpups);
        list.add(map73dpups);
        map71.put(DELETE_PREVENTING_U_PS, list);

        map80.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map80.put(PRODUCT_REVISION, "R4D80");
        map80.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017114813"));
        map80.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        map72.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map72.put(PRODUCT_REVISION, "R4D72");
        map72.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017105758"));
        List<HashMap<String, Object>> list2 = new ArrayList<HashMap<String, Object>>();
        list2.add(map75dpups);
        map72.put(DELETE_PREVENTING_U_PS, list2);

        map81.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map81.put(PRODUCT_REVISION, "R4D81");
        map81.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017120331"));
        map81.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        map73.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73.put(PRODUCT_REVISION, "R4D73");
        map73.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017105758", "Backup_18102017104013"));
        map73.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        map75.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map75.put(PRODUCT_REVISION, "R4D75");
        map75.put(DELETE_PREVENTING_C_VS, Collections.EMPTY_LIST);
        map75.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        preventingUpsAndCvs.put("CXP102051/1**|**R4D71", map71);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D80", map80);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D81", map81);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D72", map72);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D73", map73);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D75", map75);

        Map<String, Object> lowestPossibleDeltaUP73 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertEquals(map73, lowestPossibleDeltaUP73);

        objectUnderTest.removeDeltaUPOccurences(1l, 1l, preventingUpsAndCvs, (String) lowestPossibleDeltaUP73.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP73.get(PRODUCT_REVISION));
        Assert.assertNull(preventingUpsAndCvs.get("CXP102051/1**|**R4D73"));
        Assert.assertEquals(0, getNoOfReference(preventingUpsAndCvs, (String) lowestPossibleDeltaUP73.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP73.get(PRODUCT_REVISION)));

        Map<String, Object> lowestPossibleDeltaUP75 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertEquals(map75, lowestPossibleDeltaUP75);

        objectUnderTest.removeDeltaUPOccurences(1l, 1l, preventingUpsAndCvs, (String) lowestPossibleDeltaUP75.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP75.get(PRODUCT_REVISION));
        Assert.assertNull(preventingUpsAndCvs.get("CXP102051/1**|**R4D75"));
        Assert.assertEquals(0, getNoOfReference(preventingUpsAndCvs, (String) lowestPossibleDeltaUP75.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP75.get(PRODUCT_REVISION)));

        Map<String, Object> lowestPossibleDeltaUP72 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertEquals(map72, lowestPossibleDeltaUP72);

        objectUnderTest.removeDeltaUPOccurences(1l, 1l, preventingUpsAndCvs, (String) lowestPossibleDeltaUP72.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP72.get(PRODUCT_REVISION));
        Assert.assertNull(preventingUpsAndCvs.get("CXP102051/1**|**R4D72"));
        Assert.assertEquals(0, getNoOfReference(preventingUpsAndCvs, (String) lowestPossibleDeltaUP72.get(PRODUCT_NUMBER), (String) lowestPossibleDeltaUP72.get(PRODUCT_REVISION)));

        Map<String, Object> lowestPossibleDeltaUP71 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertTrue(lowestPossibleDeltaUP71.isEmpty());
        Map<String, Object> lowestPossibleDeltaUP80 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D80");
        Assert.assertTrue(lowestPossibleDeltaUP80.isEmpty());
        Map<String, Object> lowestPossibleDeltaUP81 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D81");
        Assert.assertTrue(lowestPossibleDeltaUP81.isEmpty());

        Assert.assertEquals(3, preventingUpsAndCvs.size());
        verify(activityUtils, times(3)).prepareJobPropertyObjectList(Matchers.anyList(), Matchers.eq(UpgradeActivityConstants.PREVENTING_UP_CV_INFO), Matchers.any());
        verify(persistJobData, times(3)).persistPropertiesLogsAndProgress(Matchers.eq(1l), Matchers.eq(1l), Matchers.anyList(), Matchers.anyList(), Matchers.eq(0.0));
    }

    @Test
    public void testGetAndRemoveDeltaUPs() {
        Map<String, HashMap<String, Object>> preventingUpsAndCvs = new HashMap<String, HashMap<String, Object>>();
        preventingUpsAndCvs.put("CXP102051/1**|**R4D71", new HashMap<String, Object>());
        Map<String, Object> response = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertEquals(0, response.size());
    }

    @Test
    public void testGetAndRemoveDeltaUPs2() {
        Map<String, HashMap<String, Object>> preventingUpsAndCvs = new HashMap<String, HashMap<String, Object>>();
        HashMap<String, Object> map71 = new HashMap<String, Object>();
        HashMap<String, Object> map73 = new HashMap<String, Object>();
        HashMap<String, Object> map80 = new HashMap<String, Object>();
        HashMap<String, Object> map81 = new HashMap<String, Object>();
        HashMap<String, Object> map80dpups = new HashMap<String, Object>();
        map80dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map80dpups.put(PRODUCT_REVISION, "R4D80");

        HashMap<String, Object> map81dpups = new HashMap<String, Object>();
        map81dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map81dpups.put(PRODUCT_REVISION, "R4D81");

        HashMap<String, Object> map70dpups = new HashMap<String, Object>();
        map70dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map70dpups.put(PRODUCT_REVISION, "R4D70");

        HashMap<String, Object> map73dpups = new HashMap<String, Object>();
        map73dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73dpups.put(PRODUCT_REVISION, "R4D73");

        map71.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map71.put(PRODUCT_REVISION, "R4D71");
        map71.put(DELETE_PREVENTING_C_VS, Collections.EMPTY_LIST);
        List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
        list.add(map73dpups);
        list.add(map80dpups);
        map71.put(DELETE_PREVENTING_U_PS, list);

        map80.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map80.put(PRODUCT_REVISION, "R4D80");
        map80.put(DELETE_PREVENTING_C_VS, Collections.EMPTY_LIST);
        List<HashMap<String, Object>> list2 = new ArrayList<HashMap<String, Object>>();
        list2.add(map81dpups);
        list2.add(map70dpups);
        map80.put(DELETE_PREVENTING_U_PS, list2);

        map81.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map81.put(PRODUCT_REVISION, "R4D81");
        map81.put(DELETE_PREVENTING_C_VS, Arrays.asList("Backup_18102017120331"));
        map81.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        map73.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73.put(PRODUCT_REVISION, "R4D73");
        map73.put(DELETE_PREVENTING_C_VS, Collections.EMPTY_LIST);
        map73.put(DELETE_PREVENTING_U_PS, Collections.EMPTY_LIST);

        preventingUpsAndCvs.put("CXP102051/1**|**R4D71", map71);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D73", map73);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D80", map80);
        preventingUpsAndCvs.put("CXP102051/1**|**R4D81", map81);

        Map<String, Object> lowestPossibleDeltaUP70 = objectUnderTest.getLowestPossibleDeltaUP(preventingUpsAndCvs, PNO_CXP102051_1, "R4D71");
        Assert.assertEquals(Collections.EMPTY_MAP, lowestPossibleDeltaUP70);
    }

    private int getNoOfReference(Map<String, HashMap<String, Object>> preventingUpsAndCvs, String productNumber, String productRevision) {
        int referencesCount = 0;
        for (Entry<String, HashMap<String, Object>> map : preventingUpsAndCvs.entrySet()) {
            final List<Map<String, String>> deltaUPsPerMainUP = (List<Map<String, String>>) map.getValue().get(UpgradeActivityConstants.DELETE_PREVENTING_UPS);

            for (Map<String, String> eachDeltaUP : deltaUPsPerMainUP) {
                if (eachDeltaUP.containsValue(productNumber) && eachDeltaUP.containsValue(productRevision)) {
                    referencesCount++;
                }
            }
        }
        return referencesCount;
    }

    @Test
    public void testfindPreventiveUpsAndCvs() {
        Map<String, Object> map71 = new HashMap<String, Object>();
        List<HashMap<String, Object>> map71dpups = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map73dpups = new HashMap<String, Object>();
        map73dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73dpups.put(PRODUCT_REVISION, "R4D73");
        HashMap<String, Object> map80dpups = new HashMap<String, Object>();
        map80dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map80dpups.put(PRODUCT_REVISION, "R4D80");
        HashMap<String, Object> admindata71 = new HashMap<String, Object>();
        admindata71.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata71.put(PRODUCT_REVISION, "R4D71");
        map71dpups.add(map73dpups);
        map71dpups.add(map80dpups);
        map71.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, map71dpups);
        map71.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata71);
        UpgradePackageMO upMO71 = new UpgradePackageMO("UP=R4D71", map71);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, productDataBean.getProductNumber(), productDataBean.getProductRevision())).thenReturn(upMO71);

        Map<String, Object> map80 = new HashMap<String, Object>();
        map80.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, Collections.EMPTY_LIST);
        HashMap<String, Object> admindata80 = new HashMap<String, Object>();
        admindata80.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata80.put(PRODUCT_REVISION, "R4D80");
        map80.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata80);
        UpgradePackageMO upMO80 = new UpgradePackageMO("UP=R4D80", map80);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D80")).thenReturn(upMO80);

        Map<String, Object> map73 = new HashMap<String, Object>();
        List<HashMap<String, Object>> map73dpupsList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map81dpups = new HashMap<String, Object>();
        map81dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map81dpups.put(PRODUCT_REVISION, "R4D81");
        map73dpupsList.add(map81dpups);
        map73.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, map73dpupsList);
        HashMap<String, Object> admindata73 = new HashMap<String, Object>();
        admindata73.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata73.put(PRODUCT_REVISION, "R4D73");
        map73.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata73);
        UpgradePackageMO upMO73 = new UpgradePackageMO("UP=R4D73", map73);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D73")).thenReturn(upMO73);

        Map<String, Object> map81 = new HashMap<String, Object>();
        map81.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, Collections.EMPTY_LIST);
        HashMap<String, Object> admindata81 = new HashMap<String, Object>();
        admindata81.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata81.put(PRODUCT_REVISION, "R4D81");
        map81.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata81);
        UpgradePackageMO upMO81 = new UpgradePackageMO("UP=R4D81", map81);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D81")).thenReturn(upMO81);

        Mockito.when(info.isPreventUpDeletable()).thenReturn(true);
        Map<String, HashMap<String, Object>> preventingUpsAndCvs = objectUnderTest.findPreventiveUpsAndCvs(0, info, productDataBean, new HashMap<String, HashMap<String, Object>>());
        Assert.assertNotNull(preventingUpsAndCvs);
        Assert.assertEquals(4, preventingUpsAndCvs.size());
        Assert.assertTrue(preventingUpsAndCvs.keySet()
                .containsAll(Arrays.asList(PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D81",
                        PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D73", PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D71",
                        PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D80")));
    }

    @Test
    public void testfindPreventiveUpsAndCvs2() {
        Map<String, Object> map71 = new HashMap<String, Object>();
        List<HashMap<String, Object>> map71dpups = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map73dpups = new HashMap<String, Object>();
        map73dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map73dpups.put(PRODUCT_REVISION, "R4D73");
        HashMap<String, Object> map80dpups = new HashMap<String, Object>();
        map80dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map80dpups.put(PRODUCT_REVISION, "R4D80");
        HashMap<String, Object> admindata71 = new HashMap<String, Object>();
        admindata71.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata71.put(PRODUCT_REVISION, "R4D71");
        map71dpups.add(map73dpups);
        map71dpups.add(map80dpups);
        map71.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, map71dpups);
        map71.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata71);
        UpgradePackageMO upMO71 = new UpgradePackageMO("UP=R4D71", map71);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, productDataBean.getProductNumber(), productDataBean.getProductRevision())).thenReturn(upMO71);

        Map<String, Object> map80 = new HashMap<String, Object>();
        map80.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, Collections.EMPTY_LIST);
        HashMap<String, Object> admindata80 = new HashMap<String, Object>();
        admindata80.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata80.put(PRODUCT_REVISION, "R4D80");
        map80.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata80);
        UpgradePackageMO upMO80 = new UpgradePackageMO("UP=R4D80", map80);
        Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D80")).thenReturn(upMO80);

        Map<String, Object> map73 = new HashMap<String, Object>();
        List<HashMap<String, Object>> map73dpupsList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map81dpups = new HashMap<String, Object>();
        map81dpups.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        map81dpups.put(PRODUCT_REVISION, "R4D81");
        map73dpupsList.add(map81dpups);
        map73.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, map73dpupsList);
        HashMap<String, Object> admindata73 = new HashMap<String, Object>();
        admindata73.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata73.put(PRODUCT_REVISION, "R4D73");
        map73.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata73);
        //UpgradePackageMO upMO73 = new UpgradePackageMO("UP=R4D73", map73);
        //Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D73")).thenReturn(upMO73);

        Map<String, Object> map81 = new HashMap<String, Object>();
        map81.put(UpgradeActivityConstants.DELETE_PREVENTING_UPS, Collections.EMPTY_LIST);
        HashMap<String, Object> admindata81 = new HashMap<String, Object>();
        admindata81.put(PRODUCT_NUMBER, PNO_CXP102051_1);
        admindata81.put(PRODUCT_REVISION, "R4D81");
        map81.put(UpgradeActivityConstants.ADMINISTRATIVE_DATA, admindata81);
        //UpgradePackageMO upMO81 = new UpgradePackageMO("UP=R4D81", map81);
        //Mockito.when(upgradePackageService.getUPMOAttributes(0, PNO_CXP102051_1, "R4D81")).thenReturn(upMO81);

        Mockito.when(info.isPreventUpDeletable()).thenReturn(true);
        Map<String, HashMap<String, Object>> preventingUpsAndCvs = objectUnderTest.findPreventiveUpsAndCvs(0, info, productDataBean, new HashMap<String, HashMap<String, Object>>());
        Assert.assertNotNull(preventingUpsAndCvs);
        Assert.assertEquals(2, preventingUpsAndCvs.size());
        Assert.assertTrue(preventingUpsAndCvs.keySet().containsAll(Arrays.asList(PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D71",
                PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D80")));
        HashMap<String, Object> dpcvs = preventingUpsAndCvs.get(PNO_CXP102051_1 + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + "R4D71");
        Assert.assertEquals(1, ((List<HashMap<String, Object>>) dpcvs.get(UpgradeActivityConstants.DELETE_PREVENTING_UPS)).size());
    }
}
