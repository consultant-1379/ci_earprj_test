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

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.INSTANTANEOUS_LICENSING_NAMESPACE

import javax.inject.Inject

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider
import com.ericsson.oss.services.shm.es.api.ActivityCallback
import com.ericsson.oss.services.shm.es.api.JobActivityInfo
import com.ericsson.oss.services.shm.es.api.JobUpdateService
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoService
import com.ericsson.oss.services.shm.es.ecim.licensing.common.LicenseMoServiceRetryProxy
import com.ericsson.oss.services.shm.es.impl.ActivityServiceProvider
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.license.refresh.api.LkfImportResponse
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMHandler
import com.ericsson.oss.services.shm.inventory.license.ecim.api.LMVersionHandlersProviderFactory
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum
import com.ericsson.oss.services.shm.model.NotificationSubject
import com.ericsson.oss.services.shm.model.events.instlicense.ShmLicenseRefreshElisRequest
import com.ericsson.oss.services.shm.model.licensekeyinfo.LicenseRequestStatus
import com.ericsson.oss.services.shm.model.notification.ShmElisLicenseRefreshNotification
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider
import com.ericsson.oss.services.shm.notifications.api.Notification
import com.ericsson.oss.services.shm.notifications.api.NotificationRegistry
import com.ericsson.oss.services.shm.notifications.impl.FdnNotificationSubject
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants

class RequestServiceActivityDataProviderSpec extends CdiSpecification{

    @MockedImplementation
    PlatformTypeProviderImpl platformTypeProvider

    @MockedImplementation
    NeJobStaticDataProvider neJobStaticDataProvider

    @MockedImplementation
    Notification notification

    @MockedImplementation
    JobStaticDataProvider jobStaticDataProvider

    @MockedImplementation
    ActivityUtils activityUtils

    @MockedImplementation
    EventSender<ShmLicenseRefreshElisRequest> shmLicenseRefreshElisRequestEvent

    @MockedImplementation
    JobUpdateService jobUpdateService

    @MockedImplementation
    NotificationRegistry registry

    @MockedImplementation
    NotificationSubject notificationSubject

    @MockedImplementation
    ActivityServiceProvider activityServiceProvider

    @MockedImplementation
    ActivityCallback activityImpl

    @MockedImplementation
    LkfImportResponse lkfImportResponse

    @MockedImplementation
    LicenseMoServiceRetryProxy licenseMoServiceRetryProxy

    @MockedImplementation
    UpgradeLicenseKeyServiceProvider UpgradeLicenseKeyServiceProvider

    @Inject
    SystemRecorder systemRecorder

    @Inject
    LicenseRefreshServiceProvider licenseRefreshServiceProvider

    def nodeName = "NR01gNodeBRadio00001"
    def radioNodeFingerPrint = "NR01gNodeBRadio00001_fp"
    @MockedImplementation
    OssModelInfoProvider ossModelInfoProvider

    @MockedImplementation
    OssModelInfo ossModelInfo

    @MockedImplementation
    LMVersionHandlersProviderFactory lmVersionHandlersProviderFactory

    @MockedImplementation
    LMHandler lmHandler

    @MockedImplementation
    LicenseMoService licenseMoService

    @MockedImplementation
    RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    def activityJobId
    def neJobId
    def mainJobId
    def radioNodeFdn = "NetworkElement=NR01gNodeBRadio00001,SystemFunctions=1,InstantaneousLicensing=1"
    def templateJobId
    def SUCCESS="SUCCESS"
    def EXCEPTION="EXCEPTION"
    def FAIL="FAIL"
    def JOB_DATA_NOT_FOUND_EXCEPTION = "JOB_DATA_NOT_FOUND_EXCEPTION"
    def UNSUPPORTED_FRAGMENT_EXCEPTION = "UNSUPPORTED_FRAGMENT_EXCEPTION"
    def businessKey

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps.class)
    def jobActivityInfo
    def fdnNotificationSubject
    def neJobStaticData

    Map<String, Object> fingerprintMap = new HashMap<>()
    def Map<String, Object> mainJobAttributes=new HashMap<String,Object>();
    def Map<String, Object> templateJobAttributes=new HashMap<String,Object>();

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.cache")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.api")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.job.utils")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.rest")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs.common.modelentities")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.exception.MoNotFoundException")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.api")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.impl")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.impl")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh.notification")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.license.refresh.api")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def loadLicenseRefreshJobProperties(final String nodeName, final String activityName, final String result, final String refreshType) {
        if(refreshType.equals("UpgradeLicensekeys")){
            loadTemplateJobAttributes()
            loadInstantaneouslicensingMoAttribute()
            loadSoftwarePackagePo()
            remoteSoftwarePackageManager.getSoftwarPackageReleaseVersion(_ as String) >> "19.Q3"
            licenseMoService.getFingerPrintFromNode(_) >> radioNodeFingerPrint
        }else{
            loadInstantaneouslicensingMoAttribute()
            upgradeLicenseKeyServiceProvider.getLkfRequestTypeInitiatedByNodeOrSoftwarePackage(_) >> null
        }
        loadLicenseRefreshJobAttributes(nodeName, activityName,result)
    }

    def loadLicenseRefreshJobAttributes(final String nodeName, final String activityName, final String result) {
        Map<String, Object> jobConfigurationDetails = new HashMap<>()
        Map<String, Object> neJobProperty = new HashMap()
        Map<String, String> jobProperty = new HashMap<>()
        Map<String, String> swpJobProperty = new HashMap<>()
        List<Map<String, Object>> mainJobAttributesList = new ArrayList<>()
        List<Map<String, Object>> neJobPropertyList = new ArrayList()
        List<Map<String, String>> jobproperties = new ArrayList()
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "LICENSE_FILEPATH")
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "/home/smrs/smrsroot/license/NR01gNodeBRadio00001_fp.xml")
        swpJobProperty.put(ActivityConstants.JOB_PROP_KEY,UpgradeActivityConstants.SWP_NAME);
        swpJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "17ARadioNodePackage3");
        jobproperties.add(jobProperty)
        jobproperties.add(swpJobProperty)
        neJobProperty.put("neName", nodeName)
        neJobProperty.put(ActivityConstants.JOB_PROPERTIES, jobproperties)
        neJobPropertyList.add(neJobProperty)
        jobConfigurationDetails.put(ShmJobConstants.NEJOB_PROPERTIES, neJobPropertyList)
        mainJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails)
        mainJobAttributes.put(ShmConstants.JOBTEMPLATEID, templateJobId)
        PersistenceObject mainJob =runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("Job").addAttributes(mainJobAttributes).build()
        mainJobId = mainJob.getPoId();

        loadNetworkElementJobAttributes(nodeName)
        loadActivityJobPropertiesForRequest(activityName,result)

        neJobStaticData = new NEJobStaticData(neJobId, mainJobId, nodeName, businessKey, "ECIM", 5L, "")
        jobActivityInfo = new JobActivityInfo(activityJobId, "request", JobTypeEnum.LICENSE_REFRESH, PlatformTypeEnum.ECIM)
        businessKey = radioNodeFingerPrint+"@"+String.valueOf(activityJobId)
        fdnNotificationSubject = new FdnNotificationSubject(radioNodeFdn, activityJobId, jobActivityInfo)
    }

    def loadTemplateJobAttributes(){
        List<Map<String, String>> jobproperties = new ArrayList()
        Map<String, Object> jobConfigurationDetails = new HashMap<>()
        Map<String, String> jobProperty = new HashMap<>()
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "LicenseRefreshType")
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "UpgradeLicensekeys")
        jobproperties.add(jobProperty)
        jobConfigurationDetails.put(ActivityConstants.JOB_PROPERTIES, jobproperties)
        templateJobAttributes.put(ActivityConstants.JOB_CONFIGURATION_DETAILS, jobConfigurationDetails)
        PersistenceObject templateJob =runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("JobTemplate").addAttributes(templateJobAttributes).build()
        templateJobId = templateJob.getPoId();
    }

    private loadActivityJobPropertiesForRequest(final String activityName, final String result) {
        final Map<String, Object> activityJobAttributesMap = getRequestActivityJobAttributesMap(activityName, result)
        PersistenceObject activityJob =  runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ActivityJob").addAttributes(activityJobAttributesMap).build()
        activityJobId = activityJob.getPoId();
    }

    private loadNetworkElementJobAttributes(final String nodeName) {

        final Map<String, Object> neJobAttributesMap = new HashMap<>()
        Map<String, String> jobProperty = new HashMap<>()
        Map<String, String> requestIdJobProperty = new HashMap<>();
        Map<String, String> requestTypeJobProperty = new HashMap<>();
        List<Map<String, String>> jobproperties = new ArrayList<>()
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, "LICENSE_FILEPATH")
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, "home/smrs/smrsroot/license/ilPath")
        jobproperties.add(jobProperty)
        requestIdJobProperty.put(ActivityConstants.JOB_PROP_KEY, LicenseRefreshConstants.REQUEST_ID);
        requestIdJobProperty.put(ActivityConstants.JOB_PROP_VALUE, "345");
        jobproperties.add(requestIdJobProperty);
        requestTypeJobProperty.put(ActivityConstants.JOB_PROP_KEY, LicenseRefreshConstants.REQUEST_TYPE);
        requestTypeJobProperty.put(ActivityConstants.JOB_PROP_VALUE, LicenseRefreshConstants.LKF_REFRESH);
        jobproperties.add(requestTypeJobProperty);
        neJobAttributesMap.put(ShmConstants.MAIN_JOB_ID,mainJobId)
        neJobAttributesMap.put(ShmConstants.NE_NAME, nodeName)
        neJobAttributesMap.put(ActivityConstants.JOB_PROPERTIES, jobproperties)
        PersistenceObject neJob  =  runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("NeJob").addAttributes(neJobAttributesMap).build()
        neJobId = neJob.getPoId();
    }

    def Map<String,Object> getRequestActivityJobAttributesMap(final String activityName, final String result) {
        final  Map<String, Object> activityjobAttributes = new HashMap<>()
        final Map<String, Object> activityJobAttributesMap = new HashMap<>()
        final List<Map<String, Object>> activityJobAttributesList = new ArrayList<>()

        activityjobAttributes.put(ShmJobConstants.KEY, ShmConstants.RESULT)
        activityjobAttributes.put(ShmJobConstants.VALUE, result)
        activityJobAttributesList.add(activityjobAttributes)

        Map<String, Object> fingerprintMap = new HashMap<>()
        fingerprintMap.put(ShmJobConstants.KEY, LicenseRefreshConstants.FINGERPRINT)
        fingerprintMap.put(ShmJobConstants.VALUE, radioNodeFingerPrint)
        activityJobAttributesList.add(fingerprintMap)

        activityJobAttributesMap.put(ShmJobConstants.JOBPROPERTIES, activityJobAttributesList)
        activityJobAttributesMap.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJobId)
        activityJobAttributesMap.put(ShmConstants.NE_NAME,nodeName)
        return activityJobAttributesMap
    }

    def buildDataForRequestActivity(final String nodeName,final String scenario) {

        switch(scenario) {
            case EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw new Exception("Exception Occurred") }
                break
            case JOB_DATA_NOT_FOUND_EXCEPTION:
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> { throw new JobDataNotFoundException("JobDataNotFoundException Occurred") }
                break
            default :
                neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY) >> neJobStaticData
        }
    }

    def String getLicenseRefreshJobProperty(final String propertyName,final List<Map<String, String>> jobProperties){

        for(Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return  jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null
    }

    def long buildLicenseRefreshRequestPO(){
        PersistenceObject licenseRefreshRequestDataPo = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("ShmNodeLicenseRefreshRequestData").addAttributes(prepareLicenseRefreshRequestData()).build()
        return licenseRefreshRequestDataPo.getPoId()
    }

    def long prepareLkfRequestDataPo() {
        Map<String, Object> lfkRequestDataAttributes = new HashMap<>()
        Map<String, String> jobIds = new HashMap<>()
        jobIds.put("NR01gNodeBRadio00001_fp", "2")
        jobIds.put("NR01gNodeBRadio00002_fp", "89898")
        lfkRequestDataAttributes.put("jobIds", jobIds)
        lfkRequestDataAttributes.put("requestId", "F8FA8AAF-6CEF-4E2C-9727-481686BB1AFB")
        lfkRequestDataAttributes.put("requestStatus", LicenseRequestStatus.REQUEST_COMPLETE.toString())
        lfkRequestDataAttributes.put("packagePath", "/home/smrs/smrsroot/NR01gNodeBRadio00001_fp_180924_141625.zip")
        lfkRequestDataAttributes.put("errorInformation", "")
        PersistenceObject lkfRequestDataPo = runtimeDps.addPersistenceObject().namespace(ShmJobConstants.NAMESPACE).type("LkfRequestData").addAttributes(lfkRequestDataAttributes).build()
        return lkfRequestDataPo.getPoId()
    }

    def Map<String, Object> prepareLicenseRefreshRequestData(){
        final Map<String, Object> licenseRefreshRequestData = new HashMap<>()
        licenseRefreshRequestData.put("nodeType", "RadioNode")
        licenseRefreshRequestData.put("neJobId", "2")
        licenseRefreshRequestData.put("networkElementName", nodeName)
        licenseRefreshRequestData.put("correlationId", "12345")
        licenseRefreshRequestData.put("licenseRefreshSource", "NODE")
        licenseRefreshRequestData.put("status", "NODE_REFRESH_REQUESTED")
        licenseRefreshRequestData.put("licenseRefreshRequestInfo", prepareLicenseRefreshRequestInfo())
        return licenseRefreshRequestData
    }

    def Map<String, Object> prepareLicenseRefreshRequestInfo(){
        final Map<String, Object> licenseRefreshRequestInfo = new HashMap<>()
        licenseRefreshRequestInfo.put("fingerprint", radioNodeFingerPrint)
        licenseRefreshRequestInfo.put("swRelease", "19.Q2")
        licenseRefreshRequestInfo.put("euft", "euft1234")
        licenseRefreshRequestInfo.put("swltId", "swltId1234")
        licenseRefreshRequestInfo.put("licenseRefreshRequestType", "NODE_REFRESH_REQUESTED")
        licenseRefreshRequestInfo.put("actionId", 345)
        licenseRefreshRequestInfo.put("dnPrefix", "dnPrefix1233")
        licenseRefreshRequestInfo.put("basebandType", "basebandType1235")
        licenseRefreshRequestInfo.put("nodeType", "5G")
        licenseRefreshRequestInfo.put("capacities", getCapacities())
        return licenseRefreshRequestInfo
    }

    def List<Map<String, Object>> getCapacities() {
        final List<Map<String, Object>> capacities = new ArrayList<>()
        for (int i = 0; i <= 2; i++) {
            final Map<String, Object> capacityMap = new HashMap<>()
            capacityMap.put("keyId", "key_" + i)
            capacityMap.put("requiredLevel", "100" + i)
            capacities.add(capacityMap)
        }
        return capacities
    }

    def ShmElisLicenseRefreshNotification prepareShmElisLicenseRefreshNotification(final String state, final String status, final String additionalInfo){
        ShmElisLicenseRefreshNotification shmElisLicenseRefreshNotification = new ShmElisLicenseRefreshNotification()
        shmElisLicenseRefreshNotification.setFingerPrint(radioNodeFingerPrint)
        shmElisLicenseRefreshNotification.setNeJobId(String.valueOf(neJobId))
        shmElisLicenseRefreshNotification.setState(state)
        shmElisLicenseRefreshNotification.setStatus(status)
        shmElisLicenseRefreshNotification.setAdditionalInfo(additionalInfo)
        shmElisLicenseRefreshNotification.setEventAttributes(new HashMap())
        return shmElisLicenseRefreshNotification
    }

    def loadInstantaneouslicensingMoAttribute(){
        PersistenceObject targetPO   = runtimeDps.addPersistenceObject()
                .namespace("DPS")
                .type("null")
                .addAttribute("name",nodeName)
                .addAttribute('type', "RadioNode")
                .addAttribute('category', "NODE")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+","+"CmFunction=1")
                .namespace("OSS_NE_CM_DEF")
                .addAttribute("syncStatus", "SYNCHRONIZED")
                .type("CmFunction")
                .build()
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName)
                .addAttribute('MeContextId', nodeName)
                .addAttribute('neType', 'MSC-BC-BSP')
                .addAttribute('platformType', 'AXE')
                .namespace('OSS_TOP')
                .version("3.0.0")
                .type("MeContext")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', "RadioNode")
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("utcOffset","utcOffset")
                .addAttribute("timeZone","timeZone")
                .addAttribute("technologyDomain", ['5GS', 'EPS'])
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .target(targetPO)
                .type("NetworkElement")
                .build()
        runtimeDps.build().getLiveBucket().findMoByFdn("NetworkElement="+nodeName).addAssociation("nodeRootRef", runtimeDps.build().getLiveBucket().findMoByFdn("MeContext="+nodeName))
        Map<String, String> attributes = new HashMap()
        attributes.put(LicenseRefreshConstants.EUFT,"sampleEuft")
        attributes.put(LicenseRefreshConstants.SWLT_ID,"sampleSWLT_ID");
        runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+",ManagedElement="+nodeName+",NodeSupport=1,LicenseSupport=1,InstantaneousLicensing=1")
                .namespace(INSTANTANEOUS_LICENSING_NAMESPACE).type(LicenseRefreshConstants.INSTANTANEOUS_LICENSING_MO).addAttributes(attributes).build()
    }

    def loadSoftwarePackagePo(){
        Map<String, String> attributes = new HashMap()
        attributes.put( "release","19.Q3")
        runtimeDps.addPersistenceObject().namespace("ImportSoftwarePackage").type("EcimSoftwarePackage").addAttributes(attributes).build()
    }
}
