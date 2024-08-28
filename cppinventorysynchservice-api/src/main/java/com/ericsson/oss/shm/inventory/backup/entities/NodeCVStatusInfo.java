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

public class NodeCVStatusInfo {

	private UpgradePackage executingUpPkg;
	private String userLabel;
	private String currentUpgradePackage;
	private List<String> rollbackList;
	private boolean isAutoCreationCVEnabled;
	private boolean isConfirmationAwaited;

	/**
	 * @param userLabel
	 *            the userLabel to set
	 */
	public void setUserLabel(final String userLabel) {
		this.userLabel = userLabel;
	}

	/**
	 * @param currentUpgradePackage
	 *            the currentUpgradePackage to set
	 */
	public void setCurrentUpgradePackage(final String currentUpgradePackage) {
		this.currentUpgradePackage = currentUpgradePackage;
	}

	/**
	 * @param isAutoCreationCVEnabled
	 *            the isAutoCreationCVEnabled to set
	 */
	public void setAutoCreationCVEnabled(final boolean isAutoCreationCVEnabled) {
		this.isAutoCreationCVEnabled = isAutoCreationCVEnabled;
	}

	/**
	 * @param isConfirmationAwaited
	 *            the isConfirmationAwaited to set
	 */
	public void setConfirmationAwaited(final boolean isConfirmationAwaited) {
		this.isConfirmationAwaited = isConfirmationAwaited;
	}

	/**
	 * @return the executingUpPkg
	 */
	public UpgradePackage getExecutingUpPkg() {
		return executingUpPkg;
	}

	/**
	 * @param executingUpPkg
	 *            the executingUpPkg to set
	 */
	public void setExecutingUpPkg(final UpgradePackage executingUpPkg) {
		this.executingUpPkg = executingUpPkg;
	}

	/**
	 * @return the userLabel
	 */
	public String getUserLabel() {
		return userLabel;
	}

	/**
	 * @return the isAutoCreationCVEnabled
	 */
	public boolean isAutoCreationCVEnabled() {
		return isAutoCreationCVEnabled;
	}

	/**
	 * @return the isConfirmationAwaited
	 */
	public boolean isConfirmationAwaited() {
		return isConfirmationAwaited;
	}

	/**
	 * @return the currentUpgradePackage
	 */
	public String getCurrentUpgradePackage() {
		return currentUpgradePackage;
	}

	/**
	 * @return the rollbackList
	 */
	public List<String> getRollbackList() {
		return rollbackList;
	}

	/**
	 * @param rollbackList
	 *            the rollbackList to set
	 * @return
	 */
	public List<String> setRollbackList(final List<String> rollbackList) {
		return this.rollbackList = rollbackList;
	}

}
