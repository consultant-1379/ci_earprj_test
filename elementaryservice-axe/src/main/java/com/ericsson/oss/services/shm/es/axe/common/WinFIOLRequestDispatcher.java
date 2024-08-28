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

import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.ShmException;

public class WinFIOLRequestDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WinFIOLRequestDispatcher.class);

    public <T> ClientResponse<T> initiateRestCall(final RestRequest restRequest) {
        final ClientRequest clientRequest = new ClientRequest(restRequest.getUrl());
        final Map<String, Object> headers = restRequest.getHeaders();
        for (Entry<String, Object> headerMap : headers.entrySet()) {
            clientRequest.header(headerMap.getKey(), headerMap.getValue());
        }
        clientRequest.accept(MediaType.APPLICATION_JSON);
        final ClientResponse<T> response = getResponse(restRequest, clientRequest);

        if (response != null) {
            LOGGER.info("Response Status code {} for request {}", response.getStatus(), restRequest.getUrl());
            if (response.getStatus() == Status.OK.getStatusCode()) {
                return response;
            } else {
                throw new ShmException(response.getResponseStatus().getReasonPhrase());
            }
        } else {
            final String message = "Invalid response response from Rest: null";
            throw new ShmException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ClientResponse<T> getResponse(final RestRequest restRequest, final ClientRequest request) {
        ClientResponse<T> response = null;
        try {
            if (HttpMethod.GET.equalsIgnoreCase(restRequest.getHttpMethod())) {
                response = request.get();
            } else if (HttpMethod.POST.equalsIgnoreCase(restRequest.getHttpMethod())) {
                request.body(MediaType.APPLICATION_JSON, restRequest.getBody());
                response = request.post();
            } else if (HttpMethod.DELETE.equalsIgnoreCase(restRequest.getHttpMethod())) {
                response = request.delete();
            }

        } catch (Exception e) {
            LOGGER.error("Exception ocurred while fetching response {}", e.getMessage());
        }
        LOGGER.info("Rest call requested: restRequest {}", restRequest);
        return response;
    }

}
