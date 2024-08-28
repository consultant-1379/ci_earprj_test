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
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * @deprecated
 */
@Deprecated
public class ShmJobsComparator {

    private static int performStringComparision(final String object1, final String object2, final boolean isAscendingOrder) {
        if (isAscendingOrder) {
            return DataTypeComparator.performStringComparision(object1, object2);
        } else {
            return DataTypeComparator.performStringComparision(object2, object1);
        }

    }

    private static int performDoubleComparision(final Double object1, final Double object2, final boolean isAscendingOrder) {
        if (isAscendingOrder) {
            return DataTypeComparator.performDoubleComparision(object1, object2);
        } else {
            return DataTypeComparator.performDoubleComparision(object2, object1);
        }

    }

    private static int performIntegerComparision(final Integer object1, final Integer object2, final boolean isAscendingOrder) {
        if (isAscendingOrder) {
            return DataTypeComparator.performIntegerComparision(object1, object2);
        } else {
            return DataTypeComparator.performIntegerComparision(object2, object1);
        }

    }

    public static class JobNameComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public JobNameComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performStringComparision(object1.getJobName(), object2.getJobName(), isAscending);
        }

    }

    public static class jobTypeComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobTypeComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performStringComparision(object1.getJobType(), object2.getJobType(), isAscending);
        }

    }

    public static class jobCreatedByComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobCreatedByComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performStringComparision(object1.getCreatedBy(), object2.getCreatedBy(), isAscending);
        }
    }

    public static class jobProgressComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobProgressComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performDoubleComparision(object1.getProgress(), object2.getProgress(), isAscending);
        }
    }

    public static class jobStatusComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobStatusComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performStringComparision(object1.getStatus(), object2.getStatus(), isAscending);
        }
    }

    public static class jobResultComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobResultComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performStringComparision(object1.getResult(), object2.getResult(), isAscending);
        }
    }

    public static class jobStartDateComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobStartDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            final String formattedDate1 = DateTimeUtils.getStringDateFromLongValue(object1.getStartDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            final String formattedDate2 = DateTimeUtils.getStringDateFromLongValue(object1.getStartDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            final Date FormattedDate1 = DateTimeUtils.getDateFromStringValue(formattedDate1);
            final Date FormattedDate2 = DateTimeUtils.getDateFromStringValue(formattedDate2);
            return DataTypeComparator.performDateComparision(FormattedDate1, FormattedDate2, isAscending);
        }
    }

    public static class jobEndDateComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobEndDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            final String formattedDate1 = DateTimeUtils.getStringDateFromLongValue(object1.getEndDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            final String formattedDate2 = DateTimeUtils.getStringDateFromLongValue(object1.getEndDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            final Date FormattedDate1 = DateTimeUtils.getDateFromStringValue(formattedDate1);
            final Date FormattedDate2 = DateTimeUtils.getDateFromStringValue(formattedDate2);
            return DataTypeComparator.performDateComparision(FormattedDate1, FormattedDate2, isAscending);
        }
    }

    public static class jobTotalNoOfNEsComparator implements Comparator<SHMJobData> {

        final private boolean isAscending;

        public jobTotalNoOfNEsComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMJobData object1, final SHMJobData object2) {
            return performIntegerComparision(Integer.valueOf(object1.getTotalNoOfNEs()), Integer.valueOf(object2.getTotalNoOfNEs()), isAscending);
        }
    }
}
