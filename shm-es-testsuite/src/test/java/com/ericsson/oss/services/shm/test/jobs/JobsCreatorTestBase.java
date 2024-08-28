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
package com.ericsson.oss.services.shm.test.jobs;

import java.util.*;

import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JobsCreatorTestBase {

    @Inject
    DataPersistenceServiceProxy dps;

    protected static String jobTemplatePoId = "jobTemplatePoId";
    protected static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    protected static String activityJobPoId = "activityJobPoId";
    protected String state;
    protected static Map<String, Long> poIds = new HashMap<String, Long>();
    protected static Map<String, Long> poIdsWithoutActivity = new HashMap<String, Long>();

    private final static String SHM_NAMESPACE = "shm";
    private final static String JOBTEMPLATE = "JobTemplate";
    private final static String JOB = "Job";
    private final static String NEJOB = "NEJob";
    private final static String ACTIVITYJOB = "ActivityJob";
    protected static String jobTemplatePoId_1 = "jobTemplatePoId_1";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * tnonExistingjobLog
     */
    public void createAllJobsWithouActivity(final String state) {
        this.state = state;
        createAllJobsWithouActivity();
    }

    public void createAllJobsWithouActivity() {

        final PersistenceObject jobTemplate = createPO(getJobTemplateAttributes(), SHM_NAMESPACE, JOBTEMPLATE);
        poIdsWithoutActivity.put(jobTemplatePoId, jobTemplate.getPoId());
        final PersistenceObject mainJobPo = createPO(getMainJob(), SHM_NAMESPACE, JOB);
        poIdsWithoutActivity.put(mainJobPoId, mainJobPo.getPoId());
        final PersistenceObject neJobPo = createPO(getNeJobData(), SHM_NAMESPACE, NEJOB);
        poIdsWithoutActivity.put(neJobPoId, neJobPo.getPoId());
    }

    /**
     * @return List of PO id's map for delete_Jobs_Fail_Multiple_deletions test case
     */

    public List<Map<String, Long>> getMultipleJobList() {

        final List<Map<String, Long>> multipleJobPOidList = new ArrayList<Map<String, Long>>();

        final Map<String, Long> map1 = createMultipleJobsPo("jobone", "COMPLETED");
        final Map<String, Long> map2 = createMultipleJobsPo("jobtwo", "RUNNING");
        final Map<String, Long> map3 = createMultipleJobsPo("jobthree", "CANCELLED");
        final Map<String, Long> map4 = createMultipleJobsPo("jobfour", "COMPLETED");
        multipleJobPOidList.add(map1);
        multipleJobPOidList.add(map2);
        multipleJobPOidList.add(map3);
        multipleJobPOidList.add(map4);
        return multipleJobPOidList;

    }

    private Map<String, Long> createMultipleJobsPo(final String name, final String state) {

        final HashMap<String, Long> poIdMap = new HashMap<String, Long>();

        final PersistenceObject jobTemplate = createPO(getJobTemplateAttributesForMultipleJobs(name), SHM_NAMESPACE, JOBTEMPLATE);
        poIdMap.put(jobTemplatePoId, jobTemplate.getPoId());

        final PersistenceObject mainJobPo = createPO(createMainJob(state, jobTemplate.getPoId()), SHM_NAMESPACE, JOB);
        poIdMap.put(mainJobPoId, mainJobPo.getPoId());

        final PersistenceObject neJobPo = createPO(createNeJobData(state, mainJobPo.getPoId()), SHM_NAMESPACE, NEJOB);
        poIdMap.put(neJobPoId, neJobPo.getPoId());

        final PersistenceObject activityJobPo = createPO(CreateActivityJobData(state, neJobPo.getPoId()), SHM_NAMESPACE, ACTIVITYJOB);
        poIdMap.put(activityJobPoId, activityJobPo.getPoId());

        logger.info("PO ids found : {} {} {} {} ", jobTemplate.getPoId(), mainJobPo.getPoId(), neJobPo.getPoId(), activityJobPo.getPoId());
        return poIdMap;

    }

    /**
     * @param Data
     *            for delete_Jobs_Fail_Multiple_deletions test case
     */
    private HashMap<String, Object> getJobTemplateAttributesForMultipleJobs(final String name) {

        final HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();

        JobTemplateMap.put("jobType", "UPGRADE");
        JobTemplateMap.put("name", name);
        JobTemplateMap.put("owner", "shmtest");
        JobTemplateMap.put("description", "Test");
        JobTemplateMap.put("creationTime", new Date());
        JobTemplateMap.put("jobConfigurationDetails", getJobConfiguration());
        return JobTemplateMap;

    }

    private HashMap<String, Object> createMainJob(final String state, final Long jobTemplateId) {
        final HashMap<String, Object> jobMap = new HashMap<String, Object>();
        jobMap.put("executionIndex", 1);
        jobMap.put("jobComment", getJobComment());
        jobMap.put("jobConfigurationDetails", getJobConfiguration());
        jobMap.put("templateJobId", jobTemplateId);
        jobMap.putAll(getAbstractJobMap1(state));
        return jobMap;
    }

    private HashMap<String, Object> CreateActivityJobData(final String state, final Long neJobId) {
        final HashMap<String, Object> activityjobMap = new HashMap<String, Object>();
        activityjobMap.put("name", "install");
        activityjobMap.put("order", 1);
        activityjobMap.put("neJobId", neJobId);
        activityjobMap.putAll(getAbstractJobMap1(state));
        return activityjobMap;
    }

    private HashMap<String, Object> createNeJobData(final String state, final Long mainJobId) {
        final HashMap<String, Object> NEjobMap = new HashMap<String, Object>();
        //NEjobMap.put("comment", "NEjobPo");
        NEjobMap.put("neName", "Ne1");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", "222");
        NEjobMap.put("mainJobId", mainJobId);
        NEjobMap.putAll(getAbstractJobMap1(state));
        return NEjobMap;
    }

    public HashMap<String, Object> getAbstractJobMap1(final String state) {
        final HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", state);
        abstractJobMap.put("progressPercentage", 100.00);
        abstractJobMap.put("result", "SUCCESS");
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());
        abstractJobMap.put("jobProperties", Arrays.asList(getJobProperties()));

        final HashMap<String, Object> jobLogMap = new HashMap<String, Object>();
        jobLogMap.put("entryTime", new Date());
        jobLogMap.put("message", "Test");
        jobLogMap.put("type", "NE");
        final List<Map<String, Object>> listOfJobLog = new ArrayList<Map<String, Object>>();
        listOfJobLog.add(jobLogMap);
        abstractJobMap.put("log", listOfJobLog);
        return abstractJobMap;
    }

    /**
     * @param Data
     *            for all other test cases
     */

    public PersistenceObject createJobConfiguration() {
        final PersistenceObject jobTemplate = createPO(getJobTemplateAttributes(), SHM_NAMESPACE, JOBTEMPLATE);
        poIds.put(jobTemplatePoId, jobTemplate.getPoId());
        return jobTemplate;
    }

    public void createAllJobs(final String state) {
        this.state = state;
        createAllJobs();
    }

    public void createAllJobs() {

        createJobConfiguration();
        final PersistenceObject mainJobPo = createPO(getMainJob(), SHM_NAMESPACE, JOB);
        poIds.put(mainJobPoId, mainJobPo.getPoId());
        final PersistenceObject neJobPo = createPO(getNeJobData(), SHM_NAMESPACE, NEJOB);
        poIds.put(neJobPoId, neJobPo.getPoId());
        final PersistenceObject activityJobPo = createPO(getActivityJobData(), SHM_NAMESPACE, ACTIVITYJOB);
        poIds.put(activityJobPoId, activityJobPo.getPoId());
    }

    public long getMainJobId() {
        return poIds.get(mainJobPoId);
    }

    /**
     * @return
     */

    private Map<String, Object> getJobTemplateAttributes() {

        final HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();

        JobTemplateMap.put("jobType", "UPGRADE");
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
        jobMap.put("jobComment", getJobComment());
        jobMap.put("jobConfigurationDetails", getJobConfiguration());
        jobMap.put("templateJobId", poIds.get(jobTemplatePoId));
        jobMap.putAll(getAbstractJobMap());
        return jobMap;
    }

    /**
     * @return
     */
    private List<Map<String, Object>> getJobComment() {
        final List<Map<String, Object>> jobComments = new ArrayList<Map<String, Object>>();
        final HashMap<String, Object> jobComment = new HashMap<String, Object>();
        jobComment.put("userName", "Test");
        jobComment.put("comment", "Test");
        jobComment.put("date", new Date());

        jobComments.add(jobComment);
        return jobComments;
    }

    /**
     * @return
     */
    private HashMap<String, Object> getJobProperties() {
        final HashMap<String, Object> jobPropertiesMap = new HashMap<String, Object>();
        jobPropertiesMap.put("key", "Test");
        jobPropertiesMap.put("value", "Test");
        return jobPropertiesMap;
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
        scheduledpropertymap.put("value", "statdate");
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
        listOfNeNames.add("ne1");
        listOfNeNames.add("ne2");
        NeInfoMap.put("collectionNames", listOfCollectionNames);
        NeInfoMap.put("neNames", listOfNeNames);

        jobConfigurationMap.put("activities", Arrays.asList(getActivity()));
        jobConfigurationMap.put("mainSchedule", getScheduleMap());
        jobConfigurationMap.put("jobProperties", Arrays.asList(getJobProperties()));
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
        NEjobMap.put("neName", "Ne1");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", "222");
        NEjobMap.put("mainJobId", poIds.get(mainJobPoId));
        NEjobMap.putAll(getAbstractJobMap());
        return NEjobMap;
    }

    public HashMap<String, Object> getAbstractJobMap() {
        final HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", state);
        abstractJobMap.put("progressPercentage", 100.00);
        abstractJobMap.put("result", "SUCCESS");
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());
        abstractJobMap.put("jobProperties", Arrays.asList(getJobProperties()));

        final HashMap<String, Object> jobLogMap = new HashMap<String, Object>();
        jobLogMap.put("entryTime", new Date());
        jobLogMap.put("message", "Test");
        jobLogMap.put("type", "NE");
        final List<Map<String, Object>> listOfJobLog = new ArrayList<Map<String, Object>>();
        listOfJobLog.add(jobLogMap);
        abstractJobMap.put("log", listOfJobLog);
        return abstractJobMap;
    }

    public int cleanUpTestData() {

        int deletedPos = 0;
        final List<String> jobModelList = new ArrayList<String>();
        jobModelList.add(JOBTEMPLATE);
        jobModelList.add(JOB);
        jobModelList.add(NEJOB);
        jobModelList.add(ACTIVITYJOB);
        final Iterator<String> jobModelIterator = jobModelList.iterator();

        while (jobModelIterator.hasNext()) {

            final QueryBuilder queryBuilder = dps.getDataPersistenceService().getQueryBuilder();
            final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(SHM_NAMESPACE, jobModelIterator.next());
            final Iterator<PersistenceObject> datbaseEntries = dps.getDataPersistenceService().getLiveBucket().getQueryExecutor().execute(typeQuery);

            while (datbaseEntries.hasNext()) {
                final PersistenceObject po = datbaseEntries.next();
                deletedPos = deletedPos + dps.getDataPersistenceService().getLiveBucket().deletePo(po);
            }
        }

        return deletedPos;
    }

    public int cleanUpTestData(final Long POId) {
        int deletedPos = 0;
        final PersistenceObject persistenceObject = dps.getDataPersistenceService().getLiveBucket().findPoById(POId);
        if (persistenceObject != null) {
            deletedPos = deletedPos + dps.getDataPersistenceService().getLiveBucket().deletePo(persistenceObject);
        }
        return deletedPos;
    }

    public PersistenceObject checkPO() {
        final Long mainJobId = JobsCreatorTestBase.poIds.get("mainJobPoId");
        final PersistenceObject po = dps.getDataPersistenceService().getLiveBucket().findPoById(mainJobId);
        return po;
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

    /**
     * @param state
     * @param poId
     */
    public void createTempJob(String state, long poId) {
        final PersistenceObject pObject = createPO(createMainJob(state, poId), "shm", "Job");
        poIds.put(jobTemplatePoId_1, pObject.getPoId());

    }

}
