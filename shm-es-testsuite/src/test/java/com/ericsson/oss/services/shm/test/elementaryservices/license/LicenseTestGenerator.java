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
package com.ericsson.oss.services.shm.test.elementaryservices.license;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.test.common.TestConstants;
import com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase;

/*
 * @author xmanush
 */
@Stateless
public class LicenseTestGenerator {

    @Inject
    private IElementaryServicesTestBase elementaryDataBean;

    private String licensingMOFdn;

    /**
     * @return the licensingMOFdn
     */
    public String getLicensingMOFdn() {
        return licensingMOFdn;
    }

    public void prepareLicenseJobData(final String activityName) {
        final List<Map<String, Object>> jobProperties = getJobProperties();
        elementaryDataBean.createJobDetails(JobTypeEnum.LICENSE, jobProperties, activityName);
    }

    public PersistenceObject prepareLicensePOTestData() {
        final Map<String, Object> licenseAttributes = new HashMap<String, Object>();
        licenseAttributes.put("fingerPrint", "ERBSREF1");
        licenseAttributes.put("sequenceNumber", "1001");
        licenseAttributes.put("installedOn", new Date());
        licenseAttributes.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, "/home/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        final PersistenceObject licenseDataPO = elementaryDataBean.preparePOTestData(LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE, LicensingActivityConstants.LICENSE_DATA_PO,
                licenseAttributes);
        return licenseDataPO;
    }

    public ManagedObject prepareBaseMOTestData() throws Throwable {
        final ManagedObject createdManagedElementMO = elementaryDataBean.prepareBaseMOTestData();
        return createdManagedElementMO;
    }

    public void prepareLicensingMOTestData() throws Throwable {
        final ManagedObject createdSystemFunctionMO = elementaryDataBean.createSystemFunctionMO(prepareBaseMOTestData());
        final Map<String, Object> licensingMOAttributesMap = createLicensingMOMap();
        final ManagedObject licensingMO = createLicensingMO(licensingMOAttributesMap, createdSystemFunctionMO);
        licensingMOFdn = licensingMO.getFdn();
    }

    private List<Map<String, Object>> getJobProperties() {
        final Map<String, Object> jobPropertiesMap = new HashMap<String, Object>();
        final Map<String, Object> jobPropertiesMap1 = new HashMap<String, Object>();
        // Job Properties for License Elementary Services
        jobPropertiesMap.put("key", LicensingActivityConstants.LICENSE_FILE_PATH);
        jobPropertiesMap.put("value", "/home/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        jobPropertiesMap1.put("key", "LAST_LICENSING_PI_CHANGE");
        jobPropertiesMap1.put("value", "131022_131637");
        return Arrays.asList(jobPropertiesMap, jobPropertiesMap1);
    }

    /**
     * @return
     */
    private Map<String, Object> createLicensingMOMap() {
        final Map<String, Object> licAttributes = new HashMap<String, Object>();
        licAttributes.put("LicensingId", "1");
        licAttributes.put("userLabel", "userLabel");
        licAttributes.put("lastLicensingPiChange", "141022_131637");
        licAttributes.put("fingerprint", "ERBSREF1");
        licAttributes.put("licenseFileUrl", "http://192.168.101.208:80/cello/licensing/LTED1189-Syncnodeteam-sim/ATHEAST00001/ERBSREF1_141113_101852.xml");

        return licAttributes;
    }

    /**
     * Creates a Managed Object for Licensing MO in database
     * 
     * @returns Licensing Managed Object
     */
    private ManagedObject createLicensingMO(final Map<String, Object> licensingMOAttributesMap, final ManagedObject parent) {
        final ManagedObject licensingMO = elementaryDataBean.createChildMO("1", LicensingActivityConstants.LICENSE_MO, parent, TestConstants.ERBS_NODE_MODEL_NAMESPACE, parent.getVersion(),
                licensingMOAttributesMap);
        return licensingMO;
    }
}
