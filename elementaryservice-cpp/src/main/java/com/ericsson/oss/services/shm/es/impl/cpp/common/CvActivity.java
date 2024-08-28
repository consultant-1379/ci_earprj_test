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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentDetailedActivity;
import com.ericsson.oss.services.shm.es.impl.cpp.backup.CVCurrentMainActivity;

/**
 * CvActivity POJO that holds both mainActrivity and detailedActivity
 * 
 * @author xchedoo
 * 
 */

public class CvActivity {
    final private CVCurrentMainActivity mainActivity;
    final private CVCurrentDetailedActivity detailedActivity;

    public CvActivity(final CVCurrentMainActivity mainActivity, final CVCurrentDetailedActivity detailedActivity) {
        this.mainActivity = mainActivity == null ? CVCurrentMainActivity.UNKNOWN : mainActivity;
        this.detailedActivity = detailedActivity == null ? CVCurrentDetailedActivity.UNKNOWN : detailedActivity;
    }

    /**
     * @return the mainActivity
     */
    public CVCurrentMainActivity getMainActivity() {
        return mainActivity;
    }

    /**
     * @return the mainActivityDescription
     */
    public String getMainActivityDesc() {
        return mainActivity.getActivityMessage();
    }

    /**
     * @return the detailedActivity
     */
    public CVCurrentDetailedActivity getDetailedActivity() {
        return detailedActivity;
    }

    /**
     * @return the detailedActivityDescription
     */
    public String getDetailedActivityDesc() {
        return detailedActivity.getActivityMessage();
    }

    public String toString() {
        return "[mainActivity=" + mainActivity.getActivityMessage() + ", detailedActivity=" + detailedActivity.getActivityMessage() + "]";
    }

}
