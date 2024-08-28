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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FileResource;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.license.InstallLicenseService;
import com.ericsson.oss.services.shm.ecim.common.ActionResultType;
import com.ericsson.oss.services.shm.ecim.common.ActionStateType;
import com.ericsson.oss.services.shm.ecim.common.AsyncActionProgress;
import com.ericsson.oss.services.shm.ecim.common.EcimCommonConstants;
import com.ericsson.oss.services.shm.es.api.CommonLicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.license.LicenseKeyFileDeleteService;
import com.ericsson.oss.services.shm.es.impl.license.LicensingActivityConstants;
import com.ericsson.oss.services.shm.es.impl.license.LicensingRetryService;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

@Profiled
@Traceable
@ApplicationScoped
public class EcimLmUtils {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private FileResource fileResource;

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private LicenseKeyFileDeleteService licenseKeyFileDeleteService;

    @Inject
    private LicensingRetryService licensingRetryService;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private InstallLicenseService installLicenseService;

    private static final String ACTION_STATUS = "actionStatus";
    public static final String ACTIVATE_TRIGGERED = "isActivateTriggered";
    private static final String INSTALL_STATUS = "installStatus";

    /**
     * This method will return the license job information.
     * 
     * @param activityJobId
     * @return
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EcimLmUtils.class);

    @SuppressWarnings("unchecked")
    public EcimLicensingInfo getLicensingInfo(final long activityJobId, final NEJobStaticData neJobStaticData, final NetworkElementData networkElement) {
        LOGGER.debug("getLicensingInfo for activityJobId {}", activityJobId);

        short actionId = 0;
        boolean isActivateTriggered = false;
        String installStatus = null;
        final long neJobId = neJobStaticData.getNeJobId();
        final long mainJobId = neJobStaticData.getMainJobId();
        final Map<String, Object> activityJobAttributes = jobConfigurationServiceRetryProxy.getActivityJobAttributes(activityJobId);
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId);
        final Map<String, Object> mainJobProperties = jobConfigurationServiceRetryProxy.getMainJobAttributes(mainJobId);
        LOGGER.debug("activityJobPropertyList out getLicensingInfo() {}", activityJobAttributes);

        final List<Map<String, Object>> activityJobPropertyList = (List<Map<String, Object>>) activityJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        if (activityJobPropertyList != null) {
            for (final Map<String, Object> jobProperty : activityJobPropertyList) {
                if (EcimCommonConstants.ReportProgress.REPORT_PROGRESS_ACTION_ID.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    actionId = Short.parseShort((String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE));
                }
                if (ACTIVATE_TRIGGERED.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    isActivateTriggered = Boolean.parseBoolean((String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE));
                    LOGGER.debug("EcimLmUtils-isActivateTriggered {} in getLicensingInfo() for activityJobId {}", isActivateTriggered, activityJobId);
                }
                if (INSTALL_STATUS.equals(jobProperty.get(ActivityConstants.JOB_PROP_KEY))) {
                    installStatus = (String) jobProperty.get(ActivityConstants.JOB_PROP_VALUE);
                    LOGGER.debug("EcimLmUtils-isInstallSucess {} in getLicensingInfo() for activityJobId {}", installStatus, activityJobId);
                }
            }
        }
        final String licenseKeyFilePath = getLicenseKeyFilePath(neJobStaticData, networkElement, mainJobProperties);

        final EcimLicensingInfo ecimLicensingInfo = new EcimLicensingInfo("", licenseKeyFilePath);
        final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);
        ecimLicensingInfo.setBusinessKey(businessKey);
        ecimLicensingInfo.setActionId(actionId);
        ecimLicensingInfo.setActivateTriggered(isActivateTriggered);
        ecimLicensingInfo.setInstallStatus(installStatus);
        LOGGER.debug("ecimLicensingInfo {}", ecimLicensingInfo);
        return ecimLicensingInfo;
    }

    /**
     * Returns LicenseKeyFilePath from DB.
     * 
     * @param neJobStaticData
     * @param networkElement
     * @param mainJobProperties
     * @return
     */
    public String getLicenseKeyFilePath(final NEJobStaticData neJobStaticData, final NetworkElementData networkElement, final Map<String, Object> mainJobProperties) {

        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobProperties.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        final Map<String, String> propertyValue = jobPropertyUtils.getPropertyValue(Arrays.asList(CommonLicensingActivityConstants.LICENSE_FILE_PATH), jobConfigurationDetails,
                neJobStaticData.getNodeName(), networkElement.getNeType(), neJobStaticData.getPlatformType());
        String licenseKeyFilePath = propertyValue.get(CommonLicensingActivityConstants.LICENSE_FILE_PATH);
        LOGGER.debug("LicenseKeyfilePath from MainJob {}", licenseKeyFilePath);
        if(licenseKeyFilePath == null) {
            licenseKeyFilePath = getLicenseKeyFilePathFromNeJob(neJobStaticData.getNeJobId());
        }
        return licenseKeyFilePath;
    }

    /**
     * @param neJobStaticData
     * @param licenseKeyFilePath
     * @return
     */
    private String getLicenseKeyFilePathFromNeJob(final long neJobId) {
        String licenseKeyFilePath = "";
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobId);
        final List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ShmJobConstants.JOBPROPERTIES);
        if (CollectionUtils.isNotEmpty(neJobProperties)) {
            for (final Map<String, String> nejobProperty : neJobProperties) {
                if (nejobProperty.get(ShmConstants.KEY).equals(CommonLicensingActivityConstants.LICENSE_FILE_PATH)) {
                    licenseKeyFilePath = nejobProperty.get(ShmConstants.VALUE);
                    break;
                }
            }
        }
        LOGGER.debug("licenseKeyFilePath {} fetched form NeJob {}", licenseKeyFilePath, neJobProperties);
        return licenseKeyFilePath;
    }

    @SuppressWarnings("unchecked")
    public String getLicenseKeyFilePathFromFingerPrint(final String fingerprint) {
        final String licenseKeyFilePath = installLicenseService.generateLicenseKeyFilePath(fingerprint);
        LOGGER.debug("License Key file Path from DB {}", licenseKeyFilePath);
        return licenseKeyFilePath;
    }

    @SuppressWarnings("unchecked")
    public String getSequenceNumber(final String fingerprint) {
        final String sequenceNumber = installLicenseService.getSequencenumber(fingerprint);
        LOGGER.debug("Sequence number of ECIM node from DB is {} with fingerprint {}", sequenceNumber, fingerprint);
        return sequenceNumber;
    }

    /**
     * Based on the data from reportProgress preparing result map, which contains FAILURE/SUCCESS , completion and log message information.
     * 
     * @param reportProgress
     * @return
     */
    public Map<String, Object> getActionStatus(final AsyncActionProgress reportProgress, final String actionName) {
        JobResult jobResult = JobResult.FAILED;
        boolean isActionCompleted = false;
        JobLogLevel jobLogLevel = JobLogLevel.INFO;
        String logmessage = "";
        if (reportProgress.getState() == ActionStateType.FINISHED) {
            if (reportProgress.getResult() == ActionResultType.SUCCESS) {
                jobResult = JobResult.SUCCESS;
                jobLogLevel = JobLogLevel.INFO;
                logmessage = (actionName.equalsIgnoreCase(ActivityConstants.INSTALL)) ? String.format(JobLogConstants.ACTIVITY_COMPLETED_SUCCESSFULLY, actionName) : String.format(
                        JobLogConstants.STEP_COMPLETED_SUCCESSFULLY, actionName);
                isActionCompleted = true;
            } else if (reportProgress.getResult() == ActionResultType.FAILURE) {
                jobResult = JobResult.FAILED;
                jobLogLevel = JobLogLevel.ERROR;
                logmessage = String.format(JobLogConstants.ACTIVITY_FAILED, actionName);
                isActionCompleted = true;
            } else if (reportProgress.getResult() == ActionResultType.NOT_AVAILABLE) {
                jobResult = JobResult.FAILED;
                jobLogLevel = JobLogLevel.ERROR;
                logmessage = String.format(JobLogConstants.STILL_EXECUTING, actionName);
                isActionCompleted = true;
            } else {
                jobResult = JobResult.CANCELLED;
                jobLogLevel = JobLogLevel.INFO;
                logmessage = String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, actionName);
                isActionCompleted = true;
            }

        } else if (reportProgress.getState() == ActionStateType.RUNNING) {
            jobResult = JobResult.FAILED;
            jobLogLevel = JobLogLevel.ERROR;
            logmessage = String.format(JobLogConstants.STILL_EXECUTING, actionName);
            isActionCompleted = false;
        } else if (reportProgress.getState() == ActionStateType.CANCELLED) {
            jobResult = JobResult.FAILED;
            jobLogLevel = JobLogLevel.ERROR;
            logmessage = String.format(JobLogConstants.ACTIVITY_CANCELLED_SUCCESSFULLY, actionName);
            isActionCompleted = true;
        }

        final Map<String, Object> actionStatusMap = new HashMap<String, Object>();
        actionStatusMap.put(ActivityConstants.JOB_RESULT, jobResult);
        actionStatusMap.put(ACTION_STATUS, isActionCompleted);
        actionStatusMap.put(ActivityConstants.JOB_LOG_MESSAGE, logmessage);
        actionStatusMap.put(ActivityConstants.JOB_LOG_LEVEL, jobLogLevel);
        return actionStatusMap;
    }

    public boolean isLicenseKeyFileExistsInSMRS(final String licenseKeyFilePath) {

        return fileResource.exists(licenseKeyFilePath);

    }

    public boolean isLicensingPOExists(final String licenseKeyFileName) {
        final Map<String, Object> restrictionAttributes = new HashMap<String, Object>();
        restrictionAttributes.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseKeyFileName);
        final List<PersistenceObject> licensePOs = getAttributesListOfLicensePOs(restrictionAttributes);
        if (licensePOs.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<PersistenceObject> getAttributesListOfLicensePOs(final Map<String, Object> restrictions) {
        return getLicensePOs(restrictions);
    }

    private List<PersistenceObject> getLicensePOs(final Map<String, Object> restrictions) {

        final List<PersistenceObject> persistenceObjectList = new ArrayList<PersistenceObject>();
        final QueryExecutor queryExecutor = getLiveBucket().getQueryExecutor();
        final Query<TypeRestrictionBuilder> query = buildRestrictions(LicensingActivityConstants.LICENSE_DATA_PO_NAMESPACE, LicensingActivityConstants.LICENSE_DATA_PO, restrictions);
        final Iterator<PersistenceObject> iterator = queryExecutor.execute(query);
        while (iterator.hasNext()) {
            persistenceObjectList.add(iterator.next());
        }
        LOGGER.debug("persistenceObjectList size {} ", persistenceObjectList.size());
        return persistenceObjectList;
    }

    private DataBucket getLiveBucket() {
        try {
            return dataPersistenceService.getLiveBucket();
        } catch (final RuntimeException ex) {
            LOGGER.error("Exception while retrieving live bucket. Reason : {}", ex);
            throw new ServerInternalException(EcimCommonConstants.INV_INTERNAL_ERROR);
        }
    }

    private Query<TypeRestrictionBuilder> buildRestrictions(final String nameSpace, final String type, final Map<String, Object> restrictions) {

        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> query = queryBuilder.createTypeQuery(nameSpace, type);

        Restriction queryRestriction;
        final List<Restriction> restrictionList = new ArrayList<Restriction>();

        for (final Map.Entry<String, Object> entry : restrictions.entrySet()) {

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
        return query;
    }

    /**
     * This method updates the installedOn value in the LicenseData PO when the license key file is installed
     * 
     * @param jobEnvironment
     * @param licensingMOFdn
     */
    public void persistLicenseInstalledTime(final NEJobStaticData neJobStaticData, final Map<String, Object> restrictionAttributes) {
        final boolean isUpdateSuccess = licensingRetryService.updateLicenseInstalledTime(restrictionAttributes);
        LOGGER.debug("Installed date updated in LicenseData PO : {} ", isUpdateSuccess);
        if (!isUpdateSuccess) {
            final String logMessage = "Updating of InstalledOn attribute got failed because LicenseData PO does not exists in the DPS.";
            LOGGER.debug(logMessage);
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_PROCESS_NOTIFICATION, neJobStaticData.getNodeName(), null, "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getNodeName()
                    + ":" + logMessage);
        }
    }

    /**
     * This method deletes the License Key Files.If there are License Key files that are already installed with the same FingerPrint and lesser Sequence Number than the License Key file that to be
     * installed, then the historic license key files are deleted from the DPS and also from SMRS location. logs in job logs.
     * 
     * @param jobEnvironment
     * @param licensingMOFdn
     */
    public void deleteLicenseKeyFile(final NEJobStaticData neJobStaticData, final Map<String, Object> restrictionAttributes) {
        // get the licenseData PO
        final List<Map<String, Object>> poAttributesList = licensingRetryService.getAttributesListOfLicensePOs(restrictionAttributes);
        if (poAttributesList.isEmpty()) {
            final String logMessage = "There is no data present in the database.";
            LOGGER.debug("The reason is:{}", logMessage);
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_DELETE, neJobStaticData.getNodeName(), null, "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getMainJobId() + ":"
                    + logMessage);
        }
        for (final Map<String, Object> poAttributes : poAttributesList) {
            // obtaining license data key file details for the deletion of PO
            final String fingerPrint = (String) poAttributes.get(LicensingActivityConstants.LICENSE_DATA_FINGERPRINT);
            final String sequenceNumber = (String) poAttributes.get(LicensingActivityConstants.LICENSE_DATA_SEQUENCE_NUMBER);

            final String logMessage = licenseKeyFileDeleteService.deleteHistoricLicensePOs(fingerPrint, sequenceNumber);
            LOGGER.debug("The delete result is:{} for NeJobId {}", logMessage, neJobStaticData.getNeJobId());
            // Recording the event
            activityUtils.recordEvent(SHMEvents.LICENSE_INSTALL_DELETE, neJobStaticData.getNodeName(), null, "SHM:" + neJobStaticData.getNeJobId() + ":" + neJobStaticData.getMainJobId() + ":"
                    + logMessage);
        }
    }

    /**
     * Updates the installed Date in LicenseData PO and delete license key files which are having less sequence number than currently installed license.
     * 
     * @param ecimLicensingInfo
     *            TODO
     * @param jobEnvironment
     * @param licensingMoFdn
     */
    public Map<String, Object> getRestrictionAttributes(final EcimLicensingInfo ecimLicensingInfo) {
        final Map<String, Object> restrictionAttributes = new HashMap<>();
        final String licenseKeyFilePath = ecimLicensingInfo.getLicenseKeyFilePath();
        restrictionAttributes.put(LicensingActivityConstants.LICENSE_DATA_LICENSE_KEYFILE_PATH, licenseKeyFilePath);
        return restrictionAttributes;
    }
}
