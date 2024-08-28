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

public class AdminProductData {

	private String productInfo;
	private String productionDate;
	private String productName;
	private String productNumber;
	private String productRevision;

	public AdminProductData() {

	}

	public AdminProductData(final String productInfo,
			final String productionDate, final String productName,
			final String productNumber, final String productRevision) {
		this.productInfo = productInfo;
		this.productionDate = productionDate;
		this.productName = productName;
		this.productNumber = productNumber;
		this.productRevision = productRevision;
	}

	/**
	 * @return the productInfo
	 */
	public String getProductInfo() {
		return productInfo;
	}

	/**
	 * @return the productionDate
	 */
	public String getProductionDate() {
		return productionDate;
	}

	/**
	 * @return the productName
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * @return the productNumber
	 */
	public String getProductNumber() {
		return productNumber;
	}

	/**
	 * @return the productRevision
	 */
	public String getProductRevision() {
		return productRevision;
	}

    @Override
    public String toString() {
        return String.format("Product Name : %s, Product Number : %s, Product Revision : %s,  Production Date : %s, Product Information : %s", productName, productNumber, productRevision,
                productionDate, productInfo);
    }

}
