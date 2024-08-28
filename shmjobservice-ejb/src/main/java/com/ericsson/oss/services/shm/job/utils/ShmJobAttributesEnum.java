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
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * @deprecated
 */
@Deprecated
public enum ShmJobAttributesEnum {

    JOB_NAME("jobName", new ShmJobsComparator.JobNameComparator(true), new ShmJobsComparator.JobNameComparator(false), new ShmJobsFilter.jobNameFilter()), JOB_TYPE("jobType",
            new ShmJobsComparator.jobTypeComparator(true), new ShmJobsComparator.jobTypeComparator(false), new ShmJobsFilter.jobTypeFilter()), JOB_CREATED_BY("createdBy",
                    new ShmJobsComparator.jobCreatedByComparator(true), new ShmJobsComparator.jobCreatedByComparator(false), new ShmJobsFilter.jobCreatedByFilter()), JOB_PROGRESS("progress",
                            new ShmJobsComparator.jobProgressComparator(true), new ShmJobsComparator.jobProgressComparator(false), new ShmJobsFilter.jobProgressFilter()), JOB_STATUS("status",
                                    new ShmJobsComparator.jobStatusComparator(true), new ShmJobsComparator.jobStatusComparator(false), new ShmJobsFilter.jobStatusFilter()), JOB_RESULT("result",
                                            new ShmJobsComparator.jobResultComparator(true), new ShmJobsComparator.jobResultComparator(false),
                                            new ShmJobsFilter.jobResultFilter()), JOB_START_DATE("startDate", new ShmJobsComparator.jobStartDateComparator(true),
                                                    new ShmJobsComparator.jobStartDateComparator(false), new ShmJobsFilter.jobStartDateFilter()), JOB_END_DATE("endDate",
                                                            new ShmJobsComparator.jobEndDateComparator(true), new ShmJobsComparator.jobEndDateComparator(false),
                                                            new ShmJobsFilter.jobEndDateFilter()), JOB_TOTAL_NO_OF_NES("totalNoOfNEs", new ShmJobsComparator.jobTotalNoOfNEsComparator(true),
                                                                    new ShmJobsComparator.jobTotalNoOfNEsComparator(false), new ShmJobsFilter.jobTotalNoOfNEsFilter());

    private final String attribute;
    private final Comparator<? super SHMJobData> ascending;
    private final Comparator<? super SHMJobData> descending;
    private final Filter<SHMJobData> filter;
    private final static Map<String, ShmJobAttributesEnum> comparatorMap = new HashMap<String, ShmJobAttributesEnum>();

    static {
        for (final ShmJobAttributesEnum attribute : ShmJobAttributesEnum.values()) {
            comparatorMap.put(attribute.getAttribute(), attribute);
        }
    }

    private ShmJobAttributesEnum(final String attribute, final Comparator<? super SHMJobData> asc, final Comparator<? super SHMJobData> desc, final Filter<SHMJobData> filter) {
        this.attribute = attribute;
        this.ascending = asc;
        this.descending = desc;
        this.filter = filter;
    }

    private String getAttribute() {
        return attribute;
    }

    private Comparator<? super SHMJobData> getAscending() {
        return ascending;
    }

    private Comparator<? super SHMJobData> getDescending() {
        return descending;
    }

    private Filter<SHMJobData> getShmJobFilter() {
        return filter;
    }

    public static Comparator<? super SHMJobData> getShmJobComparator(final String name, final boolean isAsc) {

        final ShmJobAttributesEnum shmhobComparator = comparatorMap.get(name);
        if (isAsc) {
            return shmhobComparator.getAscending();

        } else {
            return shmhobComparator.getDescending();
        }

    }

    public static Filter<SHMJobData> getShmJobsFilter(final String columnName) {
        final ShmJobAttributesEnum shmJobAttributeEnum = comparatorMap.get(columnName);
        return shmJobAttributeEnum.getShmJobFilter();
    }

    public static boolean isAValidAttribute(final String name) {
        if (comparatorMap.get(name) == null) {
            return false;
        }
        return true;
    }

    public static boolean validate(final FilterDetails filterDetails) {

        final ShmJobAttributesEnum shmJobAttributesEnum = comparatorMap.get(filterDetails.getColumnName());
        final Filter<SHMJobData> filter = shmJobAttributesEnum.getShmJobFilter();
        return filter.validate(filterDetails);
    }

}
