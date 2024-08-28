package com.ericsson.oss.services.shm.test.webpush;

import java.util.*;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;
import com.ericsson.oss.services.shm.test.common.TestConstants;

/*
 * @author xcharoh
 */
@Stateless
public class ShmWebPushTestBase implements IShmWebPushTestBase {

    @Inject
    protected DataPersistenceServiceProxy dataPersistenceServiceProxy;

    protected static String jobTemplatePoId = "jobTemplatePoId";
    public static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    public static String activityJobPoId = "activityJobPoId";
    protected static String dataPoId = "poId";
    public static Map<String, Long> poIds = new HashMap<String, Long>();

    /**
     * @return
     */
    private DataBucket getLiveBucket() {
        return dataPersistenceServiceProxy.getDataPersistenceService().getLiveBucket();
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
    public void createJobDetails() {
        System.out.println("Creating Template Job");
        final PersistenceObject jobTemplatePo = createPO(getJobTemplateAttributes(), TestConstants.SHM_NAMESPACE, TestConstants.TEMPLATE_JOB);
        System.out.println("Template Job created with ID" + jobTemplatePo.getPoId());
        poIds.put(jobTemplatePoId, jobTemplatePo.getPoId());
        System.out.println("Creating Main Job");
        final PersistenceObject mainJobPo = createPO(getMainJobAttributes(), TestConstants.SHM_NAMESPACE, TestConstants.JOB);
        System.out.println("Main Job created with ID" + mainJobPo.getPoId());
        poIds.put(mainJobPoId, mainJobPo.getPoId());
        System.out.println("Creating NE Job");
        final PersistenceObject neJobPo = createPO(getNeJobAttributes(), TestConstants.SHM_NAMESPACE, TestConstants.NE_JOB);
        System.out.println("NE Job created with ID" + neJobPo.getPoId());
        poIds.put(neJobPoId, neJobPo.getPoId());
        System.out.println("Creating Activity Job");
        final PersistenceObject activityJobPo = createPO(getActivityJobAttributes(), TestConstants.SHM_NAMESPACE, TestConstants.ACTIVITY_JOB);
        System.out.println("Activity Job created with ID" + activityJobPo.getPoId());
        poIds.put(activityJobPoId, activityJobPo.getPoId());
    }

    /**
     * @return
     */

    private Map<String, Object> getJobTemplateAttributes() {
        final HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();
        JobTemplateMap.put("jobType", "UPGRADE");
        JobTemplateMap.put("name", TestConstants.JOB_NAME);
        JobTemplateMap.put("owner", TestConstants.JOB_OWNER);
        JobTemplateMap.put("description", "Test");
        JobTemplateMap.put("creationTime", new Date());
        return JobTemplateMap;

    }

    /**
     * @param jobConfigurationMap
     */
    private HashMap<String, Object> getMainJobAttributes() {
        final HashMap<String, Object> jobMap = new HashMap<String, Object>();
        jobMap.put("executionIndex", 1);
        jobMap.put("templateJobId", poIds.get(jobTemplatePoId));
        jobMap.putAll(getAbstractJobMap());
        return jobMap;
    }

    private HashMap<String, Object> getAbstractJobMap() {
        final HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", "RUNNING");
        abstractJobMap.put("progressPercentage", 10.00);
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());

        return abstractJobMap;
    }

    /**
     * 
     */
    private HashMap<String, Object> getNeJobAttributes() {
        final HashMap<String, Object> NEjobMap = new HashMap<String, Object>();
        NEjobMap.put("neName", "ERBS101");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", poIds.get(mainJobPoId) + "@" + TestConstants.NETWORK_ELEMENT_NAME);
        NEjobMap.put("mainJobId", poIds.get(mainJobPoId));
        NEjobMap.putAll(getAbstractJobMap());
        return NEjobMap;
    }

    /**
     * 
     */
    private HashMap<String, Object> getActivityJobAttributes() {
        final HashMap<String, Object> activityjobMap = new HashMap<String, Object>();
        activityjobMap.put("name", "install");
        activityjobMap.put("order", 1);
        activityjobMap.put("neJobId", poIds.get(neJobPoId));
        activityjobMap.putAll(getAbstractJobMap());
        return activityjobMap;
    }

    @Override
    public int deleteJobDetails() {
        int deletedPos = 0;
        for (final Entry<String, Long> entry : poIds.entrySet()) {
            final PersistenceObject persistenceObject = getLiveBucket().findPoById(entry.getValue());
            if (persistenceObject != null) {
                deletedPos = deletedPos + getLiveBucket().deletePo(persistenceObject);
            }
        }
        return deletedPos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.webpush.IShmWebPushTestBase#updateJob(long, java.util.Map)
     */
    @Override
    public PersistenceObject updateJob(final long jobId, final Map<String, Object> attributes) {
        final PersistenceObject jobPo = getLiveBucket().findPoById(jobId);
        if (jobPo != null) {
            jobPo.setAttributes(attributes);
        } else {
            System.out.println("Job PO doesn't exist with poId" + jobId);
        }
      return jobPo;
    }

}
