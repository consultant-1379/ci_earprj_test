package com.ericsson.oss.services.shm.es.instrumentation;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

@EService
@Remote
public interface ActivityInstrumentation {

    void preStart(String platformType, String jobType, String name);

    void postFinish(String platformType, String jobType, String name);

}