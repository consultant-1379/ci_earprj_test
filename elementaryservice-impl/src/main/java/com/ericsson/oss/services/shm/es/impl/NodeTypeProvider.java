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
package com.ericsson.oss.services.shm.es.impl;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.nms.security.smrs.api.NodeType;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsReader;

@Traceable
@Profiled
@Stateless
public class NodeTypeProvider {

    private static final Logger logger = LoggerFactory.getLogger(NodeTypeProvider.class);

    @Inject
    DpsReader dpsReader;

    private static final String NE_TYPE = "neType";
    private static final String MO_FDN_PREFIX = "NetworkElement=";

    public NodeType getNodeType(final String nodeName) {

        NodeType nodeType = null;

        final String nodeTypeValue = getNodeTypeValue(nodeName);
        if (nodeTypeValue == null) {
            throw new IllegalArgumentException("the path on OSS could not be determined.");
        }
        try {
            nodeType = NodeType.getEnumForString(nodeTypeValue);
            logger.debug("NodeType for Node Name {} : {}", nodeName, nodeType);
        } catch (final Exception e) {
            throw new IllegalArgumentException("there is no support in OSS filestore for the given node type " + nodeTypeValue + " .");
        }
        return nodeType;

    }

    private String getNodeTypeValue(final String nodeName) {

        final String moFdn = MO_FDN_PREFIX + nodeName;

        final ManagedObject networkMo = dpsReader.findMoByFdn(moFdn);
        if (networkMo != null) {
            final Map<String, Object> attributesMap = networkMo.getAllAttributes();
            logger.debug("AttributesMap for Network MO {} having FDN {} :  {}", networkMo, moFdn, attributesMap);
            final String nodeTypeValue = (String) attributesMap.get(NE_TYPE);
            return nodeTypeValue;
        }
        return null;
    }
}
