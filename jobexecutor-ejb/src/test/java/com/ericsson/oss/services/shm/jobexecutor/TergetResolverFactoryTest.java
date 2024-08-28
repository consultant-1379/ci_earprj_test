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
package com.ericsson.oss.services.shm.jobexecutor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.testng.Assert;

import com.ericsson.oss.services.shm.jobexecutorlocal.TargetResolver;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * Test class for TargetResolverFactory
 * 
 * @author xeswpot
 * 
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TergetResolverFactoryTest {

    @InjectMocks
    private TargetResolverFactory targetResolverFactory;

    @Mock
    private NetworkElementResolver networkElementResolverMock;

    @Mock
    private NFVOResolver nfvoResolverMock;

    @Test
    public void testTargetResolverForUpgradeJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.UPGRADE.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForBackupJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.BACKUP.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForBackupHousekeepingJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.BACKUP_HOUSEKEEPING.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForDeleteBackupJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.DELETEBACKUP.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForLicenseJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.LICENSE.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForNodeRestartJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.NODERESTART.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForRestoreJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.RESTORE.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverWhenJobTypeIsSystem() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.SYSTEM.toString());
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverForOnboardJob() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(JobTypeEnum.ONBOARD.toString());
        Assert.assertTrue(targetResolver instanceof NFVOResolver);
    }

    @Test
    public void testTargetResolverWhenJobTypeIsNull() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver(null);
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

    @Test
    public void testTargetResolverWhenJobTypeIsInvalid() {
        final TargetResolver targetResolver = targetResolverFactory.getTargetResolver("SOME_INVALID_JOBTYPE");
        Assert.assertTrue(targetResolver instanceof NetworkElementResolver);
    }

}
