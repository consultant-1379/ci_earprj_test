package com.ericsson.oss.services.shm.backupservice.remote.impl;

import java.util.*;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.AlreadyDefinedException;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DelegateFailureException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

public abstract class AbstractManagedObject implements ManagedObject {

    Map<String, Object> attributes;
    String fdn;

    AbstractManagedObject(final Map<String, Object> attributes, final String fdn) {
        this.attributes = attributes;
        this.fdn = fdn;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public long getPoId() {
        return 0;
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
    public <T> T getAttribute(final String attributeName) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(final Collection<String> attributeNames) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(final String[] attributeNames) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public abstract Map<String, Object> getAllAttributes();

    @Override
    public void setAttribute(final String attributeName, final Object attributeValue) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public void setAttributes(final Map<String, Object> attributes) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public <T extends PersistenceObject> Map<String, Collection<T>> getAssociations(final String... endpointNames) {
        return null;
    }

    @Override
    public <T extends PersistenceObject> Collection<T> getAssociations(final String endpointName) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public void addAssociation(final String endpointName, final PersistenceObject otherPo) throws ModelConstraintViolationException, NotDefinedInModelException, AlreadyDefinedException,
            DelegateFailureException {

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
    public PersistenceObject getEntityAddressInfo() {
        return null;
    }

    @Override
    public void setEntityAddressInfo(final PersistenceObject addressInfo) {

    }

    @Override
    public Object performAction(final String actionName, final Map<String, Object> actionArguments) throws NotDefinedInModelException, ModelConstraintViolationException, DelegateFailureException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public abstract String getFdn();

    @Override
    public short getLevel() {
        return 0;
    }

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
    
    @Override
    public int getChildrenSize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
