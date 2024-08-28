package com.ericsson.oss.shm.upgradepackage.entities;


public class UPProductDetailsPO {

	private String productName;
	private String productNumber;
	private String productRevision;
	private String releaseDate;
	private String type;
	private String description;

	public String getProductName() {
		return productName;
	}

	public void setProductName(final String productName) {
		this.productName = productName;
	}

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(final String productNumber) {
		this.productNumber = productNumber;
	}

	public String getProductRevision() {
		return productRevision;
	}

	public void setProductRevision(final String productRevision) {
		this.productRevision = productRevision;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(final String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

}
