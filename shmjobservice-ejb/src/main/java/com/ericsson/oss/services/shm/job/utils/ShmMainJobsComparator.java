/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.utils;

import java.util.Comparator;
import java.util.Date;

import com.ericsson.oss.services.shm.common.DataTypeComparator;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJobDto;

public class ShmMainJobsComparator {

    private ShmMainJobsComparator() {

    }

    private static int performStringComparision(final String object1, final String object2, final boolean isAscendingOrder) {
        if (isAscendingOrder) {
            return DataTypeComparator.performStringComparision(object1, object2);
        } else {
            return DataTypeComparator.performStringComparision(object2, object1);
        }

    }

    public static class NameComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public NameComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            return performStringComparision(object1.getJobName(), object2.getJobName(), isAscending);
        }

    }

    public static class TypeComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public TypeComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            return performStringComparision(object1.getJobType(), object2.getJobType(), isAscending);
        }

    }

    public static class CreatedByComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public CreatedByComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            return performStringComparision(object1.getCreatedBy(), object2.getCreatedBy(), isAscending);
        }
    }

    public static class ProgressComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public ProgressComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            if (isAscending) {
                return DataTypeComparator.performDoubleComparision(object1.getProgress(), object2.getProgress());
            } else {
                return DataTypeComparator.performDoubleComparision(object2.getProgress(), object1.getProgress());
            }
        }
    }

    public static class StatusComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public StatusComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            return performStringComparision(object1.getStatus(), object2.getStatus(), isAscending);
        }
    }

    public static class ResultComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public ResultComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            return performStringComparision(object1.getResult(), object2.getResult(), isAscending);
        }
    }

    public static class StartDateComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public StartDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            final Date date1 = (object1.getStartTime() == null || object1.getStartTime().equals(new Date(0))) ? object1.getCreationTime() : object1.getStartTime();
            final Date date2 = (object2.getStartTime() == null || object2.getStartTime().equals(new Date(0))) ? object2.getCreationTime() : object2.getStartTime();
            return isAscending ? DataTypeComparator.performDateComparision(date1, date2, isAscending) : DataTypeComparator.performDateComparision(date2, date1, isAscending);
        }
    }

    public static class EndDateComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public EndDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            final Date date1 = object1.getEndTime();
            final Date date2 = object2.getEndTime();
            return isAscending ? DataTypeComparator.performDateComparision(date1, date2, isAscending) : DataTypeComparator.performDateComparision(date2, date1, isAscending);
        }
    }

    public static class TotalNoOfNEsComparator implements Comparator<SHMMainJobDto> {

        private final boolean isAscending;

        public TotalNoOfNEsComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final SHMMainJobDto object1, final SHMMainJobDto object2) {
            if (isAscending) {
                return DataTypeComparator.performIntegerComparision(Integer.valueOf(object1.getTotalNoOfNEs()), Integer.valueOf(object2.getTotalNoOfNEs()));
            } else {
                return DataTypeComparator.performIntegerComparision(Integer.valueOf(object2.getTotalNoOfNEs()), Integer.valueOf(object1.getTotalNoOfNEs()));
            }
        }
    }

}
