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
import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;

public enum ShmJobLogEnum {

    NE_NAME("neName", new ShmJobLogsComparator.NeNameComparator(true), new ShmJobLogsComparator.NeNameComparator(false), new ShmJobLogsFilter.neNameFilter()), ACTIVITY_NAME("activityName",
            new ShmJobLogsComparator.ActivityNameComparator(true), new ShmJobLogsComparator.ActivityNameComparator(false), new ShmJobLogsFilter.activityNameFilter()), CREATED_TIME("entryTime",
            new ShmJobLogsComparator.EntryTimeComparator(true), new ShmJobLogsComparator.EntryTimeComparator(false), new ShmJobLogsFilter.timeFilter()), MESSAGE_INFO("message",
            new ShmJobLogsComparator.MessageComparator(true), new ShmJobLogsComparator.MessageComparator(true), new ShmJobLogsFilter.messageFilter()), NE_TYPE("nodeType",
            new ShmJobLogsComparator.NodeTypeComparator(true), new ShmJobLogsComparator.NodeTypeComparator(true), new ShmJobLogsFilter.nodeTypeFilter());

    private final String attribute;
    private final Comparator<? super JobLogResponse> ascending;
    private final Comparator<? super JobLogResponse> descending;
    private final Filter<JobLogResponse> filter;
    private final static Map<String, ShmJobLogEnum> comparatorMap = new HashMap<String, ShmJobLogEnum>();

    static {
        for (final ShmJobLogEnum attribute : ShmJobLogEnum.values()) {
            comparatorMap.put(attribute.getAttribute(), attribute);
        }
    }

    private ShmJobLogEnum(final String attribute, final Comparator<? super JobLogResponse> asc, final Comparator<? super JobLogResponse> desc, final Filter<JobLogResponse> filter) {
        this.attribute = attribute;
        this.ascending = asc;
        this.descending = desc;
        this.filter = filter;
    }

    private String getAttribute() {
        return attribute;
    }

    private Comparator<? super JobLogResponse> getAscending() {
        return ascending;
    }

    private Comparator<? super JobLogResponse> getDescending() {
        return descending;
    }

    private Filter<JobLogResponse> getShmJobLogFilter() {
        return filter;
    }

    public static Comparator<? super JobLogResponse> getShmJobComparator(final String name, final boolean isAsc) {

        final ShmJobLogEnum shmJobLogComparator = comparatorMap.get(name);
        if (isAsc) {
            return shmJobLogComparator.getAscending();

        } else {
            return shmJobLogComparator.getDescending();
        }

    }

    public static Filter<JobLogResponse> getShmJobLogFilterFilter(final String columnName) {
        final ShmJobLogEnum shmJobAttributeEnum = comparatorMap.get(columnName);
        return shmJobAttributeEnum.getShmJobLogFilter();
    }

    public static boolean isAValidAttribute(final String name) {

        if (comparatorMap.get(name) == null) {
            return false;
        }
        return true;
    }

    public static boolean validate(final FilterDetails filterDetails) {

        final ShmJobLogEnum shmJobLogEnum = comparatorMap.get(filterDetails.getColumnName());
        final Filter<JobLogResponse> filter = shmJobLogEnum.getShmJobLogFilter();
        return filter.validate(filterDetails);
    }

}
