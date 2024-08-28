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

public class NodeCVAttributes {

	private String name;
	private String identity;
	private String type;
	private String upgradePackageId;
	private String operatorName;
	private String operatorComment;
	private String date;
	private String status;
	private int rollbackPosition;
	private List<Boolean> isStartableList;
	private List<Boolean> isLastCreatedList;
	private List<Boolean> isExecutingList;
	private List<Boolean> isLoadedList;

	public NodeCVAttributes() {
	}

	public NodeCVAttributes(final String name, final String identity,
			final String type, final String upgradePackageId,
			final String operatorName, final String operatorComment,
			final String date, final String status) {
		this.name = name;
		this.type = type;
		this.identity = identity;
		this.upgradePackageId = upgradePackageId;
		this.operatorName = operatorName;
		this.operatorName = operatorName;
		this.operatorComment = operatorComment;
		this.date = date;
		this.status = status;

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the identity
	 */
	public String getIdentity() {
		return identity;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the upgradePackageId
	 */
	public String getUpgradePackageId() {
		return upgradePackageId;
	}

	/**
	 * @return the operatorName
	 */
	public String getOperatorName() {
		return operatorName;
	}

	/**
	 * @return the operatorComment
	 */
	public String getOperatorComment() {
		return operatorComment;
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the rollbackPosition
	 */
	public int getRollbackPosition() {
		return rollbackPosition;
	}

	/**
	 * @param rollbackPosition
	 *            the rollbackPosition to set
	 */
	public void setRollbackPosition(final int rollbackPosition) {
		this.rollbackPosition = rollbackPosition;
	}

	/**
	 * @return the isStartable
	 */

	public List<Boolean> isStartableList() {
		return isStartableList;
	}

	/**
	 * @param isStartable
	 *            the isStartable to set
	 * @return
	 */

	public List<Boolean> setStartableList(final List<Boolean> isStartableList) {
		return this.isStartableList = isStartableList;
	}

	/**
	 * @return the isLastCreated
	 */
	public List<Boolean> isLastCreatedList() {
		return isLastCreatedList;
	}

	/**
	 * @param isLastCreated
	 *            the isLastCreated to set
	 * @return
	 */
	public List<Boolean> setLastCreatedList(
			final List<Boolean> isLastCreatedList) {
		return this.isLastCreatedList = isLastCreatedList;
	}

	/**
	 * @return the isExecuting
	 */
	public List<Boolean> isExecutingList() {
		return isExecutingList;
	}

	/**
	 * @param isExecuting
	 *            the isExecuting to set
	 * @return
	 */
	public List<Boolean> setExecutingList(final List<Boolean> isExecutingList) {
		return this.isExecutingList = isExecutingList;
	}

	/**
	 * @return the isLoaded
	 */
	public List<Boolean> isLoadedList() {
		return isLoadedList;
	}

	/**
	 * @param isLoaded
	 *            the isLoaded to set
	 * @return
	 */
	public List<Boolean> setLoadedList(final List<Boolean> isLoadedList) {
		return this.isLoadedList = isLoadedList;
	}

}
