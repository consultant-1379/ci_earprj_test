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
package com.ericsson.oss.services.shm.jobexecutor.ecim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.activities.JobExecutionValidator;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroup;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementGroupPreparator;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.instantaneouslicensing.InstantaneousLicensingNesValidateService;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;

/**
 * This class implements the validations on ECIM network elements depending on the job type.
 * 
 * @author xyerrsr
 * 
 */
@PlatformAnnotation(name = PlatformTypeEnum.ECIM)
public class EcimJobExecutionValidatorImpl implements JobExecutionValidator {

    @Inject
    private NetworkElementGroupPreparator neGroupPreparator;

    @Inject
    private InstantaneousLicensingNesValidateService instantaniousLicensingNesValidateService;

    private final static Logger LOGGER = LoggerFactory.getLogger(EcimJobExecutionValidatorImpl.class);

    private static final Map<JobTypeEnum, FragmentType> JOBTYPE_FRAGMENTTYPE_MAP = new HashMap<JobTypeEnum, FragmentType>();

    static {
        JOBTYPE_FRAGMENTTYPE_MAP.put(JobTypeEnum.BACKUP, FragmentType.ECIM_BRM_TYPE);
        JOBTYPE_FRAGMENTTYPE_MAP.put(JobTypeEnum.UPGRADE, FragmentType.ECIM_SWM_TYPE);
        JOBTYPE_FRAGMENTTYPE_MAP.put(JobTypeEnum.LICENSE, FragmentType.ECIM_LM_TYPE);
        JOBTYPE_FRAGMENTTYPE_MAP.put(JobTypeEnum.NODE_HEALTH_CHECK, FragmentType.ECIM_HCM_TYPE);
        JOBTYPE_FRAGMENTTYPE_MAP.put(JobTypeEnum.LICENSE_REFRESH, FragmentType.ECIM_LM_TYPE);
    }

    /**
     * Method to validate whether the selected ECIM network element supports the fragment corresponding to the job type.
     * 
     * @param mainJobId
     * @param jobType
     * @param selectedEcimNEs
     * 
     * @return ecimNEListNotSupportingUpgrade
     */
    @Override
    public Map<NetworkElement, String> findUnSupportedNEs(final JobTypeEnum jobType, final List<NetworkElement> selectedEcimNEs) {
        final Map<NetworkElement, String> msgnetworkElementMap = new HashMap<NetworkElement, String>();
        if (JobTypeEnum.NODERESTART.toString().equals(jobType.name())) {
            LOGGER.info("Selected Ecim Nes ", selectedEcimNEs);
            for (final NetworkElement networkElement : selectedEcimNEs) {
                msgnetworkElementMap.put(networkElement, "Skipped Node restart for " + networkElement.getNeType() + " as not supported.");
            }
            return msgnetworkElementMap;
        } else {
            final FragmentType fragmentTypeEnum = EcimJobExecutionValidatorImpl.JOBTYPE_FRAGMENTTYPE_MAP.get(jobType);
            if (fragmentTypeEnum == null) {
                return null;
            } else {
                //Group the selected Network Elements based on their support for the respective fragment
                final NetworkElementGroup ecimNEGroupsByFragmentSupport = neGroupPreparator.groupNetworkElementsByModelidentity(selectedEcimNEs, fragmentTypeEnum.getFragmentName());

                //Fetch the set of NE names not supporting the respective fragment.
                final Map<String, String> ecimNENamesNoFragmentSupport = ecimNEGroupsByFragmentSupport.getUnSupportedNetworkElements();
                LOGGER.debug("NEs not supporting {} fragment are : {}", fragmentTypeEnum.getFragmentName(), ecimNENamesNoFragmentSupport.size());

                //Prepare a map of all the selected ECIM NEs, with key as node name.
                final Map<String, NetworkElement> networkElementMap = new HashMap<String, NetworkElement>();
                for (final NetworkElement ecimNE : selectedEcimNEs) {
                    networkElementMap.put(ecimNE.getName(), ecimNE);
                }
                prepareNetworkElementMap(msgnetworkElementMap, ecimNENamesNoFragmentSupport, networkElementMap);
                if (JobTypeEnum.LICENSE_REFRESH.toString().equals(jobType.name())) {
                    selectedEcimNEs.removeAll(msgnetworkElementMap.keySet());
                    if (!selectedEcimNEs.isEmpty()) {
                        final Map<NetworkElement, String> unSupportedNes = instantaniousLicensingNesValidateService.filterInstantaneousLicensingSupportedNes(selectedEcimNEs);
                        unSupportedNes.putAll(msgnetworkElementMap);
                        return unSupportedNes;
                    } else {
                        return msgnetworkElementMap;
                    }
                }
                return msgnetworkElementMap;
            }
        }
    }

    private void prepareNetworkElementMap(final Map<NetworkElement, String> msgnetworkElementMap, final Map<String, String> ecimNENamesNoFragmentSupport,
            final Map<String, NetworkElement> networkElementMap) {
        for (Entry<String, String> ecimNodeNames : ecimNENamesNoFragmentSupport.entrySet()) {
            if (networkElementMap.containsKey(ecimNodeNames.getKey())) {
                msgnetworkElementMap.put(networkElementMap.get(ecimNodeNames.getKey()), ecimNodeNames.getValue());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.activities.JobExecutionValidator#findNesWithComponents(java.util.List, java.util.List, java.util.Map,
     * com.ericsson.oss.services.shm.networkelement.NetworkElementResponse)
     */
    @Override
    public Map<String, List<NetworkElement>> findNesWithComponents(final JobTypeEnum jobTypeEnum, final List<NetworkElement> platformSpecificNEList,
            final List<Map<String, Object>> nesWithComponentInfo, final Map<String, String> neDetailsWithParentName, final NetworkElementResponse networkElementResponse,
            final boolean flagForValidateNes) {
        return null;
    }

}
