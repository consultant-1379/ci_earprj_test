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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

public class JobExecutionIndexAndState {

	private int jobExecutionIndex;

	private JobState jobState;

	/**
	 * @return the jobExecutionIndex
	 */
	public int getJobExecutionIndex() {
		return jobExecutionIndex;
	}

	/**
	 * @param jobExecutionIndex
	 *            the jobExecutionIndex to set
	 */
	public void setJobExecutionIndex(final int jobExecutionIndex) {
		this.jobExecutionIndex = jobExecutionIndex;
	}

	/**
	 * @return the jobState
	 */
	public JobState getJobState() {
		return jobState;
	}

	/**
	 * @param jobState
	 *            the jobState to set
	 */
	public void setJobState(final JobState jobState) {
		this.jobState = jobState;
	}
}
