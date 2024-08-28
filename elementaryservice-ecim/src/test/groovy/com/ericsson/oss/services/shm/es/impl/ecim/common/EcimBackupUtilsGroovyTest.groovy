/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.common

import java.text.SimpleDateFormat

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.ejb.EjbProxyController
import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement
import com.ericsson.oss.services.shm.common.modelservice.api.ProductData
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupConstants
import com.ericsson.oss.services.shm.es.ecim.backup.common.EcimBackupUtils
import com.ericsson.oss.services.shm.es.ecim.upgrade.common.UpMoServiceRetryProxy
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider
import com.ericsson.oss.services.shm.es.impl.JobLogConstants
import com.ericsson.oss.services.shm.inventory.backup.ecim.common.EcimBackupInfo
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy
import com.ericsson.oss.services.shm.job.cache.JobStaticData
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants
import com.ericsson.oss.services.shm.model.NetworkElementData
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean

/**
 * Test cases for customized backup name provided from operator in create backup rest call
 * 
 * @author xkalkil
 *
 */
public class EcimBackupUtilsGroovyTest extends CdiSpecification {

    @ObjectUnderTest
    private EcimBackupUtils objectUderTest;

    @ImplementationInstance
    private FdnServiceBeanRetryHelper networkElementsProvider = Mock(FdnServiceBeanRetryHelper)

    @MockedImplementation
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @MockedImplementation
    private JobConfigurationServiceRetryProxy configServiceRetryProxy;

    @MockedImplementation
    private NetworkElementData networkElementData;

    @MockedImplementation
    private UpMoServiceRetryProxy upMoServiceRetryProxy;

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

    def NEJobStaticData neJobStaticData = new NEJobStaticData(1L, 2L, NODE_NAME, "1234", "ECIM", 5L, "");

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom("com.ericsson.oss.services.shm")
        injectionProperties.addProxyController(new EjbProxyController(true))
    }

    def Map<String, Object>  prepareInputData(String backupName) {
        networkElementRetrivalBean.getNetworkElementData(NODE_NAME) >> networkElementData;
        networkElementData.getNeType() >> "Radionode"

        final Map<String, Object> neJobAttributes =  new HashMap<>()
        List<Map<String, Object>> jobPropertiesList = new ArrayList<>();
        Map<String, Object> jobProperties1 = new HashMap<>()
        jobProperties1.put(ShmConstants.KEY, JobPropertyConstants.AUTO_GENERATE_BACKUP)
        jobProperties1.put(ShmConstants.VALUE,"true")
        Map<String, Object> jobProperties2 = new HashMap<>()
        jobProperties2.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_NAME)
        jobProperties2.put(ShmConstants.VALUE, backupName)
        Map<String, Object> jobProperties3 = new HashMap<>()
        jobProperties3.put(ShmConstants.KEY, EcimBackupConstants.BRM_BACKUP_MANAGER_ID)
        jobProperties3.put(ShmConstants.VALUE,"System/Systemdata")
        jobPropertiesList.add(jobProperties1)
        jobPropertiesList.add(jobProperties2)
        jobPropertiesList.add(jobProperties3)
        neJobAttributes.put(ShmConstants.JOBPROPERTIES, jobPropertiesList)
        return neJobAttributes;
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

    def prepareMockData(String inputBackupName, String autogeneratedBackupFlag) {
        prepareProductData()
        Map map =prepareInputData(inputBackupName)
        Map<String, String> jobProperties = new HashMap<>()
        jobProperties.put(JobPropertyConstants.AUTO_GENERATE_BACKUP, autogeneratedBackupFlag)
        jobProperties.put( EcimBackupConstants.BRM_BACKUP_NAME, inputBackupName)
        jobProperties.put(EcimBackupConstants.BRM_BACKUP_MANAGER_ID, "System/Systemdata")

        jobStaticDataProvider.getJobStaticData(neJobStaticData.getMainJobId()) >> jobStaticData
        jobStaticData.getOwner() >> "user"
        configServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId()) >> new HashMap()
        configServiceRetryProxy.getMainJobAttributes(neJobStaticData.getMainJobId()) >> map
        jobPropertyUtils.getPropertyValue(_, _, _,_,_) >> jobProperties

        upMoServiceRetryProxy.getActiveSoftwareDetailsFromNode(NODE_NAME) >> new HashMap<>()
    }


    def 'Test get customizand backup name when autogeneratedbackupnflag is true and activesoftware details empty'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputBackupName, autogeneratedBackupFlag)

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        EcimBackupInfo ecimBackupInfo = objectUderTest.getBackupWithAutoGeneratedName(neJobStaticData, new ArrayList())

        then:"Verify preapared backup name for different inputs"
        assert(ecimBackupInfo.getBackupName().contains( returnedBackupName))

        where: "different input backup names vs returned preapared backup name values "
        autogeneratedBackupFlag | inputBackupName                                          |  returnedBackupName
        "true"                  |  "SampleBackupName"                                      |  "SampleBackupName"
        "true"                  | '$productnumber_$productrevision'                        |  "Backup"
        "true"                  | '$productnumber_$productrevision_$nodename'              |  "_" + NODE_NAME
        "true"                  | '$productnumber_$productrevision_$nodename_$timestamp'   |  NODE_NAME + "_"
        "true"                  | '$productnumber_$nodename'                               |   "_" +  NODE_NAME
        "true"                  | '$productrevision_$nodename'                             |   "_" + NODE_NAME
        "true"                  | '$productnumber'                                         |  "Backup"
        "true"                  | '$productrevision'                                       |  "Backup"
        "true"                  | '$nodename'                                              |  NODE_NAME
        "true"                  | 'XYZ_$productnumber'                                     |  "XYZ"
        "true"                  | '$productnumber$productrevision$nodename'                |  NODE_NAME
        "true"                  | 'Sample_$xYZ'                                            |  'Sample_$xYZ'
        "true"                  | 'Sample_&xYZ'                                            |  'Sample__xYZ'
    }


    def 'Test get customizand backup name when autogeneratedbackupnflag is false and activesoftware details empty'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputBackupName, autogeneratedBackupFlag)

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        EcimBackupInfo ecimBackupInfo = objectUderTest.getBackupWithAutoGeneratedName(neJobStaticData, new ArrayList())

        then:"Verify preapared backup name for different inputs"
        assert(ecimBackupInfo.getBackupName().contains( returnedBackupName))

        where: "different input backup names vs returned preapared backup name values "
        autogeneratedBackupFlag | inputBackupName                                          |  returnedBackupName
        "false"                 | "SampleBackupName"                                       |  "SampleBackupName"
        "false"                 | '$productnumber_$productrevision_$nodename'              |  "_" + NODE_NAME
        "false"                 | '$productnumber_$productrevision_$nodename_$timestamp'   |  NODE_NAME + "_"
        "false"                 | '$productnumber_$nodename'                               |   "_" +  NODE_NAME
        "false"                 | '$productrevision_$nodename'                             |   "_" + NODE_NAME
        "false"                 | '$nodename'                                              |  NODE_NAME
        "false"                 | 'XYZ_$productnumber'                                     |  "XYZ"
        "false"                 | '$productnumber$productrevision$nodename'                |  NODE_NAME
        "false"                 | 'Sample_$xYZ'                                            |  'Sample_$xYZ'
        "false"                 | 'Sample_&xYZ'                                            |  'Sample__xYZ'
    }


    def 'Test get customizand backup name contains only product number, revision when autogeneratedbackupnflag is false and activesoftware details empty'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputBackupName, autogeneratedBackupFlag)

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        try {
            EcimBackupInfo ecimBackupInfo = objectUderTest.getBackupWithAutoGeneratedName(neJobStaticData, new ArrayList())
        }catch(Exception e) {
            assert(e.getMessage() == exception)
        }

        then:"Verify get exception when different inputs like product number and revision provided"

        where: "different input backup names vs exception received"
        autogeneratedBackupFlag | inputBackupName                    |  exception
        "false"                 | '$productnumber_$productrevision'  | JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
        "false"                 | '$productnumber'                   | JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
        "false"                 | '$productrevision'                 | JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
        "false"                 |  ""                                | JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST
    }

    def 'Test get customized and auto generated backup name 2'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputBackupName, autogeneratedBackupFlag)
        activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(NODE_NAME)) >> getActiveSoftwareDet()

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        EcimBackupInfo ecimBackupInfo = objectUderTest.getBackupWithAutoGeneratedName(neJobStaticData, new ArrayList())

        then:"Verify preapared backup name for different inputs"
        assert(returnedBackupName == ecimBackupInfo.getBackupName())

        where: "different input backup names vs returned preapared backup name values "
        autogeneratedBackupFlag | inputBackupName                                          |  returnedBackupName
        "true"                  | "SampleBackupName"                                       |  "SampleBackupName"
        "true"                  |'$productnumber_$productrevision'                         |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        "true"                  | '$productnumber_$productrevision_$nodename'              |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION + "_" + NODE_NAME
        "true"                  | '$productnumber_$productrevision'                        |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        "true"                  | '$productnumber_$nodename'                               |  PRODUCT_NUMBER + "_" +  NODE_NAME
        "true"                  | '$productrevision_$nodename'                             |  PRODUCT_REVISION + "_" + NODE_NAME
        "true"                  | '$productnumber'                                         |  PRODUCT_NUMBER
        "true"                  | '$productrevision'                                       |  PRODUCT_REVISION
        "true"                  | '$nodename'                                              |  NODE_NAME
        "true"                  | 'XYZ_$productnumber'                                     |  "XYZ_"+ PRODUCT_NUMBER
        "true"                  | '$productnumber$productrevision$nodename'                |  PRODUCT_NUMBER + PRODUCT_REVISION  + NODE_NAME
        "true"                  | 'Sample_$xYZ'                                            |  'Sample_$xYZ'
        "true"                  | 'Sample_&xYZ'                                            |  'Sample__xYZ'
        "true"                  | '$productnumberSample'                                  |  PRODUCT_NUMBER +"Sample"
        "false"                 | "SampleBackupName"                                       |  "SampleBackupName"
        "false"                 |'$productnumber_$productrevision'                         |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        "false"                 | '$productnumber_$productrevision_$nodename'              |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION + "_" + NODE_NAME
        "false"                 | '$productnumber_$productrevision'                        |  PRODUCT_NUMBER + "_" + PRODUCT_REVISION
        "false"                 | '$productnumber_$nodename'                               |  PRODUCT_NUMBER + "_" +  NODE_NAME
        "false"                 | '$productrevision_$nodename'                             |  PRODUCT_REVISION + "_" + NODE_NAME
        "false"                 | '$productnumber'                                         |  PRODUCT_NUMBER
        "false"                 | '$productrevision'                                       |  PRODUCT_REVISION
        "false"                 | '$nodename'                                              |  NODE_NAME
        "false"                 | 'XYZ_$productnumber'                                     |  "XYZ_"+ PRODUCT_NUMBER
        "false"                 | '$productnumber$productrevision$nodename'                |  PRODUCT_NUMBER + PRODUCT_REVISION  + NODE_NAME
        "false"                 | 'Sample_$xYZ'                                            |  'Sample_$xYZ'
        "false"                 | 'Sample_&xYZ'                                            |  'Sample__xYZ'
    }

    def 'Test get backup name when autogeneratedbackupnflag is false and activesoftware details empty'() {
        given:"prepare job data with given inputs"
        prepareMockData(inputBackupName, autogeneratedBackupFlag)

        when:"Perform  get backup name method - for autogenerated and customized inputs"
        EcimBackupInfo ecimBackupInfo = objectUderTest.getBackupWithAutoGeneratedName(neJobStaticData, new ArrayList())

        then:"Verify preapared backup name for different inputs"
        assert(ecimBackupInfo.getBackupName().contains( returnedBackupName))

        where: "different input backup names vs returned preapared backup name values "
        autogeneratedBackupFlag | inputBackupName  | returnedBackupName
        "true"                 |  ""              | EcimBackupConstants.DEFAULT_BACKUP_NAME +"_" + getCurrentTimeStamp()
    }
}
