package com.ericsson.oss.shm.upgradepackage.entities;

import java.util.Date;
import java.util.List;

@SuppressWarnings("PMD")
public class CppUpgradePackage extends UpgradePackagePO {

	private String nodeFunction;	
	
	private List<Param> jobParameters;
	
	private List<Param> neParams;
	
	private List<SoftwarePackageActivity> activities;

	public CppUpgradePackage(final String packageName, final String filePath,
			final String importedBy, final Date importDate, final String description,
			final String hash, final String nodePlatform, final Long poId, final String nodeName,
			final String nodeType, final List<UPProductDetailsPO> upProductDetailsList,
			final String nodeFunction) {
		super(packageName, filePath, importedBy, importDate, description,
				hash, nodePlatform, poId, nodeName, nodeType,
				upProductDetailsList);
		this.nodeFunction = nodeFunction;
	}
	
	public CppUpgradePackage()	{}
	
	/**
	 * @param nodeFunction the nodeFunction to set
	 */
	public void setNodeFunction(final String nodeFunction) {
		this.nodeFunction = nodeFunction;
	}

	public String getNodeFunction() {
		return nodeFunction;
	}

	/**
	 * @return the jobParameters
	 */
	public List<Param> getJobParameters() {
		return jobParameters;
	}

	/**
	 * @param jobParameters the jobParameters to set
	 */
	public void setJobParameters(final List<Param> jobParameters) {
		this.jobParameters = jobParameters;
	}

	/**
	 * @return the neParams
	 */
	public List<Param> getNeParams() {
		return neParams;
	}

	/**
	 * @param neParams the neParams to set
	 */
	public void setNeParams(final List<Param> neParams) {
		this.neParams = neParams;
	}

	/**
	 * @return the activities
	 */
	public List<SoftwarePackageActivity> getActivities() {
		return activities;
	}

	/**
	 * @param activities the activities to set
	 */
	public void setActivities(final List<SoftwarePackageActivity> activities) {
		this.activities = activities;
	}

}
