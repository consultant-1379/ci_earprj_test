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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;

import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy.RetryPolicyBuilder;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;

public abstract class AbstractUpgradeActivityTest {

    @Mock
    RetryPolicyBuilder retryPolicyBuilderMock;

    @Mock
    DpsRetryConfigurationParamProvider dpsConfigMock;

    @Mock
    RetryPolicy retryPolicyMock;

    @Mock
    private DpsRetryPolicies dpsRetryPolicies;

    @Mock
    protected RetryManager retryManagerMock;

    @SuppressWarnings("unchecked")
    protected Class<? extends Exception>[] exceptionsArray = new Class[] { IllegalStateException.class, EJBException.class };

    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] exceptionArrayForNodeReadCalls = new Class[] { EJBTransactionRolledbackException.class };

    protected void mockGeneralRetryPolicies() {
        mockRetryPolicy();
        when(retryPolicyBuilderMock.retryOn(exceptionsArray)).thenReturn(retryPolicyBuilderMock);
        when(dpsRetryPolicies.getDpsGeneralRetryPolicy()).thenReturn(retryPolicyMock);
    }

    protected void mockReadAttributeRetryPolicies() {
        mockRetryPolicy();
        when(retryPolicyBuilderMock.retryOn(exceptionArrayForNodeReadCalls)).thenReturn(retryPolicyBuilderMock);
        when(dpsRetryPolicies.getReadAttributesRetryPolicy()).thenReturn(retryPolicyMock);
    }

    protected void mockRetryPolicy() {
        PowerMockito.mockStatic(RetryPolicy.class);
        when(RetryPolicy.builder()).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.attempts(anyInt())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.waitInterval(anyInt(), eq(TimeUnit.SECONDS))).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.exponentialBackoff(anyDouble())).thenReturn(retryPolicyBuilderMock);
        when(retryPolicyBuilderMock.build()).thenReturn(retryPolicyMock);
        when(dpsRetryPolicies.getDpsMoActionRetryPolicy()).thenReturn(retryPolicyMock);
    }
}
