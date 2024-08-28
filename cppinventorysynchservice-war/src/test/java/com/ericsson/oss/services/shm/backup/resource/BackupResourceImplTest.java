///*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2012
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/
//package com.ericsson.oss.services.shm.backup.resource;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.ws.rs.core.Response;
//
//import org.junit.*;
//import org.mockito.*;
//
//import com.ericsson.oss.services.shm.backup.entities.*;
//import com.ericsson.oss.services.shm.backup.service.BackupService;
//
//public class BackupResourceImplTest {
//
//	@Mock
//	BackupService backupServiceMock;
//
//	@Mock
//	BackupResponse backupResponse;
//
//	@InjectMocks
//	BackupResourceImpl backupResource;
//
//	@Before
//	public void setUp() throws Exception {
//		MockitoAnnotations.initMocks(this);
//	}
//
//	@Test
//	@Ignore
//	public void testGetConfigurationVersions() {
//
//		final BackupRequest backupRequest = getBackupInput();
//		Mockito.when(backupServiceMock.getConfigurationVersions(backupRequest))
//				.thenReturn(backupResponse);
//		final Response actualResponse = backupResource
//				.getConfigurationVersions(backupRequest);
//
//		assertEquals(200, actualResponse.getStatus());
//
//	}
//
//	private BackupRequest getBackupInput() {
//		final List<String> fdns = new ArrayList<String>();
//		fdns.add("fdn1");
//		fdns.add("fdn2");
//
//		final BackupEnum backupEnum1 = BackupEnum.STARTABLE_CV;
//		final BackupEnum backupEnum2 = BackupEnum.STARTABLE_CV;
//		final List<BackupEnum> columns = new ArrayList<BackupEnum>();
//		columns.add(backupEnum1);
//		columns.add(backupEnum2);
//
//		final BackupRequest backupRequest = new BackupRequest();
//		backupRequest.setFdns(fdns);
//		backupRequest.setColumns(columns);
//		backupRequest.setOffset(0);
//		backupRequest.setLimit(0);
//		final BackupEnum sortBy = BackupEnum.STARTABLE_CV;
//		backupRequest.setSortBy(sortBy);
//		backupRequest.setOrderBy("dummy");
//		Mockito.when(backupServiceMock.getConfigurationVersions(backupRequest))
//				.thenReturn(backupResponse);
//		final Response actualResponse = backupResource
//				.getConfigurationVersions(backupRequest);
//
//		assertEquals(200, actualResponse.getStatus());
//
//		return backupRequest;
//	}
//}
