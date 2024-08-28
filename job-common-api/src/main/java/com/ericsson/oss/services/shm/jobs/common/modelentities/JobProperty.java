package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.io.Serializable;

public class JobProperty implements Serializable {

    private static final long serialVersionUID = 1234567L;
    final private String key;
    final private String value;

    public JobProperty(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
