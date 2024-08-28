/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.remote.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.services.shm.job.entities.CancelResponse;
import com.ericsson.oss.services.shm.job.entities.DeleteResponse;
import com.ericsson.oss.services.shm.job.entities.ExportJobLogRequest;
import com.ericsson.oss.services.shm.job.entities.ReportLogRequest;
import com.ericsson.oss.services.shm.job.exceptions.NoJobConfigurationException;
import com.ericsson.oss.services.shm.job.exceptions.NoMeFDNSProvidedException;
import com.ericsson.oss.services.shm.jobservice.common.CollectionDetails;
import com.ericsson.oss.services.shm.jobservice.common.SavedSearchDetails;

/**
 * Interface to handle external remote calls to handle job related functionalities
 *
 * @author xnagvar
 *
 */
@EService
@Remote
public interface ShmJobRemoteService {

    /**
     * Provides the response domain and type for Ecim nodes for given neName.
     *
     * @param jobActivitiesQueryList
     * @return
     */
    DomainTypeResponse getDomainTypeList(String neNameOrFdn);

    /**
     * Remote function to create SHM Job from external services. ShmJob Data is expected to be filled with proper details in order to create job
     *
     * @param shmJobData
     * @return
     * @throws SecurityViolationException
     *             in-case if user does not have permission to do this operation
     */
    ShmJobCreationResponse createJob(ShmRemoteJobData shmRemoteJobData) throws SecurityViolationException;

    /**
     * Provides Job Status response for the query given.
     *
     * @param jobStatusQueryParams
     * @return
     */
    JobStatusResponse getJobStatus(JobStatusQuery jobStatusQueryParams);

    /**
     * Remote function to create Job from external services.
     *
     * @param jobCreationRequest
     * @return map
     */
    Map<String, Object> createJob(JobCreationRequest jobCreationRequest) throws NoJobConfigurationException, NoMeFDNSProvidedException;

    /**
     * Remote method to get job configuration data. This method returns null if data is not found for given job template id
     *
     * @param jobTemplateId
     * @return
     */
    RemoteRestJobConfiguration getJobConfigurationDetails(final Long jobTemplateId);

    /**
     * Invokes Continue Report
     *
     * @param jobIds
     * @param loggedInUser
     * @return Response
     */
    void invokeMainJobs(final List<Long> jobIds, final String loggedInUser);

    /**
     * Invokes NeReport Continue
     *
     * @param jobIds
     * @param loggedInUser
     */
    void invokeNeJobs(final List<Long> neJobIds, final String loggedInUser);

    /**
     * Remote method to update current number of nodes involved for provided main job Id with new count.
     *
     * @param mainJobId
     * @param nodeCount
     * @return
     */
    boolean updateNodeCount(final Long mainJobId, final int nodeCount);

    /**
     * Get collection details if given user had right access on the given collectionId
     *
     * @param collectionId
     * @param jobOwner
     * @return
     * @throws DpsNotAvailableException
     */
    CollectionDetails getCollectionDetails(final String collectionId, final String jobOwner);

    /**
     * Get saved search details if given user had right access on the given savedSearchId
     *
     * @param savedSearchId
     * @param jobOwner
     * @return
     * @throws DpsNotAvailableException
     */
    SavedSearchDetails getSavedSearchDetails(final String savedSearchId, final String jobOwner);

    /**
     * Remote function to delete Job from external services.
     *
     * @param jobIds
     * @return DeleteJobOutput
     */
    DeleteResponse deleteReports(final List<String> jobIds);

    ReportLogResponse viewReportLogs(final ReportLogRequest reportLogRequest);

    String mainJobLogsToExport(final ExportJobLogRequest jobLogRequestForExport);

    /**
     * Remote function to cancel Job from external services.
     *
     * @param jobIds
     * @return
     */
    CancelResponse cancelReports(final List<Long> jobIds);

    /**
     * Remote function to export Job Logs from external services.
     *
     * @param neJobIds
     *            ,mainJobId
     * @return
     */
    String exportJobLogs(final String neJobIds, final String mainJobId);

    /**
     * Fetches collection PO ID based on provided collection name for remote users.
     *
     * @param collectionName
     * @param jobOwner
     * @return PO ID of the corresponding Collection.
     */
    String getCollectionPoId(final String collectionName, final String jobOwner);

    /**
     * Fetches collection PO ID based on provided saved search name for remote users.
     *
     * @param savedSearchName
     * @param jobOwner
     * @return PO ID of the corresponding saved search.
     */
    String getSavedSearchPoId(final String savedSearchName, final String jobOwner);

    /**
     * Fetches NE information based on provided collection ID for remote users.
     *
     * @param collectionId
     * @param jobOwner
     * @return
     */
    Set<String> getCollectionInfo(final String collectionId, final String jobOwner);

    /**
     * Fetches NE information based on provided saved search ID for remote users.
     *
     * @param savedSearchId
     * @param jobOwner
     * @return
     */
    Set<String> getSavedSearchInfo(final String savedSearchId, final String jobOwner);
}
