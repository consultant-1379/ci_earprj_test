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

import java.util.Map;

public class WorkFlowObject {

	private String workflowDefinitionId;

	private Map<String, Object> processVariables;

	/**
	 * @return the workflowDefinitionId
	 */
	public String getWorkflowDefinitionId() {
		return workflowDefinitionId;
	}

	/**
	 * @param workflowDefinitionId
	 *            the workflowDefinitionId to set
	 */
	public void setWorkflowDefinitionId(final String workflowDefinitionId) {
		this.workflowDefinitionId = workflowDefinitionId;
	}

	/**
	 * @return the processVariables
	 */
	public Map<String, Object> getProcessVariables() {
		return processVariables;
	}

	/**
	 * @param processVariables
	 *            the processVariables to set
	 */
	public void setProcessVariables(final Map<String, Object> processVariables) {
		this.processVariables = processVariables;
	}

}
