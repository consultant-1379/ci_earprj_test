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
package com.ericsson.oss.services.shm.test.elementaryservices.restore;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.ConfigurationVersionType;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase;

@Stateless
public class RestoreTestGenerator {

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    private String configurationVersionMOFdn;
    final String CV_NAME = "CV_Name";
    final String CV_IDENTITY = "Some Identity";
    final ConfigurationVersionType CV_TYPE = ConfigurationVersionType.STANDARD;

    /**
     * @return the licensingMOFdn
     */
    public String getConfigurationVersionMOFdn() {
        return configurationVersionMOFdn;
    }

    public void prepareRestoreJobData(final String activityName) {
        final List<Map<String, Object>> jobProperties = getJobProperties();
        elementaryDataBean.createJobDetails(JobTypeEnum.RESTORE, jobProperties, activityName);
    }

    /**
     * @return
     */
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
        final Map<String, Object> forcedRestoreActionTypeMap = new HashMap<String, Object>();
        forcedRestoreActionTypeMap.put(ShmConstants.KEY, BackupActivityConstants.ACTION_FORCED_RESTORE);
        forcedRestoreActionTypeMap.put(ShmConstants.VALUE, ActivityConstants.CHECK_FALSE);
        final Map<String, Object> restoreActionTypeMap = new HashMap<String, Object>();
        restoreActionTypeMap.put(ShmConstants.KEY, BackupActivityConstants.ACTION_RESTORE);
        restoreActionTypeMap.put(ShmConstants.VALUE, ActivityConstants.CHECK_TRUE);
        //the jobproperties in jobConfigurationDetails of rmainJobAttributes
        final Map<String, Object> autoConfigurationMap = new HashMap<String, Object>();
        autoConfigurationMap.put(ShmConstants.KEY, ActivityConstants.AUTO_CONFIGURATION);
        autoConfigurationMap.put(ShmConstants.VALUE, "ON");

        return Arrays.asList(cvIdentityProperty, cvTypeProperty, cvNameProperty, forcedRestoreActionTypeMap, restoreActionTypeMap, autoConfigurationMap);
    }

    public ManagedObject prepareBaseMOTestData() throws Throwable {
        final ManagedObject createdManagedElementMO = elementaryDataBean.prepareBaseMOTestData();
        return createdManagedElementMO;
    }

    /**
     * @throws Throwable
     * 
     */
    public void prepareConfigurationVersionMOTestData() throws Throwable {
        final ManagedObject createdSwManagementMo = elementaryDataBean.createSwManagementMO(prepareBaseMOTestData());
        final Map<String, Object> configurationVersionMOAttributesMap = createConfigurationVersionMOMap();
        final ManagedObject cvMo = createConfigurationVersionMO(configurationVersionMOAttributesMap, createdSwManagementMo);
        configurationVersionMOFdn = cvMo.getFdn();
    }

    /**
     * @throws Throwable
     * 
     */
    /*
     * public void prepareBaseMOTestData() throws Throwable { elementaryDataBean.prepareBaseMOTestData(); }
     */
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
        cvMoAttributes.put("ConfigurationVersionId", "1");
        return cvMoAttributes;
    }
}
