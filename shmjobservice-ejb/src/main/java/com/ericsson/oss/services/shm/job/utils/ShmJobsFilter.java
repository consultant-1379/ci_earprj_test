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

import java.util.List;

import com.ericsson.oss.services.shm.common.DateFilter;
import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DoubleFilter;
import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.FilterOperatorEnum;
import com.ericsson.oss.services.shm.common.IntegerFilter;
import com.ericsson.oss.services.shm.common.StringFilter;
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * @deprecated
 */
@Deprecated
public abstract class ShmJobsFilter {

    public static class jobNameFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getJobName(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class jobTypeFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getJobType(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class jobCreatedByFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getCreatedBy(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class jobProgressFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {

            boolean isFilterTextMatched = false;
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                isFilterTextMatched = StringFilter.getInstance().check(String.valueOf(value.getProgress()), filterCriteria);
            } else {
                try {
                    isFilterTextMatched = DoubleFilter.getInstance().check(value.getProgress(), filterCriteria);
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

    public static class jobStatusFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getStatus(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class jobResultFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getResult(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class jobStartDateFilter implements Filter<SHMJobData> {

        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            final String formattedDate = DateTimeUtils.getStringDateFromLongValue(value.getStartDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
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

    public static class jobEndDateFilter implements Filter<SHMJobData> {
        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            final String formattedDate = DateTimeUtils.getStringDateFromLongValue(value.getEndDate(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
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

    public static class jobTotalNoOfNEsFilter implements Filter<SHMJobData> {
        @Override
        public boolean check(final SHMJobData value, final FilterDetails filterCriteria) {
            boolean isFilterTextMatched = false;
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                isFilterTextMatched = StringFilter.getInstance().check(value.getTotalNoOfNEs(), filterCriteria);
            } else {
                try {
                    isFilterTextMatched = IntegerFilter.getInstance().check(Integer.parseInt(value.getTotalNoOfNEs()), filterCriteria);
                } catch (final NumberFormatException ex) {
                    isFilterTextMatched = false;
                }
            }
            return isFilterTextMatched;
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return IntegerFilter.getInstance().validate(filterCriteria);
        }
    }

    /**
     * This Utility applies filter on given ShmJobData returns true if filter text matches with row value from ShmJobData, otherwise returns false.
     * 
     * @param SHMJobData
     * @param filterDetails
     * @return
     */

    public static boolean applyFilter(final SHMJobData shmJobData, final List<FilterDetails> filterDetails) {
        boolean isFilterValueMatched = true;
        for (final FilterDetails filterDetail : filterDetails) {
            if (!filterDetail.getFilterText().trim().equals("")) {
                final Filter<SHMJobData> filter = ShmJobAttributesEnum.getShmJobsFilter(filterDetail.getColumnName());
                isFilterValueMatched = filter.check(shmJobData, filterDetail);
                if (!isFilterValueMatched) {
                    break;
                }
            }
        }
        return isFilterValueMatched;
    }

}
