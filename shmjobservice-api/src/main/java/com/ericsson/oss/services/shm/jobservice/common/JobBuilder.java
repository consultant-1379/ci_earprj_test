package com.ericsson.oss.services.shm.jobservice.common;

import java.util.Map;

/**
 * This interface helps to build job properties and configuration.
 */
public interface JobBuilder {
    /**
     * This method populates the Job Configuration for a Job.
     * 
     * @param jobInfo
     *            , configurations
     * @return void
     */
    void populateJobConfiguration(JobInfo jobInfo, Map<String, Object> configurations);
}
