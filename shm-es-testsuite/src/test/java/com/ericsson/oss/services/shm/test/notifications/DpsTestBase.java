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
package com.ericsson.oss.services.shm.test.notifications;

import java.util.*;
import java.util.Map.Entry;

import javax.ejb.*;

import com.ericsson.oss.itpf.datalayer.dps.*;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.impl.cpp.restore.RestoreServiceConstants;
import com.ericsson.oss.services.shm.test.common.TestConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DpsTestBase implements IDpsTestBase {

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    private static final String LIVE_BUCKET = "Live";

    //osstop
    private static final String TOP_NAMESPACE = "OSS_TOP";
    private static final String TOP_NAMESPACE_VERSION = "3.0.0";
    private static final String ME_CONTEXT_MO = "MeContext";
    public static final String MECONTEXT_NAME = "ERBS101";
    private static final String MANDATORY_ATTRIBUTE_MECONTEXTID = "MeContextId";
    private static final String MANDATORY_ATTRIBUTE_NETYPE = "neType";
    private static final String MANDATORY_ATTRIBUTE_VALUE_NETYPE = "ERBS";
    private static final String PALTFORM_TYPE = "platformType";

    //cpp_node_model
    private static final String ERBS_NAMESPACE = "ERBS_NODE_MODEL";

    //me
    private static final String MANAGED_ELEMENT_MO = "ManagedElement";
    private static final String MANDATORY_ATTRIBUTE_MANAGEDELEMENTID = "ManagedElementId";
    private static final String MANDATORY_ATTRIBUTE_MANAGEDELEMENTID_VALUE = "1";
    public static final String NETYPE = "neType";

    //swm
    private static final String SWMANAGEMENT_MO = "SwManagement";
    private static final String MANDATORY_ATTRIBUTE_SWMANAGEMENTID = "SwManagementId";
    private static final String MANDATORY_ATTRIBUTE_SWMANAGEMENTID_VALUE = "1";

    //up
    private static final String UPGRADEPACKAGE_MO = "UpgradePackage";
    private static final String MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID = "UpgradePackageId";
    private static final String MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID_VALUE = "1";
    private static final String MANDATORY_ATTRIBUTE_FTPSERVERIP = "ftpServerIpAddress";
    private static final String MANDATORY_ATTRIBUTE_FTPSERVERIP_VALUE = "10.1.1.1";
    private static final String MANDATORY_ATTRIBUTE_FTPDIRPATH = "upFilePathOnFtpServer";
    private static final String MANDATORY_ATTRIBUTE_FTPDIRPATH_VALUE = "/ericsson/tor/smrs/SOFTWARE/cxp_121/cxp_3333.xml";
    private static final String OPTIONAL_ATTRIBUTE_USERLABEL = "userLabel";
    private static final String OPTIONAL_ATTRIBUTE_USERLABEL_VALUE = "shm text";
    private static final String MANDATORY_ATTRIBUTE_USER = "user";
    private static final String MANDATORY_ATTRIBUTE_USER_VALUE = "mm-lran-user";
    private static final String MANDATORY_ATTRIBUTE_PASSWORD = "password";
    private static final String MANDATORY_ATTRIBUTE_PASSWORD_VALUE = "P455W0RD";

    //cv
    private static final String CONFIGURATIONVERSION_MO = "ConfigurationVersion";
    private static final String MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID = "ConfigurationVersionId";
    private static final String MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID_VALUE = "1";

    public static String UPMO_FDN;
    public static String CVMO_FDN;
    public static String SWMMO_FDN;

    //up attrs
    public static final String UP_STATE = "state";
    public static final String UP_PROGRESS_COUNT = "progressCount";
    public static final String UP_PROGRESS_TOTAL = "progressTotal";
    public static final String UP_PROGRESS_HEADER = "progressHeader";

    //cv attrs
    public static final String CURRENT_DETAILED_ACTIVITY = "currentDetailedActivity";
    public static final String CURRENT_MAIN_ACTIVITY = "currentMainActivity";
    public static final String STORED_CONFIGURATION_VERSION = "storedConfigurationVersions";
    public static final String STORED_CONFIGRUATION_VERSION_NAME = "name";
    public static final String STORED_CONFIGRUATION_VERSION_STATUS = "status";
    public static final String STORED_CONFIGRUATION_VERSION_DATE = "date";
    public static final String STORED_CONFIGRUATION_VERSION_IDENTITY = "identity";
    public static final String STORED_CONFIGRUATION_VERSION_OPERATOR_COMMENT = "operatorComment";
    public static final String STORED_CONFIGRUATION_VERSION_OPERATOR_NAME = "operatorName";
    public static final String STORED_CONFIGRUATION_VERSION_TYPE = "type";
    public static final String STORED_CONFIGRUATION_VERSION_UPGRADE_PACKAGE_ID = "upgradePackageId";

    public static ManagedObject cvMo;
    public static ManagedObject neMO;
    public static String NEMO_FDN;

    private static final String MANDATORY_ATTRIBUTE_NEID = "networkElementId";
    private static final String NETWORKELEMENT_MO = "NetworkElement";
    private static final String SW_PROD_NUMBER = "CXP1020511_R4D26";

    public static String UPMO_FDN_2 = ME_CONTEXT_MO + "=" + MECONTEXT_NAME + "," + MANAGED_ELEMENT_MO + "=" + MANDATORY_ATTRIBUTE_MANAGEDELEMENTID_VALUE + "," + SWMANAGEMENT_MO + "="
            + MANDATORY_ATTRIBUTE_SWMANAGEMENTID_VALUE + "," + UPGRADEPACKAGE_MO + "=" + SW_PROD_NUMBER + "_";

    @Override
    public void initMOdata() {
        final DataBucket bucket = dataPersistenceService.getDataBucket("Live", BucketProperties.SUPPRESS_CONSTRAINTS);
        final Map<String, Object> mecAttrs = new HashMap<String, Object>();
        mecAttrs.put(MANDATORY_ATTRIBUTE_MECONTEXTID, MECONTEXT_NAME);
        mecAttrs.put(MANDATORY_ATTRIBUTE_NETYPE, MANDATORY_ATTRIBUTE_VALUE_NETYPE);
        mecAttrs.put(PALTFORM_TYPE, "CPP");
        final ManagedObject meContext = bucket.getMibRootBuilder().namespace(TOP_NAMESPACE).type(ME_CONTEXT_MO).name(MECONTEXT_NAME).version(TOP_NAMESPACE_VERSION).addAttributes(mecAttrs).create();

        final Map<String, Object> meAttrs = new HashMap<String, Object>();
        meAttrs.put(MANDATORY_ATTRIBUTE_MANAGEDELEMENTID, MANDATORY_ATTRIBUTE_MANAGEDELEMENTID_VALUE);
        meAttrs.put("userLabel", "Netsim ERBS5");
        meAttrs.put(MANDATORY_ATTRIBUTE_NETYPE, MANDATORY_ATTRIBUTE_VALUE_NETYPE);
        meAttrs.put(PALTFORM_TYPE, "CPP");
        final ManagedObject managedElement = bucket.getMibRootBuilder().namespace(ERBS_NAMESPACE).type(MANAGED_ELEMENT_MO).name(MANDATORY_ATTRIBUTE_MANAGEDELEMENTID_VALUE)
                .version(TestConstants.C14B_SUPPORTED_MIM_VERSION).addAttributes(meAttrs).parent(meContext).create();

        final Map<String, Object> swmattrs = new HashMap<String, Object>();
        swmattrs.put(MANDATORY_ATTRIBUTE_SWMANAGEMENTID, MANDATORY_ATTRIBUTE_SWMANAGEMENTID_VALUE);
        final ManagedObject swm = bucket.getManagedObjectBuilder().type(SWMANAGEMENT_MO).name(MANDATORY_ATTRIBUTE_SWMANAGEMENTID_VALUE).addAttributes(swmattrs).parent(managedElement).create();
        SWMMO_FDN = swm.getFdn();

        final Map<String, Object> upgradePkgMOAttributes = new HashMap<String, Object>();
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID, MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_FTPSERVERIP, MANDATORY_ATTRIBUTE_FTPSERVERIP_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_FTPDIRPATH, MANDATORY_ATTRIBUTE_FTPDIRPATH_VALUE);
        upgradePkgMOAttributes.put(OPTIONAL_ATTRIBUTE_USERLABEL, OPTIONAL_ATTRIBUTE_USERLABEL_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_USER, MANDATORY_ATTRIBUTE_USER_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_PASSWORD, MANDATORY_ATTRIBUTE_PASSWORD_VALUE);
        final ManagedObject up = bucket.getManagedObjectBuilder().name(MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID_VALUE).type(UPGRADEPACKAGE_MO).parent(swm).addAttributes(upgradePkgMOAttributes).create();
        System.out.println("upfdn=" + up.getFdn());
        UPMO_FDN = up.getFdn();

        final Map<String, Object> cvMOAttributes = new HashMap<String, Object>();
        cvMOAttributes.put(MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID, MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID_VALUE);
        cvMOAttributes.put(MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID, MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID_VALUE);
        cvMOAttributes.put(RestoreServiceConstants.CVMO_MISSING_UPGRADE_PACKAGES, getMissingAdminData());
        cvMOAttributes.put(RestoreServiceConstants.CVMO_CORRUPTED_UPGRADE_PACKAGES, getCorruptedAdminData());
        cvMo = bucket.getManagedObjectBuilder().name(MANDATORY_ATTRIBUTE_CONFIGURATIONVERSIONID_VALUE).type(CONFIGURATIONVERSION_MO).parent(swm).addAttributes(cvMOAttributes).create();

        CVMO_FDN = cvMo.getFdn();

        final Map<String, Object> neMOAttributes = new HashMap<String, Object>();
        neMOAttributes.put(MANDATORY_ATTRIBUTE_NEID, MANDATORY_ATTRIBUTE_MANAGEDELEMENTID_VALUE);
        neMOAttributes.put(MANDATORY_ATTRIBUTE_NETYPE, MANDATORY_ATTRIBUTE_VALUE_NETYPE);
        neMOAttributes.put(PALTFORM_TYPE, "CPP");
        //cvMo = bucket.getManagedObjectBuilder().name(MECONTEXT_NAME).type(NETWORKELEMENT_MO).parent(swm).addAttributes(cvMOAttributes).create();
        neMO = bucket.getMibRootBuilder().namespace("OSS_NE_DEF").type(NETWORKELEMENT_MO).name(MECONTEXT_NAME).version("1.0.0").addAttributes(neMOAttributes).create();
        NEMO_FDN = cvMo.getFdn();              
        
    }

    /**
     * @return
     */
    private List<Map<String, Object>> getMissingAdminData() {
        final Map<String, Object> cvMOAttributes = new HashMap<String, Object>();
        cvMOAttributes.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_NUMBER, SW_PROD_NUMBER);
        cvMOAttributes.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_REVISION, "1");
        final Map<String, Object> cvMOAttributes2 = new HashMap<String, Object>();
        cvMOAttributes2.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_NUMBER, SW_PROD_NUMBER);
        cvMOAttributes2.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_REVISION, "3");
        return Arrays.asList(cvMOAttributes, cvMOAttributes2);
    }

    private List<Map<String, Object>> getCorruptedAdminData() {
        final Map<String, Object> cvMOAttributes = new HashMap<String, Object>();
        cvMOAttributes.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_NUMBER, SW_PROD_NUMBER);
        cvMOAttributes.put(RestoreServiceConstants.CVMO_ADMINDATA_PRODUCT_REVISION, "2");
        return Arrays.asList(cvMOAttributes);
    }

    @Override
    public void delete() {
        final DataBucket bucket = dataPersistenceService.getLiveBucket();
        final QueryExecutor qx = bucket.getQueryExecutor();
        final QueryBuilder qb = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> q = qb.createTypeQuery(TOP_NAMESPACE, ME_CONTEXT_MO);
        final Iterator<ManagedObject> it = qx.execute(q);
        while (it.hasNext()) {
            bucket.deletePo(it.next());
        }
        final Query<TypeRestrictionBuilder> query = qb.createTypeQuery("OSS_NE_DEF", NETWORKELEMENT_MO);
        final Iterator<ManagedObject> queryIt = qx.execute(query);
        while (queryIt.hasNext()) {
            bucket.deletePo(queryIt.next());
        }
    }

    @Override
    public void moUpdateROAttrs(final String fdn, final Map<String, Object> attributes) {
        final DataBucket bucket = dataPersistenceService.getDataBucket(LIVE_BUCKET, BucketProperties.SUPPRESS_CONSTRAINTS);
        final ManagedObject mo = bucket.findMoByFdn(fdn);
        System.out.println("upfdn=" + mo.getFdn());
        mo.setAttributes(attributes);
    }

    @Override
    public int performAction(final String moFdn, final String actionName, final Map<String, Object> actionArguments) {
        int actionId = -1;
        try {
            final DataBucket bucket = dataPersistenceService.getDataBucket("Live");
            final ManagedObject managedObject = bucket.findMoByFdn(moFdn);
            final Object object = managedObject.performAction(actionName, actionArguments);
            if (object != null) {

                final String actionIdString = object.toString();
                actionId = Integer.parseInt(actionIdString);
            }
            return actionId;
        } catch (final Exception e) {
            return -1;
        }

    }

    @Override
    public List<ManagedObject> getManagedObjects(final String namespace, final String type, final Map<String, Object> restrictions, final String nodeName) {

        final String nodeFdn = "MeContext=" + nodeName;
        final Query<TypeContainmentRestrictionBuilder> query = constructQuery(namespace, type, restrictions, nodeFdn);
        final Iterator<ManagedObject> iterator = dataPersistenceService.getLiveBucket().getQueryExecutor().execute(query);
        ManagedObject managedObject;
        //        PersistenceObjectWrapper persistenceObjectWrapper;
        final List<ManagedObject> managedObjectDataList = new ArrayList<ManagedObject>();

        while (iterator.hasNext()) {
            managedObject = iterator.next();
            //            persistenceObjectWrapper.setFdn(managedObject.getFdn());
            //            persistenceObjectWrapper.setName(managedObject.getName());
            //            persistenceObjectWrapper.setNamespace(managedObject.getNamespace());
            //            persistenceObjectWrapper.setType(managedObject.getType());
            //            persistenceObjectWrapper.setVersion(managedObject.getVersion());
            //            persistenceObjectWrapper.setAttributesMap(managedObject.getAllAttributes());
            managedObjectDataList.add(managedObject);
        }

        return managedObjectDataList;

    }

    private Query<TypeContainmentRestrictionBuilder> constructQuery(final String namespace, final String type, final Map<String, Object> restrictions, final String nodeFdn) {

        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeContainmentRestrictionBuilder> query = queryBuilder.createTypeQuery(namespace, type, nodeFdn);
        if (restrictions != null) {
            Restriction queryRestriction;
            final List<Restriction> restrictionList = new ArrayList<Restriction>();
            for (final Entry<String, Object> entry : restrictions.entrySet()) {
                queryRestriction = query.getRestrictionBuilder().equalTo(entry.getKey(), entry.getValue());
                restrictionList.add(queryRestriction);
            }

            if (!restrictionList.isEmpty()) {
                Restriction finalRestriction = null;
                int index = 0;
                for (final Restriction restriction : restrictionList) {
                    if (index == 0) {
                        finalRestriction = query.getRestrictionBuilder().allOf(restriction);
                    } else {
                        finalRestriction = query.getRestrictionBuilder().allOf(finalRestriction, restriction);
                    }
                    index++;
                }
                query.setRestriction(finalRestriction);
            }
        }
        return query;
    }

    @Override
    public ManagedObject findMoByFdn(final String moFdn) {
        try {
            final DataBucket bucket = dataPersistenceService.getDataBucket("Live");
            final ManagedObject managedObject = bucket.findMoByFdn(moFdn);
            return managedObject;
        } catch (final Exception e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.notifications.IDpsTestBase#checkUPMoState()
     */
    @Override
    public String checkUPMoState(final int moID, final String attributeName) {
        final ManagedObject upMO = findMoByFdn(UPMO_FDN_2 + moID);
        final Map<String, Object> attributes = upMO.getAllAttributes();
        System.out.println(attributes);
        final String stateAttribute = (String) attributes.get(attributeName);
        System.out.println("++++++++++++++++++++++++ State of the UP MO is " + stateAttribute + ",  on FDN " + UPMO_FDN_2);
        return stateAttribute;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.test.notifications.IDpsTestBase#createUpgradePackageMO()
     */
    @Override
    public void createUpgradePackageMO(final String moId) {
        final DataBucket bucket = dataPersistenceService.getDataBucket("Live", BucketProperties.SUPPRESS_CONSTRAINTS);
        final Map<String, Object> upgradePkgMOAttributes = new HashMap<String, Object>();
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID, MANDATORY_ATTRIBUTE_UPGRADEPACKAGEID_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_FTPSERVERIP, MANDATORY_ATTRIBUTE_FTPSERVERIP_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_FTPDIRPATH, MANDATORY_ATTRIBUTE_FTPDIRPATH_VALUE);
        upgradePkgMOAttributes.put(OPTIONAL_ATTRIBUTE_USERLABEL, OPTIONAL_ATTRIBUTE_USERLABEL_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_USER, MANDATORY_ATTRIBUTE_USER_VALUE);
        upgradePkgMOAttributes.put(MANDATORY_ATTRIBUTE_PASSWORD, MANDATORY_ATTRIBUTE_PASSWORD_VALUE);
        final ManagedObject parentMO = bucket.findMoByFdn(SWMMO_FDN);
        final ManagedObject up = bucket.getManagedObjectBuilder().name(SW_PROD_NUMBER + "_" + moId).type(UPGRADEPACKAGE_MO).parent(parentMO).addAttributes(upgradePkgMOAttributes).create();
        System.out.println("upfdn=" + up.getFdn());
    }
}
