/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.impl

import static org.junit.Assert.assertEquals

import java.util.HashMap;
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.shm.job.remote.api.ActivitySchedule
import com.ericsson.oss.services.shm.job.remote.api.ActivitySchedulesValue
import com.ericsson.oss.services.shm.job.remote.api.JobCreationRequest
import com.ericsson.oss.services.shm.job.remote.api.ScheduledTaskValue
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService

public class ShmJobRemoteServiceImplGroovyTest extends CdiSpecification {

   @ObjectUnderTest
    private ShmJobRemoteServiceImpl objectUnderTest;

    @MockedImplementation
    TopologyEvaluationService topologyEvaluationService;

    private final String collectionId = "123456789";
    private final String savedSearchId = "12345678912";
    private final String jobOwner = "User";
    private final String collectionName = "Collection_1"
    private final String savedSearchName = "Saved_Search_1"
    private final String category_public = "PUBLIC"
    private final String category_private = "PRIVATE"

    def "Get collection details should return collection details when collectionid is given" () {
        given: "Prepare a collection with valid data and return to the method getCollectionDetails"

        CollectionDetails collectionDetails = prepareCollectionDetails();
        topologyEvaluationService.getCollectionDetails(collectionId, jobOwner) >> collectionDetails;

        when: "call get collection details method"
        collectionDetails = objectUnderTest.getCollectionDetails(collectionId, jobOwner);

        then: "Response status is given as OK"
        if (collectionDetails.getCollectionId().equalsIgnoreCase(collectionId)) {
            assertEquals(collectionDetails.getName(), collectionName);
            assertEquals(collectionDetails.getCategory(),category_public);
        }
    }

    def "Get saved search details should return saved search details when savedSearchId is given" () {
        given: "Prepare a SavedSearchDetails with valid data and return to the method getSavedSearchDetails"

        SavedSearchDetails savedSearchDetails = prepareSavedSearchDetails();
        topologyEvaluationService.getSavedSearchDetails(savedSearchId, jobOwner) >> savedSearchDetails;

        when: "call get collection details method"
        savedSearchDetails = objectUnderTest.getSavedSearchDetails(savedSearchId, jobOwner);

        then: "Response status is given as OK"
        if (savedSearchDetails.getSavedSearchId().equalsIgnoreCase(savedSearchId)) {
            assertEquals(savedSearchDetails.getName(), savedSearchName);
            assertEquals(savedSearchDetails.getCategory(),category_private);
        }
    }

    def "Converting Activity schedules" () {
        given: "Prepare a JobCreationRequest"
        JobCreationRequest jobCreationRequest=new JobCreationRequest();
        ActivitySchedule activityScheduleEcim=new ActivitySchedule();
        ActivitySchedule activityScheduleCpp=new ActivitySchedule();
        activityScheduleEcim.setPlatformType("ECIM");
        activityScheduleCpp.setPlatformType("CPP");
        ActivitySchedulesValue activityEcim=new ActivitySchedulesValue();
        ActivitySchedulesValue activityCpp=new ActivitySchedulesValue();
        activityEcim.setNeType("RadioNode");
        activityCpp.setNeType("ERBS");
        ScheduledTaskValue scheduledTaskValueEcim= new ScheduledTaskValue();
        ScheduledTaskValue scheduledTaskValueCpp= new ScheduledTaskValue();
        scheduledTaskValueEcim.setActivityName("EnmHealthCheck");
        scheduledTaskValueCpp.setActivityName("EnmHealthCheck");
        activityEcim.setValue(scheduledTaskValueEcim);
        activityCpp.setValue(scheduledTaskValueCpp);
        ActivitySchedulesValue[] dataEcim=[activityEcim];
        activityScheduleEcim.setValue(dataEcim)
        ActivitySchedulesValue[] dataCpp=[activityCpp];
        activityScheduleCpp.setValue(dataCpp)
        ActivitySchedule[] activityScheduleData=[activityScheduleEcim,activityScheduleCpp];
        jobCreationRequest.setActivitySchedules(activityScheduleData);

        when: "jobCreation request with activity schedules"
        List<Map<String, Object>> response= objectUnderTest.convertActivitySchedules(jobCreationRequest);

        then: "Response status is given as OK"
        HashMap<String, Object> activiyScheduleEcim = (HashMap)(response.get(0));
        HashMap<String, Object> neScheduleEcim = activiyScheduleEcim.get(ShmConstants.VALUE);
        HashMap<String, Object> scheduleEcim = neScheduleEcim.get(ShmConstants.VALUE);
        HashMap<String, Object> activiyScheduleCpp = (HashMap)(response.get(1));
        HashMap<String, Object> neScheduleCpp = activiyScheduleCpp.get(ShmConstants.VALUE);
        HashMap<String, Object> scheduleCpp = neScheduleCpp.get(ShmConstants.VALUE);

        assert(activiyScheduleEcim.get(ShmConstants.PLATFORMTYPE)=="ECIM")
        assert(neScheduleEcim.get(ShmConstants.NETYPE)=="RadioNode")
        assert(scheduleEcim.get(ShmConstants.ACTIVITYNAME)=="EnmHealthCheck")
        assert(activiyScheduleCpp.get(ShmConstants.PLATFORMTYPE)=="CPP")
        assert(neScheduleCpp.get(ShmConstants.NETYPE)=="ERBS")
        assert(scheduleCpp.get(ShmConstants.ACTIVITYNAME)=="EnmHealthCheck")
    }

    def JobCreationRequest createRequest() {
        JobCreationRequest jobCreationRequest=new JobCreationRequest();
        jobCreationRequest.setActivitySchedules(activityScheduleData)
    }

    CollectionDetails prepareCollectionDetails(){
        CollectionDetails collectionDetails = new CollectionDetails();
        collectionDetails.setCollectionId(collectionId);
        collectionDetails.setName(collectionName);
        collectionDetails.setCategory(category_public);
        final Set<String> managedObjectsIDs = new HashSet<>();
        managedObjectsIDs.add("12345");
        managedObjectsIDs.add("456123");
        collectionDetails.setManagedObjectsIDs(managedObjectsIDs);
        return collectionDetails
    }

    SavedSearchDetails prepareSavedSearchDetails(){
        SavedSearchDetails savedSearchDetails = new SavedSearchDetails();
        savedSearchDetails.setSavedSearchId(savedSearchId);
        savedSearchDetails.setName(savedSearchName);
        savedSearchDetails.setQuery("NetworkElement where name = LTE01");
        savedSearchDetails.setCategory(category_private);
        return savedSearchDetails;
    }
}