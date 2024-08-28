/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobservice.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

public abstract class AbstractPersistenceObject implements PersistenceObject {

    Map<String, Object> attributesMap;

    AbstractPersistenceObject(final Map<String, Object> attributesMap) {
        this.attributesMap = attributesMap;
    }

    @Override
    public Map<String, Object> getAttributes(final Collection<String> arg0) {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(final String[] arg0) {
        return null;
    }

    @Override
    public void setAttribute(final String arg0, final Object arg1) {

    }

    @Override
    public void setAttributes(final Map<String, Object> arg0) {

    }

    @Override
    public void addAssociation(final String arg0, final PersistenceObject arg1) {

    }

    @Override
    public <T extends PersistenceObject> Map<String, Collection<T>> getAssociations(final String... endpointNames) {
        return null;
    }

    @Override
    public <T extends PersistenceObject> Collection<T> getAssociations(final String arg0) {
        return Collections.emptyList();
    }

    @Override
    public boolean removeAllAssociations(final String arg0) {
        return false;
    }

    @Override
    public boolean removeAssociation(final String arg0, final PersistenceObject arg1) {
        return false;
    }

    @Override
    public PersistenceObject getEntityAddressInfo() {
        return null;
    }

    @Override
    public Object performAction(final String arg0, final Map<String, Object> arg1) {
        return null;
    }

    @Override
    public void setEntityAddressInfo(final PersistenceObject arg0) {

    }

    @Override
    public Date getCreatedTime() {
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public long getPoId() {
        return 0;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public int getAssociatedObjectCount(final String endpointName) {
        return 0;
    }

    @Override
    public Map<String, Object> readAttributesFromDelegate(final String... attrsNames) {
        return null;
    }
}
