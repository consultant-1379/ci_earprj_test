package com.ericsson.oss.shm.jobs.common.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.mapper.JobLogMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;
import com.ericsson.oss.services.shm.jobs.common.modelentities.LogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.MainJobLogDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeJobLogDetails;

@RunWith(MockitoJUnitRunner.class)
public class JobLogMapperTest {

    @InjectMocks
    JobLogMapper objectTobeTested;

    @Test
    public void testGetNEJobLogResponseWithError() {
        final NeJobLogDetails neJobLogDetails = new NeJobLogDetails();
        neJobLogDetails.setError("JobFailed.");
        final List<JobLogResponse> jobLogResponseList = objectTobeTested.getNEJobLogResponse(neJobLogDetails);
        Assert.assertNotNull(jobLogResponseList);
        Assert.assertTrue(jobLogResponseList.size() == 1);
    }

    @Test
    public void testGetNEJobLogResponse() {
        final NeJobLogDetails neJobLogDetails = new NeJobLogDetails();
        neJobLogDetails.setNeJobName("First Job");
        neJobLogDetails.setJobLogDetails(preparejobLogDetails());
        final List<JobLogResponse> jobLogResponseList = objectTobeTested.getNEJobLogResponse(neJobLogDetails);
        Assert.assertNotNull(jobLogResponseList);
        Assert.assertTrue(jobLogResponseList.size() == 1);
    }

    @Test
    public void testGetMainJobLogResponse() {
        final MainJobLogDetails mainJobLogDetails = new MainJobLogDetails();
        mainJobLogDetails.setJobLogDetails(preparejobLogDetails());
        final List<JobLogResponse> jobLogResponseList = objectTobeTested.getMainJobLogResponse(mainJobLogDetails);
        Assert.assertNotNull(jobLogResponseList);
        Assert.assertTrue(jobLogResponseList.size() == 1);
    }

    @Test
    public void testMapJobAttributesToJobLogDetailsWithJobNameAsNull() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<HashMap<String, Object>> logDetails = new ArrayList<HashMap<String, Object>>();
        final HashMap<String, Object> logDetail = new HashMap<String, Object>();
        logDetail.put("entryTime", new Date());
        logDetail.put("message", "Notification Not Received.");
        logDetail.put("logLevel", "INFO");
        logDetails.add(logDetail);
        jobAttributes.put("log", logDetails);
        final JobLogDetails jobLogDetails = objectTobeTested.mapJobAttributesToJobLogDetails(jobAttributes);
        Assert.assertNotNull(jobLogDetails);
        Assert.assertTrue(jobLogDetails.getActivityLogs().size() == 1);
        Assert.assertTrue(("").equals(jobLogDetails.getActivityName()));
    }

    @Test
    public void testMapJobAttributesToJobLogDetailsWithJobName() {
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        final List<HashMap<String, Object>> logDetails = new ArrayList<HashMap<String, Object>>();
        final HashMap<String, Object> logDetail = new HashMap<String, Object>();
        logDetail.put("entryTime", new Date());
        logDetail.put("message", "Notification Not Received.");
        logDetail.put("logLevel", "INFO");
        logDetails.add(logDetail);
        jobAttributes.put("name", "Job1");
        jobAttributes.put("log", logDetails);
        final JobLogDetails jobLogDetails = objectTobeTested.mapJobAttributesToJobLogDetails(jobAttributes);
        Assert.assertNotNull(jobLogDetails);
        Assert.assertTrue(jobLogDetails.getActivityLogs().size() == 1);
        Assert.assertTrue(("Job1").equals(jobLogDetails.getActivityName()));
    }

    @Test
    public void testMapNEJobLogDetailsFromJobLogDetails() {
        final NeJobLogDetails neJobLogDetails = objectTobeTested.mapNEJobLogDetailsFromJobLogDetails(new ArrayList<JobLogDetails>(), "LTE01", "ERBS");
        Assert.assertNotNull(neJobLogDetails);
        Assert.assertTrue(("LTE01").equals(neJobLogDetails.getNeJobName()));
    }

    @Test
    public void testMapMainJobLogDetailsFromJobLogDetails() {
        final MainJobLogDetails mainJobLogDetails = objectTobeTested.mapMainJobLogDetailsFromJobLogDetails(new ArrayList<JobLogDetails>());
        Assert.assertNotNull(mainJobLogDetails);
    }

    private final List<JobLogDetails> preparejobLogDetails() {
        final List<LogDetails> logDetails = new ArrayList<LogDetails>();
        final LogDetails logDetail = new LogDetails();
        logDetail.setEntryTime("2016-11-01 10:45:00");
        logDetail.setLogLevel("INFO");
        logDetail.setMessage("Upgrade Successful.");
        logDetails.add(logDetail);
        final List<JobLogDetails> jobLogDetails = new ArrayList<JobLogDetails>();
        final JobLogDetails jobLogDetail = new JobLogDetails();
        jobLogDetail.setActivityName("Upgrade");
        jobLogDetail.setActivityLogs(logDetails);
        jobLogDetails.add(jobLogDetail);
        return jobLogDetails;
    }
}