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
import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.services.shm.common.Filter;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJobDto;

public enum ShmJobDataEnum {

    JOB_NAME("jobName", new ShmMainJobsComparator.NameComparator(true), new ShmMainJobsComparator.NameComparator(false), new ShmMainJobsFilter.JobNameFilter()),
    /**
     * 
     */
    JOB_TYPE("jobType", new ShmMainJobsComparator.TypeComparator(true), new ShmMainJobsComparator.TypeComparator(false), new ShmMainJobsFilter.JobTypeFilter()),
    /**
     * 
     */
    JOB_CREATED_BY("createdBy", new ShmMainJobsComparator.CreatedByComparator(true), new ShmMainJobsComparator.CreatedByComparator(false), new ShmMainJobsFilter.JobCreatedByFilter()),
    /**
      * 
      */
    JOB_PROGRESS("progress", new ShmMainJobsComparator.ProgressComparator(true), new ShmMainJobsComparator.ProgressComparator(false), new ShmMainJobsFilter.JobProgressFilter()),
    /**
     * 
     */
    JOB_STATUS("status", new ShmMainJobsComparator.StatusComparator(true), new ShmMainJobsComparator.StatusComparator(false), new ShmMainJobsFilter.JobStatusFilter()),
    /**
     * 
     */
    JOB_RESULT("result", new ShmMainJobsComparator.ResultComparator(true), new ShmMainJobsComparator.ResultComparator(false), new ShmMainJobsFilter.JobResultFilter()),
    /**
     * 
     */
    JOB_START_DATE("startTime", new ShmMainJobsComparator.StartDateComparator(true), new ShmMainJobsComparator.StartDateComparator(false), new ShmMainJobsFilter.JobStartDateFilter()),
    /**
     * 
     */
    JOB_END_DATE("endTime", new ShmMainJobsComparator.EndDateComparator(true), new ShmMainJobsComparator.EndDateComparator(false), new ShmMainJobsFilter.JobEndDateFilter()),
    /**
     * 
     */
    JOB_TOTAL_NO_OF_NES("totalNoOfNEs", new ShmMainJobsComparator.TotalNoOfNEsComparator(true), new ShmMainJobsComparator.TotalNoOfNEsComparator(false), new ShmMainJobsFilter.JobTotalNoOfNEsFilter());

    private final String attribute;
    private final Comparator<? super SHMMainJobDto> ascending;
    private final Comparator<? super SHMMainJobDto> descending;
    private final Filter<SHMMainJobDto> filter;
    private static final Map<String, ShmJobDataEnum> comparatorMap = new HashMap<>();

    static {
        for (final ShmJobDataEnum attribute : ShmJobDataEnum.values()) {
            comparatorMap.put(attribute.getAttribute(), attribute);
        }
    }

    private ShmJobDataEnum(final String attribute, final Comparator<? super SHMMainJobDto> asc, final Comparator<? super SHMMainJobDto> desc, final Filter<SHMMainJobDto> filter) {
        this.attribute = attribute;
        this.ascending = asc;
        this.descending = desc;
        this.filter = filter;
    }

    private String getAttribute() {
        return attribute;
    }

    private Comparator<? super SHMMainJobDto> getAscending() {
        return ascending;
    }

    private Comparator<? super SHMMainJobDto> getDescending() {
        return descending;
    }

    private Filter<SHMMainJobDto> getShmJobFilter() {
        return filter;
    }

    public static Comparator<? super SHMMainJobDto> getShmJobComparator(final String name, final String orderBy) {

        final ShmJobDataEnum shmjobComparator = comparatorMap.get(name);
        if (ShmConstants.DESENDING.equalsIgnoreCase(orderBy)) {
            return shmjobComparator.getDescending();

        } else {
            return shmjobComparator.getAscending();
        }

    }

    public static Filter<SHMMainJobDto> getShmJobsFilter(final String columnName) {
        final ShmJobDataEnum shmJobDataEnum = comparatorMap.get(columnName);
        return shmJobDataEnum.getShmJobFilter();
    }

    public static boolean isAValidAttribute(final String name) {
        return comparatorMap.get(name) != null;

    }

    public static boolean validate(final FilterDetails filterDetails) {

        final ShmJobDataEnum shmJobDataEnum = comparatorMap.get(filterDetails.getColumnName());
        final Filter<SHMMainJobDto> filter = shmJobDataEnum.getShmJobFilter();
        return filter.validate(filterDetails);
    }

}
