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
package com.ericsson.oss.services.shm.test.enmupgrade;

import java.util.*;
import java.util.Map.Entry;

import javax.cache.Cache;
import javax.ejb.*;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.test.common.DataPersistenceServiceProxy;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MockCacheRegister {

    @Inject
    DataPersistenceServiceProxy databean;

    @Inject
    @NamedCache("CppInventorySynchServiceNotificationRegistryCache")
    private Cache<String, NotificationSubject> cache;

    protected static String jobTemplatePoId = "jobTemplatePoId";
    protected static String mainJobPoId = "mainJobPoId";
    protected static String neJobPoId = "neJobPoId";
    protected static String activityJobPoId = "activityJobPoId";

    protected static Map<String, Long> poIds = new HashMap<String, Long>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void registerToCacheAndCreateJobs() {

        logger.info(".....creating Jobs....{}");
        createAllJobs();

        logger.info(".....REGISTERING CACHE.....");
        JobActivityInfo jobActivityInfo = new JobActivityInfo(poIds.get(activityJobPoId), "restore", JobTypeEnum.RESTORE, PlatformTypeEnum.CPP);
        FdnNotificationSubject subject = new FdnNotificationSubject("MeContext=1,NetworkElement=1", poIds.get(activityJobPoId), jobActivityInfo);
        cache.put(subject.getKey(), subject);
        logger.info(".....REGISTERING CACHE DONE...{}..", cache);

    }

    /**
     * 
     */
    private void createAllJobs() {
        final PersistenceObjectBuilder jobTemplatePoBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace("shm").type("JobTemplate").addAttributes(getJobTemplateAttributes());
        if (jobTemplatePoBuilder != null) {
            poIds.put(jobTemplatePoId, jobTemplatePoBuilder.create().getPoId());

        }
        logger.info("...........jobTemplatePoId........." + poIds);
        final PersistenceObjectBuilder mainJobPoBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace("shm").type("Job").addAttributes(getMainJob());
        if (mainJobPoBuilder != null) {
            poIds.put(mainJobPoId, mainJobPoBuilder.create().getPoId());
        }
        logger.info("...........mainJobPoId........." + poIds);
        final PersistenceObjectBuilder neJobPoBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace("shm").type("NEJob").addAttributes(getNeJobData());
        if (neJobPoBuilder != null) {
            poIds.put(neJobPoId, neJobPoBuilder.create().getPoId());
        }
        logger.info("...........neJobPoId........." + poIds);
        final PersistenceObjectBuilder activityjobPoBuilder = getLiveBucket().getPersistenceObjectBuilder().namespace("shm").type("ActivityJob").addAttributes(getActivityJobData());
        if (activityjobPoBuilder != null) {
            poIds.put(activityJobPoId, activityjobPoBuilder.create().getPoId());
        }
        logger.info("...........activityJobPoId........." + poIds);
    }

    /**
     * @return
     */
    private DataBucket getLiveBucket() {
        return databean.getDataPersistenceService().getLiveBucket();
    }

    /**
     * @return
     */

    private Map<String, Object> getJobTemplateAttributes() {

        HashMap<String, Object> JobTemplateMap = new HashMap<String, Object>();

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
        HashMap<String, Object> jobMap = new HashMap<String, Object>();
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
    private HashMap<String, Object> getJobProperties() {
        HashMap<String, Object> jobPropertiesMap = new HashMap<String, Object>();
        jobPropertiesMap.put("Test", "Test");
        jobPropertiesMap.put("Value", "Test");
        return jobPropertiesMap;
    }

    /**
     * @return
     */
    private HashMap<String, Object> getActivity() {
        HashMap<String, Object> activityMap = new HashMap<String, Object>();
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
        HashMap<String, Object> scheduledpropertymap = new HashMap<String, Object>();
        scheduledpropertymap.put("name", "START_DATE");
        scheduledpropertymap.put("value", "statdate");
        List<Map<String, Object>> listOfScheduledproperty = new ArrayList<Map<String, Object>>();
        listOfScheduledproperty.add(scheduledpropertymap);
        HashMap<String, Object> scheduleMap = new HashMap<String, Object>();
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
        HashMap<String, Object> jobConfigurationMap = new HashMap<String, Object>();
        HashMap<String, Object> NeInfoMap = new HashMap<String, Object>();
        List<String> listOfCollectionNames = new ArrayList<String>();
        List<String> listOfNeNames = new ArrayList<String>();
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
        HashMap<String, Object> activityjobMap = new HashMap<String, Object>();
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
        HashMap<String, Object> NEjobMap = new HashMap<String, Object>();
        //NEjobMap.put("comment", "NEjobPo");
        NEjobMap.put("neName", "Ne1");
        NEjobMap.put("wfsId", "111");
        NEjobMap.put("businessKey", "222");
        NEjobMap.put("mainJobId", poIds.get(mainJobPoId));
        NEjobMap.putAll(getAbstractJobMap());
        return NEjobMap;
    }

    public HashMap<String, Object> getAbstractJobMap() {
        HashMap<String, Object> abstractJobMap = new HashMap<String, Object>();
        abstractJobMap.put("state", "CREATED");
        abstractJobMap.put("progressPercentage", 100.00);
        abstractJobMap.put("result", "SUCCESS");
        abstractJobMap.put("startTime", new Date());
        abstractJobMap.put("endTime", new Date());
        abstractJobMap.put("jobProperties", Arrays.asList(getJobProperties()));

        HashMap<String, Object> jobLogMap = new HashMap<String, Object>();
        jobLogMap.put("entryTime", new Date());
        jobLogMap.put("message", "Test");
        jobLogMap.put("type", "NE");
        List<Map<String, Object>> listOfJobLog = new ArrayList<Map<String, Object>>();
        listOfJobLog.add(jobLogMap);
        abstractJobMap.put("log", listOfJobLog);
        return abstractJobMap;
    }

    public void deletePos() {
        for (Entry<String, Long> entry : poIds.entrySet()) {
            logger.info("Deleting a PO for {} with id:{} ", entry.getKey(), entry.getValue());
            PersistenceObject persistenceObject = getLiveBucket().findPoById(entry.getValue());
            if (persistenceObject != null) {
                logger.info("Deleted pos::{}", getLiveBucket().deletePo(persistenceObject));
            }
        }
    }
}
