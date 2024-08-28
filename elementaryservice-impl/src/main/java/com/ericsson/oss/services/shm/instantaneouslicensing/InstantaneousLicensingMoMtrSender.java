/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.instantaneouslicensing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.AdministrativeState;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.Attributes;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.AvailabilityStatus;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingMOConstants.OperationalState;
import com.ericsson.oss.services.shm.model.event.based.mediation.InstantaneousLicensingMOMediationTaskRequest;

/**
 * Prepares and triggers InstantaneousLicensing MO's Mediation Task Request
 * 
 * @author Team Royals
 *
 */
@Stateless
public class InstantaneousLicensingMoMtrSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstantaneousLicensingMoMtrSender.class);

    @Inject
    private InstantaneousLicensingMOHandler instantaneousLicensingMOHandler;

    @Inject
    @Modeled
    private EventSender<InstantaneousLicensingMOMediationTaskRequest> eventSender;

    public Map<String, Object> getInstantaneousLicensingMOAttributes(final String fdn) {
        return instantaneousLicensingMOHandler.getInstantaneousLicensingMOAttributes(fdn);
    }

    public void sendMTR(final Map<String, Object> instantaneousLicensingMOAttributes, final String fdn) {
        if (!instantaneousLicensingMOAttributes.isEmpty()) {
            final AdministrativeState administrativeState = getAdministrativeState(instantaneousLicensingMOAttributes);
            final OperationalState operationalState = getOperationalState(instantaneousLicensingMOAttributes);
            final List<String> availabilityStatus = getAvailabilityStatus(instantaneousLicensingMOAttributes);
            if (isValidAttributeValues(administrativeState, operationalState, availabilityStatus)) {
                eventSender.send(prepareMTR(administrativeState, operationalState, fdn));
                LOGGER.debug("Instantaneous licensing mo mediation task requestEvent has been sent");
            } else {
                LOGGER.warn("InstantaneousLicensing MO MTR event not sent because, administrativeState is {},operationalState is {} and availabilityStatus is {} ", administrativeState,
                        operationalState, availabilityStatus);
            }
        }
    }

    /**
     * @param administrativeState
     * @param operationalState
     * @param availabilityStatus
     * @return
     */
    private boolean isValidAttributeValues(final AdministrativeState administrativeState, final OperationalState operationalState, final List<String> availabilityStatus) {
        return (administrativeState != null && administrativeState.equals(AdministrativeState.UNLOCKED)) && (operationalState != null && operationalState.equals(OperationalState.DISABLED))
                && (availabilityStatus != null && !availabilityStatus.isEmpty() && availabilityStatus.contains(AvailabilityStatus.OFF_LINE.getValue()));
    }

    /**
     * @param instantaneousLicensingMOAttributes
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<String> getAvailabilityStatus(final Map<String, Object> instantaneousLicensingMOAttributes) {
        return instantaneousLicensingMOAttributes.get(Attributes.AVAILABILITYSTATUS.getAttributeName()) != null
                ? (ArrayList<String>) instantaneousLicensingMOAttributes.get(Attributes.AVAILABILITYSTATUS.getAttributeName()) : null;
    }

    /**
     * @param instantaneousLicensingMOAttributes
     * @return
     */
    private OperationalState getOperationalState(final Map<String, Object> instantaneousLicensingMOAttributes) {
        return instantaneousLicensingMOAttributes.get(Attributes.OPERATIONALSTATE.getAttributeName()) != null
                ? OperationalState.valueOf((String) instantaneousLicensingMOAttributes.get(Attributes.OPERATIONALSTATE.getAttributeName())) : null;
    }

    /**
     * @return
     */
    private AdministrativeState getAdministrativeState(final Map<String, Object> instantaneousLicensingMOAttributes) {
        return instantaneousLicensingMOAttributes.get(Attributes.ADMINISTRATIVESTATE.getAttributeName()) != null
                ? AdministrativeState.valueOf((String) instantaneousLicensingMOAttributes.get(Attributes.ADMINISTRATIVESTATE.getAttributeName())) : null;
    }

    /**
     * @param administrativeState
     * @param operationalState
     * @param availabilityStatus
     * @param fdn
     */
    private InstantaneousLicensingMOMediationTaskRequest prepareMTR(final AdministrativeState administrativeState, final OperationalState operationalState, final String fdn) {
        final InstantaneousLicensingMOMediationTaskRequest instantaneousLicensingMOMediationTaskRequest = new InstantaneousLicensingMOMediationTaskRequest();
        instantaneousLicensingMOMediationTaskRequest.setNodeAddress(fdn);
        final Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put(Attributes.ADMINISTRATIVESTATE.getAttributeName(), administrativeState.getValue());
        eventAttributes.put(Attributes.AVAILABILITYSTATUS.getAttributeName(), AvailabilityStatus.OFF_LINE.getValue());
        eventAttributes.put(Attributes.OPERATIONALSTATE.getAttributeName(), operationalState.getValue());
        instantaneousLicensingMOMediationTaskRequest.setEventAttributes(eventAttributes);
        return instantaneousLicensingMOMediationTaskRequest;
    }

}
