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
package com.ericsson.oss.services.shm.job.utils;

import java.util.Comparator;
import java.util.Date;

import com.ericsson.oss.services.shm.common.DataTypeComparator;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;

public class ShmJobLogsComparator {

    private static int performStringComparision(final String object1, final String object2, final boolean isAscendingOrder) {
        if (isAscendingOrder) {
            return DataTypeComparator.performStringComparision(object1, object2);
        } else {
            return DataTypeComparator.performStringComparision(object2, object1);
        }

    }

    public static class NeNameComparator implements Comparator<JobLogResponse> {

        final private boolean isAscending;

        public NeNameComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final JobLogResponse object1, final JobLogResponse object2) {
            return performStringComparision(object1.getNeName(), object2.getNeName(), isAscending);
        }

    }

    public static class ActivityNameComparator implements Comparator<JobLogResponse> {

        final private boolean isAscending;

        public ActivityNameComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final JobLogResponse object1, final JobLogResponse object2) {
            return performStringComparision(object1.getActivityName(), object2.getActivityName(), isAscending);
        }

    }

    public static class MessageComparator implements Comparator<JobLogResponse> {

        final private boolean isAscending;

        public MessageComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final JobLogResponse object1, final JobLogResponse object2) {
            return performStringComparision(object1.getMessage(), object2.getMessage(), isAscending);
        }

    }

    public static class EntryTimeComparator implements Comparator<JobLogResponse> {

        final private boolean isAscending;

        public EntryTimeComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final JobLogResponse object1, final JobLogResponse object2) {
            final String formattedDate1 = DateTimeUtils.getStringDateFromLongValue(object1.getEntryTime(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            final String formattedDate2 = DateTimeUtils.getStringDateFromLongValue(object2.getEntryTime(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);

            final Date FormattedDate1 = DateTimeUtils.getDateFromStringValue(formattedDate1);
            final Date FormattedDate2 = DateTimeUtils.getDateFromStringValue(formattedDate2);
            return DataTypeComparator.performDateComparision(FormattedDate1, FormattedDate2, isAscending);
        }

    }

    public static class NodeTypeComparator implements Comparator<JobLogResponse> {

        final private boolean isAscending;

        public NodeTypeComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final JobLogResponse object1, final JobLogResponse object2) {
            return performStringComparision(object1.getNodeType(), object2.getNodeType(), isAscending);
        }

    }

}
