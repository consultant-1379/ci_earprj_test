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
package com.ericsson.oss.services.shm.es.axe.common;

import java.util.List;
import java.util.Map;

public class RestRequest {

    private String url;
    private List<String> pathParameters;
    private Map<String, Object> headers;
    private Object body;
    private String httpMethod;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public List<String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(final List<String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, Object> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(final Object body) {
        this.body = body;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return "RestRequest [url=" + url + ", pathParameters=" + pathParameters + ", headers=" + headers + ", body=" + body + ", httpMethod=" + httpMethod + "]";
    }

}
