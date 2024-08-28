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
import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.FilterOperatorEnum;
import com.ericsson.oss.services.shm.common.StringFilter;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;

public abstract class ShmJobLogsFilter {

    public static class neNameFilter implements Filter<JobLogResponse> {

        @Override
        public boolean check(final JobLogResponse value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNeName(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class activityNameFilter implements Filter<JobLogResponse> {

        @Override
        public boolean check(final JobLogResponse value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getActivityName(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class messageFilter implements Filter<JobLogResponse> {

        @Override
        public boolean check(final JobLogResponse value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getMessage(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    public static class timeFilter implements Filter<JobLogResponse> {

        @Override
        public boolean check(final JobLogResponse value, final FilterDetails filterCriteria) {
            final String formattedDate = DateTimeUtils.getStringDateFromLongValue(value.getEntryTime(), DateTimeUtils.SHM_SPECIFIED_FILTER_DATE_FORMAT);
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

    public static class nodeTypeFilter implements Filter<JobLogResponse> {

        @Override
        public boolean check(final JobLogResponse value, final FilterDetails filterCriteria) {
            return StringFilter.getInstance().check(value.getNodeType(), filterCriteria);
        }

        @Override
        public boolean validate(final FilterDetails filterCriteria) {
            return true;
        }
    }

    /**
     * This Class applies filter on given jobLogResponse by given filter details and return true if filter text matches with row value from HardwareItemDto, otherwise returns false.
     * 
     * @param jobLogResponse
     * @param filterDetails
     * @return
     */

    public static boolean applyFilter(final JobLogResponse jobLogResponse, final List<FilterDetails> filterDetails) {
        boolean isFilterValueMatched = true;
        for (final FilterDetails filterDetail : filterDetails) {
            if (!filterDetail.getFilterText().trim().equals("")) {
                final Filter<JobLogResponse> filter = ShmJobLogEnum.getShmJobLogFilterFilter(filterDetail.getColumnName());
                isFilterValueMatched = filter.check(jobLogResponse, filterDetail);
                if (!isFilterValueMatched) {
                    break;
                }
            }
        }
        return isFilterValueMatched;
    }

}
