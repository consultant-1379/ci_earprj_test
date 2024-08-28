package com.ericsson.oss.services.shm.scheduler;

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

public class SchedulerException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1475527767351567863L;
    private static final String MESSAGE = "Delete timer failed for templateId= %s";

    private final long jobTemplateId;

    /**
     * @return the jobTemplateId
     */
    public long getJobTemplateId() {
        return jobTemplateId;
    }

    public SchedulerException(final long templateId, final Throwable cause) {
        super(String.format(MESSAGE, templateId), cause);
        this.jobTemplateId = templateId;
    }

}
