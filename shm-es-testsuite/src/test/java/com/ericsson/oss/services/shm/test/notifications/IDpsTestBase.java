package com.ericsson.oss.services.shm.test.notifications;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

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

@Local
public interface IDpsTestBase {
    void initMOdata();

    void delete();

    void moUpdateROAttrs(final String fdn, final Map<String, Object> attributes);

    int performAction(String moFdn, String actionName, Map<String, Object> actionArguments);

    List<ManagedObject> getManagedObjects(String namespace, String type, Map<String, Object> restrictions, String nodeName);

    ManagedObject findMoByFdn(String moFdn);

    String checkUPMoState(int noOfMosToCheck, String stateValue);

    void createUpgradePackageMO(final String moName);

}
