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
package com.ericsson.oss.services.shm.test.elementaryservices.upgrade;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.ActionResultInformation;
import com.ericsson.oss.services.shm.es.impl.cpp.upgrade.UpgradePackageMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase;

@Stateless
public class UpgradeTestGenerator {

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    private String upgradePackageMOFdn;

    private final String PACKAGE_NAME = "CXP102051_1_R4D73";
    private final String UCF = "CXP1020511_R4D73.xml";
    private final String FILE_PATH = "src/test/resources/elementary/upgrade/";
    private final String HASH = "CXP102051_1_R4D73";
    private final String UPGRADE_PACKAGE_ID = "CXP102051_1_R4D73";

    /**
     * @return the configurationVersionMOFdn
     */
    public String getUpgradePackageMOFdn() {
        return upgradePackageMOFdn;
    }

    public void prepareUpgradeJobData(final String activityName) {
        final List<Map<String, Object>> jobProperties = getJobProperties();
        elementaryDataBean.createJobDetails(JobTypeEnum.UPGRADE, jobProperties, activityName);
    }

    private List<Map<String, Object>> getJobProperties() {
        final Map<String, Object> swPkgNameProperty = new HashMap<String, Object>();
        swPkgNameProperty.put(ShmConstants.KEY, UpgradeActivityConstants.SWP_NAME);
        swPkgNameProperty.put(ShmConstants.VALUE, PACKAGE_NAME);
        final Map<String, Object> ucfProperty = new HashMap<String, Object>();
        ucfProperty.put(ShmConstants.KEY, UpgradeActivityConstants.UCF);
        ucfProperty.put(ShmConstants.VALUE, UCF);
        return Arrays.asList(swPkgNameProperty, ucfProperty);
    }

    /**
     * 
     */
    public void prepareUpgradePacakgePO() {
        final Map<String, Object> upgradeAttributes = new HashMap<String, Object>();
        upgradeAttributes.put(UpgradeActivityConstants.UP_PO_PACKAGE_NAME, PACKAGE_NAME);
        upgradeAttributes.put(UpgradeActivityConstants.UP_PO_FILE_PATH, FILE_PATH);
        upgradeAttributes.put(UpgradeActivityConstants.UP_PO_HASH, HASH);
        elementaryDataBean.preparePOTestData(UpgradeActivityConstants.UPPKG_NAMESPACE, UpgradeActivityConstants.UPPKG_TYPE_CPP, upgradeAttributes);

    }

    /**
     * @throws Throwable
     * 
     */
    public void prepareUpgradePackageMOTestData() throws Throwable {
        final ManagedObject createdManagedElementMO = elementaryDataBean.prepareBaseMOTestData();
        final ManagedObject createdSwManagementMo = elementaryDataBean.createSwManagementMO(createdManagedElementMO);
        final Map<String, Object> upgradePackageMOAttributesMap = createUpgradePackageMOMap();
        final ManagedObject upgradePackageMO = createUpgradePackageMO(upgradePackageMOAttributesMap, createdSwManagementMo);
        upgradePackageMOFdn = upgradePackageMO.getFdn();
    }

    /**
     * @param configurationVersionMOAttributesMap
     * @param createdSwManagementMo
     * @return
     */
    private ManagedObject createUpgradePackageMO(final Map<String, Object> upgradePackageMOAttributesMap, final ManagedObject createdSwManagementMo) {
        System.out.println("Creating Upgrade Package MO");
        final ManagedObject upgradePackageMO = elementaryDataBean.createChildMO(UPGRADE_PACKAGE_ID, UpgradeActivityConstants.UP_MO_TYPE, createdSwManagementMo,
                TestConstants.ERBS_NODE_MODEL_NAMESPACE, createdSwManagementMo.getVersion(), upgradePackageMOAttributesMap);
        System.out.println("Upgrade Package MO Created.");
        return upgradePackageMO;
    }

    /**
     * @return
     */
    private Map<String, Object> createUpgradePackageMOMap() {
        final Map<String, Object> upMoAttributes = new HashMap<String, Object>();
        upMoAttributes.put(UpgradePackageMoConstants.UP_MO_UPGRADE_PACKAGE_ID, UPGRADE_PACKAGE_ID);
        return upMoAttributes;
    }

    public List<Map<String, Object>> getActionResultList() {
        final List<Map<String, Object>> actionResultList = new ArrayList<Map<String, Object>>();

        //First Action Result
        final Map<String, Object> firstActionResult = new HashMap<String, Object>();
        firstActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 1L);
        firstActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "First Additional Info");
        firstActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTED.name());
        firstActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "First Time in String");
        firstActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "INSTALL");
        actionResultList.add(firstActionResult);

        //Second Action Result
        final Map<String, Object> secondActionResult = new HashMap<String, Object>();
        secondActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 2L);
        secondActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "Second Additional Info");
        secondActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTION_FAILED.name());
        secondActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "Second Time in String");
        secondActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "UPGRADE");
        actionResultList.add(secondActionResult);

        //Third Action Result
        final Map<String, Object> thirdActionResult = new HashMap<String, Object>();
        thirdActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ACTION_ID, 3L);
        thirdActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_ADDITIONAL_INFO, "Third Additional Info");
        thirdActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_INFO, ActionResultInformation.EXECUTED_WITH_WARNINGS.name());
        thirdActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TIME, "Third Time in String");
        thirdActionResult.put(UpgradePackageMoConstants.UP_ACTION_RESULT_TYPE_OF_INVOKED_ACTION, "INSTALL");
        actionResultList.add(thirdActionResult);

        return actionResultList;
    }
}
