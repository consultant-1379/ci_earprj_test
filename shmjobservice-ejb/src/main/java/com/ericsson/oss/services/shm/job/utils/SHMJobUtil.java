package com.ericsson.oss.services.shm.job.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FilterDetails;
import com.ericsson.oss.services.shm.common.FilterUtils;
import com.ericsson.oss.services.shm.common.exception.InvalidFilterException;
import com.ericsson.oss.services.shm.common.exception.InvalidSortException;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.entities.OrderByEnum;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.api.ShmJobDetailAttributeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Job;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobDetails;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ScheduleProperty;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ShmJobs;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobConstants;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobUtilConstants;
import com.ericsson.oss.services.shm.shared.constants.PeriodicSchedulerConstants;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@Traceable
@Profiled
@SuppressWarnings("PMD")
public class SHMJobUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobUtil.class);

    @Deprecated
    public void validateShmJobData(final JobInput jobInput) {

        if (!(jobInput.getFilterDetails() == null || jobInput.getFilterDetails().isEmpty())) {
            for (FilterDetails filterDetails : jobInput.getFilterDetails()) {
                if (!ShmJobAttributesEnum.isAValidAttribute(filterDetails.getColumnName())) {
                    throw new InvalidFilterException("FilterBy \"" + filterDetails.getColumnName() + "\" is not a valid column");
                }

                if (!FilterUtils.isValidFilterOperator(filterDetails.getFilterOperator())) {
                    throw new InvalidFilterException("Condtion  \"" + filterDetails.getFilterOperator() + "\" is not a valid filter condition");
                }
                if (!ShmJobAttributesEnum.validate(filterDetails)) {
                    throw new InvalidFilterException("Invalid filter text \"" + filterDetails.getFilterText() + "\" entered for the column \"" + filterDetails.getColumnName() + "\". ");
                }
            }
        }
    }

    public void validateShmMainJobData(final JobInput jobInput) {
        if (!ShmJobDataEnum.isAValidAttribute(jobInput.getSortBy())) {
            throw new InvalidSortException(String.format(SHMJobConstants.INVALID_COLUMN, SHMJobConstants.SORT_BY, jobInput.getSortBy()));
        }

        if (!(jobInput.getFilterDetails() == null || jobInput.getFilterDetails().isEmpty())) {
            for (FilterDetails filterDetails : jobInput.getFilterDetails()) {
                if (!ShmJobDataEnum.isAValidAttribute(filterDetails.getColumnName())) {
                    throw new InvalidFilterException("FilterBy \"" + filterDetails.getColumnName() + "\" is not a valid column");
                }

                if (!FilterUtils.isValidFilterOperator(filterDetails.getFilterOperator())) {
                    throw new InvalidFilterException("Condtion  \"" + filterDetails.getFilterOperator() + "\" is not a valid filter condition");
                }
                if (!ShmJobDataEnum.validate(filterDetails)) {
                    throw new InvalidFilterException("Invalid filter text \"" + filterDetails.getFilterText() + "\" entered for the column \"" + filterDetails.getColumnName() + "\". ");
                }
            }
        }
    }

    public void validateShmJobLog(final JobLogRequest jobLogRequest) {
        if (!(jobLogRequest.getFilterDetails() == null || jobLogRequest.getFilterDetails().isEmpty())) {
            for (FilterDetails filterDetails : jobLogRequest.getFilterDetails()) {
                if (!ShmJobLogEnum.isAValidAttribute(filterDetails.getColumnName())) {
                    throw new InvalidFilterException("FilterBy \"" + filterDetails.getColumnName() + "\" is not a valid column");
                }
                if (!FilterUtils.isValidFilterOperator(filterDetails.getFilterOperator())) {
                    throw new InvalidFilterException("Condtion  \"" + filterDetails.getFilterOperator() + "\" is not a valid filter condition");
                }
                if (!ShmJobLogEnum.validate(filterDetails)) {
                    throw new InvalidFilterException("Invalid filter text \"" + filterDetails.getFilterText() + "\" entered for the column \"" + filterDetails.getColumnName() + "\". ");
                }
            }
        }

    }

    public void validateShmJobDetail(final NeJobInput jobInput) {
        if (!(jobInput.getFilterDetails() == null || jobInput.getFilterDetails().isEmpty())) {
            for (FilterDetails filterDetails : jobInput.getFilterDetails()) {
                if (!ShmJobDetailAttributeEnum.isAValidAttribute(filterDetails.getColumnName())) {
                    throw new InvalidFilterException("FilterBy \"" + filterDetails.getColumnName() + "\" is not a valid column");
                }
                if (!FilterUtils.isValidFilterOperator(filterDetails.getFilterOperator())) {
                    throw new InvalidFilterException("Condtion  \"" + filterDetails.getFilterOperator() + "\" is not a valid filter condition");
                }
                if (!ShmJobDetailAttributeEnum.validate(filterDetails)) {
                    throw new InvalidFilterException("Invalid filter text \"" + filterDetails.getFilterText() + "\" entered for the column \"" + filterDetails.getColumnName() + "\". ");
                }
            }
        }
    }

    public List<SHMJobData> getJobDetailsList(final ShmJobs shmJobs) {
        final Map<Long, JobDetails> jobDetailsMap = shmJobs.getJobDetailsMap();
        final List<SHMJobData> shmJobDataList = new ArrayList<>();
        for (final Entry<Long, JobDetails> jobTemplateset : jobDetailsMap.entrySet()) {
            final JobDetails jobDetails = jobTemplateset.getValue();

            String jobName = null;
            String jobType = null;
            String createdBy = null;
            String status = null;
            final int noOfMEs = 0;

            final JobTemplate jobTemplate = jobDetails.getJobTemplate();
            if (jobTemplate != null) {
                if (jobTemplate.getName() != null) {
                    jobName = jobTemplate.getName();
                } else {
                    jobName = "";
                }
                if (jobTemplate.getJobType() != null) {
                    jobType = jobTemplate.getJobType().getJobTypeName();
                    createdBy = jobTemplate.getOwner();
                } else {
                    jobType = "";
                    createdBy = "";
                }

                final JobConfiguration jobConfigurationDetails = jobTemplate.getJobConfigurationDetails();

                for (final Job job : jobDetails.getJobList()) {
                    final long jobId = job.getIdAsLong();
                    if (job.getState() != null) {
                        status = job.getState();
                    } else {
                        status = "";
                    }
                    double progress = 0;
                    String result = null;
                    String startDate = null;
                    String endDate = null;
                    String creationTime = "";

                    progress = job.getProgressPercentage();
                    if (job.getResult() != null) {
                        result = job.getResult();
                    } else {
                        result = "";
                    }
                    if (job.getStartTime() != null) {
                        startDate = String.valueOf(job.getStartTime().getTime());

                    } else {
                        startDate = "";
                        if (jobTemplate.getCreationTime() != null) {
                            creationTime = String.valueOf(jobTemplate.getCreationTime().getTime());

                        }
                    }

                    if (job.getEndTime() != null) {
                        endDate = String.valueOf(job.getEndTime().getTime());
                    } else {
                        endDate = "";
                    }
                    final List<Map<String, Object>> formattedCommentList = formatJobComment(job);
                    //setting total count of NEs by retrieving attrib from main job

                    final String totalNEs = job.getNumberOfNetworkElements() > 0 ? String.valueOf(job.getNumberOfNetworkElements()) : ShmConstants.NE_NOT_AVAILABLE;
                    final SHMJobData shmJobData = new SHMJobData();
                    shmJobData.setCreatedBy(createdBy);
                    shmJobData.setEndDate(endDate);
                    shmJobData.setJobName(jobName);
                    shmJobData.setJobType(jobType);
                    shmJobData.setNoOfMEs(noOfMEs);//redundant
                    shmJobData.setProgress(progress);
                    shmJobData.setResult(result);
                    shmJobData.setStartDate(startDate);
                    shmJobData.setCreationTime(creationTime);
                    shmJobData.setStatus(status);
                    shmJobData.setJobTemplateId(jobTemplateset.getKey());
                    shmJobData.setJobId(jobId);
                    shmJobData.setComment(formattedCommentList);
                    shmJobData.setTotalNoOfNEs(totalNEs);
                    shmJobDataList.add(shmJobData);

                    if (jobConfigurationDetails != null && jobConfigurationDetails.getMainSchedule().getExecMode() == ExecMode.SCHEDULED) {

                        for (final ScheduleProperty scheduleProperty : jobConfigurationDetails.getMainSchedule().getScheduleAttributes()) {

                            if ("REPEAT_COUNT".equals(scheduleProperty.getName())) {
                                shmJobData.setPeriodic(true);
                            }
                            if (PeriodicSchedulerConstants.CRON_EXP.equals(scheduleProperty.getName())) {
                                shmJobData.setPeriodic(true);
                            }

                        }
                    }
                }
            } else {
                LOGGER.error("JobTemplate not found for the corresponding Main Job.");
            }

        }
        return shmJobDataList;
    }

    /**
     * @deprecated
     * @param job
     * @return
     */
    @Deprecated
    public List<Map<String, Object>> formatJobComment(final Job job) {
        final List<Map<String, Object>> formattedCommentList = new ArrayList<>();

        if (job.getComment() != null && !job.getComment().isEmpty()) {
            final List<Map<String, Object>> commentList = job.getComment();
            for (final Map<String, Object> comment : commentList) {
                final Date commentDate = (Date) comment.get(ShmConstants.DATE);
                final String commentMessage = (String) comment.get(ShmConstants.COMMENT);
                final String commentUser = (String) comment.get(ShmConstants.USERNAME);

                final Map<String, Object> formattedComment = new HashMap<>();
                formattedComment.put(ShmConstants.DATE, String.valueOf(commentDate.getTime()));
                formattedComment.put(ShmConstants.COMMENT, commentMessage);
                formattedComment.put(ShmConstants.USERNAME, commentUser);
                formattedCommentList.add(formattedComment);
            }
        }
        return formattedCommentList;
    }

    public JobOutput sortAndGetPageData(final List<SHMJobData> shmJobDataList, final JobInput jobInput) {
        if (ShmConstants.ASENDING.equals(jobInput.getOrderBy())) {
            ascendingJob(shmJobDataList, jobInput);
        } else if (ShmConstants.DESENDING.equals(jobInput.getOrderBy())) {
            descendingJob(shmJobDataList, jobInput);
        }
        final JobOutput jobOutput = new JobOutput();
        jobOutput.setTotalCount(shmJobDataList.size());
        List<SHMJobData> resultList = Collections.emptyList();
        int start = -1;
        int end = -1;
        if (jobInput.getOffset() <= shmJobDataList.size()) {
            start = jobInput.getOffset() - 1;
            if (jobInput.getLimit() > shmJobDataList.size()) {
                end = shmJobDataList.size();
            } else {
                end = jobInput.getLimit();
            }
        } else {
            if (jobInput.getFilterDetails() != null) {
                start = 0;
                final int pageLimit = (jobInput.getLimit() - jobInput.getOffset()) + 1;
                end = (shmJobDataList.size() >= pageLimit) ? pageLimit : shmJobDataList.size();
            }
        }
        if (start != -1 && end != -1) {
            resultList = shmJobDataList.subList(start, end);
        }
        jobOutput.setResult(resultList);
        jobOutput.setColumns(getColumMap());
        jobOutput.setClearOffset(FilterUtils.isClearOffsetRequired(shmJobDataList.size(), jobInput.getOffset()));
        return jobOutput;
    }

    /**
     * @deprecated
     */
    @Deprecated
    private void descendingJob(final List<SHMJobData> shmJobDataList, final JobInput jobInput) {
        if (ShmConstants.JOBTEMPLATE_ID.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return (int) (shmJobData2.getJobTemplateIdAsLong() - shmJobData1.getJobTemplateIdAsLong());
                }
            });
        } else if (ShmConstants.TOTAL_NO_OF_NES.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final Integer noOfNEs1 = shmJobData1.getTotalNoOfNEs().equalsIgnoreCase(ShmConstants.NE_NOT_AVAILABLE) ? 0 : Integer.parseInt(shmJobData1.getTotalNoOfNEs());
                    final Integer noOfNEs2 = shmJobData2.getTotalNoOfNEs().equalsIgnoreCase(ShmConstants.NE_NOT_AVAILABLE) ? 0 : Integer.parseInt(shmJobData2.getTotalNoOfNEs());
                    return noOfNEs2.compareTo(noOfNEs1);
                }
            });
        } else if (ShmConstants.PROGRESS.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final SHMJobData jobData1 = formatProgressForSorting(shmJobData1);
                    final SHMJobData jobData2 = formatProgressForSorting(shmJobData2);
                    final Double progressPercentageValue1 = jobData1.getProgress();
                    final Double progressPercentageValue2 = jobData2.getProgress();
                    return progressPercentageValue2.compareTo(progressPercentageValue1);
                }

            });
        } else if (ShmConstants.JOBNAME.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData2.getJobName().compareTo(shmJobData1.getJobName());
                }
            });
        } else if (ShmConstants.JOB_TYPE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData2.getJobType().compareTo(shmJobData1.getJobType());
                }
            });
        } else if (ShmConstants.CREATED_BY.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData2.getCreatedBy().compareTo(shmJobData1.getCreatedBy());
                }
            });
        } else if (ShmConstants.STATUS.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData2.getStatus().compareTo(shmJobData1.getStatus());
                }
            });
        } else if (ShmConstants.RESULT.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData2.getResult().compareTo(shmJobData1.getResult());
                }
            });
        } else if (ShmConstants.STARTDATE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    int compare = 0;
                    final Long startDate1 = shmJobData1.getStartDate().equals("") ? 0 : Long.parseLong(shmJobData1.getStartDate());
                    final Long creationDate1 = shmJobData1.getCreationTime().equals("") ? 0 : Long.parseLong(shmJobData1.getCreationTime());
                    final Long startDate2 = shmJobData2.getStartDate().equals("") ? 0 : Long.parseLong(shmJobData2.getStartDate());
                    final Long creationDate2 = shmJobData2.getCreationTime().equals("") ? 0 : Long.parseLong(shmJobData2.getCreationTime());

                    if (startDate1 != 0 && startDate2 != 0) {
                        compare = startDate2.compareTo(startDate1);
                    } else if (startDate1 != 0 && startDate2 == 0) {
                        compare = creationDate2.compareTo(startDate1);
                    } else if (startDate1 == 0 && startDate2 != 0) {
                        compare = startDate2.compareTo(creationDate1);
                    } else {
                        compare = creationDate2.compareTo(creationDate1);
                    }
                    return compare;
                }
            });
        } else if (ShmConstants.ENDDATE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final Long endDate1 = shmJobData1.getEndDate().equals("") ? 0 : Long.parseLong(shmJobData1.getEndDate());
                    final Long endDate2 = shmJobData2.getEndDate().equals("") ? 0 : Long.parseLong(shmJobData2.getEndDate());
                    int compare = 0;

                    compare = endDate2.compareTo(endDate1);
                    return compare;
                }
            });
        }
        if (ShmConstants.JOBID.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return (int) (shmJobData2.getJobIdAsLong() - shmJobData1.getJobIdAsLong());
                }
            });
        }

    }

    /**
     * @deprecated
     */
    @Deprecated
    private void ascendingJob(final List<SHMJobData> shmJobDataList, final JobInput jobInput) {
        if (ShmConstants.JOBID.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return (int) (shmJobData1.getJobTemplateIdAsLong() - shmJobData2.getJobTemplateIdAsLong());
                }
            });
        } else if (ShmConstants.TOTAL_NO_OF_NES.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final Integer noOfNEs1 = shmJobData1.getTotalNoOfNEs().equalsIgnoreCase(ShmConstants.NE_NOT_AVAILABLE) ? 0 : Integer.parseInt(shmJobData1.getTotalNoOfNEs());
                    final Integer noOfNEs2 = shmJobData2.getTotalNoOfNEs().equalsIgnoreCase(ShmConstants.NE_NOT_AVAILABLE) ? 0 : Integer.parseInt(shmJobData2.getTotalNoOfNEs());
                    return noOfNEs1.compareTo(noOfNEs2);
                }
            });
        } else if (ShmConstants.PROGRESS.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final SHMJobData jobData1 = formatProgressForSorting(shmJobData1);
                    final SHMJobData jobData2 = formatProgressForSorting(shmJobData2);
                    final Double progressPercentageValue1 = jobData1.getProgress();
                    final Double progressPercentageValue2 = jobData2.getProgress();
                    return progressPercentageValue1.compareTo(progressPercentageValue2);
                }
            });
        } else if (ShmConstants.JOBNAME.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData1.getJobName().compareTo(shmJobData2.getJobName());
                }
            });
        } else if (ShmConstants.JOB_TYPE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData1.getJobType().compareTo(shmJobData2.getJobType());
                }
            });
        } else if (ShmConstants.CREATED_BY.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData1.getCreatedBy().compareTo(shmJobData2.getCreatedBy());
                }
            });
        } else if (ShmConstants.STATUS.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData1.getStatus().compareTo(shmJobData2.getStatus());
                }
            });
        } else if (ShmConstants.RESULT.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return shmJobData1.getResult().compareTo(shmJobData2.getResult());
                }
            });
        }

        else if (ShmConstants.STARTDATE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    int compare = 0;
                    final Long startDate1 = shmJobData1.getStartDate().equals("") ? 0 : Long.parseLong(shmJobData1.getStartDate());
                    final Long creationDate1 = shmJobData1.getCreationTime().equals("") ? 0 : Long.parseLong(shmJobData1.getCreationTime());
                    final Long startDate2 = shmJobData2.getStartDate().equals("") ? 0 : Long.parseLong(shmJobData2.getStartDate());
                    final Long creationDate2 = shmJobData2.getCreationTime().equals("") ? 0 : Long.parseLong(shmJobData2.getCreationTime());

                    if (startDate1 != 0 && startDate2 != 0) {
                        compare = startDate1.compareTo(startDate2);
                    } else if (startDate1 != 0 && startDate2 == 0) {
                        compare = startDate1.compareTo(creationDate2);
                    } else if (startDate1 == 0 && startDate2 != 0) {
                        compare = creationDate1.compareTo(startDate2);
                    } else {
                        compare = creationDate1.compareTo(creationDate2);
                    }
                    return compare;
                }
            });
        } else if (ShmConstants.ENDDATE.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    final Long endDate1 = shmJobData1.getEndDate().equals("") ? 0 : Long.parseLong(shmJobData1.getEndDate());
                    final Long endDate2 = shmJobData2.getEndDate().equals("") ? 0 : Long.parseLong(shmJobData2.getEndDate());
                    int compare = 0;

                    compare = endDate1.compareTo(endDate2);
                    return compare;
                }
            });
        } else if (ShmConstants.JOBID.equals(jobInput.getSortBy())) {
            Collections.sort(shmJobDataList, new Comparator<SHMJobData>() {
                @Override
                public int compare(final SHMJobData shmJobData1, final SHMJobData shmJobData2) {
                    return (int) (shmJobData1.getJobIdAsLong() - shmJobData2.getJobIdAsLong());
                }
            });
        }
    }

    public List<Map<String, String>> getColumMap() {
        final List<Map<String, String>> columnList = new ArrayList<Map<String, String>>();

        final Map<String, String> jobName = new HashMap<String, String>();
        jobName.put("title", "Job Name");
        jobName.put("attribute", "jobName");
        columnList.add(jobName);

        final Map<String, String> jobType = new HashMap<String, String>();
        jobType.put("title", "Job Type");
        jobType.put("attribute", "jobType");
        columnList.add(jobType);

        final Map<String, String> createdBy = new HashMap<String, String>();
        createdBy.put("title", "Created By");
        createdBy.put("attribute", "createdBy");
        columnList.add(createdBy);

        final Map<String, String> noOfMEs = new HashMap<String, String>();
        noOfMEs.put("title", "No Of Nodes");
        noOfMEs.put("attribute", "totalNoOfNEs");
        columnList.add(noOfMEs);

        final Map<String, String> progress = new HashMap<String, String>();
        progress.put("title", "Progress");
        progress.put("attribute", "progress");
        columnList.add(progress);

        final Map<String, String> status = new HashMap<String, String>();
        status.put("title", "Status");
        status.put("attribute", "status");
        columnList.add(status);

        final Map<String, String> result = new HashMap<String, String>();
        result.put("title", "Result");
        result.put("attribute", "result");
        columnList.add(result);

        final Map<String, String> startDate = new HashMap<String, String>();
        startDate.put("title", "Start Date");
        startDate.put("attribute", "startDate");
        columnList.add(startDate);

        final Map<String, String> endDate = new HashMap<String, String>();
        endDate.put("title", "End Date");
        endDate.put("attribute", "endDate");
        columnList.add(endDate);

        return columnList;
    }

    public JobReportData getNeJobOutput(final JobReportData jobReportData, final NeJobInput jobInput) {

        final List<NeJobDetails> neJobList = jobReportData.getNeDetails().getResult();

        if (ShmConstants.ASENDING.equals(jobInput.getOrderBy())) {

            ascendingNeJob(neJobList, jobInput);
        } else if (ShmConstants.DESENDING.equals(jobInput.getOrderBy())) {
            descendingNeJob(neJobList, jobInput);
        }

        final NeDetails neDetails = new NeDetails();
        neDetails.setTotalCount(neJobList.size());

        List<NeJobDetails> resultList = Collections.emptyList();
        int start = -1;
        int end = -1;
        if (jobInput.getOffset() <= neJobList.size()) {
            start = jobInput.getOffset() - 1;
            if (jobInput.getLimit() > neJobList.size()) {
                end = neJobList.size();
            } else {
                end = jobInput.getLimit();
            }
        }
        if (start != -1 && end != -1) {
            resultList = neJobList.subList(start, end);
        }
        neDetails.setResult(new ArrayList<NeJobDetails>(resultList));
        jobReportData.setNeDetails(neDetails);
        return jobReportData;
    }

    // sorting methods
    private void descendingNeJob(final List<NeJobDetails> neJobList, final NeJobInput jobInput) {
        if (JobConfigurationConstants.NE_NODE_NAME.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeNodeName().compareTo(neJobDetails1.getNeNodeName());
                }
            });
        } else if (JobConfigurationConstants.NE_ACTIVITY.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeActivity().compareTo(neJobDetails1.getNeActivity());
                }
            });
        } else if (JobConfigurationConstants.NE_PROGRESS.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return (int) (neJobDetails2.getNeProgress() - neJobDetails1.getNeProgress());
                }
            });
        } else if (JobConfigurationConstants.NE_STATUS.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeStatus().compareTo(neJobDetails1.getNeStatus());
                }
            });
        } else if (JobConfigurationConstants.NE_RESULT.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeResult().compareTo(neJobDetails1.getNeResult());
                }
            });
        } else if (JobConfigurationConstants.NE_START_DATE.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeStartDate().compareTo(neJobDetails1.getNeStartDate());
                }
            });
        } else if (JobConfigurationConstants.NE_END_DATE.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails2.getNeEndDate().compareTo(neJobDetails1.getNeEndDate());
                }
            });
        }
    }

    private void ascendingNeJob(final List<NeJobDetails> neJobList, final NeJobInput jobInput) {

        if (JobConfigurationConstants.NE_NODE_NAME.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeNodeName().compareTo(neJobDetails2.getNeNodeName());
                }
            });
        } else if (JobConfigurationConstants.NE_ACTIVITY.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    if (neJobDetails1.getNeActivity() == null && neJobDetails2.getNeActivity() == null) {
                        return 0;
                    } else if (neJobDetails1.getNeActivity() == null && neJobDetails2.getNeActivity() != null) {
                        return -1;
                    } else if (neJobDetails1.getNeActivity() != null && neJobDetails2.getNeActivity() == null) {
                        return 1;
                    } else {
                        return neJobDetails1.getNeActivity().compareTo(neJobDetails2.getNeActivity());
                    }
                }
            });
        } else if (JobConfigurationConstants.NE_PROGRESS.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return (int) (neJobDetails1.getNeProgress() - neJobDetails2.getNeProgress());
                }
            });
        } else if (JobConfigurationConstants.NE_STATUS.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeStatus().compareTo(neJobDetails2.getNeStatus());
                }
            });
        } else if (JobConfigurationConstants.NE_RESULT.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeResult().compareTo(neJobDetails2.getNeResult());
                }
            });
        } else if (JobConfigurationConstants.NE_START_DATE.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeStartDate().compareTo(neJobDetails2.getNeStartDate());
                }
            });
        } else if (JobConfigurationConstants.NE_END_DATE.equals(jobInput.getSortBy())) {
            Collections.sort(neJobList, new Comparator<NeJobDetails>() {
                @Override
                public int compare(final NeJobDetails neJobDetails1, final NeJobDetails neJobDetails2) {
                    return neJobDetails1.getNeEndDate().compareTo(neJobDetails2.getNeEndDate());
                }
            });
        }
    }

    /**
     * This class receives the NE Job IDs as comma separated String and converts it into List<Long> of NE Job IDs
     * 
     * @param String
     *            neJobIds
     * 
     * @return List<Long>
     * @exception NumberFormatException
     */
    public List<Long> getNEJobIdListforExport(final String neJobIds) {

        final List<Long> neJobIdList = new ArrayList<>();
        Long jobID = null;

        final String[] jobIds = neJobIds.split(JobModelConstants.neJobIdSeparator);
        for (final String jobId : jobIds) {
            try {
                jobID = Long.parseLong(jobId);
                neJobIdList.add(jobID);
            } catch (final NumberFormatException e) {
                LOGGER.error("Unable to convert the given jobId : {} into Number, because:", jobId, e);
            }
        }

        return neJobIdList;
    }

    /**
     * This class receives the NE Job IDs as comma separated String and converts it into List<Long> of NE Job IDs
     * 
     * @param JobLogRequest
     * @return Set<Long>
     * @exception NumberFormatException
     */
    public Set<Long> getNEJobIdList(final JobLogRequest jobLogRequest) {

        final String neJobIds = jobLogRequest.getNeJobIds();
        final Set<Long> setOfNeJobId = new HashSet<>();
        Long jobID = null;
        try {
            final String[] jobIds = neJobIds.split(JobModelConstants.neJobIdSeparator);
            for (final String jobId : jobIds) {
                jobID = Long.parseLong(jobId);
                setOfNeJobId.add(jobID);
            }
        } catch (final NumberFormatException numberFormatException) {
            LOGGER.error("Unable to parse jobId : {}", jobID);
        }
        return setOfNeJobId;
    }

    /**
     * This method takes care of sorting and pagination.
     * 
     * @param List
     *            <JobLogResponse> response
     * @param JobLogRequest
     *            jobLogInput
     * @return JobOutput
     */
    public JobOutput getJobLogResponse(final List<JobLogResponse> response, final JobLogRequest jobLogRequest) {
        final long checkingLogLevel = System.currentTimeMillis();
        final JobOutput jobOutput = new JobOutput();
        final List<JobLogResponse> jobLogList = new ArrayList<>();
        if (jobLogRequest.getLogLevel().equals("ERROR")) {
            for (final JobLogResponse jobLog : response) {
                if (jobLog.getLogLevel().equals("ERROR")) {
                    jobLogList.add(jobLog);
                }
            }
        } else if (jobLogRequest.getLogLevel().equals("WARNING")) {
            for (final JobLogResponse jobLog : response) {
                if (jobLog.getLogLevel().equals("ERROR") || jobLog.getLogLevel().equals("WARN")) {
                    jobLogList.add(jobLog);
                }
            }
        } else if (jobLogRequest.getLogLevel().equals("INFO")) {
            for (final JobLogResponse jobLog : response) {
                if (jobLog.getLogLevel().equals("ERROR") || jobLog.getLogLevel().equals("WARN") || jobLog.getLogLevel().equals("INFO")) {
                    jobLogList.add(jobLog);
                }
            }
        } else if (jobLogRequest.getLogLevel().equals("DEBUG")) {
            for (final JobLogResponse jobLog : response) {
                if (jobLog.getLogLevel().equals("ERROR") || jobLog.getLogLevel().equals("WARN") || jobLog.getLogLevel().equals("INFO") || jobLog.getLogLevel().equals("DEBUG")) {
                    jobLogList.add(jobLog);
                }
            }
        } else {
            for (final JobLogResponse jobLog : response) {
                jobLogList.add(jobLog);
            }
        }
        final long logLevelChecked = System.currentTimeMillis();
        LOGGER.debug("Time taken to check log level for retrieval of JobLogs :  {} milli seconds.", Math.abs(checkingLogLevel - logLevelChecked));
        if (jobLogRequest.getOrderBy().equals(OrderByEnum.asc)) {
            final long ascendingSorting = System.currentTimeMillis();
            ascendingJobList(jobLogList, jobLogRequest);
            final long ascendingSorted = System.currentTimeMillis();
            LOGGER.debug("Time taken to sort ascending for retrieval of JobLogs :  {} milli seconds.", Math.abs(ascendingSorting - ascendingSorted));
        } else if (jobLogRequest.getOrderBy().equals(OrderByEnum.desc)) {
            final long descendingSorting = System.currentTimeMillis();
            descendingJobList(jobLogList, jobLogRequest);
            final long descendingSorted = System.currentTimeMillis();
            LOGGER.debug("Time taken to sort descending for retrieval of JobLogs :  {} milli seconds.", Math.abs(descendingSorting - descendingSorted));
        }
        LOGGER.debug("Size of respone after sorting {}", jobLogList.size());
        jobOutput.setTotalCount(jobLogList.size());
        final long filteringStarts = System.currentTimeMillis();
        List<JobLogResponse> resultList = Collections.emptyList();
        int start = -1;
        int end = -1;
        if (jobLogRequest.getOffset() <= jobLogList.size()) {
            start = jobLogRequest.getOffset() - 1;
            if (jobLogRequest.getLimit() > jobLogList.size()) {
                end = jobLogList.size();
            } else {
                end = jobLogRequest.getLimit();
            }
        } else {
            if (jobLogRequest.getFilterDetails() != null) {
                start = 0;
                final int pageLimit = (jobLogRequest.getLimit() - jobLogRequest.getOffset()) + 1;
                end = (jobLogList.size() >= pageLimit) ? pageLimit : jobLogList.size();
            }
        }
        if (start != -1 && end != -1) {
            resultList = jobLogList.subList(start, end);
        }
        jobOutput.setResult(resultList);
        jobOutput.setClearOffset(FilterUtils.isClearOffsetRequired(jobLogList.size(), jobLogRequest.getOffset()));
        final long filteringEnds = System.currentTimeMillis();
        LOGGER.debug("Time taken to filter for retrieval of JobLogs :  {} milli seconds.", Math.abs(filteringStarts - filteringEnds));
        return jobOutput;
    }

    /**
     * This method takes care of sorting.
     * 
     * @param List
     *            <JobLogOutput> jobLogOutputList
     * @param JobLogRequest
     *            jobLogInput
     * @return void
     */
    private void ascendingJobList(final List<JobLogResponse> jobLogOutputList, final JobLogRequest jobLogInput) {

        for (final JobLogResponse joblogOutput : jobLogOutputList) {
            if (joblogOutput.getActivityName() == null) {
                joblogOutput.setActivityName("null");
            }
            if (joblogOutput.getEntryTime() == null) {
                joblogOutput.setEntryTime("null");
            }

            if (joblogOutput.getMessage() == null) {
                joblogOutput.setMessage("null");
            }
            if (joblogOutput.getNeName() == null) {
                joblogOutput.setNeName("null");
            }
            if (joblogOutput.getNodeType() == null) {
                joblogOutput.setNodeType("null");
            }
        }

        if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.neName)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput1.getNeName().compareTo(jobLogOutput2.getNeName());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.activityName)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput1.getActivityName().compareTo(jobLogOutput2.getActivityName());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.entryTime)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput1.getEntryTime().compareTo(jobLogOutput2.getEntryTime());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.message)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput1.getMessage().compareTo(jobLogOutput2.getMessage());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.nodeType)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput1.getNodeType().compareTo(jobLogOutput2.getNodeType());
                }
            });
        }
    }

    /**
     * This method takes care of pagination.
     * 
     * @param List
     *            <JobLogOutput> jobLogOutputList
     * @param JobLogRequest
     *            jobLogInput
     * @return void
     */
    private void descendingJobList(final List<JobLogResponse> jobLogOutputList, final JobLogRequest jobLogInput) {

        for (final JobLogResponse joblogOutput : jobLogOutputList) {
            if (joblogOutput.getActivityName() == null) {
                joblogOutput.setActivityName("null");
            }
            if (joblogOutput.getEntryTime() == null) {
                joblogOutput.setEntryTime("null");
            }
            if (joblogOutput.getMessage() == null) {
                joblogOutput.setMessage("null");
            }
            if (joblogOutput.getNeName() == null) {
                joblogOutput.setNeName("null");
            }
            if (joblogOutput.getNodeType() == null) {
                joblogOutput.setNodeType("null");
            }
        }
        if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.neName)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput2.getNeName().compareTo(jobLogOutput1.getNeName());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.activityName)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput2.getActivityName().compareTo(jobLogOutput1.getActivityName());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.entryTime)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput2.getEntryTime().compareTo(jobLogOutput1.getEntryTime());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.message)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput2.getMessage().compareTo(jobLogOutput1.getMessage());
                }
            });
        } else if (jobLogInput.getSortBy().equals(SHMJobUtilConstants.nodeType)) {
            Collections.sort(jobLogOutputList, new Comparator<JobLogResponse>() {
                @Override
                public int compare(final JobLogResponse jobLogOutput1, final JobLogResponse jobLogOutput2) {
                    return jobLogOutput2.getNodeType().compareTo(jobLogOutput1.getNodeType());
                }
            });
        }
    }

    /**
     * This method converts the date from client time zone to local time zone.
     * 
     * @param fromTimeZoneString
     * @param toTimeZoneString
     * @param fromDateTime
     * 
     * @return String
     */
    public static String convertTimeZones(final String fromTimeZoneString, final String toTimeZoneString, final String fromDateTime) {
        final DateTimeZone fromTimeZone = DateTimeZone.forID(fromTimeZoneString);
        final DateTimeZone toTimeZone = DateTimeZone.forID(toTimeZoneString);
        final DateTime dateTime = new DateTime(fromDateTime, fromTimeZone);
        final DateTimeFormatter outputFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'ZUTC'Z").withZone(toTimeZone);
        return outputFormatter.print(dateTime);
    }

    /**
     * This method converts the date from client time zone to local time zone taking date to be converted as input
     * 
     * @param fromTimeZoneString
     * @param toTimeZoneString
     * @param fromDateTime
     * @return
     */
    public static String convertTimeZones(final String fromTimeZoneString, final String toTimeZoneString, final Date fromDateTime) {
        final DateTimeZone fromTimeZone = DateTimeZone.forID(fromTimeZoneString);
        final DateTimeZone toTimeZone = DateTimeZone.forID(toTimeZoneString);
        final DateTime dateTime = new DateTime(fromDateTime, fromTimeZone);
        final DateTimeFormatter outputFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'ZUTC'Z").withZone(toTimeZone);
        return outputFormatter.print(dateTime);
    }

    /**
     * Delimiter is used to remove "GMT+0530" in date, received from SHM UI.
     * 
     * @param startTime
     * @return parsedDate
     */
    public static String getFormattedDate(String date) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = "";
        try {
            final String delims = " ";
            Date formattedTime = new Date();
            final StringTokenizer st = new StringTokenizer(date, delims);
            final String formattedScheduleTime = st.nextToken() + " " + st.nextToken();
            LOGGER.debug("formattedScheduleTime :{}", formattedScheduleTime);
            try {
                formattedTime = formatter.parse(formattedScheduleTime);
                LOGGER.debug("startTime or endTime :{}", formattedTime);
            } catch (final ParseException e) {
                LOGGER.error("Cannot parse date. due to :", e);
            }
            if (formattedTime != null) {
                formattedDate = String.valueOf(formattedTime.getTime());
            } else {
                formattedDate = "";
            }
            LOGGER.debug("parsedDate:{}", formattedDate);
        } catch (final StringIndexOutOfBoundsException e) {
            LOGGER.error("StartTime does not contain Time Zone info. StartTime : {}", date);
        }
        return formattedDate;
    }

    /**
     * For sorting jobs properly based on progress percentage we are setting progress percentage values like -2 for scheduled jobs and -1 for manual jobs.
     * 
     * @param shmJobData
     * @return shmJobData
     */

    private SHMJobData formatProgressForSorting(final SHMJobData shmJobData) {
        final String status = shmJobData.getStatus();

        if (status.equalsIgnoreCase(JobState.SCHEDULED.getJobStateName())) {
            shmJobData.setProgress(-2.0);

        } else if (status.equalsIgnoreCase(JobState.WAIT_FOR_USER_INPUT.getJobStateName())) {
            shmJobData.setProgress(-1.0);
        }

        return shmJobData;
    }
}
