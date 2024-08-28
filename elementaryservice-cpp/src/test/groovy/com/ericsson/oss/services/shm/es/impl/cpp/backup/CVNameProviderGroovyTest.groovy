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

package com.ericsson.oss.services.shm.es.impl.cpp.backup

import java.text.SimpleDateFormat

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider
import com.ericsson.oss.services.shm.es.impl.JobLogConstants
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean
import com.ericsson.oss.services.shm.notifications.api.*

/**
 * Test cases for customized cv name provided from operator in create backup rest call
 *
 * @author xkalkil
 *
 */

public class CVNameProviderGroovyTest extends CdiSpecification {

    protected RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)

    @ObjectUnderTest
    private CvNameProvider objectUderTest;

    @ImplementationInstance
    private FdnServiceBeanRetryHelper networkElementsProvider = Mock(FdnServiceBeanRetryHelper)

    @MockedImplementation
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @MockedImplementation
    private NetworkElementData networkElementData;

    @MockedImplementation
    private JobConfigurationServiceRetryProxy configServiceRetryProxy;

    @MockedImplementation
    private JobPropertyUtils jobPropertyUtils;

    @MockedImplementation
    private JobStaticDataProvider jobStaticDataProvider;

    @MockedImplementation
    private  JobStaticData jobStaticData ;

    @MockedImplementation
    private EAccessControl accessControl;

    @MockedImplementation
    private ActiveSoftwareProvider activeSoftwareProvider;

    private static final String PRODUCT_REVISION = "R40A109"
    private static final String PRODUCT_NUMBER = "CXP9024418_6"
    private static final String NODE_NAME = "LTE01dg2ERBS00001"

    def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 2L, NODE_NAME, "1234", "CPP", 5L, "");

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
    }

    def Map<String, Object>  prepareInputData(String backupName, boolean isPeriodic) {
        networkElementRetrivalBean.getNetworkElementData(NODE_NAME) >> networkElementData;
        networkElementData.getNeType() >> "ERBS"

        final Map<String, Object> jobAttributes =  new HashMap<>()
        Map<String, Object> jobProperties = new HashMap<>()
        Map<String, Object> mainSchedule = new HashMap<>()

        final List<Map<String, Object>> scheduleAttributes = new ArrayList<>();
        if(isPeriodic) {
            Map<String, Object> scheduled = new HashMap<>()
            scheduled.put(ShmConstants.NAME, JobPropertyConstants.REPEAT_TYPE)
            scheduleAttributes.add(scheduled)
        }

        mainSchedule.put(ShmConstants.SCHEDULINGPROPERTIES, scheduleAttributes)

        jobProperties.put(ShmConstants.KEY, JobPropertyConstants.AUTO_GENERATE_BACKUP)
        jobProperties.put(ShmConstants.VALUE,"true")
        jobProperties.put(ShmConstants.KEY, "CV_NAME")
        jobProperties.put(ShmConstants.VALUE, backupName)
        jobProperties.put(ShmConstants.MAIN_SCHEDULE, mainSchedule)

        jobAttributes.put(ShmConstants.JOBCONFIGURATIONDETAILS, jobProperties)
        return jobAttributes;
    }

    def prepareProductData() {
        def List<NetworkElement> networkElements = new ArrayList<NetworkElement>();
        NetworkElement networkElement = new NetworkElement();
        List<ProductData> neProductVersion =new ArrayList<>()
        ProductData pd = new ProductData(PRODUCT_NUMBER, PRODUCT_REVISION);
        neProductVersion.add(pd);

        networkElement.setPlatformType(PlatformTypeEnum.ECIM);
        networkElement.setNeProductVersion(neProductVersion);
        networkElements.add(networkElement);
        networkElementsProvider.getNetworkElements(_) >> networkElements
    }

    def Map<String, String> getActiveSoftwareDet() {
        final Map<String, String> activeSoftwareDetails =new HashMap<>()
        activeSoftwareDetails.put(NODE_NAME, PRODUCT_NUMBER+"||"+PRODUCT_REVISION)
        return activeSoftwareDetails
    }

    private String getCurrentTimeStamp() {
        final Date dateTime = new Date();
        final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
        return formatter.format(dateTime);
    }

    def prepareMockData(String inputBackupName, boolean isPeriodic) {
        prepareProductData()
        Map map =prepareInputData(inputBackupName, isPeriodic)
        Map<String, String> jobProperties = new HashMap<>()
        jobProperties.put("CV_NAME", inputBackupName)

        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData
        jobStaticData.getOwner() >> "user"
        configServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId()) >> new HashMap()
        configServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId()) >> map
        jobPropertyUtils.getPropertyValue(_, _, _,_,_) >> jobProperties
    }


    def 'Test get customized and auto generated backup name for given different product data inputs'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputCVName, false)
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(NODE_NAME)) >> getActiveSoftwareDet()

        when:"Perform  get cv name method - for autogenerated and customized inputs"
        String cvName = objectUderTest.getConfigurationVersionName(neJobStaticData, "CV_NAME")

        then:"Verify preapared backup name for different inputs"
        assert(cvName == returnedCVName)

        where: "different input CV names with place holders vs returned prepared cv names"
        inputCVName                                                        |  returnedCVName
        "SampleBackupName"                                                 |  "SampleBackupName"
        '$productnumber_$productrevision'                                  |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        '$productnumber_$productrevision_$nodename'                        |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION + "_" + NODE_NAME
        '$productnumber_$productrevision'                                  |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        '$productnumber_$nodename'                                         |  PRODUCT_NUMBER + "_" +  NODE_NAME
        '$productrevision_$nodename'                                       |  PRODUCT_REVISION + "_" + NODE_NAME
        '$productnumber'                                                   |  PRODUCT_NUMBER
        '$productrevision'                                                 |  PRODUCT_REVISION
        '$nodename'                                                        |  NODE_NAME
        'XYZ_$productnumber'                                               |  "XYZ_"+ PRODUCT_NUMBER
        '$productnumber$productrevision$nodename'                          |  PRODUCT_NUMBER + PRODUCT_REVISION  + NODE_NAME
        'Sample_$xYZ'                                                      |  'Sample_$xYZ'
        'Sample_&xYZ'                                                      |  'Sample__xYZ'
        '$productnumberSample'                                             |  PRODUCT_NUMBER +"Sample"
        '$productnumber$productrevision$nodenameqwertyuiopasdfghjklzxcvb'  |  PRODUCT_NUMBER + PRODUCT_REVISION  + NODE_NAME +"qwer"
        "SampleBackupName"                                                 |  "SampleBackupName"
        '$productnumber_$productrevision'                                  |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        '$productnumber_$productrevision_$nodename'                        |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION + "_" + NODE_NAME
        '$productnumber_$productrevision'                                  |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        '$productnumber_$nodename'                                         |  PRODUCT_NUMBER + "_" +  NODE_NAME
        '$productrevision_$nodename'                                       |  PRODUCT_REVISION + "_" + NODE_NAME
        '$productnumber'                                                   |  PRODUCT_NUMBER
        '$productrevision'                                                 |  PRODUCT_REVISION
        '$nodename'                                                        |  NODE_NAME
        'XYZ_$productnumber'                                               |  "XYZ_"+ PRODUCT_NUMBER
        '$productnumber$productrevision$nodename'                          |  PRODUCT_NUMBER + PRODUCT_REVISION  + NODE_NAME
        'Sample_$xYZ'                                                      |  'Sample_$xYZ'
        'Sample_&xYZ'                                                      |  'Sample__xYZ'
    }

    def 'Test get customized and auto generated backup name for given periodic jobs and when time stamp is at the end'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputCVName,  isPeriodic)
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(NODE_NAME)) >> getActiveSoftwareDet()

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        String cvName = objectUderTest.getConfigurationVersionName(neJobStaticData,"CV_NAME")
        def timestamp = cvName.substring(cvName.length() - 14);
        cvName = cvName.replace(timestamp, "");

        then:"Verify preapared backup name for different inputs"
        assert(cvName == returnedCVName)

        where: "different input CV names with place holders vs returned prepared CV name"
        isPeriodic  |  inputCVName                                                                 |  returnedCVName
        true        | '$productnumber$productrevision$nodenameqwertyuiopasdfghjklzxcvb$timestamp'  |  "CXP9024418_6R40A109LTE01dg"
        false       | '$productnumber$productrevision$nodenameqwertyuiopasdfghjklzxcvb$timestamp'  |  "CXP9024418_6R40A109LTE01dg"
        true        | '$productnumber$productrevision$nodenameqwertyuiopasdfghjklzxcvb'            |  "CXP9024418_6R40A109LTE01dg"
    }

    def 'Test get customized and auto generated backup name for given periodic jobs and with no time stamp'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputCVName,  isPeriodic)
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(NODE_NAME)) >> getActiveSoftwareDet()

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        String cvName = objectUderTest.getConfigurationVersionName(neJobStaticData,"CV_NAME")

        then:"Verify preapared backup name for different inputs"
        assert(cvName == returnedCVName)

        where: "different input CV names with place holders vs returned prepared backup name"
        isPeriodic   | inputCVName                                                              |  returnedCVName
        false        | '$productnumber$productrevision$nodenameqwertyuiopasdfghjklzxcvb'        |  "CXP9024418_6R40A109LTE01dg2ERBS00001qwer"
    }

    def 'Test get customizand CV name empty or null then it throws exception'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputCVName,  isPeriodic)
        String expMsg = ""

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        try {
            String cvName = objectUderTest.getConfigurationVersionName(neJobStaticData,"CV_NAME")
        }catch(Exception e) {
            expMsg = e.getMessage()
        }
        then:"Verify get exception when different inputs"
        assert(expMsg == exception)

        where: "different input backup names vs exception received"
        isPeriodic    | inputCVName  |  exception
        true          | ""           |  JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
        true          | null         |  JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
    }

    def 'Test get customized and auto generated backup name for given product data inputs when activesoftware details empty then thrws exception'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputCVName, false)
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(NODE_NAME)) >> new HashMap<>()
        String expMsg = ""

        when:"Perform  get cv name method - for autogenerated and customized inputs"
        try {
            String cvName = objectUderTest.getConfigurationVersionName(neJobStaticData,"CV_NAME")
        }catch(Exception e) {
            expMsg = e.getMessage()
        }

        then:"Verify get exception when different inputs"
        assert(expMsg == exception)

        where: "different input backup names vs exception received"
        inputCVName        |  exception
        '$productnumber'   |  JobLogConstants.ACTIVE_SOFTWARE_DETAILS_NOT_FOUND
    }
}
