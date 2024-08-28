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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.List;

public class JobQuery {

	private List<Long> poIds;
	private List<String> attributes;
	private String name;
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}
	/**
	 * @return the poIds
	 */
	public List<Long> getPoIds() {
		return poIds;
	}
	/**
	 * @param poIds the poIds to set
	 */
	public void setPoIds(final List<Long> poIds) {
		this.poIds = poIds;
	}
	/**
	 * @return the attributes
	 */
	public List<String> getAttributes() {
		return attributes;
	}
	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(final List<String> attributes) {
		this.attributes = attributes;
	}
}
