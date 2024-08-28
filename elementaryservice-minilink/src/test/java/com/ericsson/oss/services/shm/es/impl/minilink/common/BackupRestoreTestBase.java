package com.ericsson.oss.services.shm.es.impl.minilink.common;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.*;
import static com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants.ACTIVITY_RESULT;
import static com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants.BACKUP_ACCOUNT;
import static com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum.AVC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cds.cdi.support.configuration.InjectionProperties;
import com.ericsson.cds.cdi.support.rule.CdiInjectorRule;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsDataChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl;
import com.ericsson.oss.services.shm.common.DpsWriter;
import com.ericsson.oss.services.shm.common.FdnUtils;
import com.ericsson.oss.services.shm.common.ResourceOperations;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.smrs.SmrsAccountInfo;
import com.ericsson.oss.services.shm.common.smrs.SmrsFileStoreService;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.cache.JobStaticData;
import com.ericsson.oss.services.shm.job.cache.JobStaticDataProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.SmrsServiceConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobType;
import com.ericsson.oss.services.shm.minilinkindoor.common.FtpUtil;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;
import com.ericsson.oss.services.shm.model.NotificationSubject;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.notifications.api.Notification;
import com.ericsson.oss.services.shm.notifications.api.NotificationEventTypeEnum;
import com.ericsson.oss.services.shm.tbac.ActivityJobTBACValidator;

public abstract class BackupRestoreTestBase {

    public static final long ACTIVITY_JOB_ID = 123;
    public static final String NODE_NAME = "ML-TN";
    public static final int FTP_TABLE_ENTRY_INDEX = 15;
    public static final int FTP_ENTRY_INDEX = 1;
    public static final String PATH_ON_SERVER = "/home/smrs/MINI-LINK/MINI-LINK-Indoor/tn_backup_configuration/";
    public static final String BACKUP_NAME_TEST = "backup";
    public static final String EXPECTED_BACKUP_FILE_NAME = BACKUP_NAME_TEST + DOT + CONFIG_FILE_EXTENSION;
    public static final String BACKUP_FILE_PATH_PREFIX = PATH_ON_SERVER + NODE_NAME + SLASH;
    protected static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreTestBase.class);
    public static final String IP_ADDRESS = "10.20.100.10";
    protected final InjectionProperties injectionProperties = new InjectionProperties().autoLocateFrom("com.ericsson.oss.services.shm.es.impl.minilink")
            .autoLocateFrom("com.ericsson.oss.services.shm.es.api");

    @Rule
    public CdiInjectorRule cdiInjectorRule = new CdiInjectorRule(this, injectionProperties);

    @ImplementationInstance
    protected ActivityUtils activityUtils = new ActivityUtilsStub(ACTIVITY_JOB_ID, NODE_NAME, BACKUP_NAME_TEST);

    @MockedImplementation
    protected ManagedObjectUtil managedObjectUtil;

    @ImplementationInstance
    protected RetryManager retryManager = new RetryManagerNonCDIImpl();

    @MockedImplementation
    protected FtpUtil ftpUtil;

    @MockedImplementation
    protected UnSecureFtpUtil unSecureFtpUtil;

    @MockedImplementation
    protected DpsWriter dpsWriter;

    @MockedImplementation
    protected SmrsFileStoreService smrsFileStoreService;

    @MockedImplementation
    protected ResourceOperations resourceOperations;

    @MockedImplementation
    protected DataPersistenceService dataPersistenceService;

    @MockedImplementation
    protected FdnUtils fdnUtils;

    @MockedImplementation
    protected JobUpdateService jobUpdateService;

    @MockedImplementation
    private DpsDataChangedEvent dpsDataChangedEvent;

    protected BackupRestoreDpsStub dpsStub = new BackupRestoreDpsStub();
    protected JobUpdateServiceStub jobUpdateServiceStub = new JobUpdateServiceStub();
    protected MiniLinkDps mniiLinkDpsInfo = new MiniLinkDps();
    protected Set<String> existingResources = new HashSet<>();
    protected boolean makeSmrsFail = false;

    protected final ManagedObject xfConfigLoadObjectsMo = dpsStub.createManagedObjectMock(XF_CONFIG_LOAD_OBJECTS);
    protected final ManagedObject xfDcnFtpMo = dpsStub.createManagedObjectMock(XF_DCN_FTP);

    protected Notification getAvcNotificationMessage() {
        return new Notification() {
            @Override
            public DpsDataChangedEvent getDpsDataChangedEvent() {
                return dpsDataChangedEvent;
            }

            @Override
            public NotificationSubject getNotificationSubject() {
                return null;
            }

            @Override
            public NotificationEventTypeEnum getNotificationEventType() {
                return AVC;
            }
        };
    }

    protected void assertUnsubscribedFromXfConfigLoadObjects() {
        assertFalse(((ActivityUtilsStub) activityUtils).subscribedToFdn(xfConfigLoadObjectsMo.getFdn()));
    }

    protected void assertActivitySuccess() {
        assertTrue(jobUpdateServiceStub.getJobProperties().containsKey(ACTIVITY_RESULT));
        assertTrue(jobUpdateServiceStub.getJobProperties().get(ACTIVITY_RESULT).equals(JobResult.SUCCESS.getJobResult()));
        assertUnsubscribedFromXfConfigLoadObjects();
    }

    protected void assertActivityFailure() {
        assertTrue(jobUpdateServiceStub.getJobProperties().containsKey(ACTIVITY_RESULT));
        assertTrue(jobUpdateServiceStub.getJobProperties().get(ACTIVITY_RESULT).equals(JobResult.FAILED.getJobResult()));
        assertUnsubscribedFromXfConfigLoadObjects();
    }

    protected void assertSubscribedToXfConfigLoadObjects() {
        assertTrue(((ActivityUtilsStub) activityUtils).subscribedToFdn(xfConfigLoadObjectsMo.getFdn()));
    }

    protected void mockFtpUtil() {
        when(ftpUtil.setupFtp(eq(NODE_NAME), eq(BACKUP_ACCOUNT))).thenReturn(FTP_TABLE_ENTRY_INDEX);
    }

    protected void mockSmrsFileStroreService() {
        final SmrsAccountInfo smrsAccountInfo = mock(SmrsAccountInfo.class);
        when(smrsAccountInfo.getPathOnServer()).thenReturn(PATH_ON_SERVER);
        when(smrsFileStoreService.getSmrsDetails(eq(SmrsServiceConstants.BACKUP_ACCOUNT), eq(MINI_LINK_INDOOR), eq(NODE_NAME))).thenReturn(smrsAccountInfo);
    }

    protected void mockResourceOperations() {
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                if (makeSmrsFail)
                    throw new RuntimeException();
                final Object[] invocationArgs = invocationOnMock.getArguments();
                final String pathOnServer = (String) invocationArgs[0];
                final String nodeName = (String) invocationArgs[1];
                return existingResources.contains(pathOnServer + nodeName);
            }
        }).when(resourceOperations).isDirectoryExistsWithWritePermissions(anyString(), anyString());

        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                if (makeSmrsFail)
                    throw new RuntimeException();
                final Object[] invocationArgs = invocationOnMock.getArguments();
                final String pathOnServer = (String) invocationArgs[0];
                final String nodeName = (String) invocationArgs[1];
                existingResources.add(pathOnServer + nodeName);
                return null;
            }
        }).when(resourceOperations).createDirectory(anyString(), anyString());

        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                if (makeSmrsFail)
                    throw new RuntimeException();
                final String file = (String) invocationOnMock.getArguments()[0];
                return existingResources.contains(file);
            }
        }).when(resourceOperations).fileExists(anyString());
    }

    protected void assertLog(final String logMessage) {
        for (final Map<String, Object> log : jobUpdateServiceStub.getJobLog()) {
            if (((String) log.get("message")).contains(logMessage)) {
                assertTrue(true);
                return;
            }
        }
        printJobLog();
        assertTrue("Missing from joblog: " + logMessage, false);
    }

    protected void printJobLog() {
        LOGGER.info("<--- JobLog ---> ");
        for (final Map<String, Object> log : jobUpdateServiceStub.getJobLog()) {
            LOGGER.info(log.get("message").toString());
        }
        LOGGER.info("<--- JobLog END ---> ");
    }

    @Before
    public void initMocks() {
        dpsStub.makeDpsMock(dataPersistenceService);
        dpsStub.mockManagedObjectUtil(managedObjectUtil, NODE_NAME);
        mockFtpUtil();
        mockSmrsFileStroreService();
        mockResourceOperations();
        jobUpdateServiceStub.mockJobUpdateService(jobUpdateService);
        when(dpsDataChangedEvent.toString()).thenReturn("Test dpsDataChangedEvent.");
    }

    @ImplementationInstance
    private final NeJobStaticDataProvider neJobStaticDataProvider = new NeJobStaticDataProvider() {

        @Override
        public NEJobStaticData getNeJobStaticData(final long activityJobId, final String capability) {
            return new NEJobStaticData(123L, 345L, "testFDN", "businessKey", PlatformTypeEnum.MINI_LINK_INDOOR.getName(), new Date().getTime(), null);
        }

        @Override
        public void updateNeJobStaticDataCache(final long activityJobId, final String platformCapbility, final long activityStartTime) throws JobDataNotFoundException {
        }

        @Override
        public void clear(final long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(final long activityJobId, final NEJobStaticData neJobStaticData) {
        }

        @Override
        public long getActivityStartTime(final long activityJobId) {
            return 0;
        }
    };

    @ImplementationInstance
    private final JobStaticDataProvider jobStaticDataProvider = new JobStaticDataProvider() {

        @Override
        public JobStaticData getJobStaticData(final long mainJobId) {
            final JobStaticData jobStaticData = new JobStaticData("", new HashMap<String, Object>(), "", JobType.RESTORE,"");
            return jobStaticData;
        }

        @Override
        public void clear(final long activityJobId) {
        }

        @Override
        public void clearAll() {
        }

        @Override
        public void put(final long mainJobId, final JobStaticData jobStaticData) {
        }
    };

    @ImplementationInstance
    private final ActivityJobTBACValidator activityJobTBACValidator = new ActivityJobTBACValidator() {

        @Override
        public boolean validateTBAC(final long activityJobId, final NEJobStaticData neJobStaticData, final JobStaticData jobStaticData, final String activityName) {
            return true;
        }
    };
}
