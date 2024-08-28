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
package com.ericsson.oss.services.shm.test.elementaryservices;

import java.util.List;
import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/*
 * @author xmanush
 */
public interface IElementaryServicesTestBase {

    public PersistenceObject preparePOTestData(String namespace, String modelType, Map<String, Object> attributes);

    public int cleanUpTestPOData();

    public ManagedObject prepareBaseMOTestData() throws Throwable;

    public ManagedObject createSystemFunctionMO(final ManagedObject parentMO);

    public ManagedObject createChildMO(final String name, final String modelType, final ManagedObject parent, final String namespace, final String parentVersion, final Map<String, Object> attributes);

    public void deleteMOTestData();

    public void createJobDetails(JobTypeEnum jobType, List<Map<String, Object>> jobProperties, String activityName);

    public void updateAttributes(String fdn, Map<String, Object> attributes);

    public ManagedObject createSwManagementMO(final ManagedObject parentMO);

    public String getJobResultForActivity(final long activityJobId) throws InterruptedException;

    /**
     * 
     */
    void cleanUpgradePackageData();

}
