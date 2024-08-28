package com.ericsson.oss.shm.upgradepackage.entities;

import java.util.Date;
import java.util.List;

@SuppressWarnings("PMD")
public class UpgradePackagePO {

	private String packageName;
	private String filePath;
	private String importedBy;
	private Date importDate;
	private String description;
	private String hash;
	private String nodePlatform;
	private Long poId;
	private String nodeName;
	private String nodeType;
	private List<UPProductDetailsPO> upProductDetailsList;	

	public UpgradePackagePO(final String packageName, final String filePath,
			final String importedBy, final Date importDate, final String description,
			final String hash, final String nodePlatform, final Long poId, final String nodeName,
			final String nodeType, final List<UPProductDetailsPO> upProductDetailsList) {
		this.packageName = packageName;
		this.filePath = filePath;
		this.importedBy = importedBy;
		this.importDate = importDate;
		this.description = description;
		this.hash = hash;
		this.nodePlatform = nodePlatform;
		this.poId = poId;
		this.nodeName = nodeName;
		this.nodeType = nodeType;
		this.upProductDetailsList = upProductDetailsList;
	}

	
	public UpgradePackagePO()
	{
		
	}
	public String getPackageName() {
		return packageName;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getImportedBy() {
		return importedBy;
	}

	public Date getImportDate() {
		return importDate;
	}

	public String getDescription() {
		return description;
	}

	public String getHash() {
		return hash;
	}

	public String getNodePlatform() {
		return nodePlatform;
	}

	public Long getPoId() {
		return poId;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getNodeType() {
		return nodeType;
	}

	public List<UPProductDetailsPO> getUpProductDetailsList() {
		return upProductDetailsList;
	}
	
	/**
	 * @param packageName the packageName to set
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * @param importedBy the importedBy to set
	 */
	public void setImportedBy(String importedBy) {
		this.importedBy = importedBy;
	}

	/**
	 * @param importDate the importDate to set
	 */
	public void setImportDate(Date importDate) {
		this.importDate = importDate;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param hash the hash to set
	 */
	public void setHash(String hash) {
		this.hash = hash;
	}

	/**
	 * @param nodePlatform the nodePlatform to set
	 */
	public void setNodePlatform(String nodePlatform) {
		this.nodePlatform = nodePlatform;
	}

	/**
	 * @param poId the poId to set
	 */
	public void setPoId(Long poId) {
		this.poId = poId;
	}

	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
	 * @param nodeType the nodeType to set
	 */
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	/**
	 * @param upProductDetailsList the upProductDetailsList to set
	 */
	public void setUpProductDetailsList(
			List<UPProductDetailsPO> upProductDetailsList) {
		this.upProductDetailsList = upProductDetailsList;
	}

}
