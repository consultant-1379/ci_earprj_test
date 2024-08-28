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
package com.ericsson.oss.services.shm.es.ecim.backup.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.inventory.backup.ecim.api.EcimBackupItem;
import com.ericsson.oss.services.shm.jobs.common.api.DomainTypeProvider;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;

/**
 * To Retrieve the domain type.
 * 
 * @author xnagvar
 * 
 */
@ApplicationScoped
@PlatformAnnotation(name = PlatformTypeEnum.ECIM)
public class EcimDomainTypeProvider implements DomainTypeProvider {

    @Inject
    private EcimBackupUtils ecimBackupUtils;

    /**
     * To Retrieve the Domain and Backup type from Node.
     * 
     */
    @Override
    public Set<String> getDomainTypeList(final NeInfoQuery neInfoQuery) {
        final Map<String, List<EcimBackupItem>> allNodesBackupItems = ecimBackupUtils.getNodeBackupActivityItems(neInfoQuery);
        return ecimBackupUtils.getCommonBackupActivityItems(allNodesBackupItems);
    }

}
