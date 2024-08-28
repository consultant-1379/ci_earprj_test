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
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.services.shm.common.DpsWriter
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.es.api.*
import com.ericsson.oss.services.shm.es.api.ecim.EcimCommonConstants
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.inventory.software.ecim.api.ActionResult
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType
import com.ericsson.oss.services.shm.model.NotificationSubject
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.Notification
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator

class EcimLicenseRefreshDataProviderTest extends CdiSpecification {

    @MockedImplementation
    protected NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    protected JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator

    @MockedImplementation
    protected Notification notification

    @MockedImplementation
    protected DpsAttributeChangedEvent event

    @MockedImplementation
    protected NotificationSubject notificationSubject

    @MockedImplementation
    protected ActivityUtils activityUtils

    @MockedImplementation
    protected JobEnvironment jobEnvironment

    @MockedImplementation
    protected ActivityStepResult activityStepResult

    @Inject
    protected LicenseRefreshServiceProvider licenseRefreshServiceProvider

    @Inject
    private DpsWriter dpsWriter

    def nodeName = "LTE01dg2ERBS00001"
    def activityJobId = 3L
    def neJobId = 2L
    def mainJobId = 1L
    def actionId = 1
    def upMoFdn ="ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,InstantaneousLicensing=1"
    def ilMoFdn = "SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,InstantaneousLicensing=1"
    def JobActivityInfo jobActivityInfo = new JobActivityInfo(activityJobId, "refresh", JobTypeEnum.LICENSE_REFRESH, PlatformTypeEnum.ECIM)
    def NEJobStaticData neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, "1234", "ECIM", 5L, "")
    def JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.LICENSE_REFRESH)

    def Map<String, Object> jobConfiguration=new HashMap<String,Object>()
    def AttributeChangeData corrIdAttributeChangeData = new AttributeChangeData("correlationId", _, "2@1", new Object(), new Object())
    def progressReport = [actionName : 'refreshKeyFile', progressPercentage : (Short) 100, state : 'FINISHED', resultInfo : '202', progressInfo : 'Accepted', result : 'SUCCESS']
    def AttributeChangeData progReportAttributeChangeData = new AttributeChangeData("progressReport", null, progressReport, null, null)
    def DpsAttributeChangedEvent dpsAttributeChangedEvent = new DpsAttributeChangedEvent("shm", "ShmNodeLicenseRefreshRequestData", "1.0.0",3L, null,"liveBucket", Arrays.asList(corrIdAttributeChangeData))
    def ActionResult actionResult = new ActionResult()
    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    def modifiedAttributes = ['correlationId':(corrIdAttributeChangeData), 'progressReport' : (progReportAttributeChangeData)]

    def loadJobProperties(final String nodeName,final String activityName) {
        loadMainJob()
        loadNEJobAttributes(nodeName)
        loadActivityJobProperties(activityName)
    }

    def loadFailedJobProperties(final String nodeName,final String activityName) {
        loadMainJob()
        loadNEJobAttributes(nodeName)
        loadFailedActivityJobProperties(activityName)
    }

    def loadMainJob() {
        final Map<String, Object> mainjobAttributes = new HashMap<>()
        final Map<String, Object> mainjobAttributesSwpName = new HashMap<>()
        final Map<String, Object> mainJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> mainJobAttributesList = new ArrayList<>()

        jobConfiguration.put(ShmJobConstants.JOBPROPERTIES, mainJobAttributesList)
        mainJobAttributesMap.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfiguration)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("MainJob").addAttributes(mainJobAttributesMap).build()
    }

    def buildDataForRefreshActivity(final String nodeName) {

        neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> neJobStaticData
        jobStaticDataProvider.getJobStaticData(mainJobId) >> jobStaticData
    }

    def buildLicenseRefreshDataPo(){
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ShmNodeLicenseRefreshRequestData").version("1.0.0").addAttribute("neJobId", neJobId).addAttribute("networkElementName", nodeName).build()
    }

    private loadNEJobAttributes(final String nodeName) {
        final Map<String, Object> neJobAttributesMap = new HashMap<>()
        final Map<String, Object> neJobPropertyUserInput = new HashMap<>()
        final List<Map<String,Object>> neSpecificPropertyList = new ArrayList<>()

        neJobPropertyUserInput.put(ShmJobConstants.KEY, UpgradeActivityConstants.UP_FDN)
        neJobPropertyUserInput.put(ShmJobConstants.VALUE, upMoFdn)
        neSpecificPropertyList.add(neJobPropertyUserInput)

        neJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, neSpecificPropertyList)

        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID,mainJobId)
        neJobAttributesMap.put(ShmConstants.NE_NAME, nodeName)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("NeJob").addAttributes(neJobAttributesMap).build()
    }

    private loadActivityJobProperties(final String activityName) {

        final Map<String, Object> activityJobAttributesMap=getactivityJobAttributesMap(activityName)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
    }

    private loadFailedActivityJobProperties(final String activityName) {

        final Map<String, Object> activityJobAttributesMap=getFailedActivityJobAttributesMap(activityName)
        runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
    }

    protected Map<String,Object> getactivityJobAttributesMap(final String activityName) {
        final  Map<String, Object> activityjobAttributes = new HashMap<>()
        final Map<String, Object> activityJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()
        final Map<String,Object> activityJobProp = new HashMap<String,Object>()
        activityjobAttributes.put(ShmJobConstants.KEY, EcimCommonConstants.ACTION_TRIGGERED)
        activityjobAttributes.put(ShmJobConstants.VALUE, activityName)

        activityJobAttributesList.add(activityjobAttributes)

        activityjobAttributes.put(ShmJobConstants.KEY, ShmConstants.RESULT)
        activityjobAttributes.put(ShmJobConstants.VALUE, "SUCCESS")
        activityJobAttributesList.add(activityjobAttributes)

        Map<String, Object> correlationId = new HashMap<>()
        correlationId.put(ShmJobConstants.KEY, "correlationId")
        correlationId.put(ShmJobConstants.VALUE, "2@1")
        activityJobAttributesList.add(correlationId)

        Map<String, Object> ilMoFdnMap = new HashMap<>()
        ilMoFdnMap.put(ShmJobConstants.KEY, LicenseRefreshConstants.INSTANTANEOUS_LICENSING_MO_FDN)
        ilMoFdnMap.put(ShmJobConstants.VALUE, ilMoFdn)
        activityJobAttributesList.add(ilMoFdnMap)

        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L)
        activityJobAttributesMap.put(ShmConstants.NE_NAME,nodeName)
        final double progressPercentage = 100.0
        activityJobAttributesMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobAttributesMap.put(ShmConstants.ENTRY_TIME, new Date())
        return activityJobAttributesMap
    }

    protected Map<String,Object> getFailedActivityJobAttributesMap(final String activityName) {
        final  Map<String, Object> activityjobAttributes = new HashMap<>()
        final Map<String, Object> activityJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()
        final Map<String,Object> activityJobProp = new HashMap<String,Object>()

        activityjobAttributes.put(ShmJobConstants.KEY, ShmConstants.RESULT)
        activityjobAttributes.put(ShmJobConstants.VALUE, "FAILED")
        activityJobAttributesList.add(activityjobAttributes)

        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, 1L)
        activityJobAttributesMap.put(ShmConstants.NE_NAME,nodeName)
        final double progressPercentage = 0.0
        activityJobAttributesMap.put(ShmConstants.PROGRESSPERCENTAGE,progressPercentage)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_START_DATE, new Date())
        activityJobAttributesMap.put(ShmConstants.ENTRY_TIME, new Date())
        return activityJobAttributesMap
    }


    def getActionResultWhenActionTriggeredisSuccess() {
        actionResult.setActionId(1)
        actionResult.setTriggerSuccess(true)
        dpsWriter.performAction(_ as String ) >> actionResult
    }

    def buildInstantaneousLicensingMO(final String nodeName){

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName)
                .addAttribute('SubNetworkId', nodeName)
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("SubNetwork")
                .build()
        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("MeContextId",nodeName)
                .addAttribute('neType', 'RadioNode')
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', 'RadioNode')
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .type("NetworkElement")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+",CmFunction=1")
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName+",CmFunction=1").addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("SubNetwork="+nodeName))

        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName)
                .addAttribute('ManagedElementId',nodeName)
                .addAttribute('managedElementType ', 'RadioNode')
                .addAttribute('neType', 'RadioNode')
                .type("ManagedElement")
                .build()


        runtimeDps.addManagedObject().withFdn("SubNetwork="+nodeName+",MeContext="+nodeName+",ManagedElement="+nodeName+",SystemFunctions=1,InstantaneousLicensing=1")
                .addAttribute('instantaneousLicensingId',1)
                .namespace('RmeLicenseSupport')
                .version("2")
                .type("InstantaneousLicensing")
                .onAction("refreshKeyFile")
                .returnValue("1")
                .build()
    }


    def PersistenceObject buildShmNodeLicenseRefreshRequestDataPO(final String nodeName){

        final  Map<String, Object> shmNodeLicenseRefreshRequestDataAttributes = new HashMap<>()

        shmNodeLicenseRefreshRequestDataAttributes.put("neJobId", String.valueOf(neJobId))
        shmNodeLicenseRefreshRequestDataAttributes.put("networkElementName", nodeName)
        shmNodeLicenseRefreshRequestDataAttributes.put("actionId", 1)
        shmNodeLicenseRefreshRequestDataAttributes.put("licenseRefreshSource", "SHM")

        PersistenceObject po = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ShmNodeLicenseRefreshRequestData")
                .addAttributes(shmNodeLicenseRefreshRequestDataAttributes).create()
        return po
    }

    def String extractJobProperty(String propertyName, List<Map<String, String>> jobProperties){
        for(Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null
    }

    def String extractMessage(List log){
        if(log == null){
            return null
        }
        List logMessages = new ArrayList<>()
        for(Map logEntry:log){
            logMessages.add("message:"+logEntry.get("message")+", logLevel:"+logEntry.get("logLevel"))
        }
        return logMessages.toString()
    }
}
