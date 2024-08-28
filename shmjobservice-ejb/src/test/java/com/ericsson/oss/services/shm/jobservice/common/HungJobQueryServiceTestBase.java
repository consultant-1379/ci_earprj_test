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
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

public abstract class HungJobQueryServiceTestBase {

    private HungJobQueryServiceTestBase() {
    }

    public static PersistenceObject buildNeJobPo(PlatformTypeEnum platformTypeEnum) {
        return new AbstractPersistenceObject(createNeJobPo(platformTypeEnum)) {
            @Override
            public Map<String, Object> getAllAttributes() {
                return attributesMap;
            }

            @Override
            public PersistenceObject getTarget() {
                return null;
            }

            @Override
            public void setTarget(final PersistenceObject target) {
                //NA for NeJob PO
            }

            @Override
            public <T> T getAttribute(final String arg0) {
                return null;
            }
        };

    }

    /**
     * @param platformTypeEnum
     * @return
     */
    private static Map<String, Object> createNeJobPo(PlatformTypeEnum platformTypeEnum) {
        final Map<String, Object> neJobPoMap = new HashMap<>();
        neJobPoMap.put(ShmConstants.STATE, "RUNNING");
        neJobPoMap.put(ShmConstants.WFS_ID, "123");
        if (platformTypeEnum.equals(PlatformTypeEnum.AXE)) {
            neJobPoMap.put(ShmConstants.NE_NAME, "node1");
            neJobPoMap.put(ShmConstants.STARTTIME, new DateTime().minusHours(12).toDate());
            neJobPoMap.put(ShmConstants.JOBPROPERTIES, buildJobProperties());
        } else {
            neJobPoMap.put(ShmConstants.NE_NAME, "node2");
            neJobPoMap.put(ShmConstants.STARTTIME, new DateTime().minusHours(3).toDate());
        }
        return neJobPoMap;
    }

    /**
     * @return
     */
    private static Object buildJobProperties() {
        final List<Map<String, Object>> jobProperties = new ArrayList<>();

        final Map<String, Object> parentNamejobProperties = new HashMap<>();
        parentNamejobProperties.put(ShmConstants.KEY, ShmConstants.PARENT_NAME);
        parentNamejobProperties.put(ShmConstants.VALUE, "node1");
        jobProperties.add(parentNamejobProperties);

        final Map<String, Object> isComponentJob = new HashMap<>();
        isComponentJob.put(ShmConstants.KEY, ShmConstants.IS_COMPONENT_JOB);
        isComponentJob.put(ShmConstants.VALUE, "true");
        jobProperties.add(isComponentJob);

        return jobProperties;
    }

}
