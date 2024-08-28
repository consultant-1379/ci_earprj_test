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
package com.ericsson.oss.shm.upgradepackage.entities;

import java.util.List;

public class SoftwarePackageActivity {
	
	public String name;

	public String startup;

	public String scriptFileName;

	public List<Param> activityParams;

	public String description;	

	public boolean selected;

	
	/**
	 * @param name
	 * @param startup
	 * @param scriptFileName
	 * @param activityParams
	 * @param description
	 * @param selected
	 */
	public SoftwarePackageActivity(final String name,final String startup,final String scriptFileName,
			final List<Param> activityParams, final String description,final boolean selected) {
		super();
		this.name = name;
		this.startup = startup;
		this.scriptFileName = scriptFileName;
		this.activityParams = activityParams;
		this.description = description;
		this.selected = selected;
	}

	/**
	 * 
	 */
	public SoftwarePackageActivity() {
		// TODO Auto-generated constructor stub
	}

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
	 * @return the startup
	 */
	public String getStartup() {
		return startup;
	}

	/**
	 * @param startup the startup to set
	 */
	public void setStartup(final String startup) {
		this.startup = startup;
	}

	/**
	 * @return the scriptFileName
	 */
	public String getScriptFileName() {
		return scriptFileName;
	}

	/**
	 * @param scriptFileName the scriptFileName to set
	 */
	public void setScriptFileName(final String scriptFileName) {
		this.scriptFileName = scriptFileName;
	}

	/**
	 * @return the activityParams
	 */
	public List<Param> getActivityParams() {
		return activityParams;
	}

	/**
	 * @param activityParams the activityParams to set
	 */
	public void setActivityParams(final List<Param> activityParams) {
		this.activityParams = activityParams;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @param selected the selected to set
	 */
	public void setSelected(final boolean selected) {
		this.selected = selected;
	}


	
	

}
