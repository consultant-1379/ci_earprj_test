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
package com.ericsson.oss.shm.inventory.backup.entities;

import java.util.List;

public class NodeConfigurationVersion {

	private String nodeName;
	private String nodeType;
	private String fdn;
	private List<NodeCVAttributes> nodeCVAttributesList;
	private NodeCVStatusInfo nodeCVStatusInfo;

	public NodeConfigurationVersion() {
	}

	public NodeConfigurationVersion(final String nodeName,
			final String nodeType, final String fdn,
			final List<NodeCVAttributes> nodeCVAttributesList,
			final NodeCVStatusInfo nodeCVStatusInfo) {
		this.nodeName = nodeName;
		this.nodeType = nodeType;
		this.fdn = fdn;
		this.nodeCVAttributesList = nodeCVAttributesList;
		this.nodeCVStatusInfo = nodeCVStatusInfo;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getNodeType() {
		return nodeType;
	}

	public String getFdn() {
		return fdn;
	}

	public List<NodeCVAttributes> getNodeCVAttributesList() {
		return nodeCVAttributesList;
	}

	public NodeCVStatusInfo getNodeCVStatusInfo() {
		return nodeCVStatusInfo;
	}

}
