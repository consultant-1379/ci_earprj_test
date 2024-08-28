/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.shared.enums;

public enum JobLogType {
	
	NE("NE"),
	SYSTEM("SYSTEM");
	
	private String logType;

	private JobLogType(final String logType) {
		this.logType = logType;
	}
	
	public String getLogType() {
		return logType;
	}
	
	

}
