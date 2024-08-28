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
package com.ericsson.oss.services.shm.es.impl;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.constants.ShmJobConstants;
import com.ericsson.oss.services.shm.es.api.JobReportConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This class is used to fetch the Activities with their corresponding Step Durations (as persisted during each activity step) and calculate minimum, maximum and average of the step level time
 * durations for each main job.
 * 
 * @author xarirud
 */
@Stateless
public class MainJobStepDurationsCalculator {
    final static private Logger LOGGER = LoggerFactory.getLogger(MainJobStepDurationsCalculator.class.getName());

    @EServiceRef
    private DataPersistenceService dataPersistenceService;

    @Inject
    private JobConfigurationService jobConfigurationService;

    /**
     * Fetch the Activities with their corresponding Step Durations (as persisted during each activity step ) and calculate minimum, maximum and average of the step level time durations for each main
     * job.
     * 
     * @param mainjobPOId
     *            - The main job identifier.
     * @param mainJobAttributes
     *            - The attributes to be updated which will be persisted for the main job later.
     * @return true if successfully validated and calculated Activity and Step durations, false otherwise.
     */
    public boolean calculateMetrics(final long mainjobPOId, final Map<String, Object> mainJobAttributes) {
        LOGGER.debug("Entered into MainJobStepDurationsCalculator.calculateMetrics with main job Id:{}. Entry time : {}", mainjobPOId, new Date().getTime());
        try {
            final Map<String, Object> stepDurationsMetricTobeUpdated = calculateActivityStepDurationMetrics(mainjobPOId);
            final List<String> metricsToBePersisted = rearrangeMapToList(stepDurationsMetricTobeUpdated);
            mainJobAttributes.put(ShmConstants.STEP_DURATIONS, metricsToBePersisted.toString());
            LOGGER.debug("Exiting from MainJobStepDurationsCalculator.calculateMetrics with main job Id:{}. Exit time : {}", mainjobPOId, new Date().getTime());
            return true;
        } catch (final Exception ex) {
            LOGGER.error("Exception occurred while calculating Step durations for Main Job ID : {}, Reason : {}", mainjobPOId, ex.getMessage());
            return false;
        }
    }

    /**
     * Calculate the MIN, AVG and MAX of each step for every activity.
     * 
     * @param mainJobPoId
     *            - The main job {@link PersistenceObject} ID.
     * @return A map with the required metrics.
     */
    private Map<String, Object> calculateActivityStepDurationMetrics(final long mainJobPoId) {
        final Map<String, Object> metricsTobeUpdated = new HashMap<String, Object>();
        final List<Long> neJobIDs = jobConfigurationService.getNeJobIDs(mainJobPoId);
        LOGGER.debug("Updating activity metrics for Main Job {}, having {} NE jobs", mainJobPoId, neJobIDs.size());
        final Map<String, Map<String, List<Double>>> activityMetrics = getActivityAndStepDurations(neJobIDs);
        for (final Entry<String, Map<String, List<Double>>> activityLevel : activityMetrics.entrySet()) {
            final Map<String, Object> stepLevelMetrics = new HashMap<String, Object>();
            for (final Entry<String, List<Double>> stepLevel : activityLevel.getValue().entrySet()) {
                final Map<String, Object> metrics = new HashMap<String, Object>();
                metrics.put(JobReportConstants.MIN, findMinimum(stepLevel.getValue()));
                metrics.put(JobReportConstants.AVG, findAverage(stepLevel.getValue()));
                metrics.put(JobReportConstants.MAX, findMaximum(stepLevel.getValue()));
                stepLevelMetrics.put(stepLevel.getKey(), metrics);
            }
            metricsTobeUpdated.put(activityLevel.getKey(), stepLevelMetrics);
        }
        LOGGER.debug("Activity step durations metrics = {}", metricsTobeUpdated);
        return metricsTobeUpdated;
    }

    private Double findMinimum(final List<Double> numbers) {
        return Collections.min(numbers);
    }

    private double findAverage(final List<Double> numbers) {
        Double sum = 0.0;
        for (final Double number : numbers) {
            sum = sum + number;
        }
        return trimLong(sum / numbers.size());
    }

    private Double findMaximum(final List<Double> value) {
        return Collections.max(value);
    }

    /**
     * This method retrieves stepDurations attribute for each neJobID from DPS.
     * 
     * @param neJobIDs
     *            - The neJobIDs.
     * @return A map with activity names as key and their corresponding steps and durations.
     */
    private Map<String, Map<String, List<Double>>> getActivityAndStepDurations(final List<Long> neJobIDs) {
        String activityName = null;
        String stepAndDurationsFromDPS = null;
        // Entry will be like => Activity_name - step_name1 - List of durations for step_name1
        final Map<String, Map<String, List<Double>>> allActivitiesAndStepDurationsMap = new HashMap<String, Map<String, List<Double>>>();
        final QueryBuilder queryBuilder = dataPersistenceService.getQueryBuilder();
        final Query<TypeRestrictionBuilder> activityJobTypeQuery = queryBuilder.createTypeQuery(ShmJobConstants.NAMESPACE, ShmJobConstants.ACTIVITY_JOB);
        final TypeRestrictionBuilder restrictionBuilder = activityJobTypeQuery.getRestrictionBuilder();
        final QueryExecutor queryExecutor = dataPersistenceService.getLiveBucket().getQueryExecutor();
        final Projection activityNameProjection = ProjectionBuilder.attribute(ShmConstants.ACTIVITY_NAME);
        final Projection stepDurationProjection = ProjectionBuilder.attribute(ShmConstants.STEP_DURATIONS);
        for (final Long neJobId : neJobIDs) {
            final Restriction neJobIdRestriction = restrictionBuilder.equalTo(ShmConstants.NE_JOB_ID, neJobId);
            activityJobTypeQuery.setRestriction(neJobIdRestriction);

            final List<Object[]> activityStepDurations = queryExecutor.executeProjection(activityJobTypeQuery, activityNameProjection, stepDurationProjection);

            for (final Object[] eachActivityStepDuration : activityStepDurations) {
                activityName = (String) eachActivityStepDuration[0];
                stepAndDurationsFromDPS = (String) eachActivityStepDuration[1];
                final List<String> activityStepsDuration = convertStringToList(stepAndDurationsFromDPS);
                if (activityName != null && activityStepsDuration != null && !activityStepsDuration.isEmpty()) {
                    updateAllActivityAndStepDetailsMap(activityName, activityStepsDuration, allActivitiesAndStepDurationsMap, neJobId);
                }
            }
        }
        LOGGER.debug("AllActivitiesAndStepDurationsMap content : {}", allActivitiesAndStepDurationsMap);
        return allActivitiesAndStepDurationsMap;
    }

    /**
     * This method converts a {@link String} to {@link List}, provided the {@link String} was previously obtained from an {@link ArrayList} using {@link ArrayList#toString()} method. If the supplied
     * String is null, it will return an empty {@link List}.
     * <p>
     * NOTE : This will not convert any String representation to list, only Strings from {@link ArrayList} without any nesting can be converted.
     * <p>
     * 
     * @param strToConvert
     *            - The string to convert.
     * @return {@link List} representation of the provided strToConvert.
     */
    private List<String> convertStringToList(final String strToConvert) {
        final List<String> retList = new ArrayList<String>();
        if (strToConvert == null || strToConvert.trim().length() <= 0) {
            LOGGER.trace("Returning list as empty. {}", retList);
            return retList;
        }
        final String stringWithTrimmedBrackets = strToConvert.substring(1, (strToConvert.length() - 1));
        if (!stringWithTrimmedBrackets.contains(ShmConstants.FDN_DELIMITER)) {
            retList.add(stringWithTrimmedBrackets.trim());
            return retList;
        }
        final String[] stringArrAfterSplit = stringWithTrimmedBrackets.split(ShmConstants.FDN_DELIMITER);
        for (final String str : stringArrAfterSplit) {
            retList.add(str.trim());
        }
        return retList;
    }

    /**
     * Initiates and/or updates the activity and step durations in the map.
     * 
     * @param activityName
     *            - Name of the activity.
     * @param stepNameAndDurationsList
     *            - The list to add.
     * @param existingAllDetailsMap
     *            - The map reference to be updated.
     */
    private void updateAllActivityAndStepDetailsMap(final String activityName, final List<String> stepNameAndDurationsList, final Map<String, Map<String, List<Double>>> existingAllDetailsMap,
            final long neJobId) {
        // Map structure will be as below.
        // [Activity_Name_1] - [Step_name_1] - [List of durations]
        Map<String, List<Double>> existingStepDetailsMap = null;
        String stepName = null;
        if (existingAllDetailsMap.containsKey(activityName)) {
            existingStepDetailsMap = existingAllDetailsMap.get(activityName);
        }
        //If the activity entry is not yet present.
        else {
            existingStepDetailsMap = new HashMap<String, List<Double>>();
        }
        final Map<String, Double> stepNameAndEffectiveDurationsMap = calculateEffectiveDurationFromCumulativeDurations(stepNameAndDurationsList, neJobId);
        for (final Entry<String, Double> stepNameAndEffectiveDuration : stepNameAndEffectiveDurationsMap.entrySet()) {
            stepName = stepNameAndEffectiveDuration.getKey();
            updateMetricsMap(stepName, stepNameAndEffectiveDurationsMap.get(stepName), existingStepDetailsMap);
        }
        existingAllDetailsMap.put(activityName, existingStepDetailsMap);
        LOGGER.debug("UpdatedAllDetailsMap content : {}", existingAllDetailsMap);
    }

    /**
     * Updates the metricsMap with activityStep as key with updated metricValue in the existing List. If there is no value associated with the key then create an empty List and add to it.
     * 
     * @param activityStep
     *            - The step name as key.
     * @param metricValue
     *            - The {@linkplain Double} to be added to the existing {@linkplain List}.
     * @param metricsMap
     *            - The {@linkplain Map} to be updated.
     */
    private void updateMetricsMap(final String activityStep, final Double metricValue, final Map<String, List<Double>> metricsMap) {
        List<Double> existingValue = metricsMap.get(activityStep);
        if (existingValue == null) {
            existingValue = new ArrayList<Double>();
            existingValue.add(metricValue);
            metricsMap.put(activityStep, existingValue);
        } else {
            existingValue.add(metricValue);
            metricsMap.put(activityStep, existingValue);
        }
        LOGGER.debug("metricsMap content : {}", metricsMap);
    }

    /**
     * Re-arrange {@linkplain Map} like the one below to {@linkplain List} format.
     * <p>
     * {upgrade={PRECHECK={MAX=1.5, MIN=1.5, AVG=1.5}, EXECUTE={MAX=6.05, MIN=2.1, AVG=3.4}}, install={PRECHECK={MAX=1.83, MIN=1.68, AVG=1.76}, EXECUTE={MAX=16.19, MIN=14.64, AVG=15.41}},
     * verify={PRECHECK={MAX=0.62,MIN=0.38, AVG=0.5}, EXECUTE={MAX=2.19, MIN=1.21, AVG=1.64}}, confirm={PRECHECK={MAX=2.9, MIN=0.94, AVG=1.62}, EXECUTE={MAX=5.40, MIN=2.00, AVG=3.28}}}
     * <p>
     * 
     * @param metricsTobeUpdated
     *            - Map which needs to be re-arranged.
     * @return The re-arranged {@link List}.
     */
    private List<String> rearrangeMapToList(final Map<String, Object> metricsTobeUpdated) {
        LOGGER.trace("Entering : {}.rearrangeMapToList", this.getClass().getName());
        final List<String> convertedList = new ArrayList<String>();
        for (final Map.Entry<String, Object> entry : metricsTobeUpdated.entrySet()) {
            convertedList.add(entry.getKey() + ":" + entry.getValue());
        }
        LOGGER.trace("Exiting : {}.rearrangeMapToList", this.getClass().getName());
        return convertedList;
    }

    /**
     * Calculates the effective duration from cumulative durations for each step.
     * 
     * @param stepNameAndDurationsList
     *            - The list with stepname and cumulative durations from which effective durations will be calculated.<br>
     *            The entries needs to be in this format : [StepName : effective_Duration].
     * @return A map containing step names and effective durations.
     */
    private Map<String, Double> calculateEffectiveDurationFromCumulativeDurations(final List<String> stepNameAndDurationsList, final long neJobId) {
        final Map<String, Double> effectiveDurationsMap = new HashMap<String, Double>();
        String stepName = null;
        Double cumulativeStepDuration = null;
        String[] splittedStringArray = null;
        for (final String strToSplit : stepNameAndDurationsList) {
            splittedStringArray = strToSplit.split("=");
            stepName = splittedStringArray[0];
            cumulativeStepDuration = Double.parseDouble(splittedStringArray[1]);
            effectiveDurationsMap.put(stepName, cumulativeStepDuration);
        }
        LOGGER.debug("Cumulative time durations for each step after split : {}", effectiveDurationsMap.toString());
        for (final Map.Entry<String, Double> entry : effectiveDurationsMap.entrySet()) {
            stepName = entry.getKey();
            Double allPreviousDuration = 0.0;
            final Double precheckDuration = effectiveDurationsMap.get(ActivityStepsEnum.PRECHECK.getStep());
            final Double executeDuration = effectiveDurationsMap.get(ActivityStepsEnum.EXECUTE.getStep());
            final Double notificationDuration = effectiveDurationsMap.get(ActivityStepsEnum.PROCESS_NOTIFICATION.getStep());
            switch (ActivityStepsEnum.valueOf(stepName)) {
            case PRECHECK:
                // First step is PRECHECK, so keep the duration as it is, just format decimal points
                effectiveDurationsMap.put(stepName, trimLong(precheckDuration));
                break;
            case EXECUTE:
                // Subtract from PRECHECK, if present.
                cumulativeStepDuration = effectiveDurationsMap.get(stepName);
                if (precheckDuration != null) {
                    allPreviousDuration = precheckDuration;
                }
                effectiveDurationsMap.put(stepName, trimLong(cumulativeStepDuration - allPreviousDuration));
                break;
            case PROCESS_NOTIFICATION:
                // Subtract from PRECHECK and EXECUTE, either any one or both will be present.
                cumulativeStepDuration = effectiveDurationsMap.get(stepName);
                if (executeDuration != null) {
                    allPreviousDuration = executeDuration;
                } else if (precheckDuration != null) {
                    allPreviousDuration = precheckDuration;
                }
                effectiveDurationsMap.put(stepName, trimLong(cumulativeStepDuration - allPreviousDuration));
                break;
            case HANDLE_TIMEOUT:
                // Subtract from PRECHECK, EXECUTE and PROCESS_NOTIFICATION
                cumulativeStepDuration = effectiveDurationsMap.get(stepName);
                if (notificationDuration != null) {
                    allPreviousDuration = notificationDuration;
                } else if (executeDuration != null) {
                    allPreviousDuration = executeDuration;
                } else if (precheckDuration != null) {
                    allPreviousDuration = precheckDuration;
                }
                effectiveDurationsMap.put(stepName, trimLong(cumulativeStepDuration - allPreviousDuration));
                break;
            default:
                LOGGER.warn("stepName {} not found for the NE Job : {}", stepName, neJobId);
                break;
            }
        }
        LOGGER.debug("Effective time durations for each step : {}", effectiveDurationsMap.toString());
        return effectiveDurationsMap;
    }

    private Double trimLong(final double duration) {
        return Double.parseDouble(new DecimalFormat("#0.00").format(duration));
    }

}
