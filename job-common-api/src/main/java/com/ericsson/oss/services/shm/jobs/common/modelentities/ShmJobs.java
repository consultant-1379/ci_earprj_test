package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.Map;

public class ShmJobs {
	private Map<Long, JobDetails> jobDetailsMap;

	public Map<Long, JobDetails> getJobDetailsMap() {
		return jobDetailsMap;
	}

	public void setJobDetailsList(final Map<Long, JobDetails> jobDetailsMap) {
		this.jobDetailsMap = jobDetailsMap;
	}

}
