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

import java.util.List;

import com.ericsson.oss.services.shm.common.DateFilter;
import com.ericsson.oss.services.shm.common.DoubleFilter;
import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.FilterOperatorEnum;
import com.ericsson.oss.services.shm.common.IntegerFilter;
import com.ericsson.oss.services.shm.common.StringFilter;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJobDto;

public abstract class ShmMainJobsFilter {

    private ShmMainJobsFilter() {

    }

    public static class JobNameFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getJobName(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class JobTypeFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getJobType(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class JobCreatedByFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getCreatedBy(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class JobProgressFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {

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

    public static class JobStatusFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getStatus(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class JobResultFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getResult(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class JobStartDateFilter implements Filter<SHMMainJobDto> {

        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                return StringFilter.getInstance().check(value.getStartTime().toString(), filterCriteria);
            } else {
                return DateFilter.getInstance().check(value.getStartTime(), filterCriteria);
            }
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return DateFilter.getInstance().validate(filterCriteria);
        }
    }

    public static class JobEndDateFilter implements Filter<SHMMainJobDto> {
        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
            if (filterCriteria.getFilterOperator().equalsIgnoreCase(FilterOperatorEnum.CONTAINS.getAttribute())) {
                return StringFilter.getInstance().check(value.getEndTime().toString(), filterCriteria);
            } else {
                return DateFilter.getInstance().check(value.getEndTime(), filterCriteria);
            }
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return DateFilter.getInstance().validate(filterCriteria);
        }
    }

    public static class JobTotalNoOfNEsFilter implements Filter<SHMMainJobDto> {
        @Override
        public boolean check(final SHMMainJobDto value, final FilterDetails filterCriteria) {
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

    public static boolean applyFilter(final SHMMainJobDto shmJobData, final List<FilterDetails> filterDetails) {
        boolean isFilterValueMatched = true;
        for (final FilterDetails filterDetail : filterDetails) {
            if (!filterDetail.getFilterText().trim().equals("")) {
                final Filter<SHMMainJobDto> filter = ShmJobDataEnum.getShmJobsFilter(filterDetail.getColumnName());
                isFilterValueMatched = filter.check(shmJobData, filterDetail);
                if (!isFilterValueMatched) {
                    break;
                }
            }
        }
        return isFilterValueMatched;
    }

}
