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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.ejb.*;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;
import com.ericsson.oss.services.shm.test.common.TestConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class TestDataGenerator {

    @Inject
    DataPersistenceServiceProxy dps;

    protected static final int MAX_UPGRADE_PACKAGES = 3;
    private static final String SOFTWARE_PACKAGE_NAME = "CXP1020511_R4D26_";
    private static final String SOFTWAREPACKAGE_JOB_PARAMNAME = "paramName";
    private static final String SOFTWAREPACKAGE_JOB_PARAMTYPE = "paramType";
    private static final String SOFTWAREPACKAGE_JOB_PARAMITEMS = "items";

    protected static String jobTemplatePoId = "jobTemplatePoId";
    protected static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    public static String activityJobPoId = "activityJobPoId";
    public static Map<String, Long> poIds = new HashMap<String, Long>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void createAllTestJobs() {

        final PersistenceObject jobTemplate = createPO(getJobTemplateAttributes(), "shm", "JobTemplate");
        poIds.put(jobTemplatePoId, jobTemplate.getPoId());
        final PersistenceObject mainJobPo = createPO(getMainJob(), "shm", "Job");
        poIds.put(mainJobPoId, mainJobPo.getPoId());
        final PersistenceObject neJobPo = createPO(getNeJobData(), "shm", "NEJob");
        poIds.put(neJobPoId, neJobPo.getPoId());
        final PersistenceObject activityJobPo = createPO(getActivityJobData(), "shm", "ActivityJob");
        poIds.put(activityJobPoId, activityJobPo.getPoId());
    }

    /**
     * @return
     */

    private Map<String, Object> getJobTemplateAttributes() {

        final HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();

        JobTemplateMap.put("jobType", "RESTORE");
        JobTemplateMap.put("name", "jobName");
        JobTemplateMap.put("owner", "shmtest");
        JobTemplateMap.put("description", "Test");
        JobTemplateMap.put("creationTime", new Date());
        JobTemplateMap.put("jobConfigurationDetails", getJobConfiguration());
        return JobTemplateMap;

    }

    /**
     * @param jobConfigurationMap
     */
    private HashMap<String, Object> getMainJob() {
        final HashMap<String, Object> jobMap = new HashMap<String, Object>();
        jobMap.put("executionIndex", 1);
        //jobMap.put("comment", "jobpo");
        jobMap.put("jobConfigurationDetails", getJobConfiguration());
        jobMap.put("templateJobId", poIds.get(jobTemplatePoId));
        jobMap.putAll(getAbstractJobMap());
        return jobMap;
    }

    /**
     * @return
     */
    private List<Map<String, Object>> getJobProperties() {
        final Map<String, Object> jobPropertiesMap2 = new HashMap<String, Object>();
        jobPropertiesMap2.put("key", JobPropertyConstants.CORRUPTED_PKG_SELECTION);
        jobPropertiesMap2.put("value", "true");

        final Map<String, Object> jobPropertiesMap3 = new HashMap<String, Object>();
        jobPropertiesMap3.put("key", JobPropertyConstants.MISSING_PKG_SELECTION);
        jobPropertiesMap3.put("value", "true");

        return Arrays.asList(jobPropertiesMap2, jobPropertiesMap3);
    }

    /**
     * @return
     */
    private HashMap<String, Object> getActivity() {
        final HashMap<String, Object> activityMap = new HashMap<String, Object>();
        activityMap.put("name", "install");
        activityMap.put("order", 1);
        activityMap.put("platform", "CPP");
        activityMap.put("schedule", getScheduleMap());
        return activityMap;
    }

    /**
     * @param scheduledpropertymap
     * @return
     */
    private HashMap<String, Object> getScheduleMap() {
        final HashMap<String, Object> scheduledpropertymap = new HashMap<String, Object>();
        scheduledpropertymap.put("name", "START_DATE");
        scheduledpropertymap.put("value", new Date().toString());
        final List<Map<String, Object>> listOfScheduledproperty = new ArrayList<Map<String, Object>>();
        listOfScheduledproperty.add(scheduledpropertymap);
        final HashMap<String, Object> scheduleMap = new HashMap<String, Object>();
        scheduleMap.put("scheduleAttributes", listOfScheduledproperty);
        scheduleMap.put("execMode", "IMMEDIATE");
        return scheduleMap;
    }

    /**
     * @param NeInfoMap
     * @param scheduleMap
     * @param listOfActivities
     * @return
     */
    private HashMap<String, Object> getJobConfiguration() {
        final HashMap<String, Object> jobConfigurationMap = new HashMap<String, Object>();
        final HashMap<String, Object> NeInfoMap = new HashMap<String, Object>();
        final List<String> listOfCollectionNames = new ArrayList<String>();
        final List<String> listOfNeNames = new ArrayList<String>();
        listOfCollectionNames.add("Shmtest1");
        listOfCollectionNames.add("shmtest2");
        listOfNeNames.add("ERBS101");
        listOfNeNames.add("ERBS102");
        NeInfoMap.put("collectionNames", listOfCollectionNames);
        NeInfoMap.put("neNames", listOfNeNames);

        jobConfigurationMap.put("activities", Arrays.asList(getActivity()));
        jobConfigurationMap.put("mainSchedule", getScheduleMap());
        jobConfigurationMap.put("jobProperties", getJobProperties());
        jobConfigurationMap.put("selectedNEs", NeInfoMap);
        return jobConfigurationMap;
    }

    /**
         * 
         */
    private HashMap<String, Object> getActivityJobData() {
        final HashMap<String, Object> activityjobMap = new HashMap<String, Object>();
        activityjobMap.put("name", "install");
        activityjobMap.put("order", 1);
        activityjobMap.put("neJobId", poIds.get(neJobPoId));
        activityjobMap.putAll(getAbstractJobMap());
        return activityjobMap;
    }

    /**
         * 
         */
    private HashMap<String, Object> getNeJobData() {
        final HashMap<String, Object> NEjobMap = new HashMap<String, Object>();
        //NEjobMap.put("comment", "NEjobPo");
        NEjobMap.put("neName", "ERBS101");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", "222");
        NEjobMap.put("mainJobId", poIds.get(mainJobPoId));
        NEjobMap.putAll(getAbstractJobMap());
        return NEjobMap;
    }

    private HashMap<String, Object> getAbstractJobMap() {
        final HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", "CREATED");
        abstractJobMap.put("progressPercentage", 10.00);
        abstractJobMap.put("result", "SUCCESS");
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());
        abstractJobMap.put("jobProperties", getJobProperties());

        final HashMap<String, Object> jobLogMap = new HashMap<String, Object>();
        jobLogMap.put("entryTime", new Date());
        jobLogMap.put("message", "Test");
        jobLogMap.put("type", "NE");
        final List<Map<String, Object>> listOfJobLog = new ArrayList<Map<String, Object>>();
        listOfJobLog.add(jobLogMap);
        abstractJobMap.put("log", listOfJobLog);
        return abstractJobMap;
    }

    public int cleanUpTestJobsData() {
        int deletedPos = 0;
        for (final Entry<String, Long> entry : poIds.entrySet()) {
            logger.info("Deleting a PO for {} with id:{} ", entry.getKey(), entry.getValue());
            final PersistenceObject persistenceObject = dps.getDataPersistenceService().getLiveBucket().findPoById(entry.getValue());
            if (persistenceObject != null) {
                deletedPos = deletedPos + dps.getDataPersistenceService().getLiveBucket().deletePo(persistenceObject);
            }
        }
        return deletedPos;
    }

    /**
     * @param licenseAttributes
     * @param namespace
     * @param modelType
     * @return
     */
    private PersistenceObject createPO(final Map<String, Object> licenseAttributes, final String namespace, final String modelType) {
        final PersistenceObjectBuilder poBuilder = dps.getDataPersistenceService().getLiveBucket().getPersistenceObjectBuilder().namespace(namespace).version("1.0.0").type(modelType)
                .addAttributes(licenseAttributes);
        return poBuilder.create();
    }

    public void createSoftwarePackageTestData() throws IOException {
        for (int index = 1; index < MAX_UPGRADE_PACKAGES + 1; index++) {
            dps.getDataPersistenceService().getLiveBucket().getPersistenceObjectBuilder().namespace(TestConstants.SOFTWAREPACKAGE_NAMESPACE).type(TestConstants.CPPSOFTWAREPACKAGE_TYPE)
                    .version("1.0.0").addAttributes(createSoftwarePackageMap(index)).create();
        }
        createSwPkgMockFolders();
        //createJobsDataForDeleteUseCase();
    }

    private Map<String, Object> createSoftwarePackageMap(final int index) {
        final Map<String, Object> cppSoftwarePackageMap = new HashMap<String, Object>();
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_PACKAGENAME, SOFTWARE_PACKAGE_NAME + index);
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_PLATFORM, "CPP");
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_DESCRIPTION, "Package " + SOFTWARE_PACKAGE_NAME + index);
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_IMPORTEDDATE, new Date(System.currentTimeMillis()));
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_IMPORTEDBY, "enmuser01");
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_FILEPATH, "c:/swps/");

        final List<Object> softwarePackageProdDetailsList = new ArrayList<Object>();
        final Map<String, Object> softwarePackageProdDetails1 = createSoftwarePackageProductDetailsMap(index);
        softwarePackageProdDetailsList.add(softwarePackageProdDetails1);
        cppSoftwarePackageMap.put(TestConstants.SOFTWAREPACKAGE_PRODUCTDETAILS, softwarePackageProdDetailsList);

        final List<Map<String, Object>> jobParamsList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> jobParamsMap = createCppSoftwarePackageJobParams(index);
        jobParamsList.add(jobParamsMap);
        cppSoftwarePackageMap.put("jobParameters", jobParamsList);

        final List<Map<String, Object>> activitiesList = new ArrayList<Map<String, Object>>();
        final Map<String, Object> activityMap = createSoftwarePackageActivity(index);
        activitiesList.add(activityMap);
        cppSoftwarePackageMap.put("activities", activitiesList);

        return cppSoftwarePackageMap;
    }

    private void createSwPkgMockFolders() throws IOException {
        for (int index = 1; index < MAX_UPGRADE_PACKAGES + 1; index++) {
            final File directory = new File("c:/swps/", SOFTWARE_PACKAGE_NAME + index);
            directory.mkdirs();
            final File file = new File(directory.getAbsolutePath() + "/" + SOFTWARE_PACKAGE_NAME + index);
            file.createNewFile();
        }
    }

    private Map<String, Object> createSoftwarePackageProductDetailsMap(final int index) {
        final Map<String, Object> softwarePackageProdDetails = new HashMap<String, Object>();
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCTNAME, "CSUC");
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCTNUMBER, "CXC1721554_" + index);
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCT_DESCRIPTION, "CSUC_CXC1721554_" + index);
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCT_RELEASEDATE, new Date(System.currentTimeMillis()));
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCTREVISION, "R61N01");
        softwarePackageProdDetails.put(TestConstants.SOFTWAREPACKAGE_PRODUCT_TYPE, "CPP");
        return softwarePackageProdDetails;
    }

    private Map<String, Object> createCppSoftwarePackageJobParams(final int index) {
        final Map<String, Object> jobParamsMap = new HashMap<String, Object>();
        jobParamsMap.put(SOFTWAREPACKAGE_JOB_PARAMNAME, "SMO_UPGRADE_CONTROL_FILE");
        jobParamsMap.put(SOFTWAREPACKAGE_JOB_PARAMTYPE, "STRING");
        jobParamsMap.put(TestConstants.SOFTWAREPACKAGE_PARAM_DESCRIPTION, "Description:" + index);
        jobParamsMap.put(TestConstants.SOFTWAREPACKAGE_PARAM_PROMPT, "The name of upgrade control file");
        final List<String> itemsList = new ArrayList<String>();
        itemsList.add("CXP9012123_R4D26_" + index + ".xml");
        itemsList.add("CXP9012123_R4D261_" + index + ".xml");
        jobParamsMap.put(SOFTWAREPACKAGE_JOB_PARAMITEMS, itemsList);

        return jobParamsMap;
    }

    private Map<String, Object> createSoftwarePackageActivity(final int index) {
        final Map<String, Object> activityMap = new HashMap<String, Object>();
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_NAME, "Installation");
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_STARTUP, "IMMEDIATE");
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_SCRIPTFILENAME, "File:" + index);
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_DESCRIPTION, "Activity under progress");
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_SELECTED, true);
        final List<Map<String, Object>> paramsList = new ArrayList<>();
        for (int index_local = 1; index_local < 6; index_local++) {
            final Map<String, Object> param = createCppSoftwarePackageJobParams(index * index_local);
            paramsList.add(param);
        }
        activityMap.put(TestConstants.CPPSOFTWAREPACKAGE_ACTIVITY_PARAMS, paramsList);
        return activityMap;
    }

    /**
     * @author ekatyas
     */
    public void deleteSoftwarePackageTestData() {
        final QueryExecutor queryExecutor = dps.getDataPersistenceService().getLiveBucket().getQueryExecutor();
        final QueryBuilder queryBuilder = dps.getDataPersistenceService().getQueryBuilder();
        final Query<TypeRestrictionBuilder> packageQuery = queryBuilder.createTypeQuery(TestConstants.SOFTWAREPACKAGE_NAMESPACE, TestConstants.CPPSOFTWAREPACKAGE_TYPE);
        final Iterator<PersistenceObject> packageIterator = queryExecutor.execute(packageQuery);
        while (packageIterator.hasNext()) {
            dps.getDataPersistenceService().getLiveBucket().deletePo(packageIterator.next());
        }
        final Query<TypeRestrictionBuilder> jobQuery = queryBuilder.createTypeQuery(TestConstants.JOB_NAME_SPACE, TestConstants.JOB_PO_TYPE);
        final Iterator<PersistenceObject> jobIterator = queryExecutor.execute(jobQuery);
        while (jobIterator.hasNext()) {
            dps.getDataPersistenceService().getLiveBucket().deletePo(jobIterator.next());
        }

        final File swpsFolder = new File("c:/swps/");
        FileUtils.deleteQuietly(swpsFolder);
    }

}
