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
import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;

public enum ShmJobDetailAttributeEnum {

    NE_NODE_NAME(ShmCommonConstants.JOB_DETAIL_NE_NODE_NAME, new ShmJobDetailsComparator.JobNameComparator(true), new ShmJobDetailsComparator.JobNameComparator(false),
            new ShmJobDetailsFilter.NeNodeNameFilter()), NE_ACTIVITY(ShmCommonConstants.JOB_DETAIL_NE_ACTIVITY, new ShmJobDetailsComparator.NeActivityComparator(true),
            new ShmJobDetailsComparator.NeActivityComparator(false), new ShmJobDetailsFilter.NeActivityFilter()), NE_PROGRESS(ShmCommonConstants.JOB_DETAIL_NE_PROGRESS,
            new ShmJobDetailsComparator.JobProgressComparator(true), new ShmJobDetailsComparator.JobProgressComparator(false), new ShmJobDetailsFilter.NeProgressFilter()), NE_STATUS(
            ShmCommonConstants.JOB_DETAIL_NE_STATUS, new ShmJobDetailsComparator.JobStatusComparator(true), new ShmJobDetailsComparator.JobStatusComparator(false),
            new ShmJobDetailsFilter.NeStatusFilter()), NE_RESULT(ShmCommonConstants.JOB_DETAIL_NE_RESULT, new ShmJobDetailsComparator.JobResultComparator(true),
            new ShmJobDetailsComparator.JobResultComparator(false), new ShmJobDetailsFilter.NeResultFilter()), NE_START_DATE(ShmCommonConstants.JOB_DETAIL_NE_START_DATE,
            new ShmJobDetailsComparator.JobStartDateComparator(true), new ShmJobDetailsComparator.JobStartDateComparator(false), new ShmJobDetailsFilter.NeStartDateFilter()), NE_END_DATE(
            ShmCommonConstants.JOB_DETAIL_NE_END_DATE, new ShmJobDetailsComparator.JobEndDateComparator(true), new ShmJobDetailsComparator.JobEndDateComparator(false),
            new ShmJobDetailsFilter.NeEndDateFilter()), NE_MESSAGE(ShmCommonConstants.JOB_DETAIL_NE_MESSAGE, new ShmJobDetailsComparator.NeLastLogMessageComparator(true),
            new ShmJobDetailsComparator.NeLastLogMessageComparator(false), new ShmJobDetailsFilter.NeLastLogMessageFilter()), NE_TYPE(ShmCommonConstants.JOB_DETAIL_NE_TYPE,
            new ShmJobDetailsComparator.NeTypeComparator(true), new ShmJobDetailsComparator.NeTypeComparator(false), new ShmJobDetailsFilter.NeTypeFilter());
    private final String attribute;
    private final Comparator<? super NeJobDetails> ascending;
    private final Comparator<? super NeJobDetails> descending;
    private final Filter<NeJobDetails> filter;
    private final static Map<String, ShmJobDetailAttributeEnum> comparatorMap = new HashMap<String, ShmJobDetailAttributeEnum>();

    static {
        for (final ShmJobDetailAttributeEnum attribute : ShmJobDetailAttributeEnum.values()) {
            comparatorMap.put(attribute.getAttribute(), attribute);
        }
    }

    private String getAttribute() {
        return attribute;
    }

    private Filter<NeJobDetails> shmJobDetailFilter() {
        return filter;
    }

    private ShmJobDetailAttributeEnum(final String attribute, final Comparator<? super NeJobDetails> asc, final Comparator<? super NeJobDetails> desc, final Filter<NeJobDetails> filter) {
        this.attribute = attribute;
        this.ascending = asc;
        this.descending = desc;
        this.filter = filter;
    }

    private Comparator<? super NeJobDetails> getAscending() {
        return ascending;
    }

    private Comparator<? super NeJobDetails> getDescending() {
        return descending;
    }

    public static Comparator<? super NeJobDetails> getJobDetailEnumComparator(final String name, final boolean isAsc) {

        final ShmJobDetailAttributeEnum jobDetailComparator = comparatorMap.get(name);
        if (isAsc) {
            return jobDetailComparator.getAscending();

        } else {
            return jobDetailComparator.getDescending();
        }

    }

    public static Filter<NeJobDetails> getShmJobDetailsFilter(final String columnName) {
        final ShmJobDetailAttributeEnum jobDetailEnum = comparatorMap.get(columnName);
        return jobDetailEnum.shmJobDetailFilter();
    }

    public static boolean isAValidAttribute(final String name) {
        if (comparatorMap.get(name) == null) {
            return false;
        }
        return true;
    }

    public static boolean validate(final FilterDetails filterDetails) {
        final ShmJobDetailAttributeEnum shmJobDetailAttributeEnum = comparatorMap.get(filterDetails.getColumnName());
        final Filter<NeJobDetails> filter = shmJobDetailAttributeEnum.shmJobDetailFilter();
        return filter.validate(filterDetails);
    }

}
