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
package com.ericsson.oss.services.shm.jobs.common.api;

import java.util.Comparator;

import com.ericsson.oss.services.shm.common.DataTypeComparator;

public class ShmJobDetailsComparator {

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

    public static class JobNameComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobNameComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeNodeName(), object2.getNeNodeName(), isAscending);
        }

    }

    public static class JobProgressComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobProgressComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performDoubleComparision(object1.getNeProgress(), object2.getNeProgress(), isAscending);
        }
    }

    public static class JobStatusComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobStatusComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeStatus(), object2.getNeStatus(), isAscending);
        }
    }

    public static class JobResultComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobResultComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeResult(), object2.getNeResult(), isAscending);
        }
    }

    public static class JobStartDateComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobStartDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeStartDate(), object2.getNeStartDate(), isAscending);
        }
    }

    public static class JobEndDateComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public JobEndDateComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeEndDate(), object2.getNeEndDate(), isAscending);
        }
    }

    public static class NeActivityComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public NeActivityComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNeActivity(), object2.getNeActivity(), isAscending);
        }
    }

    public static class NeLastLogMessageComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public NeLastLogMessageComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getLastLogMessage(), object2.getLastLogMessage(), isAscending);
        }
    }

    public static class NeTypeComparator implements Comparator<NeJobDetails> {

        final private boolean isAscending;

        public NeTypeComparator(final boolean isAscending) {
            this.isAscending = isAscending;
        }

        @Override
        public int compare(final NeJobDetails object1, final NeJobDetails object2) {
            return performStringComparision(object1.getNodeType(), object2.getNodeType(), isAscending);
        }
    }

}
