package com.ericsson.oss.services.shm.test.availability;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.*;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;
import com.ericsson.oss.services.shm.test.common.TestConstants;

/*
 * @author xmanush
 */
@Stateless
public class ShmServiceAvailabilityTestBase {

    @Inject
    protected DataPersistenceServiceProxy dataPersistenceServiceProxy;

    private static final String LIVE_BUCKET = "Live";

    protected static String jobTemplatePoId = "jobTemplatePoId";
    public static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    public static String activityJobPoId = "activityJobPoId";
    protected static String dataPoId = "poId";
    public static Map<String, Long> poIds = new HashMap<String, Long>();

    private final String ERBS101 = "ERBS101";
    private final String ERBS102 = "ERBS102";

    private final List<String> nodeCollection = Arrays.asList(ERBS101, ERBS102);

    public PersistenceObject preparePOTestData(final String namespace, final String modelType, final Map<String, Object> attributes) {
        final PersistenceObjectBuilder poBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace(namespace).type(modelType).addAttributes(attributes);
        final PersistenceObject persistenceObject = poBuilder.create();
        poIds.put(dataPoId, persistenceObject.getPoId());
        return persistenceObject;
    }

    public List<ManagedObject> prepareBaseMOTestData() throws Throwable {
        List<ManagedObject> createdManagedElementMOList = new ArrayList<ManagedObject>();
        for (String nodeName : nodeCollection) {
            final ManagedObject createdMeContextMO = createMeContextMO(nodeName);
            final ManagedObject networkElementMO = createNetworkElementMO(nodeName);
            createAssociations(createdMeContextMO, networkElementMO);
            createdManagedElementMOList.add(createManagedElementMO(createdMeContextMO));
        }
        //getMeContext();
        return createdManagedElementMOList;
    }

    private ManagedObject createMeContextMO(final String meContextName) throws Throwable {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(TestConstants.MECONTEXT_ID, "1");
        attributes.put(TestConstants.NETYPE, "ERBS");
        attributes.put(TestConstants.PLATFORM_TYPE, "CPP");
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_MEDIATION);
        final ManagedObject meContext = bucket.getMibRootBuilder().namespace(TestConstants.OSSTOP_NAMESPACE).type(TestConstants.MECONTEXT_TYPE).version(TestConstants.MECONTEXT_VERSION)
                .name(meContextName).addAttributes(attributes).create();
        return meContext;
    }

    private ManagedObject createNetworkElementMO(final String neName) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(TestConstants.NETWORK_ELEMENT_ID, "1");
        attributes.put(TestConstants.NETYPE, "ERBS");
        attributes.put(TestConstants.PLATFORM_TYPE, "CPP");
        attributes.put(TestConstants.OSS_PREFIX, neName);
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_MEDIATION);
        final ManagedObject networkElement = bucket.getMibRootBuilder().namespace(TestConstants.NETWORK_ELEMENT_NAMESPACE).type(TestConstants.NETWORK_ELEMENT)
                .version(TestConstants.NETWORK_ELEMENT_VERSION).name(neName).addAttributes(attributes).create();
        System.out.println("****networkElement*****{}" + networkElement);
        return networkElement;
    }

    /**
     * @param meContextMO
     * @param networkElementMO
     */
    private void createAssociations(final ManagedObject meContextMO, final ManagedObject networkElementMO) {
        System.out.println("****Inside createAssociations*****");
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
        DataPersistenceService dataPersistenceService = dataPersistenceServiceProxy.getDataPersistenceService();
        return dataPersistenceService.getLiveBucket();
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

    public ManagedObject createSystemFunctionMO(final ManagedObject parentMO) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("SystemFunctionsId", "1");
        attributes.put("userLabel", "Netsim ERBS5");
        final ManagedObject systemFunction = getLiveBucket().getMibRootBuilder().namespace(TestConstants.ERBS_NODE_MODEL_NAMESPACE).type(TestConstants.SYSTEM_FUNCTIONS)
                .version(TestConstants.C14B_SUPPORTED_MIM_VERSION).name("1").parent(parentMO).addAttributes(attributes).create();
        return systemFunction;
    }

    public ManagedObject createChildMO(final String name, final String modelType, final ManagedObject parent, final String namespace, final String version, final Map<String, Object> attributes) {
        final DataBucket bucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        final ManagedObject createdMO = bucket.getMibRootBuilder().name(name).type(modelType).parent(parent).namespace(namespace).version(version).addAttributes(attributes).create();
        return createdMO;
    }

    public void deleteNodeMOTestData() {
        final DataBucket liveBucket = getLiveBucket();
        for (String nodeName : nodeCollection) {
            final ManagedObject networkElementMO = liveBucket.findMoByFdn(TestConstants.NETWORK_ELEMENT + "=" + nodeName);
            final ManagedObject MeContextMO = liveBucket.findMoByFdn(TestConstants.MECONTEXT_TYPE + "=" + nodeName);
            if (networkElementMO != null) {
                liveBucket.deletePo(networkElementMO);
            }
            if (MeContextMO != null) {
                liveBucket.deletePo(MeContextMO);
            }
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
        listOfNeNames.add(ERBS101);
        listOfNeNames.add(ERBS102);
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
        neJobPropertiesMap.put("neName", ERBS101);
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
        abstractJobMap.put("startTime", new Date());
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
        NEjobMap.put("neName", ERBS101);
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", poIds.get(mainJobPoId) + "@" + ERBS101);
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

    public int cleanUpTestJobsData() {
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
     * @see com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase#updateAttributes(java.lang.String, java.util.Map)
     */

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
     * @see com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase#createSwManagementMO(com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject)
     */

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
     * @see com.ericsson.oss.services.shm.test.elementaryservices.IElementaryServicesTestBase#getJobResultForActivity(long)
     */

    public Object getJobAttribute(final long jobId, final String propertyName, final boolean retryRequired) throws InterruptedException {
        Object jobResult = null;
        final DataBucket rootBucket = dataPersistenceServiceProxy.getDataPersistenceService().getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        for (int retryTime = 1; retryTime <= 20; retryTime++) {
            System.out.println("Retry Count: " + retryTime);
            Thread.sleep(10000);
            jobResult = getJobResult(rootBucket, jobId, propertyName);
            System.out.println(propertyName + ": " + jobResult);
            if (!retryRequired || jobResult != null) {
                break;
            }

        }

        return jobResult;
    }

    /**
     * @param activityJobId
     * @return
     */
    private Object getJobResult(final DataBucket rootBucket, final long activityJobId, final String propertyName) {
        final PersistenceObject jobPO = rootBucket.findPoById(activityJobId);
        Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes = jobPO.getAllAttributes();
        return jobAttributes.get(propertyName);

    }

    private String licensingMOFdn;

    /**
     * @return the licensingMOFdn
     */
    public String getLicensingMOFdn() {
        return licensingMOFdn;
    }

    public void prepareLicenseJobData(final String activityName) {
        final List<Map<String, Object>> jobProperties = getJobProperties();
        createJobDetails(JobTypeEnum.LICENSE, jobProperties, activityName);
    }

    public PersistenceObject prepareLicensePOTestData() {
        final Map<String, Object> licenseAttributes = new HashMap<String, Object>();
        licenseAttributes.put("fingerPrint", "ERBSREF1");
        licenseAttributes.put("sequenceNumber", "1001");
        licenseAttributes.put("installedOn", new Date());
        licenseAttributes.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, "/home/smrs/lran/licence/erbs/ERBS1001_141021_131437.xml");
        final PersistenceObject licenseDataPO = preparePOTestData(LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE, LicensingActivityConstants.LICENSE_DATA_PO, licenseAttributes);
        return licenseDataPO;
    }

    public void cretaeLicensingMOAndBaseMoTestData() throws Throwable {
        List<ManagedObject> prepareBaseMOTestData = prepareBaseMOTestData();
        final ManagedObject createdSystemFunctionMO = createSystemFunctionMO(prepareBaseMOTestData.get(0));
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
        final ManagedObject licensingMO = createChildMO("1", LicensingActivityConstants.LICENSE_MO, parent, TestConstants.ERBS_NODE_MODEL_NAMESPACE, parent.getVersion(), licensingMOAttributesMap);
        return licensingMO;
    }

}
