/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service

import static org.junit.Assert.*

import java.text.DateFormat
import java.text.SimpleDateFormat

import javax.inject.Inject

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.FilterDetails
import com.ericsson.oss.services.shm.job.entities.JobInput
import com.ericsson.oss.services.shm.job.entities.ShmMainJobsResponse
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobCategory;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJob


class SHMJobServiceImplForJobsTest extends CdiSpecification {

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps);

    protected long jobTemplatePoId;

    @ObjectUnderTest
    private SHMJobServiceImpl objectUnderTest;

    @MockedImplementation
    private ActiveSessionsController activeRequestsController;

    @Inject
    private SystemRecorder systemRecorder;

    private static final String DATE_WITH_TIME_FORMAT_FOR_FILTER = "yyyy-MM-dd HH:mm:ss";

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addProxyController(new EjbProxyController(true))
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
    }

    def "Read SHM Job Data from Main Page View - error cases"() {
        given: "SHM Job data"
        JobInput request = new JobInput()
        request.setOffset(1)
        if (sortByColumn != null) {
            request.setSortBy(sortByColumn)
        }

        when: "Retrieving SHM Job data for main view"
        String message;
        try {
            objectUnderTest.getShmMainJobs(request)
        } catch (Exception e) {
            message = e.getMessage()
        }

        then: "throws the exceptions"
        expectedMessage == message
        1 * systemRecorder.recordEvent(_, _, _, _, _);

        where:
        sortByColumn | expectedMessage
        null         | "sortBy \"null\" is not a valid column"
        "startTime"  | null
        "date"       | "sortBy \"date\" is not a valid column"
    }

    def "Read SHM Job Data from main view - no data"() {
        given: "There are no jobs in SHM main page"
        JobInput request = new JobInput()
        request.setSortBy("startTime")
        request.setOffset(1)

        when: "Retrieving SHM Job data for main view"
        ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)
        then: "returns empty response"
        null != response
        0 == response.getTotalCount()
        [] == response.getResult()
        1 * systemRecorder.recordEvent(_, _, _, _, _);
    }

    def "Read SHM Job data from SHM UI main view - sort all columns order by descending as default "() {
        given: "SHM Job data"
        def request = new JobInput()
        request.setSortBy(sortBy)
        request.setOffset(1)
        request.setLimit(50)
        request.setOrderBy("desc")

        buildJobData()
        when: "Retrieving SHM Job data for main view"

        ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)

        then: "check the descending sort order of Job data"
        null != response
        4 == response.getTotalCount()
        List<SHMMainJob> responseList = response.getResult()
        expectedOrder == Arrays.asList(responseList.get(0).getJobName(), responseList.get(1).getJobName(), responseList.get(2).getJobName(), responseList.get(3).getJobName())
        1 * systemRecorder.recordEvent(_, _, _, _, _);

        where:
        sortBy         | expectedOrder
        "jobName"      | ["BackupJob3", "BackupJob2", "BackupJob1", "BackupJob0"]
        "endTime"      | ["BackupJob0", "BackupJob2", "BackupJob1", "BackupJob3"]
        "progress"     | ["BackupJob1", "BackupJob0", "BackupJob2", "BackupJob3"]
        "totalNoOfNEs" | ["BackupJob0", "BackupJob2", "BackupJob1", "BackupJob3"]
        "status"       | ["BackupJob3", "BackupJob2", "BackupJob0", "BackupJob1"]
    }

    def "Read SHM Job data from SHM UI main view - sort all columns order by ascending as default "() {
        given: "SHM Job data"
        def request = new JobInput()
        request.setSortBy(sortBy)
        request.setOffset(1)
        request.setLimit(50)
        request.setOrderBy("asc")

        buildJobData()
        when: "Retrieving SHM Job data for main view"

        ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)

        then: "check the descending sort order of Job data"
        null != response
        4 == response.getTotalCount()
        List<SHMMainJob> responseList = response.getResult()
        expectedOrder == Arrays.asList(responseList.get(0).getJobName(), responseList.get(1).getJobName(), responseList.get(2).getJobName(), responseList.get(3).getJobName())
        1 * systemRecorder.recordEvent(_, _, _, _, _);

        where:
        sortBy         | expectedOrder
        "jobName"      | ["BackupJob0", "BackupJob1", "BackupJob2", "BackupJob3"]
        "endTime"      | ["BackupJob3", "BackupJob1", "BackupJob2", "BackupJob0"]
        "progress"     | ["BackupJob3", "BackupJob2", "BackupJob0", "BackupJob1"]
        "totalNoOfNEs" | ["BackupJob3", "BackupJob1", "BackupJob2", "BackupJob0"]
        "status"       | ["BackupJob1", "BackupJob0", "BackupJob2", "BackupJob3"]
    }

    /*def "Read Job Data for SHM UI main view - filter all columns"() {
     given:"SHM Job Data"
     def request = new JobInput()
     request.setSortBy("startTime")
     request.setOffset(1)
     request.setLimit(50)
     request.setOrderBy("desc")
     request.setFilterDetails(Arrays.asList(generateFilterDetails(column,filterText,filterOperator)));
     buildJobData()
     when: "Retrieving main job data"
     ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)
     then:"click the filtered main job Data are retrieved"
     null != response
     List names = new ArrayList<>();
     for(SHMMainJob mainJob:response.getResult()) {
     names.add(mainJob.getJobName())
     }
     totalRecords == response.getTotalCount()
     expectedRecords == names
     where:
     column               |filterText                   |filterOperator  |totalRecords  |expectedRecords
     "name"               |"#"                          |"*"             |0             |[]
     "name"               |"1"                          |"*ab"           |1             |["BackupJob1"]
     "name"               |"BackupJob0"                 |"="             |1             |["BackupJob0"]
     "name"               |"BackupJob0"                 |"!="            |3             |["BackupJob2", "BackupJob3", "BackupJob1"]
     "startTime"          |getStringDate(160000000000l) |">"             |1              |["BackupJob2"]
     "startTime"          |"2012"                       |"*"             |0              |[]
     "endTime"            |"2011"                       |"*"             |0              |[]
     "endTime"            |getStringDate(160000000000l) |">"             |2              |["BackupJob2", "BackupJob0"]
     "endTime"            |getStringDate(180000000000l) |"<"             |2              |["BackupJob3", "BackupJob1"]
     "progressPercentage" |"3"                          |"*"             |0              |[]
     "progressPercentage" |"0"                          |">"             |3              |["BackupJob2", "BackupJob0", "BackupJob1"]
     "progressPercentage" |"50"                         |"<"             |2              |["BackupJob2", "BackupJob3"]
     "progressPercentage" |"100"                        |"="             |1              |["BackupJob1"]
     "progressPercentage" |"50"                         |"!="            |3              |["BackupJob2", "BackupJob3", "BackupJob1"]
     "status"             |"com"                        |"*"             |1              |["BackupJob0"]
     "status"             |"&"                          |"*"             |0              |[]
     "status"             |"TeD"                        |"*ab"           |1              |["BackupJob0"]
     "status"             |"RUNNING"                    |"ab*"           |1              |["BackupJob2"]
     "status"             |"rUnnINg"                    |"="             |1              |["BackupJob2"]
     "result"             |"^"                          |"*"             |0              |[]
     "result"             |"skipped"                    |"="             |0              |[]
     "result"             |"Success"                    |"!="            |3              |["BackupJob2", "BackupJob3", "BackupJob1"]
     "totalNodes"         |"50"                         |"*"             |0              |[]
     "totalNodes"         |"0"                          |">"             |4              |["BackupJob2", "BackupJob0", "BackupJob3", "BackupJob1"]
     "totalNodes"         |"50"                         |"<"             |4              |["BackupJob2", "BackupJob0", "BackupJob3", "BackupJob1"]
     "totalNodes"         |"0"                          |"="             |0              |[]
     "totalNodes"         |"10"                         |"!="            |3              |["BackupJob2", "BackupJob3", "BackupJob1"]
     }*/

    def "Read SHM Job data from SHM UI main view - sort all columns order by descending as default with Pagination"() {
        given: "SHM Job data"
        def request = new JobInput()
        request.setSortBy("jobName")
        request.setOffset(1)
        request.setLimit(limit)
        request.setOrderBy("desc")

        buildJobData()
        when: "Retrieving SHM Job data for main view"

        ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)

        then: "check the descending sort order of Job data with Pagination"
        null != response
        List names = new ArrayList<>();
        List<SHMMainJob> responseList = response.getResult()
        for (SHMMainJob mainJob : response.getResult()) {
            names.add(mainJob.getJobName())
        }
        expectedRecords == names
        1 * systemRecorder.recordEvent(_, _, _, _, _);

        where:
        limit | totalRecords | expectedRecords
        2     | 2            | ["BackupJob3", "BackupJob2"]
        3     | 3            | ["BackupJob3", "BackupJob2", "BackupJob1"]
    }

    def "Read SHM Job data from SHM UI main view - check pagination"() {
        given: "SHM Job data"
        def request = new JobInput()
        request.setSortBy("jobName")
        request.setOffset(offset)
        request.setLimit(limit)
        request.setOrderBy("asc")

        buildJobData()
        when: "Retrieving SHM Job data for main view with pagination"

        ShmMainJobsResponse response = objectUnderTest.getShmMainJobs(request)

        then: "check the Job data with pagination"
        null != response
        List jobNames = new ArrayList<>();
        List<SHMMainJob> responseList = response.getResult()
        for (SHMMainJob mainJob : response.getResult()) {
            jobNames.add(mainJob.getJobName())
        }
        expectedJobNames == jobNames
        clearOffset == response.isClearOffset();
        expectedFilteredRecords == responseList.size();
        expectedTotalJobs == response.getTotalCount();
        1 * systemRecorder.recordEvent(_, _, _, _, _);

        where:
        offset | limit | expectedTotalJobs | expectedFilteredRecords | expectedJobNames                           | clearOffset
        1      | 2     | 4                 | 2                       | ["BackupJob0", "BackupJob1"]               | false
        1      | 3     | 4                 | 3                       | ["BackupJob0", "BackupJob1", "BackupJob2"] | false
        2      | 2     | 4                 | 2                       | ["BackupJob1", "BackupJob2"]               | false
        2      | 10    | 4                 | 3                       | ["BackupJob1", "BackupJob2", "BackupJob3"] | false
        5      | 2     | 4                 | 2                       | ["BackupJob0", "BackupJob1"]               | true
    }

    def buildJobData() {

        buildJobData0()
        buildJobData1()
        buildJobData2()
        buildJobData3()
    }

    def buildJobData0() {
        long jobTemplateId = setTemplateData("BackupJob0", new Date(140000000000l));
        setMainJobData(50d, 10, new Date(160000000000l), new Date(190000000000l), "COMPLETED", "SUCCESS", jobTemplateId)
    }

    def buildJobData1() {
        long jobTemplateId = setTemplateData("BackupJob1", new Date(140000000000l));
        setMainJobData(100d, 5, new Date(150000000000l), new Date(160000000000l), "CANCELLED", "FAILED", jobTemplateId)
    }

    def buildJobData2() {
        long jobTemplateId = setTemplateData("BackupJob2", new Date(140000000000l));
        setMainJobData(10d, 8, new Date(170000000000l), new Date(180000000000l), "RUNNING", "", jobTemplateId)
    }

    def buildJobData3() {
        long jobTemplateId = setTemplateData("BackupJob3", new Date(160000000000l));
        setMainJobData(0d, 1, null, null, "WAIT_FOR_USER_INPUT", "", jobTemplateId)
    }

    def Map setMainJobData(double progress, int nes, Date start, Date end, String status, String result, long jobTemplateId) {
        Map mainJobData = new HashMap();
        mainJobData.put(ShmConstants.PO_ID, 123l)
        mainJobData.put(ShmConstants.JOBTEMPLATEID, jobTemplateId)
        mainJobData.put(ShmConstants.PROGRESSPERCENTAGE, progress)
        mainJobData.put(ShmConstants.NO_OF_NETWORK_ELEMENTS, nes)
        mainJobData.put(ShmConstants.STARTTIME, start);
        mainJobData.put(ShmConstants.ENDTIME, end);
        mainJobData.put(ShmConstants.STATE, status);
        mainJobData.put(ShmConstants.RESULT, result);
        runtimeDps.addPersistenceObject().namespace("shm").type("Job").addAttributes(mainJobData).build();
        return mainJobData;
    }

    def long setTemplateData(String name, Date creation) {
        Map templateData = new HashMap();
        templateData.put(ShmConstants.NAME, name);
        templateData.put(ShmConstants.CREATION_TIME, creation);
        templateData.put(ShmConstants.JOB_CATEGORY, JobCategory.UI.toString())
        PersistenceObject po = runtimeDps.addPersistenceObject().namespace("shm").type("JobTemplate").addAttributes(templateData).build();
        long poId = po.getPoId();
        return poId;
    }

    def generateFilterDetails(column, filterText, filterOperator) {
        FilterDetails filterDetails = new FilterDetails();
        filterDetails.setFilterText(filterText);
        filterDetails.setColumnName(column);
        filterDetails.setFilterOperator(filterOperator);
        return filterDetails;
    }

    def String getStringDate(long date) {
        final DateFormat dateFormat = new SimpleDateFormat(DATE_WITH_TIME_FORMAT_FOR_FILTER)
        return dateFormat.format(new Date(date));
    }
}
