/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification.ShmElisLicenseRefreshNotificationObserver
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification

import spock.lang.Unroll

class ShmElisLicenseRefreshNotificationObserverSpec extends RequestServiceActivityDataProviderSpec{

    @ObjectUnderTest
    ShmElisLicenseRefreshNotificationObserver shmElisLicenseRefreshNotificationObserver

    @Unroll
    def 'ShmElisLicenseRefreshNotification is received from ELIS and is processed for #status status and #state state'(){

        given: 'prepare Elis notification'
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, null)
        registry.getListener(_) >> fdnNotificationSubject
        activityServiceProvider.getActivityNotificationHandler(_,_,_) >> activityImpl

        when: 'notification is received and is processed'
        shmElisLicenseRefreshNotificationObserver.onMessage(elisNotification)

        then: 'assert notification received from ELIS'
        assert elisNotification!=null
        assert elisNotification.getFingerPrint().equals(radioNodeFingerPrint)
        assert elisNotification.getNeJobId().equals(String.valueOf(neJobId))
        assert elisNotification.getState().equals(state)
        assert elisNotification.getStatus().equals(status)

        where:
        status          |  state
        "SUCCESS"       |  "IMPORT_INITIATED"
        "SUCCESS"       |  "IMPORT_COMPLETED"
    }

    def 'ShmElisLicenseRefreshNotification is not received from ELI Sand Exception is thrown'(){
        given: 'prepare Elis notification'
        ShmElisLicenseRefreshNotification elisNotification = new ShmElisLicenseRefreshNotification()
        registry.getListener(_) >> { throw new Exception("Invalid Notfication received from Elis.") }

        when: 'Elis notification is received and is processed'
        shmElisLicenseRefreshNotificationObserver.onMessage(elisNotification)

        then: 'verify notification received from Elis'
        assert elisNotification!=null
    }

    @Unroll
    def 'ShmElisLicenseRefreshNotification is received from ELIS and Exception is thrown during processing'(){
        given: 'prepare Elis notification'
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, null)
        registry.getListener(_) >> fdnNotificationSubject
        activityServiceProvider.getActivityNotificationHandler(_,_,_) >> { throw new Exception("Error occured during Notification process.") }

        when: 'Elis notification is received and is processed'
        shmElisLicenseRefreshNotificationObserver.onMessage(elisNotification)

        then: 'verify notification received from Elis'
        assert elisNotification!=null
        assert elisNotification.getFingerPrint().equals(radioNodeFingerPrint)
        assert elisNotification.getNeJobId().equals(String.valueOf(neJobId))
        assert elisNotification.getState().equals(state)
        assert elisNotification.getStatus().equals(status)

        where:
        status          |  state
        "FAILED"        |  "LKF_REQUEST_FAILED"
        "FAILED"        |  "PARSING_COMPLETED"
        "FAILED"        |  "INVALID_FILES_FILTER_COMPLETED"
    }

    @Unroll
    def 'ShmElisLicenseRefreshNotification is received and Notification subject is null'(){

        given: 'prepare Elis notification and pass NotificationSubject as Null'
        ShmElisLicenseRefreshNotification elisNotification = prepareShmElisLicenseRefreshNotification(state, status, null)
        registry.getListener(_) >> null

        when: 'Elis notification is received and is processed'
        shmElisLicenseRefreshNotificationObserver.onMessage(elisNotification)

        then: 'verify notification received from Elis'
        assert elisNotification!=null
        assert elisNotification.getFingerPrint().equals(radioNodeFingerPrint)
        assert elisNotification.getNeJobId().equals(String.valueOf(neJobId))
        assert elisNotification.getState().equals(state)
        assert elisNotification.getStatus().equals(status)

        where:
        status          |  state
        "FAILED"        |  "LKF_REQUEST_FAILED"
        "FAILED"        |  "PARSING_COMPLETED"
        "FAILED"        |  "INVALID_FILES_FILTER_COMPLETED"
    }
}
