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
 *----------------------------------------------------------------------------
package com.ericsson.oss.services.shm.test.availability;

import java.util.List;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.security.identitymgmtservices.*;


 * @author xmanush
 
@Stateless
public class IdentityManagementServiceMock implements IdentityManagementService {

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#createM2MUser(java.lang.String, java.lang.String,
     * java.lang.String, int)
     
    @Override
    public M2MUser createM2MUser(final String userName, final String groupName, final String homeDir, final int validDays) throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#deleteM2MUser(java.lang.String)
     
    @Override
    public boolean deleteM2MUser(final String userName) throws IdentityManagementServiceException {
        return false;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#getM2MUser(java.lang.String)
     
    @Override
    public M2MUser getM2MUser(final String userName) throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#isExistingM2MUser(java.lang.String)
     
    @Override
    public boolean isExistingM2MUser(final String userName) throws IdentityManagementServiceException {
        return false;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#getM2MPassword(java.lang.String)
     
    @Override
    public char[] getM2MPassword(final String userName) throws IdentityManagementServiceException {
        final String password = "abcde12345";
        return password.toCharArray();
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#updateM2MPassword(java.lang.String)
     
    @Override
    public char[] updateM2MPassword(final String userName) throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#getAllTargetGroups()
     
    @Override
    public List<String> getAllTargetGroups() throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#getDefaultTargetGroup()
     
    @Override
    public String getDefaultTargetGroup() throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#validateTargetGroups(java.util.List)
     
    @Override
    public List<String> validateTargetGroups(final List<String> targetGroups) throws IdentityManagementServiceException {
        return null;
    }

    
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#createProxyAgentAccount()
     * 
     * @Override public ProxyAgentAccountData createProxyAgentAccount() throws IdentityManagementServiceException { // TODO Auto-generated method stub
     * return null; }
     * 
     * 
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService#deleteProxyAgentAccount(java.lang.String)
     * 
     * @Override public boolean deleteProxyAgentAccount(String arg0) throws IdentityManagementServiceException { // TODO Auto-generated method stub
     * return false; }
     

}*/