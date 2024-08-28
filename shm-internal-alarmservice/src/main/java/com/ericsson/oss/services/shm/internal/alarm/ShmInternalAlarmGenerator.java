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
package com.ericsson.oss.services.shm.internal.alarm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.internalalarm.api.InternalAlarmRequest;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class is used to Create an Internal Alarm
 */
public class ShmInternalAlarmGenerator {

    private final static Logger logger = LoggerFactory.getLogger(ShmInternalAlarmGenerator.class);

    private final static String url = "http://haproxy-int:8081/internal-alarm-service/internalalarm/internalalarmservice/translate";

    @Inject
    protected SystemRecorder systemRecorder;

    /**
     * @param alarmDetails
     *            creates an internal alarm with all the details passed in the alarmDetails map
     */
    public void raiseInternalAlarm(final Map<String, Object> alarmDetails) {
        final InternalAlarmRequest internalAlarmRequest = generateRequestObject(alarmDetails);
        final ClientRequest request = new ClientRequest(url);
        request.accept(MediaType.APPLICATION_JSON);
        final String jsonRequest = getJsonString(internalAlarmRequest, alarmDetails);
        request.body(MediaType.APPLICATION_JSON, jsonRequest);
        logger.debug("json request which is sent to Alarmendpoint {}", request.getBody());
        ClientResponse<String> response;
        try {
            response = request.post(String.class);

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                logger.info("Raising an internal alarm is successfull for job {} ", alarmDetails.get(ShmConstants.NAME));
            } else {
                systemRecorder.recordError(ShmConstants.INTERNAL_ALARM_ERROR_ID, ErrorSeverity.NOTICE, ShmConstants.SOURCE,
                        (String) alarmDetails.get(ShmConstants.NAME), (String) alarmDetails.get(ShmConstants.ADDITIONAL_TEXT));
            }
        } catch (final Exception e) {
            systemRecorder.recordError(ShmConstants.INTERNAL_ALARM_ERROR_ID, ErrorSeverity.NOTICE, ShmConstants.SOURCE,
                    (String) alarmDetails.get(ShmConstants.NAME), (String) alarmDetails.get(ShmConstants.ADDITIONAL_TEXT));
            logger.debug("Exception in Calling restendpoint from Java {}",e);
        }
    }

    /**
     * @param AlarmDetails
     *            , creates a InternalAlarm Request object from the alarm details.
     */
    private InternalAlarmRequest generateRequestObject(final Map<String, Object> alarmDetails) {
        final InternalAlarmRequest internalAlarmRequest = new InternalAlarmRequest();
        final Map<String, String> additionalAttribute = new HashMap<String, String>();
        internalAlarmRequest.setEventType((String) alarmDetails.get(ShmConstants.EVENT_TYPE));
        internalAlarmRequest.setProbableCause((String) alarmDetails.get(ShmConstants.PROBABLE_CAUSE));
        internalAlarmRequest.setSpecificProblem((String) alarmDetails.get(ShmConstants.SPECIFIC_PROBLEM));
        internalAlarmRequest.setPerceivedSeverity((String) alarmDetails.get(ShmConstants.PERCEIVED_SEVERITY));
        internalAlarmRequest.setRecordType((String) alarmDetails.get(ShmConstants.RECORD_TYPE));
        internalAlarmRequest.setManagedObjectInstance((String) alarmDetails.get(ShmConstants.MANAGED_OBJECT_INSTANCE));
        additionalAttribute.put(ShmConstants.ADDITIONAL_TEXT, alarmDetails.get(ShmConstants.ADDITIONAL_TEXT).toString());
        internalAlarmRequest.setAdditionalAttributes(additionalAttribute);
        return internalAlarmRequest;
    }

    /**
     * @param InternalAlarmRequest
     *            ,AlarmDetails creates a Json String from the Internal alarm Request object.
     */
    private String getJsonString(final InternalAlarmRequest internalAlarmRequest, final Map<String, Object> alarmDetails) {
        final ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = null;
        try {
            jsonRequest = mapper.writeValueAsString(internalAlarmRequest);
        } catch (final JsonGenerationException jsonGenerationException) {
            systemRecorder.recordError(ShmConstants.INTERNAL_ALARM_ERROR_ID, ErrorSeverity.NOTICE, ShmConstants.SOURCE,
                    (String) alarmDetails.get(ShmConstants.NAME), (String) alarmDetails.get(ShmConstants.ADDITIONAL_TEXT));
            logger.error("JsonGenerationException in JobFailureAlarmUtils {}", jsonGenerationException);
        } catch (final JsonMappingException jsonMappingException) {
            systemRecorder.recordError(ShmConstants.INTERNAL_ALARM_ERROR_ID, ErrorSeverity.NOTICE, ShmConstants.SOURCE,
                    (String) alarmDetails.get(ShmConstants.NAME), (String) alarmDetails.get(ShmConstants.ADDITIONAL_TEXT));
            logger.error("JsonMappingException in JobFailureAlarmUtils {}", jsonMappingException);
        } catch (final IOException iOException) {
            systemRecorder.recordError(ShmConstants.INTERNAL_ALARM_ERROR_ID, ErrorSeverity.NOTICE, ShmConstants.SOURCE,
                    (String) alarmDetails.get(ShmConstants.NAME), (String) alarmDetails.get(ShmConstants.ADDITIONAL_TEXT));
            logger.error("IOException in JobFailureAlarmUtils {}", iOException);
        }
        return jsonRequest;
    }
}