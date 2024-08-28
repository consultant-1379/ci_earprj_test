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
package com.ericsson.oss.services.shm.test.webpush;

import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

/*
 * @author xmanush
 */
public interface IShmWebPushTestBase {

    public int deleteJobDetails();

    public void createJobDetails();

    public PersistenceObject updateJob(final long jobId, final Map<String, Object> attributes);

}
