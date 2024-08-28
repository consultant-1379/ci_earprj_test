/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.AbstractJaxbXsdParser;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException;
import com.ericsson.oss.services.shm.common.modelservice.PlatformTypeProviderImpl;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.ActivityInformation;
import com.ericsson.oss.services.shm.job.activity.ActivityParams;
import com.ericsson.oss.services.shm.job.activity.ActivitySelection;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.job.activity.NodeparamType;
import com.ericsson.oss.services.shm.job.activity.ObjectFactory;
import com.ericsson.oss.services.shm.job.activity.ParamType;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesProvider;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.ManageBackupActivitiesQuery;
import com.ericsson.oss.services.shm.jobs.common.api.ManagedBackupActivity;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.jobs.common.restentities.ActivityInfo;

/**
 * To provide the Activity Information
 *
 * @author xrajeke
 */
@ApplicationScoped
@Profiled
@Traceable
public class JobActivitiesProviderImpl extends AbstractJaxbXsdParser implements JobActivitiesProvider, ManagedBackupActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobActivitiesProviderImpl.class);

    private static final String DELIMETER_UNDERSCORE = "_";
    private static final String DEFAULT_NETYPE_NAME = "DEFAULT";
    private static final String XMLSFOLDER_RELATIVEPATH = "/activityxmls/";
    private static final String JOBACTIVITIES_XSD_RELATIVEPATH = "/activity_information.xsd";

    private static final Map<String, NeActivityInformation> NE_ACTIVITIES_DATAMAP = new HashMap<String, NeActivityInformation>();

    @Inject
    private JobActivityResponseModificationProviderFactory responseModificationProviderFactory;

    @Inject
    private PlatformTypeProviderImpl platformTypeProviderImpl;

    @Inject
    private JobCapabilityProvider jobCapabilityProvider;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Method parses all the activity xml files and stores each activity information in local cache
     */
    @PostConstruct
    public void constructActivities() {
        final long postConstructStarted = System.currentTimeMillis();
        final List<String> xmlFileNamesList = new ArrayList<String>();
        for (final PlatformTypeEnum platform : PlatformTypeEnum.values()) {
            for (final JobType jobType : JobType.values()) {
                xmlFileNamesList.add(platform + "_" + jobType + ".xml");
            }
        }
        for (final String xmlFileName : xmlFileNamesList) {
            LOGGER.info("Reading XmlFileName: {}", xmlFileName);
            final Resource xmlsResources = Resources.getClasspathResource(XMLSFOLDER_RELATIVEPATH + xmlFileName);
            final ActivityInformation jobActivities = parse(xmlsResources);
            if (jobActivities != null) {
                final String platformType = jobActivities.getPlatformType();
                final JobType jobType = jobActivities.getJobType();
                for (final NeActivityInformation netypeActivities : jobActivities.getNeActivityInformation()) {
                    NE_ACTIVITIES_DATAMAP.put(prepareKey(platformType, jobType.name(), netypeActivities.getNeType()), netypeActivities);
                }
            }
        }
        final long postConstructFinished = System.currentTimeMillis();
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(SHMEvents.ShmPostConstructConstants.CLASS_NAME, getClass().getSimpleName());
        eventData.put(SHMEvents.ShmPostConstructConstants.TIME_TAKEN, postConstructFinished - postConstructStarted);
        eventData.put(SHMEvents.ShmPostConstructConstants.MESSAGE, String.format("Activity Information stored in a local cache. And their keys are : %s", NE_ACTIVITIES_DATAMAP.keySet()));
        systemRecorder.recordEventData(SHMEvents.POST_CONSTRUCT, eventData);
    }

    /**
     * @param platformType
     * @param jobType
     * @param netype
     * @return
     */
    private String prepareKey(final String platformType, final String jobType, String netype) {
        netype = (netype == null || netype.equals("")) ? DEFAULT_NETYPE_NAME : netype;
        return platformType.toUpperCase() + DELIMETER_UNDERSCORE + jobType.toUpperCase() + DELIMETER_UNDERSCORE + netype.toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private ActivityInformation parse(final Resource xmlResource) {
        ActivityInformation jobActivities = null;
        final boolean isValidationRequired = true;
        try {
            if (xmlResource.exists()) {
                final JAXBElement<ActivityInformation> jaxBElement = (JAXBElement<ActivityInformation>) parse(xmlResource.getBytes(), JOBACTIVITIES_XSD_RELATIVEPATH, ObjectFactory.class,
                        isValidationRequired);
                jobActivities = jaxBElement.getValue();
                LOGGER.debug("Succesfully parsed xml file {}", xmlResource.getName());
            } else {
                LOGGER.debug("File {{}} does not exist", xmlResource.getName());
            }
        } catch (final Exception e) {
            LOGGER.error("File {{}} Parsing failed due to: {}", xmlResource.getName(), e);
        }
        return jobActivities;
    }

    /**
     * Provides the activity information for a the given request. It reads the cached xmls' data with the prepared key. Key will be prepared based on the query.
     * <p>
     * If default activity information needs to be retrieved, NeType must be empty or null.
     *
     * @param jobActivitiesQueryList
     * @return
     */
    @Override
    public List<JobActivitiesResponse> getNeTypeActivities(final List<JobActivitiesQuery> jobActivitiesQueryList) {
        final List<JobActivitiesResponse> jobActivitiesResponseList = new ArrayList<JobActivitiesResponse>();
        for (final JobActivitiesQuery jobActivitiesQuery : jobActivitiesQueryList) {
            final List<NeActivityInformation> neActivitiesList = new ArrayList<NeActivityInformation>();
            final List<NeInfoQuery> neInfoList = jobActivitiesQuery.getNeTypes();
            PlatformTypeEnum platform = null;
            final String jobType = jobActivitiesQuery.getJobType();
            JobActivitiesResponse jobActivitiesResponse = null;
            for (final NeInfoQuery neInfo : neInfoList) {
                final String inputNeType = neInfo.getNeType();
                try {
                    final String capability = jobCapabilityProvider.getCapability(JobTypeEnum.getJobType(jobType));
                    platform = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(inputNeType, capability);
                } catch (final UnsupportedPlatformException e) {
                    LOGGER.error("Platform Type not found for neType {}. Reason {}", neInfo.getNeType(), e);
                    continue;
                    //Do not re-throw the exception. Intentionally logged and swallowed because we don't want to fail the request when one neType fails.
                }
                final String neSpecificKey = prepareKey(platform.toString(), jobType, inputNeType);
                final String defaultNeKey = prepareKey(platform.toString(), jobType, DEFAULT_NETYPE_NAME);
                LOGGER.info("Retrieving activities for an inputNeType '{}' with the keys either of [{}, {}]", inputNeType, neSpecificKey, defaultNeKey);

                final NeActivityInformation neTypeActivities = NE_ACTIVITIES_DATAMAP.containsKey(neSpecificKey) ? NE_ACTIVITIES_DATAMAP.get(neSpecificKey) : NE_ACTIVITIES_DATAMAP.get(defaultNeKey);
                if (neTypeActivities != null) {
                    final NeActivityInformation clonedNeActivityInformation = cloneNeActivityInformation(neTypeActivities);
                    if (inputNeType != null && !inputNeType.equals("")) {
                        LOGGER.debug("Setting NeType to the activity information as the given in inputNeType instead of keeping '{}'", DEFAULT_NETYPE_NAME);
                        clonedNeActivityInformation.setNeType(inputNeType);
                    }
                    neActivitiesList.add(clonedNeActivityInformation);
                    LOGGER.debug("Retrieved and inserted an Activity Information, now the neActivitiesList size became: {}", neActivitiesList.size());
                }
                if (jobActivitiesResponse != null) {
                    jobActivitiesResponse.setNeActivityInformation(neActivitiesList);
                } else {
                    jobActivitiesResponse = new JobActivitiesResponse();
                    jobActivitiesResponse.setJobType(jobType);
                    jobActivitiesResponse.setPlatform(platform.toString());
                    jobActivitiesResponse.setNeActivityInformation(neActivitiesList);
                }

                final SHMJobActivitiesResponseModifier activitiesResponseModifier = responseModificationProviderFactory.getActivitiesResponseModifier(platform,
                        JobType.fromValue(jobType.toUpperCase()));

                if (activitiesResponseModifier != null) {
                    jobActivitiesResponseList.add(activitiesResponseModifier.getUpdatedJobActivities(neInfo, jobActivitiesResponse));
                } else {
                    jobActivitiesResponseList.add(jobActivitiesResponse);
                }
            }

        }
        LOGGER.info("Found {} activities for job", jobActivitiesResponseList.size());
        return jobActivitiesResponseList;
    }

    /**
     * Provides the activity information for a the given request. It reads the cached xmls' data with the prepared key. Key will be prepared based on the query.
     * <p>
     * If default activity information needs to be retrieved, NeType must be empty or null.
     *
     * @param jobActivitiesQueryList
     * @return
     */
    @Override
    public List<JobActivitiesResponse> getManageBackupNeTypeActivities(final List<ManageBackupActivitiesQuery> jobActivitiesQueryList) {
        final List<JobActivitiesResponse> jobActivitiesResponseList = new ArrayList<JobActivitiesResponse>();
        PlatformTypeEnum platform = null;
        final String jobType = JobType.BACKUP.name().toLowerCase();
        for (final ManageBackupActivitiesQuery manageBackupActivitiesQuery : jobActivitiesQueryList) {
            final List<NeActivityInformation> neActivitiesList = new ArrayList<NeActivityInformation>();
            final String inputNeType = manageBackupActivitiesQuery.getNeType();
            final Boolean multipleBackups = manageBackupActivitiesQuery.getMultipleBackups();
            LOGGER.info("inputNeType '{}' with multiple Backups '{}' ", inputNeType, multipleBackups);
            try {
                final String capability = jobCapabilityProvider.getCapability(JobTypeEnum.BACKUP);
                platform = platformTypeProviderImpl.getPlatformTypeBasedOnCapability(inputNeType, capability);
            } catch (final UnsupportedPlatformException e) {
                LOGGER.error("Platform Type not found for neType {}. Reason {}", inputNeType, e);
                continue;
                //Do not re-throw the exception. Intentionally logged and swallowed because we don't want to fail the request when one neType fails.
            }
            final String neSpecificKey = prepareKey(platform.toString(), jobType, inputNeType);
            final String defaultNeKey = prepareKey(platform.toString(), jobType, DEFAULT_NETYPE_NAME);
            LOGGER.info("Retrieving activities for an inputNeType '{}' with the keys either of [{}, {}]", inputNeType, neSpecificKey, defaultNeKey);

            final NeActivityInformation neTypeActivities = NE_ACTIVITIES_DATAMAP.containsKey(neSpecificKey) ? NE_ACTIVITIES_DATAMAP.get(neSpecificKey) : NE_ACTIVITIES_DATAMAP.get(defaultNeKey);
            if (neTypeActivities != null) {
                final NeActivityInformation clonedNeActivityInformation = cloneNeActivityInformation(neTypeActivities);
                LOGGER.debug("setting the Netype to the Activity Information as the given in inputNeType instead of keeping '{}'", DEFAULT_NETYPE_NAME);
                clonedNeActivityInformation.setNeType(inputNeType);
                neActivitiesList.add(clonedNeActivityInformation);
                LOGGER.info("Retrieved and inserted an Activity Information, now the neActivitiesList size became : {} ", neActivitiesList.size());
            }
            final JobActivitiesResponse jobActivitiesResponse = new JobActivitiesResponse();
            jobActivitiesResponse.setJobType(jobType);
            jobActivitiesResponse.setPlatform(platform.toString());
            jobActivitiesResponse.setNeActivityInformation(neActivitiesList);
            final ManageBackupActivitiesResponseModifier backupActivitiesResponseModifier = responseModificationProviderFactory.getBackupActivitiesResponseModifier(
                    PlatformTypeEnum.getPlatform(platform.toString()), JobType.fromValue(jobType.toUpperCase()));

            if (backupActivitiesResponseModifier != null) {
                jobActivitiesResponseList.add(backupActivitiesResponseModifier.getManageBackupActivities(jobActivitiesResponse, multipleBackups));
            } else {
                jobActivitiesResponseList.add(jobActivitiesResponse);
            }
        }
        LOGGER.info("Found {} activities for job", jobActivitiesResponseList.size());
        return jobActivitiesResponseList;
    }

    private NeActivityInformation cloneNeActivityInformation(final NeActivityInformation neTypeActivities) {
        final NeActivityInformation neActivityInformation = new NeActivityInformation();
        neActivityInformation.getActivity().addAll(getClonedActivities(neTypeActivities.getActivity()));
        neActivityInformation.getActivityParams().addAll(getClonedActivityParams(neTypeActivities.getActivityParams()));
        neActivityInformation.getActivitySelection().addAll(getClonedActivitySelectionParams(neTypeActivities.getActivitySelection()));
        neActivityInformation.setNeType(neTypeActivities.getNeType());
        return neActivityInformation;
    }

    private List<Activity> getClonedActivities(final List<Activity> activitiesList) {
        final List<Activity> activitiesResponseList = new ArrayList<Activity>();
        for (final Activity activity : activitiesList) {
            final Activity activityResponse = new Activity();
            activityResponse.setDependsOn(activity.getDependsOn());
            activityResponse.setExclusiveOf(activity.getExclusiveOf());
            activityResponse.setMandatory(activity.isMandatory());
            activityResponse.setName(activity.getName());
            activityResponse.setOrder(activity.getOrder());
            activityResponse.setType(activity.getType());
            activityResponse.getActivityParams().addAll(getClonedActivityParams(activity.getActivityParams()));
            activitiesResponseList.add(activityResponse);
        }
        return activitiesResponseList;
    }

    private List<ActivityParams> getClonedActivityParams(final List<ActivityParams> activityParams) {
        final List<ActivityParams> activityParamsResposnseList = new ArrayList<ActivityParams>();
        for (final ActivityParams activityParam : activityParams) {
            final ActivityParams activityParamResponse = new ActivityParams();
            activityParamResponse.getParam().addAll(getClonedParams(activityParam.getParam()));
            activityParamResponse.getNodeparam().addAll(getClonedNodeParams(activityParam.getNodeparam()));
            activityParamsResposnseList.add(activityParamResponse);
        }
        return activityParamsResposnseList;
    }

    private List<ParamType> getClonedParams(final List<ParamType> params) {

        final List<ParamType> paramTypeResponseList = new ArrayList<ParamType>();
        if (params != null && !params.isEmpty()) {
            for (final ParamType paramType : params) {
                final ParamType paramTypeResponse = new ParamType();
                paramTypeResponse.setDefaultValue(paramType.getDefaultValue());
                paramTypeResponse.setInputType(paramType.getInputType());
                paramTypeResponse.setName(paramType.getName());
                paramTypeResponse.setDeselectActivity(paramType.getDeselectActivity());
                paramTypeResponse.setSelectActivity(paramType.getSelectActivity());
                paramTypeResponse.setPattern(paramType.getPattern());
                paramTypeResponse.setSelectable(paramType.isSelectable());
                paramTypeResponse.getItem().addAll(paramType.getItem());
                if (!paramType.getParam().isEmpty()) {
                    paramTypeResponse.getParam().addAll(paramType.getParam());
                }
                paramTypeResponseList.add(paramTypeResponse);
            }
        }

        return paramTypeResponseList;
    }

    private List<ActivitySelection> getClonedActivitySelectionParams(final List<ActivitySelection> activitySelectionParams) {
        final List<ActivitySelection> activitySelectionParamsResponseList = new ArrayList<ActivitySelection>();

        for (final ActivitySelection activitySelectionParam : activitySelectionParams) {
            final ActivitySelection activitySelectionResponse = new ActivitySelection();
            activitySelectionResponse.setValid(activitySelectionParam.getValid());
            activitySelectionResponse.setInvalid(activitySelectionParam.getInvalid());
            activitySelectionParamsResponseList.add(activitySelectionResponse);
        }
        return activitySelectionParamsResponseList;
    }

    private List<NodeparamType> getClonedNodeParams(final List<NodeparamType> nodeParams) {
        final List<NodeparamType> nodeParamTypeResponseList = new ArrayList<NodeparamType>();
        for (final NodeparamType nodeparamType : nodeParams) {
            final NodeparamType nodeparamTypeResponse = new NodeparamType();
            nodeparamTypeResponse.setDefaultValue(nodeparamType.getDefaultValue());
            nodeparamTypeResponse.setInputType(nodeparamType.getInputType());
            nodeparamTypeResponse.setMandatory(nodeparamType.isMandatory());
            nodeparamTypeResponse.setName(nodeparamType.getName());
            nodeparamTypeResponse.getItem().addAll(nodeparamType.getItem());
            nodeParamTypeResponseList.add(nodeparamTypeResponse);
        }
        return nodeParamTypeResponseList;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.shm.jobs.common.api.JobActivitiesProvider#getActivityInfo(com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum, com.ericsson.oss.services.shm.job.activity.JobType)
     */
    @Override
    public List<ActivityInfo> getActivityInfo(final String platformType, final String neType, final String jobType) {
        final List<ActivityInfo> resultActivityInfo = new ArrayList<ActivityInfo>();
        final String neSpecificKey = prepareKey(platformType, jobType, neType);
        final String defaultNeKey = prepareKey(platformType, jobType, DEFAULT_NETYPE_NAME);
        LOGGER.info("Retrieving activities for an inputNeType '{}' with the keys either of [{}, {}]", neType, neSpecificKey, defaultNeKey);
        final NeActivityInformation neTypeActivities = NE_ACTIVITIES_DATAMAP.containsKey(neSpecificKey) ? NE_ACTIVITIES_DATAMAP.get(neSpecificKey) : NE_ACTIVITIES_DATAMAP.get(defaultNeKey);
        if (neTypeActivities != null) {
            for (final Activity activity : neTypeActivities.getActivity()) {
                resultActivityInfo.add(new ActivityInfo(activity.getName(), null, activity.getOrder().intValue()));
            }

        }
        LOGGER.info("Returning back {} activityInfo elements", resultActivityInfo.size());
        return resultActivityInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ericsson.oss.shm.jobs.common.api.JobActivitiesProvider#getActivityProperties(String platformType, String neType, String jobType)
     */
    @Override
    public List<String> getActivityProperties(final String platformType, final String neType, final String jobType) {
        final List<String> activityProperties = new ArrayList<String>();
        final String neSpecificKey = prepareKey(platformType, jobType, neType);
        final String defaultNeKey = prepareKey(platformType, jobType, DEFAULT_NETYPE_NAME);
        final NeActivityInformation neActivityInformation = NE_ACTIVITIES_DATAMAP.containsKey(neSpecificKey) ? NE_ACTIVITIES_DATAMAP.get(neSpecificKey) : NE_ACTIVITIES_DATAMAP.get(defaultNeKey);
        if (neActivityInformation != null) {
            for (final Activity activity : neActivityInformation.getActivity()) {
                for (final ActivityParams activityParams : activity.getActivityParams()) {
                    for (final NodeparamType nodeparamType : activityParams.getNodeparam()) {
                        activityProperties.add(nodeparamType.getName());
                    }
                    for (final ParamType paramType : activityParams.getParam()) {
                        activityProperties.add(paramType.getName());
                    }
                }
            }
        }
        return activityProperties;
    }
}
