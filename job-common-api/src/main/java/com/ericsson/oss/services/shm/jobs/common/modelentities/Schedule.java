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

import java.util.List;

public class Schedule {

	private ExecMode execMode;
	private List<ScheduleProperty> scheduleAttributes;
	/**
	 * @return the execMode
	 */
	public ExecMode getExecMode() {
		return execMode;
	}
	/**
	 * @param execMode the execMode to set
	 */
	public void setExecMode(final ExecMode execMode) {
		this.execMode = execMode;
	}
	/**
	 * @return the scheduleAttributes
	 */
	public List<ScheduleProperty> getScheduleAttributes() {
		return scheduleAttributes;
	}
	/**
	 * @param scheduleAttributes the scheduleAttributes to set
	 */
	public void setScheduleAttributes(final List<ScheduleProperty> scheduleAttributes) {
		this.scheduleAttributes = scheduleAttributes;
	} 
}
