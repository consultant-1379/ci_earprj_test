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

public class UpgradePackage {

	private AdminProductData adminProductData;

	public UpgradePackage() {

	}

	public UpgradePackage(final AdminProductData adminProductData) {
		this.adminProductData = adminProductData;
	}

	/**
	 * @return the adminProductData
	 */
	public AdminProductData getAdminProductData() {
		return adminProductData;
	}

	/**
	 * @param adminProductData
	 *            the adminProductData to set
	 */
	public void setAdminProductData(final AdminProductData adminProductData) {
		this.adminProductData = adminProductData;
	}

}
