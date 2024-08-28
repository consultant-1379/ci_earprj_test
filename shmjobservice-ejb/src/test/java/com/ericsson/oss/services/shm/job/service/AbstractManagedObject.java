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
package com.ericsson.oss.services.shm.job.service;

import java.util.*;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.AlreadyDefinedException;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DelegateFailureException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

public abstract class AbstractManagedObject implements ManagedObject {
    String fdn;
    Map<String, Object> attributesMap;
    Collection<ManagedObject> associations;

    AbstractManagedObject(final String fdn, final Map<String, Object> attributesMap, final Collection<ManagedObject> associations) {
        this.fdn = fdn;
        this.attributesMap = attributesMap;
        this.associations = associations;
    }

    @Override
    public void setEntityAddressInfo(final PersistenceObject addressInfo) {

    }

    @Override
    public Object performAction(final String actionName, final Map<String, Object> actionArguments) throws NotDefinedInModelException, ModelConstraintViolationException, DelegateFailureException {
        return null;
    }

    @Override
    public PersistenceObject getEntityAddressInfo() {
        return null;
    }

    @Override
    public boolean removeAssociation(final String endpointName, final PersistenceObject otherPo) throws ModelConstraintViolationException, NotDefinedInModelException {
        return false;
    }

    @Override
    public boolean removeAllAssociations(final String endpointName) throws NotDefinedInModelException {
        return false;
    }

    @Override
    public <T extends PersistenceObject> Collection<T> getAssociations(final String endpointName) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public <T extends PersistenceObject> Map<String, Collection<T>> getAssociations(final String... endpointNames) {
        return null;
    }

    @Override
    public void addAssociation(final String endpointName, final PersistenceObject otherPo) throws ModelConstraintViolationException, NotDefinedInModelException, AlreadyDefinedException,
            DelegateFailureException {

    }

    @Override
    public void setAttributes(final Map<String, Object> attributes) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public void setAttribute(final String attributeName, final Object attributeValue) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public Map<String, Object> getAttributes(final String[] attributeNames) throws NotDefinedInModelException{
    	return null;
    }

    @Override
    public Map<String, Object> getAttributes(final Collection<String> attributeNames) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public <T> T getAttribute(final String attributeName) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Map<String, Object> getAllAttributes() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public long getPoId() {
        return 0;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public Date getLastUpdatedTime() {
        return null;
    }

    @Override
    public Date getCreatedTime() {
        return null;
    }

    @Override
    public boolean isMibRoot() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public short getLevel() {
        return 0;
    }

    @Override
    public abstract String getFdn();

    @Override
    public ManagedObject getParent() {
        return null;
    }

    @Override
    public Collection<ManagedObject> getChildren() {
        return null;
    }

    @Override
    public ManagedObject getChild(final String childRdn) {
        return null;
    }

}
