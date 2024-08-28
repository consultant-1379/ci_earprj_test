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
package com.ericsson.oss.services.shm.test.elementaryservices.backup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionType;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMoConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase;

@Stateless
public class BackupTestGenerator {

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    private String configurationVersionMOFdn;

    final String CV_IDENTITY = "Some Identity";
    final String CV_NAME = "Some CV Name";
    final ConfigurationVersionType CV_TYPE = ConfigurationVersionType.STANDARD;
    private final String STATUS = "OK";
    private final String DATE = "Fri Jan 16 17:44:01 IST 2015";

    /**
     * @return the configurationVersionMOFdn
     */
    public String getConfigurationVersionMOFdn() {
        return configurationVersionMOFdn;
    }

    public void prepareBackupJobData(final String activityName) {
        final List<Map<String, Object>> jobProperties = getJobProperties();
        try {
            elementaryDataBean.createJobDetails(JobTypeEnum.BACKUP, jobProperties, activityName);
        } catch (final Exception e) {
            System.out.println("Exception Occured");
            e.printStackTrace();
        }
    }

    private List<Map<String, Object>> getJobProperties() {
        final Map<String, Object> cvIdentityProperty = new HashMap<String, Object>();
        cvIdentityProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_IDENTITY);
        cvIdentityProperty.put(ShmConstants.VALUE, CV_IDENTITY);
        final Map<String, Object> cvTypeProperty = new HashMap<String, Object>();
        cvTypeProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_TYPE);
        cvTypeProperty.put(ShmConstants.VALUE, CV_TYPE.name());
        final Map<String, Object> cvNameProperty = new HashMap<String, Object>();
        cvNameProperty.put(ShmConstants.KEY, BackupActivityConstants.CV_NAME);
        cvNameProperty.put(ShmConstants.VALUE, CV_NAME);
        final Map<String, Object> cvNameBackUpProperty = new HashMap<String, Object>();
        cvNameBackUpProperty.put(ShmConstants.KEY, BackupActivityConstants.PROCESSED_BACKUPS);
        cvNameBackUpProperty.put(ShmConstants.VALUE, "1");

        return Arrays.asList(cvIdentityProperty, cvTypeProperty, cvNameProperty, cvNameBackUpProperty);
    }

    /**
     * @throws Throwable
     * 
     */
    public void prepareConfigurationVersionMOTestData() throws Throwable {
        final ManagedObject createdManagedElementMO = elementaryDataBean.prepareBaseMOTestData();
        final ManagedObject createdSwManagementMo = elementaryDataBean.createSwManagementMO(createdManagedElementMO);
        final Map<String, Object> configurationVersionMOAttributesMap = createConfigurationVersionMOMap();
        final ManagedObject configurationVersionMO = createConfigurationVersionMO(configurationVersionMOAttributesMap, createdSwManagementMo);
        configurationVersionMOFdn = configurationVersionMO.getFdn();
    }

    /**
     * @param configurationVersionMOAttributesMap
     * @param createdSwManagementMo
     * @return
     */
    private ManagedObject createConfigurationVersionMO(final Map<String, Object> configurationVersionMOAttributesMap, final ManagedObject createdSwManagementMo) {
        System.out.println("Creating Configuration Version MO");
        final ManagedObject configurationVersionMO = elementaryDataBean.createChildMO("1", BackupActivityConstants.CV_MO_TYPE, createdSwManagementMo, TestConstants.ERBS_NODE_MODEL_NAMESPACE,
                createdSwManagementMo.getVersion(), configurationVersionMOAttributesMap);
        System.out.println("Configuration Version MO Created.");
        return configurationVersionMO;
    }

    /**
     * @return
     */
    private Map<String, Object> createConfigurationVersionMOMap() {
        final Map<String, Object> cvMoAttributes = new HashMap<String, Object>();
        cvMoAttributes.put(ConfigurationVersionMoConstants.CONFIGURATION_VERSION_ID, "1");
        return cvMoAttributes;
    }

    public List<Map<String, String>> getStoredConfigurationVersionList() {
        final List<Map<String, String>> storedConfigurationVersionList = new ArrayList<Map<String, String>>();

        //First CV
        final Map<String, String> firstStoredConfigurationVersion = new HashMap<String, String>();
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "First CV");
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, DATE);
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_IDENTITY, CV_IDENTITY);
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT, "");
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, STATUS);
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, CV_TYPE.name());
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_NAME, TestConstants.JOB_OWNER);
        firstStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID, "CXP102051_1_R4D73");
        storedConfigurationVersionList.add(firstStoredConfigurationVersion);

        //Second CV
        final Map<String, String> secondStoredConfigurationVersion = new HashMap<String, String>();
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "Second CV");
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, DATE);
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_IDENTITY, CV_IDENTITY);
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT, "");
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, STATUS);
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, CV_TYPE.name());
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_NAME, TestConstants.JOB_OWNER);
        secondStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID, "CXP102051_1_R4D73");
        storedConfigurationVersionList.add(secondStoredConfigurationVersion);

        //Third CV
        final Map<String, String> thirdStoredConfigurationVersion = new HashMap<String, String>();
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, "Third CV");
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, DATE);
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_IDENTITY, CV_IDENTITY);
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT, "");
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, STATUS);
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, CV_TYPE.name());
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_NAME, TestConstants.JOB_OWNER);
        thirdStoredConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID, "CXP102051_1_R4D73");
        storedConfigurationVersionList.add(thirdStoredConfigurationVersion);

        return storedConfigurationVersionList;
    }

    public Map<String, String> getConfigurationVersionDetails(final String cvName) {
        final Map<String, String> storedConfigurationVersion = new HashMap<String, String>();
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_NAME, cvName);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_DATE, DATE);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_IDENTITY, CV_IDENTITY);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT, "");
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_STATUS, STATUS);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_TYPE, CV_TYPE.name());
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_OPERATOR_NAME, TestConstants.JOB_OWNER);
        storedConfigurationVersion.put(ConfigurationVersionMoConstants.STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID, "CXP102051_1_R4D73");
        return storedConfigurationVersion;
    }
}
