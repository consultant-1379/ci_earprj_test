package com.ericsson.oss.services.shm.jobservice.common;

/**
 * This class builds the job based on its platform type.
 * 
 */
public interface BuilderFactory {
    /**
     * This method selects the platform for a Job.
     * 
     * @param platformType
     * @return JobBuilder
     */
    JobBuilder selectJobBuilder(String platformType);
}
