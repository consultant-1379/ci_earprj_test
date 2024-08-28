/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.axe.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;

import org.jboss.resteasy.client.ClientResponse;

import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.defaultne.activitiy.timeout.model.ActivityTimeoutsService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ExecMode;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

public abstract class AbstractAxeService {

    @Inject
    private WinFIOLRequestDispatcher winFIOLRequestDispatcher;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    protected JobConfigurationServiceRetryProxy jobConfigurationServiceProxy;

    @Inject
    protected NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    protected NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected JobStaticDataProvider jobStaticDataProvider;

    @Inject
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @Inject
    protected ActivityTimeoutsService activityTimeoutsService;

    @Inject
    protected JobUpdateService jobUpdateService;

    @Inject
    protected WorkflowInstanceNotifier workflowInstanceNotifier;

    private static final String WINFIOL_REST_BASE_URI = "http://%s:8084/winfiol-rest/v1/%s";

    protected <T> T executeGetRequest(final String uri, final String hostName, final Map<String, Object> headers, final Class<T> restResponseClass) {
        final RestRequest restRequest = new RestRequest();
        restRequest.setHeaders(headers);
        restRequest.setHttpMethod(HttpMethod.GET);
        restRequest.setUrl(String.format(WINFIOL_REST_BASE_URI, hostName, uri));
        return dispatchAndGetResponse(restRequest, restResponseClass);
    }

    protected <T> T executeDeleteRequest(final String uri, final String hostName, final Map<String, Object> headers, final Class<T> restResponseClass) {
        final RestRequest restRequest = new RestRequest();
        restRequest.setHeaders(headers);
        restRequest.setHttpMethod(HttpMethod.DELETE);
        restRequest.setUrl(String.format(WINFIOL_REST_BASE_URI, hostName, uri));
        return dispatchAndGetResponse(restRequest, restResponseClass);
    }

    protected <T> T executePostRequest(final String uri, final String hostName, final Map<String, Object> headers, final Map<String, Object> body, final Class<T> restResponseClass) {
        final RestRequest restRequest = new RestRequest();
        restRequest.setHeaders(headers);
        restRequest.setHttpMethod(HttpMethod.POST);
        restRequest.setBody(body);
        restRequest.setUrl(String.format(WINFIOL_REST_BASE_URI, hostName, uri));
        return dispatchAndGetResponse(restRequest, restResponseClass);
    }

    protected String getShmJobExecUser(final long mainjobid, final JobStaticData jobStaticData) {
        String shmJobExecUser = null;
        if (ExecMode.MANUAL.getMode().equals(jobStaticData.getExecutionMode())) {
            final Map<Object, Object> restrictions = new HashMap<>();
            restrictions.put(ObjectField.PO_ID, mainjobid);
            final List<Map<String, Object>> neDetails = jobConfigurationServiceProxy.getProjectedAttributes(ShmConstants.NAMESPACE, ShmConstants.JOB, restrictions,
                    Arrays.asList(ShmConstants.SHM_JOB_EXEC_USER));
            if (!neDetails.isEmpty()) {
                shmJobExecUser = (String) neDetails.get(0).get(ShmConstants.SHM_JOB_EXEC_USER);
            }
        }
        return (shmJobExecUser == null) ? jobStaticData.getOwner() : shmJobExecUser;
    }

    protected void setSystemRecordEventData(final long activityJobId, final NEJobStaticData neJobStaticData, final String activityName, final String requestType, final String shmEventType) {
        final String userMessage = requestType + " request TimedOut for the activityJobId " + activityJobId;
        final Map<String, Object> recordEventData = new HashMap<>();
        recordEventData.put(ShmConstants.ACTIVITYNAME, activityName);
        recordEventData.put(ShmConstants.NODENAME, neJobStaticData.getNodeName());
        recordEventData.put(ShmConstants.PLATEFORM_TYPE, neJobStaticData.getPlatformType());
        recordEventData.put(ShmConstants.REQUEST_TYPE, requestType);
        recordEventData.put(ShmConstants.MAIN_JOB_ID, neJobStaticData.getMainJobId());
        recordEventData.put(ShmConstants.ACTIVITY_JOB_ID, activityJobId);
        recordEventData.put(ShmConstants.USER_MESSAGE, userMessage);
        systemRecorder.recordEventData(shmEventType, recordEventData);
    }

    protected <T> T dispatchAndGetResponse(final RestRequest restRequest, final Class<T> restResponseClass) {
        final ClientResponse<T> clientResponse = winFIOLRequestDispatcher.initiateRestCall(restRequest);
        updateCookieForSessionIdResponse(clientResponse, restResponseClass);
        return clientResponse.getEntity(restResponseClass);
    }

    protected String getHostName(final String cookie, final String hostName) {
        return isCookieSet(cookie) ? AxeConstants.REST_HOST_NAME : hostName;
    }

    private <T> String getCookie(final ClientResponse<T> clientResponse) {
        final Map<String, List<Object>> metaData = clientResponse.getMetadata();
        if (metaData.containsKey(AxeConstants.SET_COOKIE_HEADER)) {
            final List<Object> cookies = metaData.get(AxeConstants.SET_COOKIE_HEADER);
            if (cookies != null) {
                return getWinfiolServerId(cookies);
            }
        }
        return null;
    }

    private String getWinfiolServerId(List<Object> cookies) {
        for (final Object cookie : cookies) {
            final String[] cookieData = cookie.toString().split(AxeConstants.SEMI_COLON);
            if (cookieData != null && cookieData.length >= 1) {
                final String cookieValue = cookieData[0];
                if (cookieValue.contains(AxeConstants.WINFIOL_SERVERID)) {
                    return cookieValue;
                }
            }
        }
        return null;
    }

    private static boolean isCookieSet(String cookie) {
        return cookie != null && !cookie.isEmpty();
    }

    protected Map<String, Object> getHeaders(final String shmJobExecUser, final String cookie) {
        final Map<String, Object> header = new HashMap<>();
        header.put(ShmConstants.USER_ID_KEY, shmJobExecUser);
        if (isCookieSet(cookie)) {
            header.put(AxeConstants.COOKIE_HEADER, cookie);
        }
        return header;
    }

    private <T> void updateCookieForSessionIdResponse(final ClientResponse<T> clientResponse, final Class<T> restResponseClass) {
        if (clientResponse.getEntity(restResponseClass).getClass().toString().contains(AxeConstants.SESSIONIDRESPONSE)) {
            SessionIdResponse response = (SessionIdResponse) clientResponse.getEntity(restResponseClass);
            final String cookie = getCookie(clientResponse);
            response.setCookie(cookie);
        }
    }
}
