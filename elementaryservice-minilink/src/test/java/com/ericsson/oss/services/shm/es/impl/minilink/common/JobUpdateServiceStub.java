package com.ericsson.oss.services.shm.es.impl.minilink.common;

import com.ericsson.oss.services.shm.es.api.JobUpdateService;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.JOB_PROP_KEY;
import static com.ericsson.oss.services.shm.shared.constants.ActivityConstants.JOB_PROP_VALUE;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;

public class JobUpdateServiceStub {

    private List<Map<String, Object>> jobLog = new ArrayList<>();
    private Map<String, Object> jobProperties = new HashMap<>();

    public List<Map<String, Object>> getJobLog() {
        return jobLog;
    }

    public Map<String, Object> getJobProperties() {
        return jobProperties;
    }

    public void mockJobUpdateService(JobUpdateService jobUpdateService) {
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] invocationArgs = invocationOnMock.getArguments();
                List<Map<String, Object>> jobPropertyList = (List<Map<String, Object>>) invocationArgs[1];
                List<Map<String, Object>> jobLogList = (List<Map<String, Object>>) invocationArgs[2];
                jobLog.addAll(jobLogList);

                for (Map<String, Object> prop: jobPropertyList) {
                    jobProperties.put((String) prop.get(JOB_PROP_KEY), prop.get(JOB_PROP_VALUE));
                }

                return null;
            }
        }).when(jobUpdateService).readAndUpdateRunningJobAttributes(anyLong(), anyList(), anyList());
    }
}
