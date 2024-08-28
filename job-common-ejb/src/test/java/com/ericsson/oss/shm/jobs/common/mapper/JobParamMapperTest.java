package com.ericsson.oss.shm.jobs.common.mapper;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.shm.jobs.common.mapper.JobParamMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobProperty;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobParam;

@RunWith(MockitoJUnitRunner.class)
public class JobParamMapperTest {

    @InjectMocks
    JobParamMapper objectTobeTested;
    @Mock
    JobProperty jobPropertyMock;

    @Mock
    ActivityInfo activityInfo;

    @Test
    public void test_createJobparam_withNoData() {
        JobParam jobparam = objectTobeTested.createJobparam(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Assert.assertNotNull(jobparam);
        Assert.assertNull(jobparam.getActivityInfoList());
        Assert.assertNull(jobparam.getJobParameterAttributes());
    }

    @Test
    public void test_createJobparam() {
        when(jobPropertyMock.getKey()).thenReturn("CV_NAME");
        JobParam jobparam = objectTobeTested.createJobparam(Arrays.asList(jobPropertyMock), Arrays.asList(activityInfo));
        Assert.assertNotNull(jobparam);
        Assert.assertNotNull(jobparam.getActivityInfoList());
        Assert.assertNotNull(jobparam.getJobParameterAttributes());
        Assert.assertEquals(1, jobparam.getJobParameterAttributes().size());
    }

    @Test
    public void test_createJobparamWithActicityInfoListNull() {
        when(jobPropertyMock.getKey()).thenReturn("CV_NAME");
        JobParam jobparam = objectTobeTested.createJobparam(Arrays.asList(jobPropertyMock), Collections.EMPTY_LIST);
        Assert.assertNotNull(jobparam);
        Assert.assertNotNull(jobparam.getJobParameterAttributes());
        Assert.assertEquals(1, jobparam.getJobParameterAttributes().size());
        Assert.assertEquals(0, jobparam.getActivityInfoList().size());
    }
}