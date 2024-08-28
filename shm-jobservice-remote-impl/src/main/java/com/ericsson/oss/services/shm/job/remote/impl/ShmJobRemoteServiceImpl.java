/*------------------------------------------------------------------------------
 *******************************************************************************
 *
 * COPYRIGHT Ericsson 2016
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EPredefinedRole;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.constants.AccessControlConstants;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.DuplicateEntityException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData;
import com.ericsson.oss.services.shm.job.entities.CancelResponse;
import com.ericsson.oss.services.shm.job.entities.DeleteResponse;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobLogRequest;
import com.ericsson.oss.services.shm.job.entities.JobOutput;
import com.ericsson.oss.services.shm.job.entities.ReportLogRequest;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.job.impl.DomainTypeProviderFactory;
import com.ericsson.oss.services.shm.job.remote.api.*;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.DomainTypeResponseCode;
import com.ericsson.oss.services.shm.job.remote.api.errorcodes.JobCreationResponseCode;
import com.ericsson.oss.services.shm.job.service.JobConfigurationDetailService;
import com.ericsson.oss.services.shm.job.service.SHMJobService;
import com.ericsson.oss.services.shm.job.service.api.JobInfoConverter;
import com.ericsson.oss.services.shm.jobexecutorlocal.JobExecutorLocal;
import com.ericsson.oss.services.shm.jobs.common.api.DomainTypeProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobQuery;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.NeParams;
import com.ericsson.oss.services.shm.jobs.common.constants.JobModelConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobLogResponse;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.SelectedNEInfo;
import com.ericsson.oss.services.shm.jobs.common.restentities.RestJobConfigurationData;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.JobInfo;
import com.ericsson.oss.services.shm.jobservice.common.Property;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;
import com.ericsson.oss.services.shm.topologyservice.TopologyEvaluationService;
import com.ericsson.oss.services.topologyCollectionsService.exception.service.TopologyCollectionsServiceException;
import com.ericsson.oss.shm.job.entities.SHMJobData;

@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ShmJobRemoteServiceImpl implements ShmJobRemoteService {
    private static final String SHM_JOB_REMOTE_SERVICE = "SHM Job Remote Service";
    private static final String CREATE_JOB_INPUT_RECEIVED_FROM_EXTERNAL_SOURCE_AND_INPUT_DATA_FOR_SHM_SERVICE = "Input received from external request for job creation : %s and Input being sent to SHM service for job creation : %s";

    @Inject
    FdnServiceBean fdnServiceBean;

    @Inject
    private DomainTypeProviderFactory domainTypeProviderFactory;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private JobInfoConverterFactory jobInfoConverterFactory;

    @Inject
    private JobStatusHelper jobStatusHelper;

    @Inject
    private SHMJobService shmJobService;

    @Inject
    private JobExecutorLocal jobExecutorLocal;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @Inject
    private JobConfigurationDetailService jobConfigurationDetailService;

    @Inject
    private TopologyEvaluationService topologyEvaluationService;

    @Inject
    private SystemRecorder systemRecorder;

    private JobInfoConverter jobInfoConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmJobRemoteServiceImpl.class);

    private static final String ERROR_CODE = "errorCode";

    private static final String SUCCESS = "success";

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public DomainTypeResponse getDomainTypeList(final String neNameOrFdn) {
        LOGGER.debug("Entered getDomainTypeList() with NE : {}", neNameOrFdn);
        final DomainTypeResponse domainTypeResponse = new DomainTypeResponse();
        try {
            final List<String> domainTypeList = new ArrayList<>();
            String neType = null;
            PlatformTypeEnum platformType = null;

            final String capability = jobCapabilityProvider.getCapability(JobTypeEnum.BACKUP);
            List<NetworkElement> networkElementsList = fdnServiceBean.getNetworkElements(Arrays.asList(neNameOrFdn), capability);

            if (networkElementsList == null || networkElementsList.isEmpty()) {
                networkElementsList = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(neNameOrFdn), capability);
            }

            if (networkElementsList != null && !networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
                platformType = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(neType, capability);
            } else {
                domainTypeResponse.setNeName(neNameOrFdn);
                domainTypeResponse.setDomainTypeErrorCode(ShmJobResponseResult.FAILED);
                domainTypeResponse.setDomainTypeResponseCode(DomainTypeResponseCode.NO_NE_EXCEPTION);
                return domainTypeResponse;
            }

            final DomainTypeProvider domainTypeProvider = domainTypeProviderFactory.getDomainTypeProvider(platformType);
            if (domainTypeProvider == null) {
                domainTypeResponse.setDomainTypeErrorCode(ShmJobResponseResult.FAILED);
                domainTypeResponse.setDomainTypeResponseCode(DomainTypeResponseCode.UNSUPPORTED_NODETYPE);
                return domainTypeResponse;
            }
            final NeInfoQuery neInfoQuery = new NeInfoQuery();
            neInfoQuery.setNeFdns(Arrays.asList(networkElementsList.get(0).getNetworkElementFdn()));
            neInfoQuery.setNeType(neType);
            neInfoQuery.setParams(new ArrayList<NeParams>());

            final Set<String> domainType = domainTypeProvider.getDomainTypeList(neInfoQuery);
            if (domainType != null) {
                domainTypeList.addAll(domainType);
            }
            domainTypeResponse.setDomainTypeErrorCode(ShmJobResponseResult.SUCCESS);
            domainTypeResponse.setNeName(networkElementsList.get(0).getName());
            domainTypeResponse.setNeType(neType);
            domainTypeResponse.setDomainTypeList(domainTypeList);
            LOGGER.debug("Exit getDomainTypeList() with domainTypelist : {}", domainTypeList.size());
        } catch (final Exception e) {
            LOGGER.error("Exception occurred while retrieving domain/type", e);
            domainTypeResponse.setDomainTypeErrorCode(ShmJobResponseResult.FAILED);
            domainTypeResponse.setDomainTypeResponseCode(DomainTypeResponseCode.DEFAULT_DOMAINTYPE_ERROR);
        }

        return domainTypeResponse;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.CREATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public ShmJobCreationResponse createJob(final ShmRemoteJobData shmRemoteJobData) throws SecurityViolationException {

        LOGGER.info("External request is received to create a job...");
        final ShmJobCreationResponse shmJobCreationResponse = new ShmJobCreationResponse();
        try {
            if (shmRemoteJobData == null) {
                shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
                return shmJobCreationResponse;
            }
            final String shmJobRemoteDataAsString = shmRemoteJobData.toString();
            jobInfoConverter = jobInfoConverterFactory.getJobInfoConverter(shmRemoteJobData.getJobType());

            final JobCreationResponseCode jobCreationResponseCode = jobInfoConverter.isValidData(shmRemoteJobData);
            if (jobCreationResponseCode != null) {
                shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
                shmJobCreationResponse.setJobCreationResponseCode(jobCreationResponseCode);
                return shmJobCreationResponse;
            }

            final JobInfo jobInfo = jobInfoConverter.prepareJobInfoData(shmRemoteJobData);
            systemRecorder.recordEvent(SHMEvents.SHM_EXTERNAL_CREATE_JOB, EventLevel.COARSE, "SHM External Create Job", SHM_JOB_REMOTE_SERVICE,
                    String.format(CREATE_JOB_INPUT_RECEIVED_FROM_EXTERNAL_SOURCE_AND_INPUT_DATA_FOR_SHM_SERVICE, shmJobRemoteDataAsString, jobInfo.toString()));

            LOGGER.debug("Calling jobservice to create job with JobInfo:{}...", jobInfo);

            final Map<String, Object> response = shmJobService.createShmJob(jobInfo);
            if (response != null && response.get(ERROR_CODE).equals(SUCCESS)) {
                shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.SUCCESS);
                shmJobCreationResponse.setJobName(shmRemoteJobData.getJobName());
            }

        } catch (final NoJobConfigurationException e) {
            LOGGER.error("Exception occured while creating the job. Reason: {}", e.getMessage(), e);
        } catch (final DuplicateEntityException de) {
            shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
            shmJobCreationResponse.setJobCreationResponseCode(JobCreationResponseCode.JOB_NAME_ALREADY_EXIST);
            LOGGER.error("DuplicateEntityis found :{} ", de.getMessage(), de);
            throw de;
        } catch (final NoMeFDNSProvidedException e) {
            shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
            shmJobCreationResponse.setJobCreationResponseCode(JobCreationResponseCode.ME_FDNS_NOT_FOUND);
            LOGGER.error("No FDN provider:{} ", e.getMessage(), e);
        } catch (final TopologyCollectionsServiceException e) {
            shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
            shmJobCreationResponse.setJobCreationResponseCode(JobCreationResponseCode.NE_SPEC_NOT_FOUND);
            LOGGER.error("Topology Collection Exception:{} ", e.getMessage(), e);
        } catch (final Exception e) {
            LOGGER.error("Caught Unexcepted Exception: {}", e);
            shmJobCreationResponse.setShmJobResponseResult(ShmJobResponseResult.FAILED);
            shmJobCreationResponse.setJobCreationResponseCode(JobCreationResponseCode.UNEXPECTED_ERROR);
        }

        return shmJobCreationResponse;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public JobStatusResponse getJobStatus(final JobStatusQuery jobStatusQueryParams) {
        final JobOutput mainJobsStatus = jobStatusHelper.fetchMainJobStatus(jobStatusQueryParams);
        final List<SHMJobData> mainJobsData = new ArrayList<>((List<SHMJobData>) mainJobsStatus.getResult());
        final JobStatusResponse jobStatusResponse = new JobStatusResponse(mainJobsData);
        if (jobStatusQueryParams.getJobName() != null) {
            LOGGER.debug("MainJobData size: {}", mainJobsData);
            if (!mainJobsData.isEmpty()) {
                jobStatusResponse.setNeLevelJobStatus(jobStatusHelper.fetchDescendantNEJobStatus(mainJobsData));
            }
        }
        return jobStatusResponse;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.services.shm.job.remote.api.ShmJobRemoteService#createJob(com.ericsson.oss.services.shm.jobservice.common.JobInfo)
     */
    @Override
    public Map<String, Object> createJob(final JobCreationRequest jobCreationRequest) throws NoJobConfigurationException, NoMeFDNSProvidedException {

        return shmJobService.createNhcJob(convertJsonRequestToJobInfo(jobCreationRequest));
    }

    @SuppressWarnings("unchecked")
    private JobInfo convertJsonRequestToJobInfo(final JobCreationRequest jobCreationRequest) {
        LOGGER.debug("jobCreationRequest {}", jobCreationRequest);
        final JobInfo jobInfo = new JobInfo();

        jobInfo.setJobType(jobCreationRequest.getJobType());
        jobInfo.setName(jobCreationRequest.getName());
        jobInfo.setOwner(jobCreationRequest.getOwner());
        jobInfo.setcollectionNames(jobCreationRequest.getcollectionNames());
        jobInfo.setFdns(jobCreationRequest.getFdns());
        jobInfo.setMainSchedule(convertMainSchedule(jobCreationRequest));
        jobInfo.setConfigurations(convertJobConfigurations(jobCreationRequest));
        jobInfo.setNeNames(convertNeNames(jobCreationRequest));
        jobInfo.setPackageNames(jobCreationRequest.getPackageNames());

        jobInfo.setActivitySchedules(convertActivitySchedules(jobCreationRequest));
        jobInfo.setDescription(jobCreationRequest.getDescription());
        jobInfo.setCreationTime(jobCreationRequest.getCreationTime());
        jobInfo.setJobProperties(jobCreationRequest.getJobProperties());
        jobInfo.setPlatformJobProperties(jobCreationRequest.getPlatformJobProperties());
        jobInfo.setNETypeJobProperties(jobCreationRequest.getNETypeJobProperties());
        jobInfo.setNeJobProperties(jobCreationRequest.getNeJobProperties());
        jobInfo.setSavedSearchIds(jobCreationRequest.getSavedSearchIds());
        jobInfo.setJobCategory(jobCreationRequest.getJobCategory());

        return jobInfo;
    }

    /**
     * @param jobInfo
     * @param neNames
     */
    private List<Map<String, Object>> convertNeNames(final JobCreationRequest jobCreationRequest) {
        final List<NeNames> neNames = jobCreationRequest.getNeNames();
        final List<Map<String, Object>> neNamesList = new ArrayList<>();
        for (final NeNames neName : neNames) {
            final Map neNameMap = new HashMap();
            neNameMap.put(ShmConstants.NAME, neName.getName());
            neNamesList.add(neNameMap);
        }
        return neNamesList;
    }

    /**
     * @param jobCreationRequest
     * @return
     */
    private List<Map<String, Object>> convertActivitySchedules(final JobCreationRequest jobCreationRequest) {
        final ActivitySchedule[] activitySchedules = jobCreationRequest.getActivitySchedules();
        final List<Map<String, Object>> listActivitySchedules = new ArrayList<>();
        for (final ActivitySchedule activitySchedule:activitySchedules) {
            final String platFormType = activitySchedule.getPlatformType();
            final ActivitySchedulesValue[] activitySchedulesValues = activitySchedule.getValue();
            final Map activityScheduleMap = new HashMap();
            final List<Map<String, Object>> listNeTypeSchedules = new ArrayList<>();
            activityScheduleMap.put(ShmConstants.PLATFORMTYPE, platFormType);
            for (final ActivitySchedulesValue activitySchedulesValue : activitySchedulesValues) {
                final ScheduledTaskValue[] scheduledTaskValues = activitySchedulesValue.getValue();
                final List scheduleDetailsList = new ArrayList();
                for (final ScheduledTaskValue scheduledTaskValue : scheduledTaskValues) {
                    final Map scheduleDetails = new HashMap();
                    scheduleDetails.put(ShmConstants.EXECUTION_MODE, scheduledTaskValue.getExecMode());
                    scheduleDetails.put(ShmConstants.ORDER, scheduledTaskValue.getOrder());
                    scheduleDetails.put(ShmConstants.ACTIVITYNAME, scheduledTaskValue.getActivityName());
                    scheduleDetailsList.add(scheduleDetails);
                }
                final Map neTypeSchedule = new HashMap();
                neTypeSchedule.put(ShmConstants.NETYPE, activitySchedulesValue.getNeType());
                neTypeSchedule.put(ShmConstants.VALUE, scheduleDetailsList);
                listNeTypeSchedules.add(neTypeSchedule);
            }
            activityScheduleMap.put(ShmConstants.VALUE, listNeTypeSchedules);
            listActivitySchedules.add(activityScheduleMap);
      }
        return listActivitySchedules;
   }

    /**
     * @param jobCreationRequest
     * @return mainSchedule
     */
    private Map<String, Object> convertMainSchedule(final JobCreationRequest jobCreationRequest) {
        final Map<String, Object> mainSchedule = new HashMap<>();
        final ScheduleAttributes[] scheduleAttributes = jobCreationRequest.getMainSchedule().getScheduleAttributes();
        final List<Map<String, String>> listSchedules = new ArrayList<>();
        for (final ScheduleAttributes scheduleAttribute : scheduleAttributes) {
            final Map<String, String> scheduleAttributesMap = new HashMap<>();
            scheduleAttributesMap.put(ShmConstants.NAME, scheduleAttribute.getName());
            scheduleAttributesMap.put(ShmConstants.VALUE, scheduleAttribute.getValue());
            listSchedules.add(scheduleAttributesMap);
        }
        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, listSchedules);
        mainSchedule.put(ShmConstants.EXECUTION_MODE, jobCreationRequest.getMainSchedule().getExecMode());
        return mainSchedule;
    }

    /**
     * @param jobCreationRequest
     * @return configurationsList
     */
    private static List<Map<String, Object>> convertJobConfigurations(final JobCreationRequest jobCreationRequest) {
        final List<JobRemoteconfigurations> jobConfigurations = jobCreationRequest.getConfigurations();
        final List<Map<String, Object>> configurationsList = new ArrayList<>();
        for (final JobRemoteconfigurations jobConfig : jobConfigurations) {
            final Map<String, Object> configuration = new HashMap<>();
            configuration.put(ShmConstants.NETYPE, jobConfig.getNeType());
            final Property[] properties = jobConfig.getProperties();
            final List<Map<String, String>> propertiesList = new ArrayList<>();
            for (int index = 0; index < properties.length; index++) {
                final Map<String, String> property = new HashMap<>();
                property.put(ShmConstants.KEY, properties[index].getKey());
                property.put(ShmConstants.VALUE, properties[index].getValue());
                propertiesList.add(property);
            }
            configuration.put(ShmConstants.PROPERTIES, propertiesList);
            configurationsList.add(configuration);

        }
        return configurationsList;
    }

    @Override
    public RemoteRestJobConfiguration getJobConfigurationDetails(final Long jobTemplateId) {
        LOGGER.info("Calling from remote layer to getSHMJobConfiguration for jobTemplateId id {} ", jobTemplateId);
        final JobQuery jobQuery = new JobQuery();
        jobQuery.setPoIds(Arrays.asList(jobTemplateId));

        final List<JobTemplate> jobTemplateList = shmJobService.getShmJobTemplates(jobQuery);
        if (!jobTemplateList.isEmpty()) {
            final JobTemplate jobTemplate = jobTemplateList.iterator().next();
            LOGGER.debug("Retrieved JobConfiguration to REST layer Successfully for the with templateId : {}", jobTemplate.getJobTemplateId());
            final RestJobConfigurationData restJobConfigurationData = jobConfigurationDetailService.getJobConfigurationDetails(jobTemplate);
            LOGGER.debug("RestJobConfigurationData {}", restJobConfigurationData);

            final RemoteRestJobConfiguration remoteRestJobConfiguration = transform(restJobConfigurationData);
            LOGGER.debug("RemoteRestJobConfiguration   {}", remoteRestJobConfiguration);
            return remoteRestJobConfiguration;
        } else {
            LOGGER.info("No Data Found for selected jobTemplateId ID {}", jobTemplateId);
            return null;
        }
    }

    private RemoteRestJobConfiguration transform(final RestJobConfigurationData restJobConfigurationData) {

        final SelectedNEInfo selectedNeInfo = restJobConfigurationData.getSelectedNEs();
        final List<NetworkElement> networkElements = selectedNeInfo.getNetworkElements();
        final List<NetworkElementConverter> networkElementConverter = new ArrayList<>();

        for (final NetworkElement ne : networkElements) {
            final List<ProductDataConverter> productDataConverters = new ArrayList<>();
            final List<ProductData> neProductVersion = ne.getNeProductVersion();
            for (final ProductData pd : neProductVersion) {
                final ProductDataConverter productDataConverter = new ProductDataConverter(pd.getRevision(), pd.getIdentity());
                productDataConverters.add(productDataConverter);
            }
            final NetworkElementConverter nc = new NetworkElementConverter(ne.getNetworkElementFdn(), ne.getNodeRootFdn(), ne.getPlatformType(), ne.getName(), ne.getNeType(), ne.getOssModelIdentity(),
                    ne.getNodeModelIdentity(), productDataConverters, ne.getUtcOffset(), ne.getTimeZone());
            networkElementConverter.add(nc);
        }

        final SelectedNEInfoConverter selectedNEInfoConverter = new SelectedNEInfoConverter(selectedNeInfo.getCollectionNames(), networkElementConverter, selectedNeInfo.getSavedSearchIds());

        return new RemoteRestJobConfiguration(restJobConfigurationData.getJobName(), restJobConfigurationData.getDescription(), restJobConfigurationData.getCreatedOn(),
                restJobConfigurationData.getJobType(), restJobConfigurationData.getStartTime(), restJobConfigurationData.getMode(), restJobConfigurationData.getJobParams(),
                restJobConfigurationData.getNeJobProperties(), selectedNEInfoConverter, restJobConfigurationData.getNeNames(), restJobConfigurationData.getSkippedNeCount(),
                restJobConfigurationData.getOwner(), restJobConfigurationData.getScheduleJobConfiguration());
    }

    @Override
    public boolean updateNodeCount(final Long mainJobId, final int nodeCount) {
        return shmJobService.updateNodeCountOfJob(mainJobId, nodeCount);
    }

    @Override
    public CollectionDetails getCollectionDetails(final String collectionId, final String jobOwner) {
        return topologyEvaluationService.getCollectionDetails(collectionId, jobOwner);
    }

    @Override
    public SavedSearchDetails getSavedSearchDetails(final String savedSearchId, final String jobOwner) {
        return topologyEvaluationService.getSavedSearchDetails(savedSearchId, jobOwner);
    }

    @Override
    public void invokeMainJobs(final List<Long> jobIds, final String loggedInUser) {
        LOGGER.info("Calling from remote layer to continue the Job with jobId - {} and user - {} ", jobIds, loggedInUser);
        jobExecutorLocal.invokeMainJobsManually(jobIds, loggedInUser);
    }

    @Override
    public void invokeNeJobs(final List<Long> jobIds, final String loggedInUser) {
        LOGGER.info("Calling from remote layer to continue the NeJob with neJobIds - {} and user - {} ", jobIds, loggedInUser);
        jobExecutorLocal.invokeNeJobsManually(jobIds, loggedInUser);
    }

    @Override
    public DeleteResponse deleteReports(final List<String> jobIds) {

        return shmJobService.deleteJobsWithNoRbac(jobIds);
    }

    @Override
    public ReportLogResponse viewReportLogs(final ReportLogRequest reportLogRequest) {

        final JobLogRequest jobLogRequest = new JobLogRequest();
        jobLogRequest.setFilterDetails(reportLogRequest.getFilterDetails());
        jobLogRequest.setLimit(reportLogRequest.getLimit());
        jobLogRequest.setLogLevel(reportLogRequest.getLogLevel());
        jobLogRequest.setMainJobId(reportLogRequest.getMainJobId());
        jobLogRequest.setOffset(reportLogRequest.getOffset());
        jobLogRequest.setOrderBy(reportLogRequest.getOrderBy());
        jobLogRequest.setSortBy(reportLogRequest.getSortBy());
        final List<Long> neJobIdList = reportLogRequest.getNeJobIds();

        final JobOutput reportLogs = shmJobService.viewReportLogs(jobLogRequest, neJobIdList);

        final List<JobLogResponse> result = new ArrayList<>((List<JobLogResponse>) reportLogs.getResult());
        final ReportLogResponse reportLogResponse = new ReportLogResponse();
        reportLogResponse.setResult(result);
        reportLogResponse.setColumns(reportLogs.getColumns());
        reportLogResponse.setTotalCount(reportLogs.getTotalCount());
        reportLogResponse.setClearOffset(reportLogs.isClearOffset());
        return reportLogResponse;
    }

    @Override
    public String mainJobLogsToExport(final ExportJobLogRequest jobLogRequestForExport) {
        return shmJobService.exportMainJobLogs(jobLogRequestForExport);
    }

    @Override
    public CancelResponse cancelReports(final List<Long> jobIds) {

        return shmJobService.cancelJobsWithNoRbac(jobIds);
    }

    @Override
    public String exportJobLogs(final String neJobIds, final String mainJobId) {
        LOGGER.debug("Export job logs of mainJobId {} ", mainJobId);
        final StringBuilder csvOutput = new StringBuilder();
        List<Long> neJobIdList;
        if (neJobIds == null || neJobIds.length() == 0) {
            final long mainJobIdLong = Long.parseLong(mainJobId);
            neJobIdList = shmJobService.getNeJobIdsForMainJob(mainJobIdLong);
        } else {
            neJobIdList = convert(neJobIds);
        }

        if (neJobIdList.isEmpty()) {
            LOGGER.warn("Job logs cannot be retrieved as there are no NE Jobs available for Main Job :{}", mainJobId);
            return "";
        }

        final JobTemplate templateJobAttributeMap = shmJobService.getJobTemplateByNeJobId(neJobIdList.get(0));
        final String jobName = templateJobAttributeMap.getName();
        final String jobType = templateJobAttributeMap.getJobType().name();
        LOGGER.debug("jobname = {} , jobtype = {}", jobName, jobType);

        final ExportJobLogRequest jobLogRequestForExport = new ExportJobLogRequest();
        jobLogRequestForExport.setJobName(jobName);
        jobLogRequestForExport.setJobType(jobType);
        for (final Long neJobId : neJobIdList) {
            jobLogRequestForExport.setNeJobIds(neJobId);
            final String neCsvOutput = shmJobService.exportLogs(jobLogRequestForExport);
            if (neCsvOutput != null) {
                csvOutput.append(neCsvOutput);
            }
        }

        if (mainJobId != null && (neJobIds == null || neJobIds.length() == 0)) {
            jobLogRequestForExport.setMainJobId(Long.parseLong(mainJobId));
            final String mainJobcsvOutput = shmJobService.exportMainJobLogs(jobLogRequestForExport);
            if (mainJobcsvOutput != null) {
                csvOutput.append(mainJobcsvOutput);
            }
        }

        return csvOutput.toString();
    }

    private List<Long> convert(final String neJobIds) {
        final List<Long> neJobIdList = new ArrayList<>();
        final String[] jobIds = neJobIds.split(JobModelConstants.neJobIdSeparator);
        for (final String jobId : jobIds) {
            try {
                neJobIdList.add(Long.parseLong(jobId));
            } catch (final NumberFormatException e) {
                LOGGER.error("Unable to convert the given jobId : {} into Number, because:{}", jobId, e);
            }
        }
        return neJobIdList;
    }

    @Override
    public String getCollectionPoId(final String collectionName, final String jobOwner) {
        return topologyEvaluationService.getCollectionPoId(collectionName, jobOwner);
    }

    @Override
    public String getSavedSearchPoId(final String savedSearchName, final String jobOwner) {
        return topologyEvaluationService.getSavedSearchPoId(savedSearchName, jobOwner);
    }

    @Override
    public Set<String> getCollectionInfo(final String collectionId, final String jobOwner) {
        return topologyEvaluationService.getCollectionInfo(jobOwner, collectionId);
    }

    @Override
    public Set<String> getSavedSearchInfo(final String savedSearchId, final String jobOwner) {
        return topologyEvaluationService.getSavedSearchInfo(jobOwner, savedSearchId);
    }

}
