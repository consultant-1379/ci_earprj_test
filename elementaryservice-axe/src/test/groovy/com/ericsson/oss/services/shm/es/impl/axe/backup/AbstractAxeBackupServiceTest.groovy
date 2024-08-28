/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.axe.backup

import java.util.Map.Entry

import javax.ws.rs.core.MultivaluedMap
import javax.xml.bind.DatatypeConverter

import org.jboss.resteasy.client.ClientResponse
import org.jboss.resteasy.specimpl.MultivaluedMapImpl

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.security.cryptography.CryptographyService
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl
import com.ericsson.oss.services.shm.common.wfs.WfsRetryConfigurationParamProvider
import com.ericsson.oss.services.shm.es.axe.common.AxeConstants
import com.ericsson.oss.services.shm.es.axe.common.SessionIdResponse
import com.ericsson.oss.services.shm.es.axe.common.WinFIOLRequestDispatcher
import com.ericsson.oss.services.shm.es.dps.events.DpsStatusInfoProvider
import com.ericsson.oss.services.shm.es.polling.PollingActivityManager
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeApgProductRevisionProvider
import com.ericsson.oss.services.shm.job.service.JobParameterChangeListener
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier

public abstract class AbstractAxeBackupServiceTest extends CdiSpecification{

    @MockedImplementation
    protected ActivityJobTBACValidator activityJobTBACValidator;

    @MockedImplementation
    protected JobParameterChangeListener jobParameterChangeListener;

    @MockedImplementation
    protected WfsRetryConfigurationParamProvider wfsRetryConfigurationParamProvider;

    @MockedImplementation
    protected WorkflowInstanceNotifier workflowInstanceNotifier;

    @MockedImplementation
    protected WinFIOLRequestDispatcher winFIOLRequestDispatcher;

    @MockedImplementation
    protected PlatformTypeProviderImpl platformProvider;

    @MockedImplementation
    protected PollingActivityManager pollingActivityManager;

    @MockedImplementation
    protected DpsStatusInfoProvider dpsStatusInfoProvider;

    @MockedImplementation
    protected CryptographyService cryptographyService;

    @MockedImplementation
    protected ClientResponse clientResponse;
    
    @MockedImplementation
    AxeApgProductRevisionProvider axeApgProductRevisionProvider;


    public PersistenceObject neJob;

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
    protected static final String NODE_NAME = "MSC-BC-BSP-01__CP1"
    protected static final String PARENT_NODE_NAME="MSC-BC-BSP-01"
    protected static final String NE_TYPE_MSC_BC = "MSC-BC-BSP"

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.shared")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.axe.common")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.nejob")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.networkelement")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.cache")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.axe")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.job.api")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.job.utils")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.es.upgrade.remote")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.rest")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.jobs.common.modelentities")
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm.common.exception.MoNotFoundException")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def addNetworkElementMOs(String nodeName){

        PersistenceObject targetPO   = runtimeDps.addPersistenceObject()
                .namespace("DPS")
                .type("null")
                .addAttribute("name",nodeName)
                .addAttribute('type', NE_TYPE_MSC_BC)
                .addAttribute('category', "NODE")
                .build()

        final ManagedObject networkElementMo=runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute("NetworkElementId",nodeName)
                .addAttribute('neType', NE_TYPE_MSC_BC)
                .addAttribute("ossModelIdentity","17A-R2YX")
                .addAttribute("nodeModelIdentity","17A-R2YX")
                .addAttribute('ossPrefix',"SubNetwork="+nodeName+",MeContext="+nodeName)
                .addAttribute("utcOffset","utcOffset")
                .addAttribute("timeZone","timeZone")
                .namespace('OSS_NE_DEF')
                .version("2.0.0")
                .target(targetPO)
                .type("NetworkElement")
                .build()
        runtimeDps.addManagedObject().withFdn("NetworkElement="+PARENT_NODE_NAME+",SecurityFunction=1,NetworkElementSecurity=1")
                .addAttribute("secureUserName", "axeUser")
                .addAttribute("secureUserPassword", encryptPassword(cryptographyService,"axePassword"))
                .namespace('OSS_NE_SEC_DEF')
                .type("NetworkElementSecurity")
                .build()
        final ManagedObject connectivityInformationMo=runtimeDps.addManagedObject().withFdn("NetworkElement="+PARENT_NODE_NAME+",MscConnectivityInformation=1")
                .addAttribute("ipAddress", "192.168.101.138")
                .addAttribute("apnodeAIpAddress", "192.168.101.139")
                .addAttribute("apnodeBIpAddress", "192.168.101.140")
                .namespace('MSC_MED')
                .type("MscConnectivityInformation")
                .build()
        targetPO.addAssociation("ciRef", connectivityInformationMo)
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+",CmFunction=1")
                .addAttribute("syncStatus","SYNCHRONIZED")
                .type("NetworkElement")
                .build()
    }

    def String encryptPassword(final CryptographyService cryptographyService, final String password) {
        byte[] encryptedNormalUserPassword = null;
        try {
            encryptedNormalUserPassword = cryptographyService.encrypt(password.getBytes(AxeConstants.CHAR_ENCODING));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        final String enCryptedPassword = encryptedNormalUserPassword == null ? "shroot" : DatatypeConverter.printBase64Binary(encryptedNormalUserPassword);
        return enCryptedPassword;
    }

    def long persistJobs(Map<String, Object> jobTemplateData, Map<String, Object> mainJobData,  Map<String, Object> neJobData, Map<String, Object> activityJobData){
        PersistenceObject jobTemplate   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOBTEMPLATE).addAttributes(jobTemplateData).build();
        mainJobData.put(ShmConstants.JOB_TEMPLATE_ID,jobTemplate.getPoId() )
        PersistenceObject mainJob       = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.JOB).addAttributes(mainJobData).build();
        neJobData.put(ShmConstants.BUSINESS_KEY,neJobData.get(ShmConstants.NE_NAME)+"@"+mainJob.getPoId());
        neJobData.put(ShmConstants.MAIN_JOB_ID, mainJob.getPoId());
        neJob = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.NE_JOB).addAttributes(neJobData).build();
        activityJobData.put(ShmConstants.ACTIVITY_NE_JOB_ID, neJob.getPoId());
        PersistenceObject activityJob   = runtimeDps.addPersistenceObject().namespace(ShmConstants.NAMESPACE).type(ShmConstants.ACTIVITY_JOB).addAttributes(activityJobData).build();
        return activityJob.getPoId()
    }

    def String extractMessage(List log){
        if(log == null){
            return null
        }
        List logMessages = new ArrayList<>();
        for(Map logEntry:log){
            logMessages.add("message:"+logEntry.get("message")+", logLevel:"+logEntry.get("logLevel"));
        }
        return logMessages.toString()
    }

    def String extractJobProperty(String propertyName, List<Map<String, String>> jobProperties){
        for(Map<String, String> jobProperty:jobProperties){
            if(propertyName.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))){
                return jobProperty.get(ActivityConstants.JOB_PROP_VALUE)
            }
        }
        return null
    }

    def generateActivityProp(Map<String, String> jobProperties){
        List properties = new ArrayList<>()
        for(Entry<String, Object> prop:jobProperties.entrySet()){
            if(prop.getValue()!=null){
                Map<String, Object> activityProp = new HashMap<>();
                activityProp.put("key",prop.getKey())
                activityProp.put("value",prop.getValue())
                properties.add(activityProp)
            }
        }
        return properties
    }

    def generateNeTypeJobProperties(Boolean rotate,Boolean overwrite, String password){
        List<Map<String,Object>> properties = new ArrayList<>()
        Map<String,Object> prop1 = new HashMap<>();
        if(rotate==null && overwrite==null){
            return properties
        }
        if(rotate){
            prop1.put(ShmConstants.KEY,"Rotate")
            prop1.put(ShmConstants.VALUE,"true")
            properties.add(prop1)
        }else{
            prop1.put(ShmConstants.KEY,"Rotate")
            prop1.put(ShmConstants.VALUE,"false")
            properties.add(prop1)
        }
        Map<String,Object> prop2 = new HashMap<>();
        if(overwrite){
            prop2.put(ShmConstants.KEY,"Overwrite")
            prop2.put(ShmConstants.VALUE,"true")
            properties.add(prop2)
        }else{
            prop2.put(ShmConstants.KEY,"Overwrite")
            prop2.put(ShmConstants.VALUE,"false")
            properties.add(prop2)
        }
        if(password !=null){
            prop2.put(ShmConstants.KEY,"Password")
            prop2.put(ShmConstants.VALUE,"bB02kydIDYEi9+MCnlhETdCLNj6ur+xEGrYelZK3250=")
            properties.add(prop2)
        }
        return properties
    }
    def  SessionIdResponse getSessionIdResponse(final String actSessionId,final String actError){
        SessionIdResponse sessionIdResponse=new SessionIdResponse();
        sessionIdResponse.setSessionId(actSessionId);
        sessionIdResponse.setHostname("svc-2-winfiol");
        sessionIdResponse.setError(actError);
        sessionIdResponse.setCookie("WINFIOL_SERVERID=s1; path=/");
        sessionIdResponse.setCode(null);
        return sessionIdResponse;
    }

    def MultivaluedMap<String, Object> setCookieData(){
        MultivaluedMap<String, Object> headers = new MultivaluedMapImpl() ;
        headers.add(AxeConstants.SET_COOKIE_HEADER,"WINFIOL_SERVERID=s1; path=/");
        return headers;
    }
    
    def Map<String, String> getNeApgVersionData(final String componenetName, final String apgVersion){
        Map<String, String> neApgData = new HashMap() ;
        neApgData.put(componenetName,apgVersion);
        return neApgData;
    }
}