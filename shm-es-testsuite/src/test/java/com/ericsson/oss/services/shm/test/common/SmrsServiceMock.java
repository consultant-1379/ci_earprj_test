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
package com.ericsson.oss.services.shm.test.common;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.nms.security.smrs.api.*;

@Stateless
public class SmrsServiceMock implements SmrsService {

    @Inject
    SmrsAccount smrsAccount;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.nms.security.smrs.api.SmrsService#getCommonAccount(com.ericsson.nms.security.smrs.api.NetworkType, com.ericsson.nms.security.smrs.api.NodeType,
     * com.ericsson.nms.security.smrs.api.CommonAccountType)
     */
    @Override
    public SmrsAccount getCommonAccount(NetworkType networkType, NodeType nodeType, CommonAccountType type) {
        smrsAccount.setHomeDirectory("/C/Work");
        smrsAccount.setUserName("enmuser");
        return smrsAccount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.nms.security.smrs.api.SmrsService#getNodeSpecificAccount(com.ericsson.nms.security.smrs.api.NetworkType, com.ericsson.nms.security.smrs.api.NodeType,
     * com.ericsson.nms.security.smrs.api.NodeSpecificAccountType, java.lang.String)
     */
    @Override
    public SmrsAccount getNodeSpecificAccount(NetworkType networkType, NodeType nodeType, NodeSpecificAccountType type, String nodeName) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.nms.security.smrs.api.SmrsService#getCommonAccountSmrsServiceAllocationAddress(com.ericsson.nms.security.smrs.api.CommonAccountType)
     */
    @Override
    public String getCommonAccountSmrsServiceAllocationAddress(CommonAccountType commonAccountType) {
        // TODO Auto-generated method stub
        return "localhost";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.nms.security.smrs.api.SmrsService#getNodeSpecificAccountSmrsServiceAllocationAddress(com.ericsson.nms.security.smrs.api.NodeSpecificAccountType)
     */
    @Override
    public String getNodeSpecificAccountSmrsServiceAllocationAddress(NodeSpecificAccountType nodeSpecificAccountType) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.nms.security.smrs.api.SmrsService#getSmrsRootDirectoryByNetworkType(com.ericsson.nms.security.smrs.api.NetworkType)
     */
    @Override
    public SmrsDirectory getSmrsRootDirectoryByNetworkType(NetworkType networkType) {
        // TODO Auto-generated method stub
        return null;
    }

}