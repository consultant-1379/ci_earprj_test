///*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2012
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/
//package com.ericsson.oss.services.shm.scheduler;
//
//import java.util.Date;
//
//import com.ericsson.oss.shm.jobs.common.modelentities.Schedule;
//
//public interface SchedulerService {
//    /**
//     * schedules the specified job template using TimerService.
//     * 
//     * @param jobTemplateId
//     *            ,schedule
//     * @param void
//     */
//    void create(long jobTemplateId, Schedule schedule);
//
//    /**
//     * Deletes the timer.
//     * 
//     * @param jobTemplateId
//     * @param void
//     */
//    void deleteTimer(final long jobTemplateId) throws SchedulerException;
//
//    /**
//     * Returns the next Time Out for the Timer
//     * 
//     * @param jobTemplateId
//     * @return Date
//     */
//    Date getNextTimeOutForTimer(final long jobTemplateId);
//}
