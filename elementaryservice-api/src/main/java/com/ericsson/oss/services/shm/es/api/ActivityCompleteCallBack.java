package com.ericsson.oss.services.shm.es.api;

import javax.ejb.Local;

/**
 * Elementary services that require processing actionResult( in fact any attribute other than what we listening for notifications ) to evaluate the job status( success or failure) should implement
 * this interface.
 * 
 * @author tcsravg
 * 
 */

@Local
public interface ActivityCompleteCallBack {

    /**
     * This method is called when the action invoked on the node is completed. This method holds the business logic for evaluating the job result based on other data structures on the node.
     * 
     * @param activityJobId
     */
    void onActionComplete(long activityJobId);

}
