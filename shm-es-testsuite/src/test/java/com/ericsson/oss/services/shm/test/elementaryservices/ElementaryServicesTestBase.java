package com.ericsson.oss.services.shm.test.elementaryservices;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;
import com.ericsson.oss.services.shm.test.common.TestConstants;

/*
 * @author xmanush
 */
@Stateless
public class ElementaryServicesTestBase implements IElementaryServicesTestBase {

    @Inject
    protected DataPersistenceServiceProxy dataPersistenceServiceProxy;

    private static final String LIVE_BUCKET = "Live";

    protected static String jobTemplatePoId = "jobTemplatePoId";
    public static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    public static String activityJobPoId = "activityJobPoId";
    protected static String dataPoId = "poId";
    public static Map<String, Long> poIds = new HashMap<String, Long>();
    private static final Logger logger = LoggerFactory.getLogger(ElementaryServicesTestBase.class);

    @Override
    public PersistenceObject preparePOTestData(final String namespace, final String modelType, final Map<String, Object> attributes) {
        final PersistenceObjectBuilder poBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace(namespace).type(modelType).addAttributes(attributes);
        final PersistenceObject persistenceObject = poBuilder.create();
        poIds.put(dataPoId, persistenceObject.getPoId());
        return persistenceObject;
    }

    @Override
    public ManagedObject prepareBaseMOTestData() throws Throwable {

        final ManagedObject createdMeContextMO = createMeContextMO();

        final ManagedObject networkElementMO = createNetworkElementMO();

        createAssociations(createdMeContextMO, networkElementMO);

        getMeContext();

        final ManagedObject createdManagedElementMO = createManagedElementMO(createdMeContextMO);

        return createdManagedElementMO;
    }

    private ManagedObject createMeContextMO() throws Throwable {

        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(TestConstants.MECONTEXT_ID, "ERBS101");
        attributes.put(TestConstants.NETYPE, "ERBS");
        attributes.put(TestConstants.PLATFORM_TYPE, "CPP");
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_MEDIATION);
        final ManagedObject meContext = bucket.getMibRootBuilder().namespace(TestConstants.OSSTOP_NAMESPACE).type(TestConstants.MECONTEXT_TYPE).version(TestConstants.MECONTEXT_VERSION)
                .name(TestConstants.MECONTEXT_NAME).addAttributes(attributes).create();

        return meContext;
    }

    private ManagedObject createNetworkElementMO() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(TestConstants.NETWORK_ELEMENT_ID, "ERBS101");
        attributes.put(TestConstants.NETYPE, "ERBS");
        attributes.put(TestConstants.PLATFORM_TYPE, "CPP");
        attributes.put(TestConstants.OSS_PREFIX, "ERBS101");
        attributes.put("ossModelIdentity", "1998-184-092");
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_MEDIATION);
        final ManagedObject networkElement = bucket.getMibRootBuilder().namespace(TestConstants.NETWORK_ELEMENT_NAMESPACE).type(TestConstants.NETWORK_ELEMENT)
                .version(TestConstants.NETWORK_ELEMENT_VERSION).name(TestConstants.NETWORK_ELEMENT_NAME).addAttributes(attributes).create();
        System.out.println("****networkElement*****{}" + networkElement);
        return networkElement;
    }

    /**
     * @param meContextMO
     * @param networkElementMO
     */
    private void createAssociations(final ManagedObject meContextMO, final ManagedObject networkElementMO) {
        networkElementMO.addAssociation(TestConstants.NODE_ROOT_REF, meContextMO);
        System.out.println("****BI-DIRECTIONAL Associations created from nodeRootRef_to_networkElementRef*****");
    }

    private void getMeContext() {
        final ManagedObject managedObject = getLiveBucket().findMoByFdn("NetworkElement=ERBS101");
        final Collection<PersistenceObject> meContextAssociated = managedObject.getAssociations(TestConstants.NODE_ROOT_REF);
        if (meContextAssociated.size() == 1) {
            final PersistenceObject po = meContextAssociated.iterator().next();
            System.out.println("Assciated NE POID to meContextAssociated {}" + po.getPoId());
        }
    }

    /**
     * @return
     */
    private DataBucket getLiveBucket() {
        return dataPersistenceServiceProxy.getDataPersistenceService().getLiveBucket();

    }

    private ManagedObject createManagedElementMO(final ManagedObject parentMO) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ManagedElementId", "1");
        attributes.put("userLabel", "Netsim ERBS5");
        attributes.put(TestConstants.NETYPE, "ERBS");
        attributes.put(TestConstants.PLATFORM_TYPE, "CPP");
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_MEDIATION);
        final ManagedObject managedElement = bucket.getMibRootBuilder().namespace(TestConstants.ERBS_NODE_MODEL_NAMESPACE).type(TestConstants.MANAGEDELEMENT_TYPE)
                .version(TestConstants.C14B_SUPPORTED_MIM_VERSION).name(TestConstants.MANAGEDELEMENT_NAME).parent(parentMO).addAttributes(attributes).create();
        return managedElement;

    }

    /**
     * @param createdManagedElementMO
     * @return
     */
    @Override
    public ManagedObject createSystemFunctionMO(final ManagedObject parentMO) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("SystemFunctionsId", "1");
        attributes.put("userLabel", "Netsim ERBS5");
        final ManagedObject systemFunction = getLiveBucket().getMibRootBuilder().namespace(TestConstants.ERBS_NODE_MODEL_NAMESPACE).type(TestConstants.SYSTEM_FUNCTIONS)
                .version(TestConstants.C14B_SUPPORTED_MIM_VERSION).name("1").parent(parentMO).addAttributes(attributes).create();

        return systemFunction;
    }

    @Override
    public ManagedObject createChildMO(final String name, final String modelType, final ManagedObject parent, final String namespace, final String version, final Map<String, Object> attributes) {
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        final ManagedObject createdMO = bucket.getMibRootBuilder().name(name).type(modelType).parent(parent).namespace(namespace).version(version).addAttributes(attributes).create();
        return createdMO;
    }

    @Override
    public void deleteMOTestData() {
        final DataBucket liveBucket = getLiveBucket();
        final ManagedObject networkElementMO = liveBucket.findMoByFdn(TestConstants.NETWORK_ELEMENT_FDN);
        final ManagedObject MeContextMO = liveBucket.findMoByFdn(TestConstants.ME_CONTEXT_FDN);
        if (networkElementMO != null) {
            liveBucket.deletePo(networkElementMO);
        }
        if (MeContextMO != null) {
            liveBucket.deletePo(MeContextMO);
        }
    }

    /**
     * 
     * @param attributes
     * @param namespace
     * @param modelType
     * @return
     */
    private PersistenceObject createPO(final Map<String, Object> attributes, final String namespace, final String modelType) {
        final PersistenceObjectBuilder poBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace(namespace).version("1.0.0").type(modelType).addAttributes(attributes);
        return poBuilder.create();
    }

    /**
     * Creation of Job Details Data.
     */
    @Override
    public void createJobDetails(final JobTypeEnum jobType, final List<Map<String, Object>> jobProperties, final String activityName) {
        final PersistenceObject jobTemplate = createPO(getJobTemplateAttributes(jobType, jobProperties, activityName), "shm", "JobTemplate");
        poIds.put(jobTemplatePoId, jobTemplate.getPoId());
        final PersistenceObject mainJobPo = createPO(getMainJob(jobProperties, activityName), "shm", "Job");
        poIds.put(mainJobPoId, mainJobPo.getPoId());
        final PersistenceObject neJobPo = createPO(getNeJobData(jobProperties), "shm", "NEJob");
        poIds.put(neJobPoId, neJobPo.getPoId());
        final PersistenceObject activityJobPo = createPO(getActivityJobData(jobProperties, activityName), "shm", "ActivityJob");
        poIds.put(activityJobPoId, activityJobPo.getPoId());
    }

    /**
     * @return
     */

    private Map<String, Object> getJobTemplateAttributes(final JobTypeEnum jobType, final List<Map<String, Object>> jobProperties, final String activityName) {
        final HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();
        JobTemplateMap.put("jobType", jobType.name());
        JobTemplateMap.put("name", TestConstants.JOB_NAME);
        JobTemplateMap.put("owner", TestConstants.JOB_OWNER);
        JobTemplateMap.put("description", "Test");
        JobTemplateMap.put("creationTime", new Date());
        JobTemplateMap.put("jobConfigurationDetails", getJobConfiguration(jobProperties, activityName));
        return JobTemplateMap;

    }

    /**
     * @param NeInfoMap
     * @param scheduleMap
     * @param listOfActivities
     * @return
     */
    private HashMap<String, Object> getJobConfiguration(final List<Map<String, Object>> jobProperties, final String activityName) {
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
        jobConfigurationMap.put("activities", Arrays.asList(getActivity(activityName)));
        jobConfigurationMap.put("mainSchedule", getScheduleMap());
        jobConfigurationMap.put("jobProperties", jobProperties);
        jobConfigurationMap.put("selectedNEs", NeInfoMap);
        jobConfigurationMap.put("neJobProperties", getNeJobProperties(jobProperties));
        return jobConfigurationMap;
    }

    /**
     * @return
     */
    private HashMap<String, Object> getActivity(final String activityName) {
        final HashMap<String, Object> activityMap = new HashMap<String, Object>();
        activityMap.put("name", activityName);
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
     * @return
     */
    private List<Map<String, Object>> getNeJobProperties(final List<Map<String, Object>> jobProperties) {
        final Map<String, Object> neJobPropertiesMap = new HashMap<String, Object>();
        neJobPropertiesMap.put("neName", "ERBS101");
        neJobPropertiesMap.put("jobProperties", jobProperties);
        return Arrays.asList(neJobPropertiesMap);
    }

    /**
     * @param jobConfigurationMap
     */
    private HashMap<String, Object> getMainJob(final List<Map<String, Object>> jobProperties, final String activityName) {
        final HashMap<String, Object> jobMap = new HashMap<String, Object>();
        jobMap.put("executionIndex", 1);
        jobMap.put("jobComment", getJobComments());
        jobMap.put("jobConfigurationDetails", getJobConfiguration(jobProperties, activityName));

        jobMap.put("templateJobId", poIds.get(jobTemplatePoId));
        jobMap.putAll(getAbstractJobMap(jobProperties));
        return jobMap;
    }

    private List<Map<String, Object>> getJobComments() {
        final Map<String, Object> jobCommentMap = new HashMap<String, Object>();
        jobCommentMap.put("userName", "administrator");
        jobCommentMap.put("comment", "This is a Job");
        jobCommentMap.put("date", new Date());
        return Arrays.asList(jobCommentMap);
    }

    private HashMap<String, Object> getAbstractJobMap(final List<Map<String, Object>> jobProperties) {
        final HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", "CREATED");
        abstractJobMap.put("progressPercentage", 10.00);
        abstractJobMap.put("result", "SUCCESS");
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());
        abstractJobMap.put("jobProperties", jobProperties);
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
     * 
     */
    private HashMap<String, Object> getNeJobData(final List<Map<String, Object>> jobProperties) {
        final HashMap<String, Object> NEjobMap = new HashMap<String, Object>();
        NEjobMap.put("neName", "ERBS101");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", poIds.get(mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME);
        NEjobMap.put("mainJobId", poIds.get(mainJobPoId));
        NEjobMap.putAll(getAbstractJobMap(jobProperties));
        return NEjobMap;
    }

    /**
     * 
     */
    private HashMap<String, Object> getActivityJobData(final List<Map<String, Object>> jobProperties, final String activityName) {
        final HashMap<String, Object> activityjobMap = new HashMap<String, Object>();
        activityjobMap.put("name", activityName);
        activityjobMap.put("order", 1);
        activityjobMap.put("neJobId", poIds.get(neJobPoId));
        activityjobMap.putAll(getAbstractJobMap(jobProperties));
        return activityjobMap;
    }

    @Override
    public int cleanUpTestPOData() {
        return deletePOs("JobTemplate") + deletePOs("Job") + deletePOs("NEJob") + deletePOs("ActivityJob");
    }

    private int deletePOs(final String type) {
        int deletedPos = 0;
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final QueryBuilder queryBuilder = dataPersistenceServiceProxy.getDataPersistenceService().getQueryBuilder();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery("shm", type);

        final Iterator<PersistenceObject> iterator = queryExecutor.execute(query);
        while (iterator.hasNext()) {
            final PersistenceObject persistenceObject = iterator.next();
            if (persistenceObject != null) {
                deletedPos = deletedPos + getLiveBucket().deletePo(persistenceObject);
            }
        }
        System.out.println(deletedPos + " POs deleted for type.." + type);
        return deletedPos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.elementaryservices. IElementaryServicesTestBase#updateAttributes(java.lang.String, java.util.Map)
     */
    @Override
    public void updateAttributes(final String fdn, final Map<String, Object> changedAttributes) {
        System.out.println("changedAttributes" + changedAttributes);
        final DataBucket rootBucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        final ManagedObject managedObject = rootBucket.findMoByFdn(fdn);
        System.out.println("The managedObject is:{}" + managedObject);
        System.out.println("The fdn is:{}" + managedObject.getFdn());
        managedObject.setAttributes(changedAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.elementaryservices. IElementaryServicesTestBase #createSwManagementMO(com.ericsson.oss.itpf.datalayer.dps. persistence.ManagedObject)
     */
    @Override
    public ManagedObject createSwManagementMO(final ManagedObject parentMO) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(TestConstants.SW_MANAGEMENT_ID, "1");
        attributes.put(TestConstants.USER_LABEL, "Netsim ERBS5");
        final ManagedObject swManagement = getLiveBucket().getMibRootBuilder().namespace(TestConstants.ERBS_NODE_MODEL_NAMESPACE).type(TestConstants.SWMANAGEMENT).version(parentMO.getVersion())
                .name("1").parent(parentMO).addAttributes(attributes).create();
        return swManagement;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.elementaryservices. IElementaryServicesTestBase#getJobResultForActivity(long)
     */
    @Override
    public String getJobResultForActivity(final long activityJobId) throws InterruptedException {
        String jobResult = null;
        final DataBucket rootBucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        for (int retryTime = 1; retryTime <= 20; retryTime++) {
            System.out.println("Retry Count: " + retryTime);
            jobResult = getJobResult(rootBucket, activityJobId);
            System.out.println("Job Result: " + jobResult);
            if (jobResult != null) {
                break;
            } else {
                Thread.sleep(10000);
            }
        }

        return jobResult;
    }

    /**
     * @param activityJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getJobResult(final DataBucket rootBucket, final long activityJobId) {
        String jobResult = null;
        final PersistenceObject activityJobPO = rootBucket.findPoById(activityJobId);
        Map<String, Object> activityJobAttributes = new HashMap<String, Object>();
        activityJobAttributes = activityJobPO.getAllAttributes();
        List<Map<String, String>> activityJobPropertyList = new ArrayList<Map<String, String>>();
        if (activityJobAttributes.get(ShmConstants.JOBPROPERTIES) != null) {
            activityJobPropertyList = (List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES);
        }
        System.out.println("TESTING::activityJobPropertyList " + activityJobPropertyList);
        if (activityJobPropertyList != null && !activityJobPropertyList.isEmpty()) {
            for (final Map<String, String> jobProperty : activityJobPropertyList) {
                if (ActivityConstants.ACTIVITY_RESULT.equals(jobProperty.get(ShmConstants.KEY))) {
                    jobResult = jobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        System.out.println("TESTING::jobResult " + jobResult);
        return jobResult;

    }

    /**
     * 
     */
    @Override
    public void cleanUpgradePackageData() {
        int deletedPos = 0;
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final QueryBuilder queryBuilder = dataPersistenceServiceProxy.getDataPersistenceService().getQueryBuilder();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery("ImportSoftwarePackage", "CppSoftwarePackage");

        final Iterator<PersistenceObject> iterator = queryExecutor.execute(query);
        while (iterator.hasNext()) {
            final PersistenceObject persistenceObject = iterator.next();
            if (persistenceObject != null) {
                deletedPos = deletedPos + getLiveBucket().deletePo(persistenceObject);
            }
        }
        System.out.println(deletedPos + " UP POs deleted ");
    }
}
