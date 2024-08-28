package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.util.List;

public class JobDetails {
	private Long id;
	private List<Job> jobList;
	private JobTemplate jobTemplate;

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public List<Job> getJobList() {
		return jobList;
	}

	public void setJobList(final List<Job> jobList) {
		this.jobList = jobList;
	}

	public JobTemplate getJobTemplate() {
		return jobTemplate;
	}

	public void setJobTemplate(final JobTemplate jobConfiguration) {
		this.jobTemplate = jobConfiguration;
	}
}
