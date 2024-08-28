package com.ericsson.oss.services.shm.es.api;

import java.io.Serializable;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

public class JobActivityInfo implements Serializable {

    private static final long serialVersionUID = 5206527454047944721L;

    private final String activityName;

    private final JobTypeEnum jobType;

    private final PlatformTypeEnum platform;

    private final long activityJobId;

    public JobActivityInfo(final long activityJobId, final String activityName, final JobTypeEnum jobType, final PlatformTypeEnum platform) {
        super();
        this.activityJobId = activityJobId;
        this.activityName = activityName;
        this.jobType = jobType;
        this.platform = platform;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the activityName
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * @return the jobType
     */
    public JobTypeEnum getJobType() {
        return jobType;
    }

    /**
     * @return the platform
     */
    public PlatformTypeEnum getPlatform() {
        return platform;
    }

    /**
     * @return the activityJobId
     */
    public long getActivityJobId() {
        return activityJobId;
    }
}
