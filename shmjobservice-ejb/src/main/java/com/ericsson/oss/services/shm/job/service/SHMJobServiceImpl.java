package com.ericsson.oss.services.shm.job.service;

import java.util.*;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EPredefinedRole;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.*;
import com.ericsson.oss.services.shm.common.constants.AccessControlConstants;
import com.ericsson.oss.services.shm.common.constants.ShmCommonConstants;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.BadRequestException;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.instrumentation.MainJobInstrumentation;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.entities.CancelResponse;
import com.ericsson.oss.services.shm.job.entities.DeleteResponse;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobInput;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.entities.OrderByEnum;
import com.ericsson.oss.services.shm.job.entities.ShmMainJobsResponse;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.utils.CsvBuilder;
import com.ericsson.oss.services.shm.job.utils.SHMJobUtil;
import com.ericsson.oss.services.shm.job.utils.ShmJobDataEnum;
import com.ericsson.oss.services.shm.job.utils.ShmJobLogsFilter;
import com.ericsson.oss.services.shm.job.utils.ShmJobsFilter;
import com.ericsson.oss.services.shm.job.utils.ShmMainJobsFilter;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityDetails;
import com.ericsson.oss.services.shm.jobs.common.api.ActivityJobDetail;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportData;
import com.ericsson.oss.services.shm.jobs.common.api.JobReportDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobDetails;
import com.ericsson.oss.services.shm.jobs.common.api.NeJobInput;
import com.ericsson.oss.services.shm.jobs.common.api.NetworkElementJobDetails;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.ExecMode;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobLogMapper;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobMapper;
import com.ericsson.oss.services.shm.jobs.common.modelentities.*;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJob;
import com.ericsson.oss.services.shm.jobs.entities.SHMMainJobDto;
import com.ericsson.oss.services.shm.jobservice.common.CommentInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobFactory;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.JobPropertyBuilder;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesInfo;
import com.ericsson.oss.services.shm.jobservice.common.NeTypesPlatformData;
import com.ericsson.oss.services.shm.jobservice.common.ShmJobHandler;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobConstants;
import com.ericsson.oss.services.shm.jobservice.constants.SHMJobUtilConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.shm.job.entities.SHMJobData;

/**
 * ServiceBean implementing Job related operations.
 */
@Stateless
@Traceable
@Profiled
@SuppressWarnings("PMD")
public class SHMJobServiceImpl implements SHMJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SHMJobServiceImpl.class);

    @Inject
    private JobTypeDetailsProviderFactory jobTypeDetailsProviderFactory;

    @Inject
    protected FdnServiceBeanRetryHelper networkElementsProvider;

    @Inject
    private JobConfigurationService jobConfigurationService;

    @Inject
    private DpsReader dpsReader;

    @Inject
    private ShmJobHandler shmJobHandler;

    @Inject
    private JobFactory jobFactory;

    @Inject
    private SHMJobUtil shmJobUtil;

    @Inject
    private JobExecutorLocal jobExecutorLocal;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private UserContextBean userContextBean;

    @Inject
    private JobMapper shmJobsMapper;

    @Inject
    private DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;

    @Inject
    private JobPropertyBuilder jobPropertyBuilder;

    @Inject
    private PoAttributesHolder poAttributesHolder;

    @Inject
    private FdnServiceBean fdnServiceBean;

    @Inject
    private JobParameterChangeListener jobParameterChangeListener;

    @Inject
    private RetryManager retryManager;

    @Inject
    private SHMJobServiceHelper shmJobServiceHelper;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobLogMapper jobLogMapper;

    @Inject
    private SHMJobServiceRetryProxy shmJobServiceRetryProxy;

    @Inject
    private JobConfigurationSummaryFactory jobConfigurationSummaryFactory;

    @Inject
    private SHMJobServiceImplHelper shmJobServiceImplHelper;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @EServiceRef
    private MainJobInstrumentation mainJobInstrumentation;

    @Inject
    private ActiveSessionsController activeRequestsController;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private JobsReader jobsReader;

    @Inject
    private ActivityTimeoutsService activityTimeoutsService;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private BatchParameterChangeListener batchParameterChangeListener;

    private static final List<String> requiredActivityJobAttributes = Arrays.asList(ShmConstants.ACTIVITY_NE_JOB_ID, ShmConstants.ACTIVITY_NAME, ShmConstants.ACTIVITY_RESULT,
            ShmConstants.ACTIVITY_END_DATE, ShmConstants.ACTIVITY_START_DATE, ShmConstants.ACTIVITY_ORDER, ShmConstants.ACTIVITY_NE_STATUS, ShmConstants.PROGRESSPERCENTAGE);

    private static final List<String> requiredNEJobAttributes = Arrays.asList(ShmConstants.NE_NAME, ShmConstants.NE_START_DATE, ShmConstants.NE_PROG_PERCENTAGE, ShmConstants.NE_RESULT,
            ShmConstants.NE_STATUS, ShmConstants.NE_END_DATE, ShmConstants.NETYPE);

    /**
     * @deprecated
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    @Deprecated
    public JobOutput getJobDetails(final JobInput jobInput) {

        activeRequestsController.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);

        try {
            final ShmJobs shmJobs = getJobs(jobInput);
            shmJobUtil.validateShmJobData(jobInput);
            List<SHMJobData> shmJobDataList = new ArrayList<>();

            if (shmJobs != null) {
                shmJobDataList = shmJobUtil.getJobDetailsList(shmJobs);
            }
            if (!(jobInput.getFilterDetails() == null || jobInput.getFilterDetails().isEmpty())) {
                filterShmJobs(shmJobDataList, jobInput);
            }
            final JobOutput jobOutput = shmJobUtil.sortAndGetPageData(shmJobDataList, jobInput);
            LOGGER.info("{} active sessions reduced to {} ", ActiveSessionsController.VIEW_MAIN_JOBS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
            return jobOutput;
        } catch (final Exception e) {
            LOGGER.info("{} active sessions reduced to {} ", ActiveSessionsController.VIEW_MAIN_JOBS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public ShmMainJobsResponse getShmMainJobs(final JobInput jobInput) {

        activeRequestsController.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_MAIN_JOBS);

        try {
            final List<SHMMainJobDto> mainJobData = jobsReader.getSHMJobData(jobInput);
            shmJobUtil.validateShmMainJobData(jobInput);

            if (jobInput.getFilterDetails() != null) {
                filterShmMainJobs(mainJobData, jobInput);
            }
            Collections.sort(mainJobData, ShmJobDataEnum.getShmJobComparator(jobInput.getSortBy(), jobInput.getOrderBy()));

            final List<SHMMainJob> mainJobDataList = new ArrayList<>();
            final List<? extends Object> mainJobDataSubList = getPageData(mainJobData, jobInput.getLimit(), jobInput.getOffset());
            for (final Object shmmainJobData : mainJobDataSubList) {
                mainJobDataList.add(new SHMMainJob((SHMMainJobDto) shmmainJobData));
            }
            final ShmMainJobsResponse shmMainJobsResponse = new ShmMainJobsResponse(mainJobData.size(), mainJobDataList,
                    FilterUtils.isClearOffsetRequired(mainJobData.size(), jobInput.getOffset()));

            LOGGER.info("Number of active sessions of {} reduced to {} ", ActiveSessionsController.VIEW_MAIN_JOBS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
            return shmMainJobsResponse;
        } catch (final Exception e) {
            LOGGER.info("Number of active sessions of {} reduced to {} ", ActiveSessionsController.VIEW_MAIN_JOBS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_MAIN_JOBS));
            throw e;
        }
    }

    private List<? extends Object> getPageData(final List<? extends Object> dtos, final int limit, final int offset) {
        int fromIndex = offset;
        if (offset - 1 >= dtos.size()) {
            fromIndex = 1;
        }
        final int toIndex = (fromIndex + limit) > dtos.size() ? dtos.size() : (fromIndex + limit - 1);

        return dtos.subList(fromIndex - 1, toIndex);
    }

    /*
     * filtering jobs based on filterDetails criteria.
     */
    public void filterShmMainJobs(final List<SHMMainJobDto> shmJobDataList, final JobInput jobInput) {

        final long startTime = System.currentTimeMillis();
        for (final Iterator<SHMMainJobDto> iterator = shmJobDataList.iterator(); iterator.hasNext();) {
            final boolean isFilterValueMatched = ShmMainJobsFilter.applyFilter(iterator.next(), jobInput.getFilterDetails());
            if (!isFilterValueMatched) {
                iterator.remove();
            }
        }
        LOGGER.debug("Size of ShmMainJobData after filtering {} and total time taken for filtering ShmMainJobData is: {} millis ", shmJobDataList.size(), System.currentTimeMillis() - startTime);

    }

    public void filterShmJobs(final List<SHMJobData> shmJobDataList, final JobInput jobInput) {

        final long startTime = System.currentTimeMillis();
        for (final Iterator<SHMJobData> iterator = shmJobDataList.iterator(); iterator.hasNext();) {
            final boolean isFilterValueMatched = ShmJobsFilter.applyFilter(iterator.next(), jobInput.getFilterDetails());
            if (!isFilterValueMatched) {
                iterator.remove();
            }
        }
        LOGGER.debug("Size of ShmJobData after filtering {} and total time taken for filtering : {} millis ", shmJobDataList.size(), System.currentTimeMillis() - startTime);

    }

    /**
     * @deprecated
     */
    @Deprecated
    private ShmJobs getJobs(final JobInput jobInput) {
        final StringBuilder timeConsumptionForJobRetrieval = new StringBuilder(ShmConstants.MAIN_JOB_RETRIEVAL);
        ShmJobs shmJobs = new ShmJobs();
        Map<Long, Map<String, Object>> jobConfigurationAttributesHolder = new HashMap<>();
        final List<Map<String, Object>> mainJobDetails = new ArrayList<>();
        final List<Long> jobTemplatePoIds = new ArrayList<>();
        final Map<Long, JobDetails> jobDetailsMap = new HashMap<>();
        shmJobs.setJobDetailsList(jobDetailsMap);

        final Date jobTemplateRetrievalStartTime = new Date();
        try {
            jobConfigurationAttributesHolder = retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<Map<Long, Map<String, Object>>>() {
                @Override
                protected Map<Long, Map<String, Object>> execute() throws Exception {
                    return poAttributesHolder.getTemplateJobDetails(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, jobInput);
                }
            });

        } catch (final RuntimeException ex) {
            LOGGER.error("Exception occurred while retrieving job configuration . Reason: ", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException("Exception occurred while retrieving JobTemplate", ex);
        }
        final Date jobTemplateRetrievalEndTime = new Date();

        for (final Long jobTemplateId : jobConfigurationAttributesHolder.keySet()) {
            jobTemplatePoIds.add(jobTemplateId);
        }
        final List<List<Long>> batchedTemplatePoIds = ListUtils.partition(jobTemplatePoIds, jobParameterChangeListener.getJobBatchSize());
        int countOfBatch = 0;
        final Date mainJobRetrievalStartTime = new Date();
        for (final List<Long> eachBatchOfTemplatePoIds : batchedTemplatePoIds) {
            try {
                final List<Map<String, Object>> response = retryManager.executeCommand(dpsRetryPolicies.getViewJobDetailsRetryPolicy(), new ShmDpsRetriableCommand<List<Map<String, Object>>>() {

                    @Override
                    protected List<Map<String, Object>> execute() throws Exception {
                        return poAttributesHolder.getMainJobDetails(ShmConstants.NAMESPACE, ShmConstants.JOB, eachBatchOfTemplatePoIds);
                    }
                });
                countOfBatch++;
                mainJobDetails.addAll(response);
            } catch (

            final RetriableCommandException retriableCommandException) {
                LOGGER.error("All retries exhausted while retrieving existing jobs. Reason: ", retriableCommandException);
                if (retriableCommandException.getCause() instanceof DatabaseNotAvailableException) {
                    throw new DatabaseNotAvailableException(retriableCommandException.getCause().getMessage());
                }
                throw new ServerInternalException("Exception occurred while retrieving Jobs. {}", retriableCommandException);
            }
        }
        final Date mainJobRetrievalEndTime = new Date();

        timeConsumptionForJobRetrieval
                .append(String.format(ShmConstants.TIME_CONSUMPTION_FOR_MAIN_JOBS_RETRIEVAL, countOfBatch, TimeSpendOnJob.getDifference(mainJobRetrievalEndTime, mainJobRetrievalStartTime)));

        systemRecorder.recordEvent(ShmConstants.JOB_RETRIEVAL_EVENT_TYPE, EventLevel.DETAILED, ShmConstants.SOURCE_FOR_JOBS_RETRIEVAL, ShmConstants.RESOURCE_FOR_JOBS_RETRIEVAL,
                timeConsumptionForJobRetrieval
                        .insert(0, String.format(ShmConstants.TIME_CONSUMPTION_FOR_JOBS_RETRIEVAL, TimeSpendOnJob.getDifference(jobTemplateRetrievalEndTime, jobTemplateRetrievalStartTime)))
                        .toString());

        if (mainJobDetails != null) {
            shmJobs = shmJobsMapper.getSHMJobsDetails(mainJobDetails, shmJobs);
        }
        if (jobConfigurationAttributesHolder != null) {
            shmJobs = shmJobsMapper.getJobConfigurationDetails(jobConfigurationAttributesHolder, shmJobs);
        }
        return shmJobs;
    }

    /**
     * This method populates the Job Configuration and creates Job.
     * 
     * @param jobInfo
     * @return Map<String, Object>
     */
    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.CREATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Object> createShmJob(final JobInfo jobInfo) throws NoMeFDNSProvidedException {
        return createJob(jobInfo);
    }

    private Map<String, Object> createJob(final JobInfo jobInfo) throws NoMeFDNSProvidedException {
        Map<String, Object> response = new HashMap<>();
        if (jobInfo != null) {
            List<Map<String, Object>> neNames = new ArrayList<>();
            List<String> savedSearchIds = new ArrayList<>();
            List<String> collectionIds = new ArrayList<>();
            final JobInfo shmJob = jobFactory.createJob(jobInfo);
            if (jobInfo.getNeNames() != null) {
                neNames = jobInfo.getNeNames();
            }
            final List<String> fdns = new ArrayList<>();

            if (neNames != null && !neNames.isEmpty()) {
                for (int i = 0; i < neNames.size(); i++) {
                    fdns.add((String) neNames.get(i).get(ShmConstants.NAME));
                }
            }
            // setting collectionids and saved searches if they exist
            if (jobInfo.getcollectionNames() != null && !jobInfo.getcollectionNames().isEmpty()) {
                collectionIds = jobInfo.getcollectionNames();
            }
            if (jobInfo.getSavedSearchIds() != null && !jobInfo.getSavedSearchIds().isEmpty()) {
                savedSearchIds = jobInfo.getSavedSearchIds();
            }

            checkCreateJobParams(fdns, collectionIds, savedSearchIds);

            for (final Map<String, Object> configurations : jobInfo.getConfigurations()) {
                jobPropertyBuilder.populateJobConfiguration(jobInfo, configurations);
            }
            shmJob.setcollectionNames(collectionIds);
            shmJob.setSavedSearchIds(savedSearchIds);
            shmJob.setFdns(fdns);
            shmJob.setJobProperties(jobInfo.getJobProperties());
            shmJob.setPlatformJobProperties(jobInfo.getPlatformJobProperties());
            shmJob.setNETypeJobProperties(jobInfo.getNETypeJobProperties());
            shmJob.setNeJobProperties(jobInfo.getNeJobProperties());
            shmJob.setNeTypeActivityJobProperties(jobInfo.getNeTypeActivityJobProperties());
            shmJob.setParentNeWithComponents(jobInfo.getParentNeWithComponents());
            shmJob.setNeTypeComponentActivityDetails(jobInfo.getNeTypeComponentActivityDetails());

            LOGGER.debug("Trying to persist job configuration data : {}", shmJob);
            try {
                response = shmJobHandler.populateAndPersistJobConfigurationData(shmJob);
            } catch (final RuntimeException ex) {
                LOGGER.error("Exception while persisting job with name : {}. Reason: ", shmJob.getName(), ex);
                dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
                throw ex;
            }
            LOGGER.info("Job Creation response {}", response);
            return response;
        } else {
            final String errorMsg = "Job Configuration is not provided";
            LOGGER.error(errorMsg);
            throw new BadRequestException(errorMsg);
        }
    }

    private void checkCreateJobParams(final List<String> meFdns, final List<String> collections, final List<String> savedSearches) throws NoMeFDNSProvidedException {
        if (meFdns.isEmpty() && collections.isEmpty() && savedSearches.isEmpty()) {
            LOGGER.error("No meFdns specified");
            throw new NoMeFDNSProvidedException("No meFdns specified");
        }
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public List<JobTemplate> getJobTemplates(final JobQuery jobConfigQuery) {
        return getTemplates(jobConfigQuery);
    }

    /* RBAC is not validated here. Caller has to take care of RBAC. */
    @Override
    public List<JobTemplate> getShmJobTemplates(final JobQuery jobConfigQuery) {
        return getTemplates(jobConfigQuery);
    }

    private List<JobTemplate> getTemplates(final JobQuery jobConfigQuery) {
        final List<JobTemplate> jobTemplatesList = new ArrayList<>();
        if (jobConfigQuery != null) {
            for (final Long poId : jobConfigQuery.getPoIds()) {
                try {
                    final PersistenceObject jobTemplatePO = dpsReader.findPOByPoId(poId);
                    if (jobTemplatePO != null) {
                        jobTemplatesList.add(shmJobsMapper.getJobTemplateDetails(jobTemplatePO.getAllAttributes(), poId));
                    } else {
                        LOGGER.debug("No data found for jobConfig Id: {}", poId);
                    }
                } catch (final RuntimeException ex) {
                    LOGGER.error("Exception occurred while retrieving job template  with PO Id : {}. Reason: ", poId, ex);
                    dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
                    // Do not re-throw the exception. Intentionally logged and swallowed because we don't want to fail the request if few out of many job retrievals are invalid.Should continue with
                    // next po retrieval .
                }
            }
        }
        return jobTemplatesList;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public Map<String, Object> cancelJobs(final List<Long> jobIds) {
        return cancelJobsCommon(jobIds);
    }

    @Override
    public JobOutput viewReportLogs(final JobLogRequest jobLogRequest, final List<Long> neJobIdList) {

        return viewJobLogs(jobLogRequest, neJobIdList);

    }

    /**
     * This method retrieves job log details from DPS
     * 
     * @param JobLogRequest
     * @return JobOutput
     */
    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.OPERATOR,
            EPredefinedRole.ADMINISTRATOR })
    public JobOutput retrieveJobLogs(final JobLogRequest jobLogRequest) {
        activeRequestsController.exitIfMaxActiveSessionsReached(ActiveSessionsController.VIEW_JOB_LOGS);
        try {
            final Set<Long> setOfNeJobId = shmJobUtil.getNEJobIdList(jobLogRequest);
            final JobOutput jobOutput = viewJobLogs(jobLogRequest, new ArrayList<>(setOfNeJobId));
            LOGGER.info("Number of active sessions of {} reduced to {} ", ActiveSessionsController.VIEW_JOB_LOGS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_JOB_LOGS));
            return jobOutput;
        } catch (final Exception e) {
            LOGGER.info("Number of active sessions of {} reduced to {} ", ActiveSessionsController.VIEW_JOB_LOGS, activeRequestsController.decrementAndGet(ActiveSessionsController.VIEW_JOB_LOGS));
            throw e;
        }
    }

    private JobOutput viewJobLogs(final JobLogRequest jobLogRequest, final List<Long> neJobIdList) {
        shmJobUtil.validateShmJobLog(jobLogRequest);
        LOGGER.debug("RetrieveJobLogs service layer method entry - NeJobIds are : {} and MainJobId {}", jobLogRequest.getNeJobIds(), jobLogRequest.getMainJobId());
        final long d1 = System.currentTimeMillis();
        boolean isOnlyMainJobIdInRequest = false;

        Map<Long, String> neJobs = new HashMap<>();
        final DataBucket liveBucket = getLiveBucket();
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = null;

        final long queryingNeJobs = System.currentTimeMillis();
        if (mainJobIdIsPresent(neJobIdList)) {
            isOnlyMainJobIdInRequest = true;
            typeQuery = setRestrictionToGetCorrespondingNeJobs(jobLogRequest.getMainJobId(), queryBuilder);
            neJobs = queryNeJobs(typeQuery, queryExecutor);
        } else {

            final List<List<Long>> batchedNeJobIds = ListUtils.partition(neJobIdList, jobParameterChangeListener.getJobBatchSize());
            for (final List<Long> eachBatchOfNeJobIds : batchedNeJobIds) {
                final Map<Long, String> batchedNeJobs = getNeJobDetails(eachBatchOfNeJobIds);
                neJobs.putAll(batchedNeJobs);
            }
        }
        final long neJobsQueried = System.currentTimeMillis();
        LOGGER.debug("Ne Jobs Size {}", neJobs.size());
        LOGGER.debug("Time taken to Query NeJobs for retrieval of JobLogs :  {} milli seconds.", Math.abs(queryingNeJobs - neJobsQueried));

        final long queryingActivityJobs = System.currentTimeMillis();
        final List<Long> neJobIds = new ArrayList<>();
        neJobIds.addAll(neJobs.keySet());
        final Map<Long, List<Map<String, Object>>> activityJobs = getActivityJobLogs(neJobIds, queryExecutor, queryBuilder);
        LOGGER.info("activityJobs Size {}", activityJobs.size());
        final long activityJobsQueried = System.currentTimeMillis();
        LOGGER.debug("Time taken to Query Activity for retrieval of JobLogs :  {} milli seconds.", Math.abs(queryingActivityJobs - activityJobsQueried));

        final long gettingJobLogResponseForAllNodes = System.currentTimeMillis();
        final List<JobLogResponse> response = getJobLogsInfo(activityJobs, neJobs, jobLogRequest.getMainJobId(), isOnlyMainJobIdInRequest);
        final long gotJobLogResponseForAllNodes = System.currentTimeMillis();
        LOGGER.debug("Time taken to get JobResponse for all nodes for retrieval of JobLogs :  {} milli seconds.", Math.abs(gettingJobLogResponseForAllNodes - gotJobLogResponseForAllNodes));

        final long checkingForInvalidNe = System.currentTimeMillis();
        // This check is required to find out if there is any neJobID for which neJob can't be fetched from DPS.
        if (!neJobIdList.isEmpty() && neJobIdList.size() != neJobs.keySet().size()) {
            for (final Long eachNeJobId : neJobIdList) {
                if (!neJobs.containsKey(eachNeJobId)) {
                    final NeJobLogDetails neJobLogDetails = new NeJobLogDetails();
                    neJobLogDetails.setError("NE Job not available with poId " + eachNeJobId);
                    final List<JobLogResponse> jobLogResponseForEveryUnavailableNode = jobLogMapper.getNEJobLogResponse(neJobLogDetails);
                    response.addAll(jobLogResponseForEveryUnavailableNode);
                }
            }
        }

        final long invalidNeChecked = System.currentTimeMillis();
        LOGGER.debug("Time taken to check invalid node for retrieval of JobLogs :  {} milli seconds.", Math.abs(checkingForInvalidNe - invalidNeChecked));

        if (!(jobLogRequest.getFilterDetails() == null || jobLogRequest.getFilterDetails().isEmpty())) {
            filterShmJobLogs(response, jobLogRequest);
        }

        final JobOutput jobOutput = shmJobUtil.getJobLogResponse(response, jobLogRequest);
        LOGGER.debug("RetrieveJobLogs service layer method exit - NeJobIds are : {} and jobOutput : {}", jobLogRequest.getNeJobIds(), jobOutput);
        final long d2 = System.currentTimeMillis();
        final long diff = Math.abs(d1 - d2);
        LOGGER.debug("Time taken to retrieve Job Logs :  {} milli seconds.", diff);
        return jobOutput;
    }

    /**
     * filtering job logs based on filterDetails Criteria.
     */
    public void filterShmJobLogs(final List<JobLogResponse> shmJobLogResponse, final JobLogRequest jobInput) {

        final long startTime = System.currentTimeMillis();
        for (final Iterator<JobLogResponse> iterator = shmJobLogResponse.iterator(); iterator.hasNext();) {
            final boolean isFilterValueMatched = ShmJobLogsFilter.applyFilter(iterator.next(), jobInput.getFilterDetails());
            if (!isFilterValueMatched) {
                iterator.remove();
            }
        }
        LOGGER.debug("Size of ShmJobData after filtering {} and total time taken for filtering : {} millis ", shmJobLogResponse.size(), System.currentTimeMillis() - startTime);

    }

    private boolean mainJobIdIsPresent(final List<Long> neJobIdList) {
        if (neJobIdList == null || neJobIdList.isEmpty()) {
            return true;
        }
        return false;
    }

    private com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> setRestrictionToGetCorrespondingNeJobs(final long mainJobId,
            final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder) {

        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE);
        final com.ericsson.oss.itpf.datalayer.dps.query.Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(ShmConstants.MAIN_JOB_ID, mainJobId);
        typeQuery.setRestriction(restriction);
        return typeQuery;
    }

    private Map<Long, String> queryNeJobs(final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery, final QueryExecutor queryExecutor) {
        List<Object[]> neJobProjector = new ArrayList<>();
        try {
            neJobProjector = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.NE_NAME));
        } catch (final RuntimeException runtimeException) {
            LOGGER.debug("Exception occurred while querying NE Jobs. Reason: ", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }

        final Map<Long, String> neJobDetail = new HashMap<>();
        for (final Object[] eachNeJobProjector : neJobProjector) {
            neJobDetail.put((Long) eachNeJobProjector[0], (String) eachNeJobProjector[1]);
        }
        return neJobDetail;

    }

    private List<Long> queryNeJobIds(final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery, final QueryExecutor queryExecutor) {
        List<Object[]> neJobProjector = new ArrayList<>();
        try {
            neJobProjector = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), ProjectionBuilder.attribute(ShmConstants.NE_NAME));
        } catch (final RuntimeException runtimeException) {
            LOGGER.debug("Exception occurred while querying NE Jobs. Reason: ", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }

        final List<Long> neJobIdList = new ArrayList<>();
        for (final Object[] eachNeJobProjector : neJobProjector) {
            neJobIdList.add((Long) eachNeJobProjector[0]);
        }
        return neJobIdList;

    }

    private Map<Long, List<Map<String, Object>>> getActivityJobLogs(final List<Long> neJobIds, final QueryExecutor queryExecutor,
            final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder) {
        final Map<Long, List<Map<String, Object>>> activityPoAttributes = new HashMap<>();
        final List<List<Long>> batchedNeJobIds = ListUtils.partition(neJobIds, batchParameterChangeListener.getJobDetailsQueryBatchSize());
        for (final List<Long> eachBatchOfNeJobIds : batchedNeJobIds) {
            activityPoAttributes.putAll(shmJobServiceRetryProxy.getActivityJobPoAttributes(queryExecutor, queryBuilder, eachBatchOfNeJobIds));
        }
        return activityPoAttributes;
    }

    private List<JobLogResponse> getJobLogsInfo(final Map<Long, List<Map<String, Object>>> activityJobs, final Map<Long, String> neJobs, final long mainJobId, final boolean isOnlyMainJobIdInRequest) {
        final List<JobLogResponse> jobLogResponseList = new ArrayList<>();

        final Set<Long> neJobIds = neJobs.keySet();
        final DataBucket liveBucket = getLiveBucket();
        JobLogDetails jobLogDetails = new JobLogDetails();

        for (final Long eachNeJobId : neJobIds) {
            String nodeType = null;
            final List<Map<String, Object>> eachGroupOfActivityJobs = activityJobs.get(eachNeJobId);
            final String nodeName = neJobs.get(eachNeJobId);
            final List<JobLogDetails> jobList = new ArrayList<>();

            if (eachGroupOfActivityJobs != null && !eachGroupOfActivityJobs.isEmpty()) {
                for (final Map<String, Object> activityJob : eachGroupOfActivityJobs) {
                    final long mappingEachActivity = System.currentTimeMillis();
                    if (activityJob != null && !activityJob.isEmpty()) {
                        jobLogDetails = jobLogMapper.mapJobAttributesToJobLogDetails(activityJob);
                        final long eachActivityMapped = System.currentTimeMillis();
                        LOGGER.debug("Time taken to Map each Activity for retrieval of JobLogs :  {} milli seconds.", Math.abs(mappingEachActivity - eachActivityMapped));
                        if (jobLogDetails != null) {
                            jobList.add(jobLogDetails);
                        }
                    }
                }
            }
            final PersistenceObject neJobPo = findPOByPoId(liveBucket, eachNeJobId);
            if (neJobPo != null) {
                final Map<String, Object> nejobPoAttributes = neJobPo.getAllAttributes();
                nodeType = (String) nejobPoAttributes.get(ShmCommonConstants.NETYPE);
                jobLogDetails = jobLogMapper.mapJobAttributesToJobLogDetails(nejobPoAttributes);
                jobList.add(jobLogDetails);
            }
            final NeJobLogDetails neJobLogDetails = jobLogMapper.mapNEJobLogDetailsFromJobLogDetails(jobList, nodeName, nodeType);

            final long mappingNeToJobLog = System.currentTimeMillis();
            final List<JobLogResponse> jobLogResponseForEachNode = jobLogMapper.getNEJobLogResponse(neJobLogDetails);
            final long neToJobLogMapped = System.currentTimeMillis();
            LOGGER.debug("Time taken to get JobResponse for retrieval of NE and Activity JobLogs :  {} milli seconds.", Math.abs(mappingNeToJobLog - neToJobLogMapped));
            jobLogResponseList.addAll(jobLogResponseForEachNode);
        }
        if (isOnlyMainJobIdInRequest) {
            final List<JobLogResponse> mainJobLogRespose = getMainJobLogs(mainJobId, liveBucket);
            jobLogResponseList.addAll(mainJobLogRespose);
        }
        return jobLogResponseList;
    }

    private List<JobLogResponse> getMainJobLogs(final long mainJobId, final DataBucket liveBucket) {
        MainJobLogDetails mainJobLogDetails = new MainJobLogDetails();
        final PersistenceObject mainJobPo = findPOByPoId(liveBucket, mainJobId);
        if (mainJobPo != null) {
            final List<JobLogDetails> mainJobList = new ArrayList<>();
            final Map<String, Object> mainJobPoAttributes = mainJobPo.getAllAttributes();
            final JobLogDetails jobLogDetails = jobLogMapper.mapJobAttributesToJobLogDetails(mainJobPoAttributes);
            mainJobList.add(jobLogDetails);
            mainJobLogDetails = jobLogMapper.mapMainJobLogDetailsFromJobLogDetails(mainJobList);
        }
        final long mappingNeToJobLog = System.currentTimeMillis();
        final List<JobLogResponse> jobLogResponseForMainJob = jobLogMapper.getMainJobLogResponse(mainJobLogDetails);
        final long neToJobLogMapped = System.currentTimeMillis();
        LOGGER.debug("Time taken to get JobResponse for retrieval of Main JobLogs :  {} milli seconds.", Math.abs(mappingNeToJobLog - neToJobLogMapped));
        return jobLogResponseForMainJob;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.OPERATOR,
            EPredefinedRole.ADMINISTRATOR })
    public List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> getJobProgressDetails(final List<Long> poIds) {
        final List<com.ericsson.oss.services.shm.jobs.common.modelentities.Job> jobs = new ArrayList<>();
        if (poIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final List<PersistenceObject> jobPos = dpsReader.findPOsByPoIds(poIds);
            if (!jobPos.isEmpty()) {
                for (final PersistenceObject jobPo : jobPos) {
                    final com.ericsson.oss.services.shm.jobs.common.modelentities.Job job = new com.ericsson.oss.services.shm.jobs.common.modelentities.Job();
                    final Map<String, Object> jobPoAttributes = jobPo.getAllAttributes();
                    final double progressPercentage = (double) jobPoAttributes.get(ShmConstants.PROGRESSPERCENTAGE);
                    job.setProgressPercentage(progressPercentage);
                    jobs.add(job);
                }
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception occurred while retrieving job progress details for PO ids: {}. Reason: ", poIds, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        return jobs;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.OPERATOR,
            EPredefinedRole.ADMINISTRATOR })
    public JobReportData getJobReportDetails(final NeJobInput jobInput) {
        shmJobUtil.validateShmJobDetail(jobInput);
        JobReportData jobReportData = new JobReportData();
        NeDetails neDetails = null;
        JobReportDetails jobReportDetails = new JobReportDetails();
        Map<String, Object> mainJobAttributes = null;
        final DataBucket liveBucket = getLiveBucket();
        final List<Long> jobIdsList = jobInput.getJobIdsList();
        if (jobInput == null || jobIdsList == null || jobIdsList.isEmpty()) {
            LOGGER.info("Inavlid Request{{}} recieved, so returning jobReportData as {}", jobInput, jobReportData);
            return jobReportData;
        }
        // Retrieving Main job details
        try {
            if (!jobIdsList.isEmpty()) {
                mainJobAttributes = jobsReader.getMainJob(jobIdsList.get(0));
            }
            if (mainJobAttributes == null || mainJobAttributes.isEmpty()) {
                LOGGER.info("No Main Job Found for the request {}", jobIdsList.get(0));
                return jobReportData;
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving main job PO with job Id : {}. Reason: ", jobIdsList.get(0), ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        PersistenceObject jobTemplatePo = null;
        try {
            jobTemplatePo = findPOByPoId(liveBucket, (Long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID));
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving job tepmlate PO with template Id : {}. Reason: ", mainJobAttributes.get(ShmConstants.JOBTEMPLATEID), ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        List<Map<String, Object>> matchedNeJobAttributes = new ArrayList<>();
        final long mainJobPoId = (Long) mainJobAttributes.get(ShmJobConstants.MAINJOBID);
        int neJobSize = 0;
        try {
            matchedNeJobAttributes = findNeJobAttributes(liveBucket, mainJobPoId);
            neJobSize = matchedNeJobAttributes.size();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving NE job POs of main job Id : {}. Reason: ", mainJobPoId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }

        final List<Long> neJobPoIds = new ArrayList<>();
        if (neJobSize > 0) {
            for (final Map<String, Object> neJobAttributes : matchedNeJobAttributes) {
                neJobPoIds.add((Long) neJobAttributes.get(ShmConstants.PO_ID));
            }
        }
        Map<Long, List<ActivityJobDetail>> activityResponse = new HashMap<>();
        try {
            activityResponse = getActivityResponse(neJobPoIds);
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving Activity Job attributes: {}. Reason: ", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        if (jobTemplatePo != null) {
            jobReportData = new JobReportData();
            jobReportDetails = shmJobsMapper.getMainJobDeatils(mainJobAttributes, jobTemplatePo.getAllAttributes());
            jobReportData.setJobReportDetails(jobReportDetails);
        }
        if (neJobSize > 0) {
            final Map<String, Object> argumentMap = new HashMap<>();
            argumentMap.put(ShmConstants.MATCHED_NE_JOB_ATTRIBUTES, matchedNeJobAttributes);
            argumentMap.put(ShmConstants.ACTIVITY_RESPONSE_LIST, activityResponse);
            argumentMap.put(ShmConstants.JOBINPUT, jobInput);
            neDetails = shmJobsMapper.getJobReportRefined(mainJobAttributes, jobReportDetails.getJobType(), argumentMap);
            jobReportData.setNeDetails(neDetails);
        } else {
            neDetails = new NeDetails();
            final List<NeJobDetails> neJobDetails = Collections.emptyList();
            final List<Map<String, Object>> neDetailsWithCustomColumns = Collections.emptyList();
            neDetails.setTotalCount(0);
            neDetails.setResult(neJobDetails);
            neDetails.setNeDetailsWithCustomColumns(neDetailsWithCustomColumns);
            jobReportData.setNeDetails(neDetails);
        }
        return jobReportData;
    }

    /**
     * @param queryBuilder
     * @param queryExecutor
     * @param neJobPoIds
     * @return
     */
    private Map<Long, List<ActivityJobDetail>> getActivityResponse(final List<Long> neJobPoIds) {
        final Map<Long, List<ActivityJobDetail>> activityPoAttributes = new HashMap<>();
        final List<List<Long>> batchedNeJobIds = ListUtils.partition(neJobPoIds, batchParameterChangeListener.getJobDetailsQueryBatchSize());
        for (final List<Long> eachBatchOfNeJobIds : batchedNeJobIds) {
            activityPoAttributes.putAll(shmJobServiceRetryProxy.getActivityJobAttributesIncludingLastLogMessage(eachBatchOfNeJobIds));
        }
        return activityPoAttributes;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.OPERATOR,
            EPredefinedRole.ADMINISTRATOR })
    @SuppressWarnings("unchecked")
    public NetworkElementJobDetails getNodeActivityDetails(final Long neJobId) {

        final NetworkElementJobDetails networkElementJobDetails = new NetworkElementJobDetails();
        final DataBucket liveBucket = getLiveBucket();
        List<Map<String, Object>> activityResponseList = new ArrayList<>();
        List<Map<String, String>> jobConfigurationDetails = new LinkedList<>();
        try {
            if (neJobId != null) {
                activityResponseList = getActivityJobs(liveBucket, neJobId);
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving main job PO with job Id : {}. Reason: ", neJobId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        if (!activityResponseList.isEmpty()) {
            LOGGER.info("activityResponseList {} ", activityResponseList);
            final List<ActivityDetails> activityDetails = shmJobsMapper.getJobActivityDetails(activityResponseList, neJobId);
            for (final Map<String, Object> jobConfigurationMap : activityResponseList) {
                jobConfigurationDetails = (List<Map<String, String>>) jobConfigurationMap.get(SHMJobConstants.JOB_CONFIGURATION);
            }
            networkElementJobDetails.setJobConfigurationDetails(jobConfigurationDetails);
            networkElementJobDetails.setActivityDetails(activityDetails);

        }
        LOGGER.info("networkElementJobDetails {} ", networkElementJobDetails);
        return networkElementJobDetails;
    }

    /**
     * @param liveBucket
     * @param neJobId
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getActivityJobs(final DataBucket liveBucket, final Long neJobId) {
        final List<Map<String, Object>> activityResponseList = new ArrayList<>();
        try {
            final List<Map<String, Object>> matchedActivityJobs = findProjectionAttributes(liveBucket, ShmConstants.NAMESPACE, ShmConstants.ACTIVITYJOB_TYPE, neJobId);
            String jobActivityName = null;
            final PersistenceObject neJobPO = findPOByPoId(liveBucket, neJobId);
            final PersistenceObject mainJobPO = findPOByPoId(liveBucket, (long) neJobPO.getAttribute(ShmConstants.MAIN_JOB_ID));
            final PersistenceObject jobTemplatePo = findPOByPoId(liveBucket, (Long) mainJobPO.getAttribute(ShmConstants.JOBTEMPLATEID));
            final JobType jobType = JobType.getJobType((String) jobTemplatePo.getAttribute(ShmConstants.JOB_TYPE));
            final Map<String, Object> jobConfiguration = jobTemplatePo.getAttribute(ShmConstants.JOBCONFIGURATIONDETAILS);
            final List<Map<String, Object>> activityListFromJobConfig = (List<Map<String, Object>>) jobConfiguration.get(ShmConstants.JOB_ACTIVITIES);
            final List<Map<String, Object>> exeModes = new ArrayList<>();
            final Map<String, Object> activityConfiguration = shmJobsMapper.prepareNeJobPropertiesMap(jobConfiguration);
            final List<Map<String, String>> jobConfigurationDetailsList = getJobConfigurationDetails(neJobId, jobConfiguration, jobTemplatePo);
            final Object neStatus = neJobPO.getAttribute(ShmConstants.NE_STATUS);

            String neName = neJobPO.getAttribute(ShmConstants.NE_NAME);
            final List<Map<String, String>> jobProperties = neJobPO.getAttribute(ShmJobConstants.JOBPROPERTIES);
            final String parentNodeName = getParentNodeName(jobProperties);
            if (parentNodeName != null) {
                neName = parentNodeName;
            }

            final JobConfigurationSummary jobConfigurationSummaryProvider = jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(jobType.getJobTypeName());
            final List<NetworkElement> networkElements = jobConfigurationSummaryProvider.getNetworkElementsByNeNames(Arrays.asList(neName),
                    jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType.name())));

            String neType = "";
            if (networkElements != null && !networkElements.isEmpty()) {
                neType = networkElements.get(0).getNeType();

            }
            for (final Map<String, Object> matchedActivityJob : matchedActivityJobs) {
                matchedActivityJob.put(ShmConstants.ACTIVITY_CONFIGURATION, activityConfiguration);
                matchedActivityJob.put(ShmConstants.NE_JOB_STATE, neStatus);
                matchedActivityJob.put(ShmConstants.ACTIVITY_NE_STATUS, matchedActivityJob.get(ShmConstants.ACTIVITY_NE_STATUS));
                matchedActivityJob.put(SHMJobConstants.JOB_CONFIGURATION, jobConfigurationDetailsList);
                for (final Map<String, Object> activitySchedule : activityListFromJobConfig) {
                    jobActivityName = (String) activitySchedule.get(ShmConstants.ACTIVITY_NAME);
                    if (jobActivityName.equals(matchedActivityJob.get(ShmConstants.ACTIVITY_NAME)) && activitySchedule.get(ShmConstants.NETYPE).equals(neType)) {
                        final String exeMode = (String) ((Map<String, Object>) activitySchedule.get(ShmConstants.ACTIVITY_SCHEDULE)).get(ShmConstants.EXECUTION_MODE);
                        matchedActivityJob.put(ShmConstants.EXECUTION_MODE, exeMode);
                        if (exeMode.equals(ExecMode.SCHEDULED.toString())) {
                            matchedActivityJob.put(ShmConstants.START_DATE, getActivityScheduleTime(activitySchedule));
                        }
                        exeModes.add(matchedActivityJob);
                        break;
                    }
                }
            }
            LOGGER.debug("matchedActivityJobs size data in getActivityJobs:{},execu:{}", matchedActivityJobs.size(), exeModes.size());
            activityResponseList.addAll(exeModes);
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving Activity job POs. Reason:{} ", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
        }
        LOGGER.debug("activityResponseList size data in getActivityJobs:{}", activityResponseList.size());
        return activityResponseList;
    }

    @SuppressWarnings("unchecked")
    private Object getActivityScheduleTime(final Map<String, Object> activitySchedule) {
        final Map<String, Object> schedules = (Map<String, Object>) activitySchedule.get(ShmConstants.ACTIVITY_SCHEDULE);
        if (schedules != null && !schedules.isEmpty()) {
            final List<Map<String, Object>> scheduleAttributes = (List<Map<String, Object>>) schedules.get(ShmConstants.SCHEDULINGPROPERTIES);
            if (scheduleAttributes != null) {
                for (final Map<String, Object> scheduleProperty : scheduleAttributes) {
                    if (scheduleProperty.get(ShmConstants.NAME).equals(ShmConstants.START_DATE) && scheduleProperty.get(ShmConstants.VALUE) != null) {
                        return scheduleProperty.get(ShmConstants.VALUE);
                    }
                }
            }
        }
        return null;
    }

    private DataBucket getLiveBucket() {
        try {
            return dataPersistenceService.getLiveBucket();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving live bucket. Reason : {}", ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw new ServerInternalException("Exception while reading the inventory. Please try again.");
        }
    }

    private PersistenceObject findPOByPoId(final DataBucket liveBucket, final long poId) {
        return liveBucket.findPoById(poId);
    }

    /**
     * This method is used to fetch the required neJob Attributes
     * 
     * @param liveBucket
     * @param mainJobId
     * @return neJobAttributes
     */
    private List<Map<String, Object>> findNeJobAttributes(final DataBucket liveBucket, final Long mainJobId) {
        final List<PersistenceObject> neJobPosList = findNEJobsByMainJobId(liveBucket, mainJobId);
        final List<Map<String, Object>> neJobAttributes = new ArrayList<>();
        for (final PersistenceObject persistenceObject : neJobPosList) {
            final Map<String, Object> attributes = persistenceObject.getAttributes(requiredNEJobAttributes);
            attributes.put(ShmConstants.PO_ID, persistenceObject.getPoId());
            if (persistenceObject.getAttribute(ShmConstants.LAST_LOG_MESSAGE) != null) {
                attributes.put(ShmConstants.LAST_LOG_MESSAGE, persistenceObject.getAttribute(ShmConstants.LAST_LOG_MESSAGE));
            } else if (JobState.isJobInactive(JobState.getJobState((String) attributes.get(ShmConstants.ACTIVITY_NE_STATUS)))) {
                attributes.put(ShmConstants.LOG, persistenceObject.getAttribute(ShmConstants.LOG));
            }
            neJobAttributes.add(attributes);
        }
        return neJobAttributes;
    }

    /**
     * This method is used to fetch the neJobPos based on mainJobId
     * 
     * @param liveBucket
     * @param mainJobId
     * @return List<PersistenceObject>
     */
    private List<PersistenceObject> findNEJobsByMainJobId(final DataBucket liveBucket, final Long mainJobId) {
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE);
        final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.MAIN_JOB_ID, mainJobId);
        typeQuery.setRestriction(restriction);
        List<PersistenceObject> neJobsPosList = new ArrayList<PersistenceObject>();
        try {
            neJobsPosList = queryExecutor.getResultList(typeQuery);
        } catch (final RuntimeException runtimeException) {
            LOGGER.debug("Exception occurred while querying Ne Jobs. Reason: ", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }
        return neJobsPosList;
    }

    /**
     * @param liveBucket
     * @param typeQuery
     * @return
     */
    private List<Map<String, Object>> getProjectedAttributes(final DataBucket liveBucket, final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery,
            final List<String> requiredAttributes) {
        LOGGER.debug("getProjectedAttributes fetching start");
        int projectionIndex = 0;
        final com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection attributesArray[] = new Projection[requiredAttributes.size()];
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        for (final String attribute : requiredAttributes) {
            attributesArray[projectionIndex] = ProjectionBuilder.attribute(attribute);
            projectionIndex++;
        }
        final List<Object[]> datbaseEntries = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID), attributesArray);
        final List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        for (final Object[] poObject : datbaseEntries) {
            final Map<String, Object> projectedAttributesResponse = new HashMap<String, Object>();
            projectedAttributesResponse.put(ShmConstants.PO_ID, poObject[0]);
            for (int attributeIndex = 1; attributeIndex < poObject.length; attributeIndex++) {
                projectedAttributesResponse.put(attributesArray[attributeIndex - 1].getProjectionValue(), poObject[attributeIndex]);
            }
            response.add(projectedAttributesResponse);
        }
        LOGGER.debug("getProjectedAttributes fetching end:{}", response.size());
        return response;
    }

    private List<Map<String, Object>> findProjectionAttributes(final DataBucket liveBucket, final String namespace, final String type, final Long neJobId) {
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(namespace, type);
        final com.ericsson.oss.itpf.datalayer.dps.query.Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId);
        typeQuery.setRestriction(restriction);
        return getProjectedAttributes(liveBucket, typeQuery, requiredActivityJobAttributes);
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.DELETE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, String> deleteShmJobs(final List<String> jobPoIdsToDelete) {
        return deleteJobs(jobPoIdsToDelete);
    }

    /**
     * This method retrieves the PO(Main Job ID) the response containing comment status for Job
     * 
     * @param commentInfo
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> addJobComment(final CommentInfo commentInfo) {
        final String jobPOId = commentInfo.getJobId();
        LOGGER.debug("The jobPOId is for adding comment {} ", jobPOId);
        final Map<String, Object> latestCommentMap = new HashMap<>();
        if (!"".equals(jobPOId) && jobPOId != null) {
            final Date date = new Date();
            final Map<String, Object> mainJobCommentMap = new HashMap<>();
            List<Map<String, Object>> commentListToBePersisted = new ArrayList<Map<String, Object>>();
            final Map<String, Object> jobAttributes = new HashMap<String, Object>();
            final long mainJobId = Long.parseLong(jobPOId);

            final String comment = commentInfo.getComment();
            final String userName = userContextBean.getLoggedInUserName();
            mainJobCommentMap.put(ShmConstants.USERNAME, userName);
            mainJobCommentMap.put(ShmConstants.COMMENT, comment);
            mainJobCommentMap.put(ShmConstants.DATE, date);

            try {
                final PersistenceObject mainJobPoForPersistence = dpsReader.findPOByPoId(mainJobId);
                final Map<String, Object> mainJobAttributesForPersistence = mainJobPoForPersistence.getAllAttributes();
                if (mainJobAttributesForPersistence.get(JobModelConstants.JOB_COMMENT) != null) {
                    commentListToBePersisted = (List<Map<String, Object>>) mainJobAttributesForPersistence.get(JobModelConstants.JOB_COMMENT);
                }
                commentListToBePersisted.add(mainJobCommentMap);

                jobAttributes.put(JobModelConstants.JOB_COMMENT, commentListToBePersisted);
            } catch (final RuntimeException ex) {
                LOGGER.error("Failed to retrieve job comments for the main job id : {}. Reason : ", mainJobId, ex);
                dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
                throw ex;
            }
            if (jobAttributes.size() > 0) {
                try {
                    // Updating the comment into the DPS.
                    dpsWriter.update(mainJobId, jobAttributes);
                } catch (final RuntimeException ex) {
                    LOGGER.error("Failed to update job comments for the main job id : {}. Reason : ", mainJobId, ex);
                    dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
                    throw ex;
                }
            }

            final String formattedDate = String.valueOf(date.getTime());

            latestCommentMap.put(ShmConstants.USERNAME, userName);
            latestCommentMap.put(ShmConstants.COMMENT, comment);
            latestCommentMap.put(ShmConstants.DATE, formattedDate);
        } else {
            LOGGER.error("No jobPOId found{}", jobPOId);
        }
        return latestCommentMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.OPERATOR,
            EPredefinedRole.ADMINISTRATOR })
    public String exportJobLogs(final ExportJobLogRequest jobLogRequestForExport) {
        return exportLogs(jobLogRequestForExport);
    }

    @Override
    public String exportLogs(final ExportJobLogRequest jobLogRequestForExport) {

        final DateTime d1 = DateTime.now();
        String csvOutput = null;
        final Long neJobId = jobLogRequestForExport.getNeJobIds();
        if (neJobId == null) {
            LOGGER.warn("Logs cannot be retrieved as the neJobs list is null");
            return "Logs cannot be retrieved as the neJobs list is null";
        } else {
            try {
                final JobLogRequest jobLogRequest = new JobLogRequest();
                jobLogRequest.setNeJobIds(neJobId.toString());
                jobLogRequest.setOffset(1);
                jobLogRequest.setOrderBy(OrderByEnum.desc);
                jobLogRequest.setSortBy(SHMJobUtilConstants.entryTime);
                jobLogRequest.setLimit(Integer.MAX_VALUE);
                jobLogRequest.setLogLevel("DEBUG");// DEBUG level is set to get total logs of a job.

                final JobOutput retrievedjoblogs = retrieveJobLogs(jobLogRequest);
                final List<String[]> logsAsStringArray = formatLogsAsArray(jobLogRequestForExport, (List<JobLogResponse>) retrievedjoblogs.getResult());
                csvOutput = CsvBuilder.constructCsv(logsAsStringArray);
            } catch (final NumberFormatException exception) {
                LOGGER.error("Internal Error found due to {}", exception);
            }
            final DateTime d2 = DateTime.now();
            final Interval duration = new Interval(d1, d2);
            systemRecorder.recordEvent("time taken to export" + "_" + duration.toDurationMillis(), EventLevel.DETAILED, "", "", "");
            return csvOutput;
        }

    }

    /**
     * @param jobLogRequestForExport
     * @param entryTime
     * @param entryTimeInDate
     * @param logsAsStringArray
     * @param retrievedjoblogs
     */
    private List<String[]> formatLogsAsArray(final ExportJobLogRequest jobLogRequestForExport, final List<JobLogResponse> logs) {
        final List<String[]> logsAsStringArray = new ArrayList<String[]>();
        String jobName = null;
        String jobType = null;
        String neName = null;
        String activityName = null;
        String entryTimeInString = null;
        String entryTime = null;
        String message = null;
        Date entryTimeInDate = null;
        String logLevel = null;
        if (logs != null) {
            for (final JobLogResponse map : logs) {
                jobName = jobLogRequestForExport.getJobName();
                jobType = jobLogRequestForExport.getJobType();
                neName = map.getNeName();
                activityName = map.getActivityName();
                entryTimeInString = map.getEntryTime();
                if (entryTimeInString != null) {
                    final long entryTimeInLong = Long.parseLong(entryTimeInString);
                    entryTimeInDate = new Date(entryTimeInLong);
                    entryTime = DateTimeUtils.format(entryTimeInDate);
                }

                message = map.getMessage();
                logLevel = map.getLogLevel();
                final JobLogExport jobLogExport = new JobLogExport(entryTimeInDate, message, neName, map.getNodeType(), activityName, logLevel);
                jobLogExport.setJobName(jobName);
                jobLogExport.setJobType(jobType);
                logsAsStringArray.add(new String[] { jobLogExport.getJobName(), jobLogExport.getJobType(), jobLogExport.getLogLevel(), jobLogExport.getNeName(), jobLogExport.getNodeType(),
                        jobLogExport.getActivityName(), entryTime, jobLogExport.getMessage() });
            }
        }
        return logsAsStringArray;
    }

    /*
     * 
     * This method retrieves the template job details in a map
     */
    @Override
    public JobTemplate getJobTemplateByNeJobId(final Long neJobId) {
        Long mainJobId = null;
        Long templateJobId = null;

        try {
            // Retrieving Main Job Id
            final Map<String, Object> neJobAttributes = dpsReader.findPOByPoId(neJobId).getAllAttributes();
            mainJobId = (long) neJobAttributes.get("mainJobId");
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving NE job attributes for NE job Id : {}, exception : {}", neJobId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        try {
            // Retrieving Template Job Id
            final Map<String, Object> mainJobAttributes = dpsReader.findPOByPoId(mainJobId).getAllAttributes();
            templateJobId = (long) mainJobAttributes.get("templateJobId");
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving main job attributes for main job Id : {}, exception : {}", neJobId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        try {
            // Retrieve Template Job Details
            final Map<String, Object> templateJobAttributeMap = dpsReader.findPOByPoId(templateJobId).getAllAttributes();
            return shmJobsMapper.getJobTemplateDetails(templateJobAttributeMap, templateJobId);
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving job template attributes for template job Id : {}, exception : {}", templateJobId, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
    }

    @Override
    public JobTemplate getJobTemplate(final String name) {

        final Map<String, Object> attributesMap = new HashMap<String, Object>();
        attributesMap.put("name", name);
        try {
            final List<PersistenceObject> jobTemplates = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOBTEMPLATE, attributesMap);
            if (!jobTemplates.isEmpty()) {

                return shmJobsMapper.getJobTemplateDetails(jobTemplates.get(0).getAllAttributes(), jobTemplates.get(0).getPoId());
            }
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving job template PO for job name : {}, exception : {}", name, ex);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(ex);
            throw ex;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.job.service.SHMJobService#getStartTimeForImmediateAndManualJobs(java.lang.Long)
     */
    @Override
    public String getJobStartTime(final Long templateJobId) {
        PersistenceObject jobPo = null;
        String startTime = null;
        final Map<String, Object> jobAttributesRestrictionMap = new HashMap<String, Object>();
        jobAttributesRestrictionMap.put(ShmConstants.JOBTEMPLATEID, templateJobId);

        final List<PersistenceObject> matchedJobPos = dpsReader.findPOs(ShmConstants.NAMESPACE, ShmConstants.JOB, jobAttributesRestrictionMap);
        // For retrieving the Job data
        if (matchedJobPos != null && matchedJobPos.size() > 0) {
            jobPo = matchedJobPos.get(0);
        }
        // To get the startTime of the job
        if (jobPo != null) {
            if (jobPo.getAllAttributes().get(ShmConstants.STARTTIME) != null) {
                final Date startTimeFromPO = (Date) jobPo.getAllAttributes().get(ShmConstants.STARTTIME);
                startTime = String.valueOf(startTimeFromPO.getTime());
            }
        }
        return startTime;
    }

    /**
     * Retrieves job comments for the provided main job Id.
     * 
     * @param mainJobId
     * @return List<JobComment> - List of JobComment as a JSON object
     */
    @Override
    public List<JobComment> retrieveJobComments(final Long mainJobId) {

        List<Map<String, Object>> jobCommentObjects = null;
        final List<JobComment> jobComments = new ArrayList<JobComment>();

        final PersistenceObject mainJobPersistenceObject = getLiveBucket().findPoById(mainJobId);
        if (mainJobPersistenceObject != null) {
            jobCommentObjects = mainJobPersistenceObject.getAttribute(JobModelConstants.JOB_COMMENT);
        }

        if (jobCommentObjects != null) {
            for (final Map<String, Object> jobCommentMap : jobCommentObjects) {

                final JobComment jobComment = new JobComment((String) jobCommentMap.get(ShmConstants.USERNAME), (String) jobCommentMap.get(ShmConstants.COMMENT),
                        (Date) jobCommentMap.get(ShmConstants.DATE));
                jobComments.add(jobComment);
            }
        }

        LOGGER.debug("Number of Job Comments for main Job {} : {}", mainJobId, jobComments.size());
        return jobComments;
    }

    private List<Map<String, String>> getJobConfigurationDetails(final Long neJobId, final Map<String, Object> jobConfiguration, final PersistenceObject jobTemplatePo) {
        List<Map<String, String>> jobConfigurationDetails = new LinkedList<>();
        PlatformTypeEnum platformType = null;
        String neType = null;
        final Map<String, Object> neJobAttributes = jobConfigurationService.retrieveJob(neJobId);
        jobConfiguration.put(ShmJobConstants.JOBPROPERTIES, neJobAttributes.get(ShmJobConstants.JOBPROPERTIES));
        String neName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
        final List<Map<String, String>> jobProperties = (List<Map<String, String>>) neJobAttributes.get(ShmJobConstants.JOBPROPERTIES);
        final String parentNodeName = getParentNodeName(jobProperties);
        if (parentNodeName != null) {
            neName = parentNodeName;
        }
        final List<String> neFdns = new ArrayList<String>();
        neFdns.add(neName);

        final com.ericsson.oss.services.shm.job.activity.JobType jobType = com.ericsson.oss.services.shm.job.activity.JobType.fromValue((String) jobTemplatePo.getAttribute(ShmConstants.JOB_TYPE));

        final JobConfigurationSummary jobConfigurationSummaryProvider = jobConfigurationSummaryFactory.getJobConfigurationSummaryProvider(jobType.name());

        try {
            final List<NetworkElement> networkElements = jobConfigurationSummaryProvider.getNetworkElementsByNeNames(neFdns,
                    jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType.name())));

            if (!networkElements.isEmpty()) {
                neType = networkElements.get(0).getNeType();
                platformType = networkElements.get(0).getPlatformType();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", neFdns);
        }

        LOGGER.debug("Resolving JobTypeDetailsProvider for PlatformType : {} and JobType : {}", platformType, jobType);
        final JobTypeDetailsProvider jobTypeDetailsProvider = jobTypeDetailsProviderFactory.getJobTypeDetailsProvider(platformType, jobType);
        if (jobTypeDetailsProvider != null) {
            jobConfigurationDetails = jobTypeDetailsProvider.getJobConfigurationDetails(jobConfiguration, platformType, neType, (String) neJobAttributes.get(ShmConstants.NE_NAME));
        }
        return jobConfigurationDetails;
    }

    /**
     * @param neJobAttributes
     * @param neName
     * @return
     */
    private String getParentNodeName(final List<Map<String, String>> jobProperties) {
        String neName = null;
        final Map<String, String> jobProperty = new HashMap<>();
        jobProperty.put(ShmConstants.KEY, ShmConstants.IS_COMPONENT_JOB);
        jobProperty.put(ShmConstants.VALUE, "true");

        if (jobProperties != null && !jobProperties.isEmpty() && jobProperties.contains(jobProperty)) {
            for (final Map<String, String> nejobProperty : jobProperties) {
                if (nejobProperty.get(ShmConstants.KEY).equals(ShmConstants.PARENT_NAME)) {
                    neName = nejobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        return neName;
    }

    /**
     * Retrieves all the Main jobs, after filtered with jobStateEnum.
     * 
     * @param jobState
     *            - Job state filter to retrieve the Main job details for.
     * @return - List of Main job PO ids.
     */
    @Override
    public List<Long> getMainJobIds(final String... jobState) {
        try {
            final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
            final DataBucket liveBucket = dataPersistenceService.getLiveBucket();
            final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
            final Query<TypeRestrictionBuilder> jobTypeQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.JOB);
            final TypeRestrictionBuilder rb = jobTypeQuery.getRestrictionBuilder();
            final Restriction jobStateRestriction = rb.in(ShmJobConstants.STATE, (Object[]) jobState);
            jobTypeQuery.setRestriction(jobStateRestriction);
            final List<Long> mainJobIds = new ArrayList<Long>();
            final List<Long> templateJobIds = new ArrayList<Long>();
            final List<Object[]> mainJobProjector = queryExecutor.executeProjection(jobTypeQuery, ProjectionBuilder.field(ObjectField.PO_ID),
                    ProjectionBuilder.attribute(ShmConstants.JOB_TEMPLATE_ID));
            for (final Object[] eachMainJobProjector : mainJobProjector) {
                mainJobIds.add((Long) eachMainJobProjector[0]);
                templateJobIds.add((Long) eachMainJobProjector[1]);
            }
            // Logic to get the running main job count for each job type to display in DDP.
            final List<String> jobTypes = getJobType(templateJobIds);
            if (jobTypes != null) {
                mainJobInstrumentation.updateRunningMainJobCount(mainJobIds.size(), jobTypes);
            }

            return mainJobIds;

        } catch (final Exception e) {
            LOGGER.error("Main job ID retrieval failed due to : {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> getJobType(final List<Long> templateJobIds) {
        final List<String> jobTypes = new ArrayList<>();
        try {
            final List<PersistenceObject> templateJobPoList = dpsReader.findPOsByPoIds(templateJobIds);
            for (final PersistenceObject templateJobPO : templateJobPoList) {
                final String jobType = templateJobPO.getAttribute(ShmConstants.JOB_TYPE);
                jobTypes.add(jobType);
            }
            return jobTypes;
        } catch (final Exception e) {
            LOGGER.error("Retrieval of Jobtypes from Job Template failed due to : {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> getNeJobIdsForMainJob(final Long mainJobId) {
        LOGGER.info("getting NE job Ids for the mainJobId {}", mainJobId);
        final DataBucket liveBucket = getLiveBucket();
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = setRestrictionToGetCorrespondingNeJobs(mainJobId, queryBuilder);
        final List<Long> neJobIdList = queryNeJobIds(typeQuery, queryExecutor);
        LOGGER.info("List containing NE job ids is retrieved {}", neJobIdList);
        return neJobIdList;

    }

    @Override
    public int getSkippedNeJobCount(final Long templateJobId) {
        LOGGER.debug("getting Skipped NE job count for the mainJob with templateJobId {}", templateJobId);
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.JOB);
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final Restriction restriction = typeQuery.getRestrictionBuilder().in(ShmConstants.JOBTEMPLATEID, templateJobId);
        typeQuery.setRestriction(restriction);
        int skippedNeCount = 0;
        final List<Object> mainJobProjection = queryExecutor.executeProjection(typeQuery, ProjectionBuilder.field(ObjectField.PO_ID));
        if (mainJobProjection != null && !mainJobProjection.isEmpty()) {
            skippedNeCount = findSkippedNEJobs(getLiveBucket(), (long) mainJobProjection.get(0));
        }
        LOGGER.debug("Skipped Ne Count {}", skippedNeCount);
        return skippedNeCount;
    }

    private int findSkippedNEJobs(final DataBucket liveBucket, final Long mainJobId) {
        final com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(ShmConstants.NAMESPACE, ShmConstants.NEJOB_TYPE);
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final Map<Object, Object> restrictionAttributes = new HashMap<Object, Object>();
        restrictionAttributes.put(ShmConstants.MAINJOBID, mainJobId);
        restrictionAttributes.put(ShmConstants.RESULT, JobResult.SKIPPED.toString());
        typeQuery.setRestriction(typeQuery.getRestrictionBuilder().allOf(setRetrictions(restrictionAttributes, typeQuery)));
        return queryExecutor.getResultList(typeQuery).size();
    }

    private Restriction[] setRetrictions(final Map<Object, Object> restrictionAttributes, final com.ericsson.oss.itpf.datalayer.dps.query.Query<TypeRestrictionBuilder> typeQuery) {
        final Restriction queryRestrictionArray[] = new Restriction[restrictionAttributes.size()];
        int restrictionIndex = 0;
        for (final Entry<Object, Object> attribute : restrictionAttributes.entrySet()) {
            if (attribute.getKey() instanceof String) {
                if (attribute.getValue() == null) {
                    queryRestrictionArray[restrictionIndex] = typeQuery.getRestrictionBuilder().nullValue((String) attribute.getKey());
                } else {
                    queryRestrictionArray[restrictionIndex] = typeQuery.getRestrictionBuilder().equalTo((String) attribute.getKey(), attribute.getValue());
                }
                restrictionIndex++;
            } else if (attribute.getKey() instanceof ObjectField) {
                LOGGER.debug("Building Restriction on ObjectField:{}", attribute.getKey());
                queryRestrictionArray[restrictionIndex] = typeQuery.getRestrictionBuilder().equalTo((ObjectField) attribute.getKey(), attribute.getValue());
                restrictionIndex++;
            }
        }
        return queryRestrictionArray;
    }

    @Override
    public boolean updateNodeCountOfJob(final Long mainJobId, final int nodeCount) {
        LOGGER.info("Updating new nodeCount {} for mainJobId : {} in db", nodeCount, mainJobId);
        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes.put(JobModelConstants.NUMBER_OF_NETWORK_ELEMENTS, nodeCount);
        try {
            final boolean updatedResult = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<Boolean>() {

                @Override
                protected Boolean execute() throws Exception {
                    return updateJobAttributes(jobAttributes, mainJobId);
                }

            });
            return updatedResult;
        } catch (final RetriableCommandException ex) {
            LOGGER.error("RetriableCommandException. Reason: {}", ex.getMessage());
            final Throwable throwable = ex.getCause();
            if (throwable instanceof DatabaseNotAvailableException) {
                LOGGER.error("DB is  unavailable, Failed to update node count in main job table");
                throw new DatabaseNotAvailableException(ShmCommonConstants.DATABASE_SERVICE_NOT_AVAILABE);
            } else {
                if (!ActivityConstants.EMPTY.equals(ExceptionParser.getReason(ex))) {
                    LOGGER.error("Falied to update node count in main job table because : {} ", ExceptionParser.getReason(ex));
                    throw new InternalServerErrorException(ExceptionParser.getReason(ex));
                } else {
                    LOGGER.error("Falied to update node count in main job table");
                    throw new InternalServerErrorException("Error while updating node count in main job table");
                }
            }
        }
    }

    private boolean updateJobAttributes(final Map<String, Object> jobAttributes, final Long mainJobId) {
        if (jobAttributes.size() > 0) {
            final PersistenceObject persistenceObject = getLiveBucket().findPoById(mainJobId);
            if (persistenceObject != null) {
                persistenceObject.setAttributes(jobAttributes);
            } else {
                LOGGER.error("Main job PO not found with Id:{}, and skiping the attributes Update", mainJobId);
                return false;
            }
        }
        return true;
    }

    @Override
    public String exportMainJobLogs(final ExportJobLogRequest jobLogRequestForExport) {
        final DateTime d1 = DateTime.now();

        String csvOutput = null;
        final Long mainJobId = jobLogRequestForExport.getMainJobId();
        if (mainJobId == null) {
            LOGGER.warn("Logs cannot be retrieved as the mainJobId is null");
            return "";
        }
        try {
            final DataBucket liveBucket = getLiveBucket();
            final List<JobLogResponse> jobLogsResponse = getMainJobLogs(jobLogRequestForExport.getMainJobId(), liveBucket);
            final List<String[]> logsAsStringArray = formatLogsAsArray(jobLogRequestForExport, jobLogsResponse);

            csvOutput = CsvBuilder.constructCsv(logsAsStringArray);
        } catch (final NumberFormatException exception) {
            LOGGER.error("Internal Error found due to {}", exception);
        }
        final DateTime d2 = DateTime.now();
        final Interval duration = new Interval(d1, d2);
        systemRecorder.recordEvent("Time taken to export mainjob logs is" + "_" + duration.toDurationMillis(), EventLevel.DETAILED, "", "", "");
        return csvOutput;
    }

    @Override
    public Map<Long, String> getNeJobDetails(final List<Long> neJobIds) {
        List<PersistenceObject> neJobPoList = new ArrayList<>();
        try {

            neJobPoList = dpsReader.findPOsByPoIds(neJobIds);

        } catch (final RuntimeException runtimeException) {
            LOGGER.error("Exception Occured while fetching NeJobDetails. Reason: {}", runtimeException);
            dpsAvailabilityInfoProvider.checkDatabaseAvailability(runtimeException);
        }
        final Map<Long, String> neJobDetails = new HashMap<>();
        if (neJobPoList != null && !neJobPoList.isEmpty()) {
            for (final PersistenceObject persistenceObject : neJobPoList) {
                neJobDetails.put(persistenceObject.getPoId(), (String) persistenceObject.getAttribute(ShmConstants.NE_NAME));
            }
        }
        return neJobDetails;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Object> createNhcJob(final JobInfo jobInfo) throws NoMeFDNSProvidedException {
        return createJob(jobInfo);
    }

    @Override
    public DeleteResponse deleteJobsWithNoRbac(final List<String> jobIds) {
        final Map<String, String> response = deleteJobs(jobIds);
        final DeleteResponse deleteJobOutput = new DeleteResponse();
        deleteJobOutput.setMessage(response.get(ShmConstants.MESSAGE));
        deleteJobOutput.setStatus(response.get(ShmConstants.STATUS));
        return deleteJobOutput;
    }

    private Map<String, String> deleteJobs(final List<String> jobPoIdsToDelete) {
        LOGGER.info("Received a request to delete {} job(s)", jobPoIdsToDelete.size());

        final List<Long> poIdList = new ArrayList<>();
        JobsDeletionReport jobsDeletionReport = new JobsDeletionReport(jobPoIdsToDelete.size());
        // Converting Job Ids from String to Long.
        for (final String poId : jobPoIdsToDelete) {
            poIdList.add(Long.valueOf(poId));
        }
        // Updating JobState as DELETING
        try {
            jobsDeletionReport = shmJobServiceHelper.updateJobStatusAndGetJobDeletionReport(poIdList);
            if (!jobsDeletionReport.getJobPoIdsFailedForDeletion().isEmpty()) {
                poIdList.removeAll(jobsDeletionReport.getJobPoIdsFailedForDeletion());
            }
            LOGGER.info("PO Id list after deleting failed job IDs is: {}", poIdList);
            shmJobServiceImplHelper.deleteJobs(poIdList, userContextBean.getLoggedInUserName());

        } catch (final Exception ex) {
            LOGGER.error("All Retries have been exhausted,no more attempts will be made to retry. Exception is: {}", ex);
        }
        return jobsDeletionReport.generateResponseForUser();
    }

    @Override
    public CancelResponse cancelJobsWithNoRbac(final List<Long> jobIds) {
        final Map<String, Object> response = cancelJobsCommon(jobIds);
        final CancelResponse cancelResponse = new CancelResponse();
        cancelResponse.setMessage((String) response.get(ShmConstants.MESSAGE));
        cancelResponse.setStatus((String) response.get(ShmConstants.STATUS));
        return cancelResponse;
    }

    private Map<String, Object> cancelJobsCommon(final List<Long> jobIds) {
        final Map<String, Object> response = new HashMap<>();
        // Call JobExecutionService to carry on with cancellation.
        jobExecutorLocal.cancelJobs(jobIds, userContextBean.getLoggedInUserName());
        response.put(ShmConstants.MESSAGE, "Job(s) are submitted for cancellation");
        response.put(ShmConstants.STATUS, "success");
        return response;
    }

    @Override
    public NeTypesPlatformData getNeTypesPlatforms(final NeTypesInfo neTypeInfo) {
        final String capabilityName = jobCapabilityProvider.getCapability(neTypeInfo.getJobType());
        final NeTypesPlatformData neTypesPlatformData = new NeTypesPlatformData();
        final Map<String, Set<String>> platformAndSupportedNeTypeMap = new HashMap<>();
        Set<String> supportedNeTypes = null;
        final Set<String> unsupportedNeTypes = new HashSet<>();
        final Set<String> neTypes = neTypeInfo.getNeTypes();
        PlatformTypeEnum platformTypeEnum = null;
        for (final String neType : neTypes) {
            try {
                platformTypeEnum = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(neType, capabilityName);
            } catch (UnsupportedPlatformException | NullPointerException ex) {
                LOGGER.error("Exception occured while retrieving platformType for neType {}. Reason: ", neType, ex);
                unsupportedNeTypes.add(neType);
                continue;
            }
            final String platformType = platformTypeEnum.name();
            if (platformAndSupportedNeTypeMap.containsKey(platformType)) {
                supportedNeTypes = platformAndSupportedNeTypeMap.get(platformType);
                supportedNeTypes.add(neType);
            } else {
                supportedNeTypes = new HashSet<>();
                supportedNeTypes.add(neType);
                platformAndSupportedNeTypeMap.put(platformType, supportedNeTypes);
            }
        }
        LOGGER.debug("supported NeTypes: {} AND unsupported NeTypes: {}", supportedNeTypes, unsupportedNeTypes);
        neTypesPlatformData.setUnsupportedNeTypes(unsupportedNeTypes);
        neTypesPlatformData.setSupportedNeTypesByPlatforms(platformAndSupportedNeTypeMap);
        return neTypesPlatformData;
    }

    @Override
    public int getActivityTimeout(final long activityJobId, final String jobType, final String activityName, final String nodeName) {
        int timeout = 0;
        String platformType = null;
        String neType = null;
        try {
            final NetworkElementData networkElement = networkElementRetrievalBean.getNetworkElementData(nodeName);
            neType = networkElement.getNeType();
            final String capability = jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType));
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, capability);
            platformType = neJobStaticData.getPlatformType();
            timeout = activityTimeoutsService.getActivityTimeoutAsInteger(neType, platformType, jobType, activityName);
            LOGGER.info("Activity timeout value retrieved for platform {}, neType: {}, jobType: {} , activityName: {} and activityJobId: {} is {}", platformType, neType, jobType, activityName,
                    activityJobId, timeout);
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while fetching activity timeout with activityJobId {}, neType: {}, jobType: {} and activityName:{}. Reason:", activityJobId, neType, jobType, activityName,
                    ex);
        }
        return timeout;
    }

    @Override
    public Map<String, Object> getSupportedNes(final List<String> neNames, final String jobType) {
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        return jobExecutorLocal.getSupportedNes(neNames, jobTypeEnum);
    }

    @Override
    public NetworkElementIdResponse getNetworkElementPoIds(final List<String> neNames) {
        return fdnServiceBean.getNetworkElementPoIds(neNames);
    }

}