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
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.activities.JobExecutionValidatorFactory;
import com.ericsson.oss.services.shm.common.DpsReader;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobexecutorlocal.TargetResolver;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.modelentities.Activity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.ComponentActivity;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobExecutionIndexAndState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobState;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobTemplate;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NEInfo;
import com.ericsson.oss.services.shm.jobs.common.modelentities.NeTypeComponentActivityDetails;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;

@Stateless
public class JobExecutorServiceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutorServiceHelper.class);

    @Inject
    DpsReader dpsReader;

    @Inject
    private DpsWriter dpsWriter;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private TopologyEvaluationServiceManager topologyEvaluationServiceManager;

    @Inject
    private TargetResolverFactory targetResolverFactory;

    @Inject
    private JobExecutionValidatorFactory jobExecutionValidationFactory;

    @Inject
    private NetworkElementResolver networkElementResolver;

    /**
     * @param jobTemplateId
     * @return
     */
    public JobExecutionIndexAndState getLatestJobExecutionIndexAndState(final long jobTemplateId) {
        JobExecutionIndexAndState latestExecutionIndexAndState = null;
        final List<JobExecutionIndexAndState> jobExecutionIndexAndStates = new ArrayList<>();
        final Map<String, Object> attributeRestrictionMap = new HashMap<>();
        attributeRestrictionMap.put(ShmConstants.JOB_TEMPLATE_ID, jobTemplateId);
        final List<Map<String, Object>> jobPOs = getPOAttributes(ShmConstants.NAMESPACE, ShmConstants.JOB, attributeRestrictionMap);
        for (final Map<String, Object> jobPO : jobPOs) {
            final JobExecutionIndexAndState indexAndState = new JobExecutionIndexAndState();
            final String state = (String) jobPO.get(ShmConstants.STATE);
            indexAndState.setJobExecutionIndex((int) jobPO.get(ShmConstants.EXECUTIONINDEX));
            indexAndState.setJobState(JobState.getJobState(state));
            jobExecutionIndexAndStates.add(indexAndState);
        }

        for (final JobExecutionIndexAndState jobExecutionIndexAndState : jobExecutionIndexAndStates) {
            int latestIndex = 0;
            if (latestIndex < jobExecutionIndexAndState.getJobExecutionIndex()) {
                latestExecutionIndexAndState = jobExecutionIndexAndState;
                latestIndex = jobExecutionIndexAndState.getJobExecutionIndex();
            }
        }

        return latestExecutionIndexAndState;
    }

    /**
     * @param attributeRestrictionMap
     * @param namespace
     * @param type
     * @return
     */
    public List<Map<String, Object>> getPOAttributes(final String namespace, final String type, final Map<String, Object> attributeRestrictionMap) {
        final List<Map<String, Object>> poAttributesList = new ArrayList<>();
        final List<PersistenceObject> jobPOs = dpsReader.findPOs(namespace, type, attributeRestrictionMap);
        if (jobPOs != null) {
            for (final PersistenceObject jobPO : jobPOs) {
                poAttributesList.add(jobPO.getAllAttributes());
            }
        }
        return poAttributesList;
    }

    public Map<String, Object> createPO(final String namespace, final String type, final String version, final Map<String, Object> jobAttributes) {
        final Map<String, Object> createdPOAttributes = new HashMap<>();
        final PersistenceObject jobPO = dpsWriter.createPO(namespace, type, version, jobAttributes);
        createdPOAttributes.put(ShmConstants.PO_ID, jobPO.getPoId());
        createdPOAttributes.putAll(jobPO.getAllAttributes());
        return createdPOAttributes;
    }

    public boolean populateNeNamesFromCollections(final long mainJobId, final List<String> neNames, final List<Map<String, Object>> topologyJobLogList, final List<String> collectionIds,
            final String jobOwner) {
        LOGGER.debug("Calling Topology Service to fetch NEs in collections");
        final Set<String> neFdns = new HashSet<>();
        boolean isDataRetrievedForAllCollectionIds = true;
        if (!collectionIds.isEmpty()) {
            for (final String collectionId : collectionIds) {
                LOGGER.debug("CollectionId sent for remoting: {}", collectionId);
                try {
                    neFdns.addAll(topologyEvaluationServiceManager.getCollectionInfo(jobOwner, collectionId));
                } catch (final Exception exception) {
                    isDataRetrievedForAllCollectionIds = false;
                    LOGGER.debug("Exception caught while retrieving NEs from collectionId {} with message {}", collectionId, exception.getMessage());
                    handleCollectionOrSavedSearchException(topologyJobLogList, exception, mainJobId, String.format(JobExecutorConstants.COLLECTION_EXCEPTION, collectionId));
                }
                if (neFdns != null && !neFdns.isEmpty()) {
                    prepareFinalListOfNENames(neNames, neFdns);
                }
            }
        }
        LOGGER.debug("IsDataRetrievedForAllCollectionIds in populateNeNamesFromCollections = {}", isDataRetrievedForAllCollectionIds);
        return isDataRetrievedForAllCollectionIds;
    }

    private void prepareFinalListOfNENames(final List<String> neNames, final Set<String> neFdns) {
        for (final String netWorkElementFdn : neFdns) {
            prepareListofNEsFromFdn(netWorkElementFdn, neNames);
        }
    }

    public boolean populateNeNamesFromSavedSearches(final long mainJobId, final List<String> neNames, final List<Map<String, Object>> topologyJobLogList, final List<String> savedSearchIds,
            final String jobOwner) {
        LOGGER.debug("Calling Topology Service to fetch NEs in saved searches");
        boolean isDataRetrievedForAllSavedSearches = true;
        final Set<String> neFdns = new HashSet<>();
        if (!savedSearchIds.isEmpty()) {
            for (final String savedSearchId : savedSearchIds) {
                LOGGER.debug("SavedSearchId sent for remoting: {}", savedSearchId);
                try {
                    neFdns.addAll(topologyEvaluationServiceManager.getSavedSearchInfo(savedSearchId, jobOwner));
                } catch (final Exception exception) {
                    isDataRetrievedForAllSavedSearches = false;
                    LOGGER.debug("Exception caught while retrieving NEs from savedsearch with message {}", exception.getMessage());
                    handleCollectionOrSavedSearchException(topologyJobLogList, exception, mainJobId, String.format(JobExecutorConstants.SAVEDSEARCH_EXCEPTION, savedSearchId));
                }
                if (neFdns != null && !neFdns.isEmpty()) {
                    prepareFinalListOfNENames(neNames, neFdns);
                }
            }
        }
        LOGGER.debug("IsDataRetrievedForAllSavedSearches in SavedSearches = {}", isDataRetrievedForAllSavedSearches);
        return isDataRetrievedForAllSavedSearches;
    }

    private void handleCollectionOrSavedSearchException(final List<Map<String, Object>> topologyJobLogList, final Exception exception, final long mainJobId, final String errorMessage) {
        final Map<String, Object> topologyCollectionResult = new HashMap<>();
        LOGGER.error(exception.getMessage(), exception);
        topologyCollectionResult.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        topologyCollectionResult.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        topologyCollectionResult.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());
        topologyCollectionResult.put(ActivityConstants.JOB_LOG_MESSAGE, errorMessage);
        topologyJobLogList.add(topologyCollectionResult);
        jobUpdateService.readAndUpdateRunningJobAttributes(mainJobId, null, topologyJobLogList);

    }

    public void prepareJobLogAtrributesList(final List<Map<String, Object>> jobLogList, final String neLogMessage, final Date entryTime, final String logType, final String logLevel) {
        final Map<String, Object> activityAttributes = new HashMap<>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, neLogMessage);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, entryTime);
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, logType);
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, logLevel);
        jobLogList.add(activityAttributes);

    }

    /**
     * Differentiates nodes present in both Collections and saved searches based on NetworkElement,MeContext and ManagedElement and adds unique nodes to neNames
     * 
     * @param nodeFdn
     * @param neNames
     * @return List<nodeNames>
     */

    private void prepareListofNEsFromFdn(final String nodeFdn, final List<String> neNames) {
        final String nodeName = FdnUtils.getNodeName(nodeFdn);
        LOGGER.debug("Extracted  Fdn {} from Topology Collection service ", nodeFdn);
        if (nodeName != null && !neNames.contains(nodeName)) {
            neNames.add(nodeName);
        }
    }

    public NetworkElementResponse getNetworkElementDetails(final long mainJobId, final JobTemplate jobTemplate, final List<Map<String, String>> jobProperties,
            final Map<String, String> neDetailsWithParentName) {
        NetworkElementResponse networkElementsResponse = null;
        final List<Map<String, Object>> topologyJobLogList = new ArrayList<>();
        final Map<String, Object> attributeMap = new HashMap<>();
        List<String> collectionIds;
        List<String> neNames;
        List<String> savedSearchIds;
        boolean isDataRetrievedForAllCollectionIds = false;
        boolean isDataRetrievedForAllSavedSearches = false;
        final boolean flagForValidateNesforCreateNejobs = true;

        final long templateJobId = jobTemplate.getJobTemplateId();
        final String jobType = jobTemplate.getJobType().name();
        final JobTypeEnum jobTypeEnum = JobTypeEnum.getJobType(jobType);
        final JobConfiguration jobConfiguration = jobTemplate.getJobConfigurationDetails();
        final NEInfo selectedNEInfo = jobConfiguration.getSelectedNEs();
        if (selectedNEInfo != null) {
            neNames = selectedNEInfo.getNeNames();
            collectionIds = selectedNEInfo.getCollectionNames();
            savedSearchIds = selectedNEInfo.getSavedSearchIds();

            if (!neNames.isEmpty() || !collectionIds.isEmpty() || !savedSearchIds.isEmpty()) {
                final String jobOwner = jobTemplate.getOwner();

                //Evaluate collections if exists
                isDataRetrievedForAllCollectionIds = populateNeNamesFromCollections(mainJobId, neNames, topologyJobLogList, collectionIds, jobOwner);
                //Evaluate savedSearches if exists
                isDataRetrievedForAllSavedSearches = populateNeNamesFromSavedSearches(mainJobId, neNames, topologyJobLogList, savedSearchIds, jobOwner);
                LOGGER.debug("isDataRetrievedForAllCollectionIds = {} and isDataRetrievedForAllSavedSearches = {}", isDataRetrievedForAllCollectionIds, isDataRetrievedForAllSavedSearches);
                if (!isDataRetrievedForAllCollectionIds || !isDataRetrievedForAllSavedSearches) {
                    markJobAsFailed(attributeMap, jobProperties);
                }

                /*
                 * Target Resolver has been introduced to provide different implementations to get target based on JobType. Currently NFVO is not being treated as NetworkElement in ENM. So to treat
                 * NFVO as NetworkElement and fit the solution in existing SHM job framework, separate implementation has been provided.
                 * 
                 * By default for all JobTypes, NetworkElementResolver is the target resolver and only for Onboard job NFVOResolver is the target resolver.
                 */
                final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(jobType);
                networkElementsResponse = targetResolver.getNetworkElementResponse(mainJobId, neNames, templateJobId, attributeMap, jobTypeEnum, true);
                LOGGER.debug("networkElementsResponse getSupportedNes = {},  getNesWithComponents {}", networkElementsResponse.getSupportedNes(), networkElementsResponse.getNesWithComponents());
                final Map<NetworkElement, String> invalidNetworkElements = networkElementsResponse.getInvalidNes();
                LOGGER.debug("Invalid Network Elements = {}", invalidNetworkElements);
                //do not use the "neNames" variable, since in above method it will be modified.
                //Validate Network Elements depending on platform type.
                final List<Map<String, Object>> neWithComponentInfo = jobTemplate.getJobConfigurationDetails().getSelectedNEs().getNeWithComponentInfo();
                validateNEs(jobTypeEnum, networkElementsResponse, neWithComponentInfo, neDetailsWithParentName, flagForValidateNesforCreateNejobs);
                networkElementsResponse.setInvalidNes(invalidNetworkElements);
            } else {
                LOGGER.info("NE names are not available for NEinfo : {}", selectedNEInfo);
            }

        }
        return networkElementsResponse;
    }

    private void markJobAsFailed(final Map<String, Object> attributeMap, final List<Map<String, String>> mainJobsPropertyList) {
        final Map<String, String> topologyEvaluation = new HashMap<>();
        topologyEvaluation.put(ShmConstants.KEY, ShmConstants.TOPOLOGY_EVALUATION_FAILED);
        topologyEvaluation.put(ShmConstants.VALUE, Boolean.toString(true));
        mainJobsPropertyList.add(topologyEvaluation);
        LOGGER.debug("After failure of topology evaluation, main jobProperty list set to {}", mainJobsPropertyList);
        attributeMap.put(ShmConstants.JOBPROPERTIES, mainJobsPropertyList);
    }

    /**
     * Method to validate the selected network elements based on their platform type. It logs the list of unsupported Network Elements to main job logs. Returns the list of network elements that
     * support the job execution, depending on platform type.
     * 
     * @param mainJobId
     * @param jobTypeEnum
     * @param neDetailsWithParentName
     * @param networkElementList
     * 
     * @return networkElementList
     */
    private NetworkElementResponse validateNEs(final JobTypeEnum jobTypeEnum, final NetworkElementResponse networkElementResponse, final List<Map<String, Object>> nesWithComponentInfo,
            final Map<String, String> neDetailsWithParentName, final boolean flagForValidateNesforCreateNejobs) {
        JobExecutionValidator jobExecutionValidator = null;
        List<NetworkElement> platformSpecificNEList = null;
        Map<NetworkElement, String> unsupportedNEMap = null;
        Map<String, List<NetworkElement>> nesWithComponentsMap = null;
        List<NetworkElement> supportedNEs = networkElementResponse.getSupportedNes();
        final Map<PlatformTypeEnum, List<NetworkElement>> neGroupsByPlatform = groupNetworkElementsByPlatform(supportedNEs);
        final Set<PlatformTypeEnum> platforms = neGroupsByPlatform.keySet();
        //unsupportedNEsList is list of unsupportedNEMap for different platforms
        final List<Map<NetworkElement, String>> unsupportedNEsList = new ArrayList<>();
        for (final PlatformTypeEnum platform : platforms) {
            jobExecutionValidator = jobExecutionValidationFactory.getJobExecutionValidator(platform);
            if (jobExecutionValidator != null) {
                platformSpecificNEList = neGroupsByPlatform.get(platform);
                unsupportedNEMap = jobExecutionValidator.findUnSupportedNEs(jobTypeEnum, platformSpecificNEList);
                nesWithComponentsMap = jobExecutionValidator.findNesWithComponents(jobTypeEnum, platformSpecificNEList, nesWithComponentInfo, neDetailsWithParentName, networkElementResponse,
                        flagForValidateNesforCreateNejobs);
                unsupportedNEsList.add(unsupportedNEMap);
                LOGGER.info("unsupportedNEsList with platform {}", unsupportedNEsList);

                if (nesWithComponentsMap != null && !nesWithComponentsMap.isEmpty()) {
                    networkElementResponse.setNesWithComponents(nesWithComponentsMap);
                    supportedNEs = networkElementResponse.getSupportedNes();
                }
                //Update the list of ECIM NEs lacking fragment support (corresponding to the job type) to main job logs. Skip such NEs from job execution.
                if (unsupportedNEMap != null && !unsupportedNEMap.isEmpty()) {
                    final Set<NetworkElement> unsupportedNEs = unsupportedNEMap.keySet();
                    supportedNEs.removeAll(unsupportedNEs);
                }

            }
        }

        if (!supportedNEs.isEmpty()) {
            networkElementResponse.setSupportedNes(supportedNEs);
        }
        networkElementResponse.setUnsupportedNes(getUnsupportedNes(unsupportedNEsList));
        LOGGER.debug("Unsupported Nes:[{}], Supported Nes:[{}], Invalid Nes:[{}], NesWithComponents Nes:[{}]]", networkElementResponse.getUnsupportedNes().keySet(),
                networkElementResponse.getSupportedNes(), networkElementResponse.getInvalidNes(), networkElementResponse.getNesWithComponents());
        return networkElementResponse;
    }

    /**
     * Method to group the Network Elements based on their platform type.
     * 
     * @param networkElementList
     * 
     * @return neGroupsByPlatform
     * 
     */
    public Map<PlatformTypeEnum, List<NetworkElement>> groupNetworkElementsByPlatform(final List<NetworkElement> networkElementList) {

        final Map<PlatformTypeEnum, List<NetworkElement>> neGroupsByPlatform = new HashMap<>();

        for (final NetworkElement networkElement : networkElementList) {
            final PlatformTypeEnum platformType = networkElement.getPlatformType();
            List<NetworkElement> neList = neGroupsByPlatform.get(platformType);
            if (neList == null) {
                neList = new ArrayList<>();
                neGroupsByPlatform.put(platformType, neList);
            }
            neList.add(networkElement);
        }
        return neGroupsByPlatform;
    }

    /**
     * Method to group the Network Elements based on their platform type.
     * 
     * @param networkElementList
     * 
     * @return neGroupsByPlatform
     * 
     */
    public Map<String, List<NetworkElement>> groupNetworkElementsByNeType(final List<NetworkElement> networkElementList) {

        final Map<String, List<NetworkElement>> neGroupsByNeType = new HashMap<>();

        for (final NetworkElement networkElement : networkElementList) {
            final String neType = networkElement.getNeType();
            List<NetworkElement> neList = neGroupsByNeType.get(neType);
            if (neList == null) {
                neList = new ArrayList<>();
                neGroupsByNeType.put(neType, neList);
            }
            neList.add(networkElement);
        }
        return neGroupsByNeType;
    }

    /**
     * getting all unsupported NEs map from list of NEs MAPs
     * 
     * @param unsupportedNEsList
     * @return unsupportedNesMap
     */
    public Map<NetworkElement, String> getUnsupportedNes(final List<Map<NetworkElement, String>> unsupportedNEsList) {
        final Map<NetworkElement, String> unsupportedNesMap = new HashMap<>();
        if (unsupportedNEsList != null && !unsupportedNEsList.isEmpty()) {
            for (final Map<NetworkElement, String> unsupportedNes : unsupportedNEsList) {
                if (unsupportedNes != null && !unsupportedNes.isEmpty()) {
                    unsupportedNesMap.putAll(unsupportedNes);
                }
            }
        }
        return unsupportedNesMap;
    }

    /**
     * Method to group the Network Elements based on their platform type.
     * 
     * @param map
     * 
     * @return neGroupsByPlatform
     * 
     */
    public Map<PlatformTypeEnum, List<NetworkElement>> groupUnsupportedNesByPlatform(final Map<NetworkElement, String> map) {

        final EnumMap<PlatformTypeEnum, List<NetworkElement>> neGroupsByPlatform = new EnumMap<>(PlatformTypeEnum.class);
        for (final NetworkElement networkElement : map.keySet()) {
            final PlatformTypeEnum platformType = networkElement.getPlatformType();
            List<NetworkElement> neList = neGroupsByPlatform.get(platformType);
            if (neList == null) {
                neList = new ArrayList<>();
                neGroupsByPlatform.put(platformType, neList);
            }
            neList.add(networkElement);
        }
        return neGroupsByPlatform;
    }

    public Map<NetworkElement, String> getFilteredUnSupportedNodes(final Map<NetworkElement, String> unSupportedNodes, final List<NetworkElement> unAuthorizedNodes) {
        final List<String> nodeNames = new ArrayList<>();
        final Map<NetworkElement, String> unSupportedNetworkElements = new HashMap<>();
        unSupportedNetworkElements.putAll(unSupportedNodes);
        for (final NetworkElement networkElement : unAuthorizedNodes) {
            nodeNames.add(networkElement.getName());
        }
        for (final NetworkElement networkElement : unSupportedNodes.keySet()) {
            if (nodeNames.contains(networkElement.getName())) {
                unSupportedNetworkElements.remove(networkElement);
            }
        }
        return unSupportedNetworkElements;
    }

    public List<NetworkElement> getFilteredSupportedNodes(final List<NetworkElement> supportedNodes, final List<NetworkElement> unAuthorizedNodes) {
        final List<String> unAuthorizedNodeNames = new ArrayList<>();

        final List<NetworkElement> supportedNetworkElement = new ArrayList<>();
        supportedNetworkElement.addAll(supportedNodes);
        for (final NetworkElement networkElement : unAuthorizedNodes) {
            unAuthorizedNodeNames.add(networkElement.getName());
        }
        for (final NetworkElement networkElement : supportedNodes) {
            if (unAuthorizedNodeNames.contains(networkElement.getName())) {
                supportedNetworkElement.remove(networkElement);
            }
        }
        return supportedNetworkElement;
    }

    /**
     * @param activities
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> groupActivitiesByNeType(final List<Activity> activities) {
        final Map<String, Object> neTypeGroupedActivities = new HashMap<>();
        for (final Activity activity : activities) {
            final String neType = activity.getNeType();
            if (neType != null && !neType.isEmpty()) {
                List<Activity> activitiesList = (List<Activity>) neTypeGroupedActivities.get(neType);
                if (activitiesList == null) {
                    activitiesList = new ArrayList<>();
                }
                activitiesList.add(activity);
                neTypeGroupedActivities.put(neType, activitiesList);
            }
        }
        return neTypeGroupedActivities;
    }

    @SuppressWarnings("unchecked")
    public List<Activity> getComponentsActivities(final List<NeTypeComponentActivityDetails> neTypeComponentActivityDetailsList, final Map<String, Object> neTypeGroupedActivities,
            final String componentName) {
        final Set<Activity> selectedComponentActivities = new HashSet<>();
        for (final NeTypeComponentActivityDetails neTypeComponentActivityDetails : neTypeComponentActivityDetailsList) {
            if (neTypeComponentActivityDetails != null) {
                for (final Entry<String, Object> neTypeGroupedActivity : neTypeGroupedActivities.entrySet()) {
                    if (neTypeGroupedActivity.getKey().equals(neTypeComponentActivityDetails.getNeType())) {
                        final List<Activity> activities = (List<Activity>) neTypeGroupedActivity.getValue();
                        final List<ComponentActivity> componentActivities = neTypeComponentActivityDetails.getComponentActivities();
                        final List<Activity> selectedActivityObject = getActivityObject(componentName, activities, componentActivities);
                        selectedComponentActivities.addAll(selectedActivityObject);
                    }
                }
            }
        }
        LOGGER.debug("In getComponentsActivity neTypeComponentActivityDetailsList {}, neTypeGroupedActivities {} componentName {} and returned selectedComponentActivities {}",
                neTypeComponentActivityDetailsList, neTypeGroupedActivities, componentName, selectedComponentActivities);
        return new ArrayList<>(selectedComponentActivities);
    }

    private List<Activity> getActivityObject(final String componentName, final List<Activity> selectedComponentActivities, final List<ComponentActivity> componentActivities) {
        final List<Activity> selectedActivityObject = new ArrayList<>();
        for (ComponentActivity componentActivity : componentActivities) {
            if (componentName.equals(componentActivity.getComponentName())) {
                final List<String> selectedActivities = componentActivity.getActivityNames();
                for (String activityName : selectedActivities) {
                    for (Activity activity : selectedComponentActivities) {
                        if (activityName.equals(activity.getName())) {
                            selectedActivityObject.add(activity);
                        }
                    }
                }
            }
        }
        return selectedActivityObject;
    }

    public void prepareInValidNes(final Map<NetworkElement, String> invalidNetworkElements, final Map<String, List<NetworkElement>> nesWithComponents) {
        if (nesWithComponents != null) {
            for (final Entry<NetworkElement, String> invalidNe : invalidNetworkElements.entrySet()) {
                if (nesWithComponents.keySet().contains(invalidNe.getKey().getName())) {
                    for (final NetworkElement invalidNeData : nesWithComponents.get(invalidNe.getKey().getName())) {
                        invalidNetworkElements.put(invalidNeData, invalidNetworkElements.get(invalidNe));
                    }
                    invalidNetworkElements.remove(invalidNe);
                    nesWithComponents.remove(invalidNe.getKey().getName());
                }
            }
        }
    }

    public void prepareUnAuthorizedNes(final List<NetworkElement> unAuthorizedNes, final Map<String, List<NetworkElement>> nesWithComponents) {
        if (nesWithComponents != null) {
            final List<NetworkElement> unAuthorizedNeList = new ArrayList<>(unAuthorizedNes);
            for (final NetworkElement unAuthorizedNe : unAuthorizedNeList) {
                if (nesWithComponents.keySet().contains(unAuthorizedNe.getName())) {
                    unAuthorizedNes.remove(unAuthorizedNe);
                    unAuthorizedNes.addAll(nesWithComponents.get(unAuthorizedNe.getName()));
                    nesWithComponents.remove(unAuthorizedNe.getName());
                }
            }
        }
    }

    public NetworkElementResponse getSupportedAndUnSupportedNetworkElementDetails(final List<String> neNames, final JobTypeEnum jobType, final List<Map<String, Object>> nesWithComponentInfo,
            final Map<String, String> neDetailsWithParentName) {
        final NetworkElementResponse networkElementsResponse = networkElementResolver.getNetworkElementResponse(-1, neNames, -1, null, jobType, false);
        if (networkElementsResponse != null) {
            final Map<NetworkElement, String> invalidNetworkElements = networkElementsResponse.getInvalidNes();
            LOGGER.debug("Invalid Network Elements = {}", invalidNetworkElements);
            validateNEs(jobType, networkElementsResponse, nesWithComponentInfo, neDetailsWithParentName, false);
            networkElementsResponse.setInvalidNes(invalidNetworkElements);
            return networkElementsResponse;
        } else {
            return new NetworkElementResponse();
        }
    }

}
