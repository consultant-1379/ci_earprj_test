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
package com.ericsson.oss.services.shm.accesscontrol;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.mockito.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.Mock;

import static org.junit.Assert.*;

import com.ericsson.oss.services.shm.job.rbac.AccessCheck;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccessControlImplTest {

	@InjectMocks
	AccessControlImpl accessControlImpl;

	@Mock
	ResponseBuilder responseBuilder;

	@Mock
	AccessCheck accessCheck;
  
	@Test
	public void test_checkAccessForCreateJob() {
		when(accessCheck.createJob()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForCreateJob();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForDeletJob() {
		when(accessCheck.deleteJob()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForDeletJob();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForViewInventory() {
		when(accessCheck.viewInventory()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForViewInventory();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForViewJobs() {
		when(accessCheck.viewJobs()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForViewJobs();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForViewSWPackages() {
		when(accessCheck.viewSWPackages()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForViewSWPackages();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForViewJobLogs() {
		when(accessCheck.viewJobLogs()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForViewJobLogs();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForExportJobLogs() {
		when(accessCheck.exportJobLogs()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForExportJobLogs();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForImportFile() {
		when(accessCheck.importFile()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForImportFile();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForDeleteFile() {
		when(accessCheck.deleteFile()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder); 
		Response response = accessControlImpl.checkAccessForDeleteFile();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	@Test
	public void test_checkAccessForControlJob() {
		when(accessCheck.controlJob()).thenReturn("grant");
		when(responseBuilder.entity(Matchers.any()))
				.thenReturn(responseBuilder);
		Response response = accessControlImpl.checkAccessForControlJob();		
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}
	
	
	
	

}
