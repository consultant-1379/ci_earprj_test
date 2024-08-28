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

import java.util.List;

import com.ericsson.oss.services.shm.common.DateFilter;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DoubleFilter;
import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.FilterOperatorEnum;
import com.ericsson.oss.services.shm.common.StringFilter;

public abstract class ShmJobDetailsFilter {

    public static class NeNodeNameFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNeNodeName(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class NeActivityFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            if (value.getNeActivity() == null) {
                value.setNeActivity("");
            }
            return StringFilter.getInstance().check(value.getNeActivity(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class NeProgressFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            boolean isFilterTextMatched = false;
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                isFilterTextMatched = StringFilter.getInstance().check(String.valueOf(value.getNeProgress()), filterCriteria);
            } else {
                try {
                    isFilterTextMatched = DoubleFilter.getInstance().check(value.getNeProgress(), filterCriteria);
                } catch (final NumberFormatException ex) {
                    isFilterTextMatched = false;
                }
            }
            return isFilterTextMatched;
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return DoubleFilter.getInstance().validate(filterCriteria);
        }
    }

    public static class NeStatusFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNeStatus(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class NeResultFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNeResult(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class NeStartDateFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            final String formattedDate = DateTimeUtils.getStringDateFromLongValue(value.getNeStartDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                return StringFilter.getInstance().check(formattedDate, filterCriteria);
            } else {
                return DateFilter.getInstance().check(DateTimeUtils.getDateFromStringValue(formattedDate), filterCriteria);
            }
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return DateFilter.getInstance().validate(filterCriteria);
        }
    }

    public static class NeEndDateFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            final String formattedDate = DateTimeUtils.getStringDateFromLongValue(value.getNeEndDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                return StringFilter.getInstance().check(formattedDate, filterCriteria);
            } else {
                return DateFilter.getInstance().check(DateTimeUtils.getDateFromStringValue(formattedDate), filterCriteria);
            }
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return DateFilter.getInstance().validate(filterCriteria);
        }
    }

    public static class NeLastLogMessageFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            if (value.getLastLogMessage() == null) {
                value.setLastLogMessage("");
            }
            return StringFilter.getInstance().check(value.getLastLogMessage(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class NeTypeFilter implements Filter<NeJobDetails> {

        @Override
        public boolean check(final NeJobDetails value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNodeType(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    /**
     * This Utility applies filter on given NeJobDetails returns true if filter text matches with row value from NeJobDetails, otherwise returns false.
     * 
     * @param NeJobDetails
     * @param filterDetails
     * @return
     */

    public static boolean applyFilter(final NeJobDetails neJobDetails, final List<FilterDetails> filterDetails) {
        boolean isFilterValueMatched = true;
        for (final FilterDetails filterDetail : filterDetails) {
            if (!filterDetail.getFilterText().trim().equals("")) {
                final Filter<NeJobDetails> filter = ShmJobDetailAttributeEnum.getShmJobDetailsFilter(filterDetail.getColumnName());
                isFilterValueMatched = filter.check(neJobDetails, filterDetail);
                if (!isFilterValueMatched) {
                    break;
                }
            }
        }
        return isFilterValueMatched;
    }

}
