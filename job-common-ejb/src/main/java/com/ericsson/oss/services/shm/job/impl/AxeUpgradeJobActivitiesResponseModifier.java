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
package com.ericsson.oss.services.shm.job.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.filestore.swpackage.api.ActivityDetails;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackage;
import com.ericsson.oss.services.shm.filestore.swpackage.api.SoftwarePackageParameter;
import com.ericsson.oss.services.shm.filestore.swpackage.constants.SoftwarePackageConstants;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.ActivityParams;
import com.ericsson.oss.services.shm.job.activity.ActivitySelection;
import com.ericsson.oss.services.shm.job.activity.InputType;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.job.activity.NodeparamType;
import com.ericsson.oss.services.shm.job.activity.ParamType;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.NeParams;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.constants.JobConfigurationConstants;
import com.ericsson.oss.services.shm.swpackage.query.api.SoftwarePackageQueryService;

@Stateless
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = com.ericsson.oss.services.shm.job.activity.JobType.UPGRADE)
public class AxeUpgradeJobActivitiesResponseModifier implements SHMJobActivitiesResponseModifier {

    private static final Logger logger = LoggerFactory.getLogger(AxeUpgradeJobActivitiesResponseModifier.class);

    private static final String INVALID_INPUT = "No Software package is selected for neType \"%s\".";

    private static final String SWP_DOES_NOT_EXISTS_IN_DB = "Package Data for \"%s\" doesn't exist in the database.";

    private static final String STR_PATTERN = "maxlength=%d";

    private static final String NUM_PATTERN = "min=%d&max=%d";

    private static final String BSC_NETYPE = "BSC";

    @EServiceRef
    private SoftwarePackageQueryService softwarePackageQueryService;

    // Mapping DataType from smoinfo.xml to InputType Enum ,as Json accepts only InputType
    public enum DataTypeToInputType {
        STRING(InputType.TEXT), NUMBER(InputType.TEXT), ENUM(InputType.SINGLE_SELECTION), BOOLEAN(InputType.MULTI_SELECTION);

        private final InputType inputType;

        DataTypeToInputType(final InputType type) {
            this.inputType = type;
        }

        public InputType getInputType() {
            return inputType;
        }

        /**
         * @param type
         * @return
         */
        public static InputType fromParamType(final String type) {
            return DataTypeToInputType.valueOf(type) != null ? DataTypeToInputType.valueOf(type).getInputType() : null;
        }
    }

    /**
     * To Retrieve the activity Data from SoftwarePackage PO ,to the Response if they found.
     * 
     * @param neInfoQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getUpdatedJobActivities(final NeInfoQuery neInfoQuery, final JobActivitiesResponse jobActivitiesResponse) {
        logger.info("Json Modification started in AxeUpgradeJobActivitiesResponseModifier.");
        final List<String> swpackageList = new ArrayList<>();
        final List<NeParams> paramsList = neInfoQuery.getParams();
        final Map<String, String> unsupportedNeTypes = new HashMap<>();
        String swPkgName = "";
        for (final NeParams param : paramsList) {
            if (JobConfigurationConstants.SOFTWAREPKG_NAME.equals(param.getName())) {
                swPkgName = param.getValue();
                if (swPkgName != null && !("".equals(swPkgName))) {
                    swpackageList.add(swPkgName);
                    break;
                }
            }
        }

        // if SWP_NAME is not present in input request then returning response
        if (swpackageList.isEmpty()) {
            unsupportedNeTypes.put(neInfoQuery.getNeType(), String.format(INVALID_INPUT, neInfoQuery.getNeType()));
            jobActivitiesResponse.setUnsupportedNeTypes(unsupportedNeTypes);
            return jobActivitiesResponse;
        }

        final Map<String, List<String>> softwarePackagesMap = new HashMap<>();
        softwarePackagesMap.put(neInfoQuery.getNeType(), swpackageList);
        // Gets the Software Package information form shm-softwarepackagemgmt repo
        final Map<String, SoftwarePackage> softwarePackagesDataMap = softwarePackageQueryService.getSoftwarePackagesBasedOnPackageName(softwarePackagesMap);

        // if SWP is not found in the database then returning response
        if (softwarePackagesDataMap.isEmpty()) {
            unsupportedNeTypes.put(neInfoQuery.getNeType(), String.format(SWP_DOES_NOT_EXISTS_IN_DB, swPkgName));
            jobActivitiesResponse.setUnsupportedNeTypes(unsupportedNeTypes);
            return jobActivitiesResponse;
        }
        logger.debug("SoftwarePackagesDataList size is : {}", softwarePackagesDataMap.size());

        final SoftwarePackage softwarePackage = softwarePackagesDataMap.get(swPkgName);

        final List<NeActivityInformation> acitivityInformationList = new ArrayList<>();

        final NeActivityInformation acitivityInformation = buildNeActivityInformation(softwarePackage, neInfoQuery.getNeType());
        acitivityInformationList.add(acitivityInformation);

        jobActivitiesResponse.setNeActivityInformation(acitivityInformationList);

        logger.debug("jobActivitiesResponse :{}", acitivityInformationList.size());
        return jobActivitiesResponse;
    }

    /**
     * @param softwarePackage
     * @param jobActivitiesResponse
     */
    private NeActivityInformation buildNeActivityInformation(final SoftwarePackage softwarePackage, final String neType) {
        final NeActivityInformation acitivityInformation = new NeActivityInformation();
        acitivityInformation.setNeType(neType);
        final List<Activity> activities = buildActivities(softwarePackage, neType);

        acitivityInformation.getActivity().addAll(activities);

        final List<ActivityParams> acitivityParams = new ArrayList<>();
        final ActivityParams acitivityParam = buildCommonParams(softwarePackage);
        acitivityParams.add(acitivityParam);

        acitivityInformation.getActivityParams().addAll(acitivityParams);

        final List<ActivitySelection> activitySelectionList = new ArrayList<>();
        final ActivitySelection activitySelection = new ActivitySelection();
        activitySelection.setValid(null);
        activitySelection.setInvalid(null);
        activitySelectionList.add(activitySelection);

        acitivityInformation.getActivitySelection().addAll(activitySelectionList);

        return acitivityInformation;
    }

    /**
     * Build NeParams and JobParams
     * 
     * @param softwarePackage
     * @return
     */
    private ActivityParams buildCommonParams(final SoftwarePackage softwarePackage) {
        final ActivityParams neTypeAcitivityParam = new ActivityParams();
        final List<ParamType> jobParams = buildJobParams(softwarePackage);
        neTypeAcitivityParam.getParam().addAll(jobParams);

        final List<NodeparamType> neParams = buildNeParams(softwarePackage);
        neTypeAcitivityParam.getNodeparam().addAll(neParams);

        return neTypeAcitivityParam;
    }

    /**
     * @param softwarePackage
     * @return
     */
    private List<NodeparamType> buildNeParams(final SoftwarePackage softwarePackage) {
        final List<NodeparamType> neParamTypes = new ArrayList<>();
        for (SoftwarePackageParameter neParam : softwarePackage.getNeParameters()) {
            final NodeparamType nodeparamType = new NodeparamType();
            nodeparamType.setName(neParam.getName());
            if (!Boolean.valueOf(neParam.getHidden()) && neParam.getType() != null) {
                nodeparamType.setInputType(DataTypeToInputType.fromParamType(neParam.getType()));
            }else{
                nodeparamType.setInputType(InputType.HIDDEN);
            }
            nodeparamType.setDefaultValue(neParam.getValue());
            nodeparamType.getItem().addAll(neParam.getItems());
            nodeparamType.setPrompt(neParam.getPrompt());
            nodeparamType.setDescription(neParam.getDescription());

            if (InputType.TEXT.equals(nodeparamType.getInputType())) {
                nodeparamType.setPattern(preparePattern(neParam.getMin(), neParam.getMax(), neParam.getType()));
            }
            neParamTypes.add(nodeparamType);
        }
        return neParamTypes;
    }

    /**
     * @param softwarePackage
     * @return
     */
    private List<ParamType> buildJobParams(final SoftwarePackage softwarePackage) {
        final List<ParamType> jobParamTypes = new ArrayList<>();
        for (SoftwarePackageParameter jobParams : softwarePackage.getJobParameters()) {
            final ParamType paramType = new ParamType();
            paramType.setName(jobParams.getName());
            if (!Boolean.valueOf(jobParams.getHidden()) && jobParams.getType() != null) {
                paramType.setInputType(DataTypeToInputType.fromParamType(jobParams.getType()));
            }else{
                paramType.setInputType(InputType.HIDDEN);
            }
            paramType.setDefaultValue(jobParams.getValue());
            paramType.getItem().addAll(jobParams.getItems());
            paramType.setPrompt(jobParams.getPrompt());
            paramType.setDescription(jobParams.getDescription());

            if (InputType.TEXT.equals(paramType.getInputType())) {
                paramType.setPattern(preparePattern(jobParams.getMin(), jobParams.getMax(), jobParams.getType()));
            }
            jobParamTypes.add(paramType);
        }
        return jobParamTypes;
    }

    /**
     * Build Activities from SoftwarePackage data
     * 
     * @param softwarePackage
     * @return
     */
    private List<Activity> buildActivities(final SoftwarePackage softwarePackage, final String neType) {
        final List<Activity> activities = new ArrayList<>();
        for (ActivityDetails dtoActivity : softwarePackage.getActivities()) {
            final Activity activity = new Activity();
            activity.setName(dtoActivity.getName());
            activity.setType(null);
            activity.setOrder(prepareOrder(dtoActivity));
            activity.setDependsOn(null);
            activity.setExclusiveOf(null);
            activity.setMandatory(false);
            activity.setSelected(dtoActivity.getSelected());
            

            final ActivityParams activityParam = new ActivityParams();

            final List<ParamType> paramTypes = new ArrayList<>();
            final List<NodeparamType> nodeparams = new ArrayList<>();

            for (SoftwarePackageParameter softwarePackageParameter : dtoActivity.getActivityParams()) {
                //TODO TORF-297439::Models to fetch list of neTypes for given platform
                if (SoftwarePackageConstants.AXESOFTWAREPACKAGE_ACTIVITY_SYNCHRONUS.equals(softwarePackageParameter.getName()) && BSC_NETYPE.equals(neType)) {
                    continue;
                }
                final ParamType paramType = new ParamType();
                paramType.setName(softwarePackageParameter.getName());
                if (!Boolean.valueOf(softwarePackageParameter.getHidden()) && softwarePackageParameter.getType() != null) {
                    paramType.setInputType(DataTypeToInputType.fromParamType(softwarePackageParameter.getType()));
                }else{
                    logger.info("param with name {} is setting to hidden ",softwarePackageParameter.getName());
                    paramType.setInputType(InputType.HIDDEN);
                }
                paramType.setDefaultValue(softwarePackageParameter.getValue());
                paramType.getItem().addAll(softwarePackageParameter.getItems());
                paramType.setPrompt(softwarePackageParameter.getPrompt());
                paramType.setDescription(softwarePackageParameter.getDescription());

                if (paramType.getInputType().equals(InputType.TEXT)) {
                    paramType.setPattern(preparePattern(softwarePackageParameter.getMin(), softwarePackageParameter.getMax(), softwarePackageParameter.getType()));
                }
                paramTypes.add(paramType);
            }

            final ParamType scriptParamType = new ParamType();
            scriptParamType.setName(SoftwarePackageConstants.AXESOFTWAREPACKAGE_ACTIVITY_SCRIPT);
            scriptParamType.setDefaultValue(dtoActivity.getScriptFileName());
            scriptParamType.setInputType(DataTypeToInputType.ENUM.getInputType());
            paramTypes.add(scriptParamType);

            activityParam.getParam().addAll(paramTypes);
            activityParam.getNodeparam().addAll(nodeparams);

            activity.getActivityParams().add(activityParam);
            activities.add(activity);
        }
        return activities;
    }

    /**
     * @param dtoActivity
     * @return
     */
    private BigInteger prepareOrder(final ActivityDetails dtoActivity) {
        BigInteger order = null;
        for (SoftwarePackageParameter softwarePackageParameter : dtoActivity.getActivityParams()) {
            if (SoftwarePackageConstants.AXESOFTWAREPACKAGE_ACTIVITY_ORDER.equals(softwarePackageParameter.getName())) {
                order = BigInteger.valueOf(Long.parseLong(softwarePackageParameter.getValue()));
                dtoActivity.getActivityParams().remove(softwarePackageParameter);
                break;
            }
        }
        return order;
    }

    /**
     * @param max
     * @param min
     * @param paramType
     * @return
     */
    private String preparePattern(final Integer min, final Integer max, final String paramType) {
        String pattern = "";
        if (DataTypeToInputType.STRING.name().equalsIgnoreCase(paramType)) {
            pattern = String.format(STR_PATTERN, max);
        } else if (DataTypeToInputType.NUMBER.name().equalsIgnoreCase(paramType)) {
            pattern = String.format(NUM_PATTERN, min, max);
        }
        return pattern;
    }

}
