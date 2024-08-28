/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.shm.common.exception.BackupDataNotFoundException;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.BackupActivityConstants;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActiveSoftwareProvider;
import com.ericsson.oss.services.shm.es.impl.AutoGenerateNameValidator;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.api.CheckPeriodicity;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class CvNameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CvNameProvider.class);
    private static final String PRODUCT_DATA_SPLIT_CHARACTER = "\\|\\|";

    @Inject
    private JobStaticDataProvider jobStaticDataProvider;

    @Inject
    private EAccessControl eAccessControl;

    @Inject
    private ActiveSoftwareProvider activeSoftwareProvider;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    private AutoGenerateNameValidator autoGenerateNameValidator;

    @Inject
    private CheckPeriodicity periodicityChecker;

    @SuppressWarnings("unchecked")
    public String getConfigurationVersionName(final NEJobStaticData neJobStaticData, final String specificKey) throws JobDataNotFoundException {
        String cvName = "";
        final String nodeName = neJobStaticData.getNodeName();
        final long neJobId = neJobStaticData.getNeJobId();
        final long mainJobId = neJobStaticData.getMainJobId();
        final Map<String, Object> neJobAttributes = jobConfigServiceRetryProxy.getNeJobAttributes(neJobId);
        final String platform = neJobStaticData.getPlatformType();
        final List<Map<String, String>> neJobPropertyList = (List<Map<String, String>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);

        cvName = getValueForSpecificKey(specificKey, neJobPropertyList);
        if (cvName == null || cvName.isEmpty()) {
            LOGGER.debug("NeJobPropertyList while getConfigurationVersionName with key {} is {}", specificKey, neJobPropertyList);
            final Map<String, Object> mainJobAttributes = jobConfigServiceRetryProxy.getMainJobAttributes(mainJobId);
            final boolean isPeriodicJob = isJobPeriodic((Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS));
            final Map<String, String> propertyKeyValueMap = getPropertyValueFromJobConfiguration(Arrays.asList(specificKey, JobPropertyConstants.AUTO_GENERATE_BACKUP), mainJobAttributes, nodeName,
                    platform);
            cvName = propertyKeyValueMap.get(specificKey);
            if ((cvName == null || cvName.isEmpty())) {
                throw new BackupDataNotFoundException(JobLogConstants.BACKUP_NAME_DOES_NOT_EXIST);
            } else {
                final JobStaticData jobStaticData = jobStaticDataProvider.getJobStaticData(mainJobId);
                eAccessControl.setAuthUserSubject(jobStaticData.getOwner());
                String customizedCVName = getCVNameWithProductData(neJobId, cvName, nodeName);
                if (customizedCVName.contains(ShmConstants.NODE_NAME_PLACEHOLDER)) {
                    customizedCVName = customizedCVName.replace(ShmConstants.NODE_NAME_PLACEHOLDER, nodeName);
                }
                customizedCVName = adjustCustomizedCVName(cvName, customizedCVName);
                customizedCVName = appendTimeStampForPeriodicJobs(isPeriodicJob, cvName, customizedCVName);
                customizedCVName = adjustCVNameLength(customizedCVName);
                String validatedCvName = autoGenerateNameValidator.getValidatedAutoGenerateBackupName(customizedCVName);
                persistCVname(neJobId, neJobAttributes, validatedCvName);
                LOGGER.debug("validatedCvName in getConfigurationVersionName {} neJobId {} nodeName {}", validatedCvName, neJobId, nodeName);
                return validatedCvName;

            }
        }
        return cvName;
    }

    private String appendTimeStampForPeriodicJobs(final boolean isPeriodicJob, final String cvName, String customizedCVName) {
        if (isPeriodicJob && !cvName.contains(ShmConstants.TIMESTAMP_PLACEHOLDER)) {
            final Date dateTime = new Date();
            final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
            final String dateValue = formatter.format(dateTime);
            if (customizedCVName.length() > BackupActivityConstants.CV_NAME_MIN_CHAR_LIMIT) {
                customizedCVName = customizedCVName.substring(0, BackupActivityConstants.CV_NAME_MIN_CHAR_LIMIT);
            }
            customizedCVName += dateValue;
        }
        return customizedCVName;
    }

    @SuppressWarnings("unchecked")
    private boolean isJobPeriodic(final Map<String, Object> jobConfigurationDetails) {
        final Map<String, Object> mainSchedule = (Map<String, Object>) jobConfigurationDetails.get(ShmConstants.MAIN_SCHEDULE);
        final List<Map<String, Object>> schedulePropertiesList = (List<Map<String, Object>>) mainSchedule.get(ShmConstants.SCHEDULINGPROPERTIES);
        return periodicityChecker.isJobPeriodic(schedulePropertiesList);
    }

    private String getCVNameWithProductData(final long neJobId, String cvName, final String nodeName) {
        LOGGER.debug("getCVNameWithProductData neJobId {}, cvName {}, nodeName {}", neJobId, cvName, nodeName);
        if (cvName.contains(ShmConstants.PRODUCT_NUMBER_PLACEHOLDER) || cvName.contains(ShmConstants.PRODUCT_REVISION_PLACEHOLDER)) {
            return preapreCVNameWithProductData(cvName, nodeName);
        }
        return cvName;
    }

    private String preapreCVNameWithProductData(String cvName, final String nodeName) {
        final Map<String, String> activeSoftware = activeSoftwareProvider.getActiveSoftwareDetails(Arrays.asList(nodeName));
        final String nodeActiveSoftware = activeSoftware.get(nodeName);
        if (nodeActiveSoftware != null && !nodeActiveSoftware.isEmpty()) {
            final String[] productDataDetails = nodeActiveSoftware.split(PRODUCT_DATA_SPLIT_CHARACTER);
            if (productDataDetails != null && productDataDetails.length >= 2) {
                return getCustomizedBackupName(cvName, productDataDetails[0], productDataDetails[1]);
            } else {
                throw new BackupDataNotFoundException(JobLogConstants.ACTIVE_SOFTWARE_DETAILS_NOT_FOUND);
            }
        } else {
            throw new BackupDataNotFoundException(JobLogConstants.ACTIVE_SOFTWARE_DETAILS_NOT_FOUND);
        }
    }

    private String adjustCVNameLength(String customizedCVName) {
        if (customizedCVName.length() > BackupActivityConstants.CV_NAME_MAX_CHAR_LIMIT) {
            customizedCVName = customizedCVName.substring(0, BackupActivityConstants.CV_NAME_MAX_CHAR_LIMIT);
        }
        return customizedCVName;
    }

    private String adjustCustomizedCVName(final String cvName, String customizedCVName) {
        if (cvName.endsWith(ShmConstants.TIMESTAMP_PLACEHOLDER)) {
            String tempUpdatedCVName = customizedCVName.replace(ShmConstants.TIMESTAMP_PLACEHOLDER, "");
            if (tempUpdatedCVName.length() > BackupActivityConstants.CV_NAME_MIN_CHAR_LIMIT) {
                customizedCVName = tempUpdatedCVName.substring(0, BackupActivityConstants.CV_NAME_MIN_CHAR_LIMIT);
                customizedCVName += ShmConstants.TIMESTAMP_PLACEHOLDER;
            }
        }
        if (customizedCVName.contains(ShmConstants.TIMESTAMP_PLACEHOLDER)) {
            final SimpleDateFormat formatter = new SimpleDateFormat(JobPropertyConstants.AUTO_GENERATE_DATE_FORMAT);
            final String timestamp = formatter.format(new Date());
            customizedCVName = customizedCVName.replace(ShmConstants.TIMESTAMP_PLACEHOLDER, timestamp);
        }
        return customizedCVName;
    }

    private String getCustomizedBackupName(String customizedCVName, String productNumber, String productRevision) {
        LOGGER.debug("getCustomizedBackupName customizedCVName {}, productNumber {}, productRevision {}", customizedCVName, productNumber, productRevision);
        if (customizedCVName.contains(ShmConstants.PRODUCT_NUMBER_PLACEHOLDER)) {
            productNumber = productNumber.replace(ActivityConstants.SLASH, ActivityConstants.UNDERSCORE);
            customizedCVName = customizedCVName.replace(ShmConstants.PRODUCT_NUMBER_PLACEHOLDER, productNumber);
        }
        if (customizedCVName.contains(ShmConstants.PRODUCT_REVISION_PLACEHOLDER)) {
            productRevision = productRevision.replace(ActivityConstants.SLASH, ActivityConstants.UNDERSCORE);
            customizedCVName = customizedCVName.replace(ShmConstants.PRODUCT_REVISION_PLACEHOLDER, productRevision);
        }
        return customizedCVName;
    }

    public static String getCvNameWithDefaultKey(final List<Map<String, String>> neJobPropertyList) {
        LOGGER.debug("CV Name not found with existing keys, so checking with default key {} in neJobPropertyList {}", BackupActivityConstants.CV_NAME, neJobPropertyList);
        return getValueForSpecificKey(BackupActivityConstants.CV_NAME, neJobPropertyList);
    }

    /**
     * This method persists NE Job property.
     * 
     * @param neJobId
     * @param neJobAttributes
     * @param cvName
     * @return void
     */
    @SuppressWarnings("unchecked")
    protected void persistCVname(final long neJobId, final Map<String, Object> neJobAttributes, final String cvName) {
        LOGGER.debug("The NEJobAttributes to be persisted are : {} ", neJobAttributes);
        List<Map<String, Object>> neJobPropertyList = new ArrayList<>();
        if (neJobAttributes.get(ActivityConstants.JOB_PROPERTIES) != null) {
            neJobPropertyList = (List<Map<String, Object>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
        }
        LOGGER.debug("The neJobPropertyList to be persisted is : {}", neJobPropertyList);
        final Map<String, Object> jobProperty = new HashMap<>();
        jobProperty.put(ActivityConstants.JOB_PROP_KEY, BackupActivityConstants.CV_NAME);
        jobProperty.put(ActivityConstants.JOB_PROP_VALUE, cvName);
        neJobPropertyList.add(jobProperty);
        jobUpdateService.updateRunningJobAttributes(neJobId, neJobPropertyList, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPropertyValueFromJobConfiguration(final List<String> keyList, final Map<String, Object> mainJobAttributes, final String nodeName, final String platform) {
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ShmConstants.JOBCONFIGURATIONDETAILS);
        Map<String, String> keyValueMap = new HashMap<>();
        LOGGER.debug("getPropertyValueFrom JobConfiguration {} with keyList {}", jobConfigurationDetails, keyList);
        String neType = null;
        if (jobConfigurationDetails != null && !jobConfigurationDetails.isEmpty()) {
            try {
                neType = networkElementRetrievalBean.getNeType(nodeName);
            } catch (final MoNotFoundException moNotFoundEx) {
                LOGGER.error("Exception while fetching neType of node :  {} due to : {}", nodeName, moNotFoundEx);
            }
            keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, nodeName, neType, platform);
            LOGGER.debug("propertyValue in getPropertyValueFromJobConfiguration method {} ", keyValueMap);
        }
        return keyValueMap;
    }

    public static String getValueForSpecificKey(final String key, final List<Map<String, String>> neJobPropertyList) {
        if (neJobPropertyList != null) {
            for (final Map<String, String> neJobProperty : neJobPropertyList) {
                if (key.equals(neJobProperty.get(ShmConstants.KEY))) {
                    return neJobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        return "";
    }
}