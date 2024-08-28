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
package com.ericsson.oss.services.shm.instantaneouslicensing

import javax.inject.Inject

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled
import com.ericsson.oss.services.shm.model.NotificationType
import com.ericsson.oss.services.shm.model.event.based.mediation.InstantaneousLicensingMOMediationTaskRequest
import com.ericsson.oss.services.shm.notifications.api.NotificationReciever
import com.ericsson.oss.services.shm.notifications.api.NotificationTypeQualifier

class InstantaneousLicensingQueueListenerTest  extends CdiSpecification {

    @ObjectUnderTest
    private InstantaneousLicensingQueueListener instantaneousLicensingQueueListener

    private static DpsAttributeChangedEvent dpsDataChangedEvent=new DpsAttributeChangedEvent()

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @MockedImplementation
    @Modeled
    private EventSender<InstantaneousLicensingMOMediationTaskRequest> eventSender

    @Inject
    private InstantaneousLicensingMoMtrSender instantaneousLicensingMOMTRSender

    @Inject
    @NotificationTypeQualifier(type = NotificationType.JOB)
    private NotificationReciever notificationReciever

    @MockedImplementation
    private DataBucket liveBucket

    def progressReport = [actionName : 'refreshKeyFile', progressPercentage : '100', state : 'FINISHED', resultInfo : '202', progressInfo : 'Accepted', result : 'SUCCESS']
    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def "verify dpsDataChangedEvent and send MTR if administrativeState,operationalState and availabilityStatus are valid "(){
        given : "dpsDataChangedEvent"
        prepareDpsDataChangeEvent(administrativeState, progressReport)
        final List<String> availStatus=new ArrayList<>()
        availStatus.add(availabilityStatus)
        prepareInstantaneousLicensingMO(administrativeState,operationalState,availStatus,progressReport)
        instantaneousLicensingQueueListener=new InstantaneousLicensingQueueListener(instantaneousLicensingMOMTRSender,notificationReciever)
        when : "dpsDataChangedEvent recevied"
        instantaneousLicensingQueueListener.onMessage(dpsDataChangedEvent)
        then : "Verify MTR is sent or not "
        expectedCount* eventSender.send(_)
        where:
        administrativeState     | operationalState      | availabilityStatus    | expectedCount
        "UNLOCKED"              |     "DISABLED"        |   "OFF_LINE"          |       1
        "LOCKED"                |     "DISABLED"        |   "OFF_LINE"          |       0
        "UNLOCKED"              |     "ENABLED"         |   "OFF_LINE"          |       0
        "UNLOCKED"              |     "DISABLED"        |   "IN_TEST"           |       0
    }

    def "verify if InstantaneousLicensingMO not found"(){
        given : "dpsDataChangedEvent"
        prepareDpsDataChangeEvent(administrativeState, progressReport)
        instantaneousLicensingQueueListener=new InstantaneousLicensingQueueListener(instantaneousLicensingMOMTRSender,notificationReciever)
        when : "dpsDataChangedEvent recevied"
        instantaneousLicensingQueueListener.onMessage(dpsDataChangedEvent)
        then : "Verify MTR is sent or not "
        expectedCount* eventSender.send(_)
        where:
        administrativeState     | expectedCount
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
    }

    def "verify if Exception occured while reading MO"(){
        given : "dpsDataChangedEvent"
        prepareDpsDataChangeEvent(administrativeState, progressReport)
        instantaneousLicensingQueueListener=new InstantaneousLicensingQueueListener(instantaneousLicensingMOMTRSender,notificationReciever)
        liveBucket.findMoByFdn(_)>> new RuntimeException()
        when : "dpsDataChangedEvent recevied"
        instantaneousLicensingQueueListener.onMessage(dpsDataChangedEvent)
        then : "Verify MTR is sent or not "
        expectedCount* eventSender.send(_)
        where:
        administrativeState     | expectedCount
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
    }

    def "verify if event is not an instanceof dps data change event"(){
        given : "dpsDataChangedEvent"
        instantaneousLicensingQueueListener=new InstantaneousLicensingQueueListener(instantaneousLicensingMOMTRSender,notificationReciever)
        when : "dpsDataChangedEvent recevied"
        instantaneousLicensingQueueListener.onMessage(new DpsObjectCreatedEvent())
        then : "Verify MTR is sent or not "
        expectedCount* eventSender.send(_)
        where:
        administrativeState     | expectedCount
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
        "UNLOCKED"              |       0
    }

    def prepareDpsDataChangeEvent(final String administrativeState, final Map<String, String> progressReportNewValues){
        dpsDataChangedEvent.setFdn("SubNetwork=NR01gNodeBRadio00001,MeContext=NR01gNodeBRadio00001,ManagedElement=NR01gNodeBRadio00001,NodeSupport=1,LicenseSupport=1,InstantaneousLicensing=1")
        dpsDataChangedEvent.setVersion("2.0.1")
        dpsDataChangedEvent.setBucketName("Live")
        dpsDataChangedEvent.setNamespace("RmeLicenseSupport")
        dpsDataChangedEvent.setType("InstantaneousLicensing;")
        final Set<AttributeChangeData> changedAttributes=new HashSet<>()
        final AttributeChangeData attributeChangeData=new AttributeChangeData()
        attributeChangeData.setName("administrativeState")
        attributeChangeData.setOldValue("LOCKED")
        attributeChangeData.setNewValue(administrativeState)
        attributeChangeData.setDeltaRemoved(null)
        attributeChangeData.setDeltaAdded(null)
        changedAttributes.add(attributeChangeData)
        final AttributeChangeData attributeChangeData1=new AttributeChangeData()
        attributeChangeData1.setName('progressReport')
        attributeChangeData1.setOldValue(null)
        attributeChangeData1.setNewValue(progressReportNewValues)
        attributeChangeData1.setDeltaRemoved(null)
        attributeChangeData1.setDeltaAdded(null)
        changedAttributes.add(attributeChangeData1)
        dpsDataChangedEvent.setChangedAttributes(changedAttributes)
    }

    def prepareInstantaneousLicensingMO(final String administrativeState,final String operationalState,final List<String> availabilityStatus, final Map<String, String> progressReportNewValues){
        ManagedObject mo = runtimeDps.addManagedObject().withFdn("SubNetwork=NR01gNodeBRadio00001,MeContext=NR01gNodeBRadio00001,ManagedElement=NR01gNodeBRadio00001,NodeSupport=1,LicenseSupport=1,InstantaneousLicensing=1")
                .namespace("RmeLicenseSupport")
                .addAttribute("administrativeState", administrativeState)
                .addAttribute("operationalState", operationalState)
                .addAttribute("availabilityStatus", availabilityStatus)
                .addAttribute("progressReport", progressReportNewValues)
                .type("InstantaneousLicensing")
                .build()
    }
}
