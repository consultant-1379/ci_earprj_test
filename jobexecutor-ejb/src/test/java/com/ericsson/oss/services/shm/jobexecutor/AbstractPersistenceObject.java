/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.AlreadyDefinedException;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DelegateFailureException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;

/**
 * Class for holding all the persistent objects.
 * 
 * @author zkummad
 * 
 */
public abstract class AbstractPersistenceObject implements PersistenceObject {

    protected String namespace;
    protected String type;
    protected String version;
    protected Map<String, Object> attributesMap;

    AbstractPersistenceObject(String namespace, String type, String version, Map<String, Object> attributesMap) {
        this.namespace = namespace;
        this.type = type;
        this.version = version;
        this.attributesMap = attributesMap;
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
    public abstract String getNamespace();

    @Override
    public long getPoId() {
        return 0;
    }

    @Override
    public abstract String getType();

    @Override
    public abstract String getVersion();

    @Override
    public abstract Map<String, Object> getAllAttributes();

    @Override
    public <T> T getAttribute(String arg0) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(Collection<String> arg0) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes(String[] arg0) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public void setAttribute(String arg0, Object arg1) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public void setAttributes(Map<String, Object> arg0) throws NotDefinedInModelException, ModelConstraintViolationException {

    }

    @Override
    public void addAssociation(String arg0, PersistenceObject arg1) throws ModelConstraintViolationException, NotDefinedInModelException, AlreadyDefinedException, DelegateFailureException {

    }

    @Override
    public <T extends PersistenceObject> Map<String, Collection<T>> getAssociations(final String... endpointNames) {
        return null;
    }

    @Override
    public <T extends PersistenceObject> Collection<T> getAssociations(String arg0) throws NotDefinedInModelException {
        return null;
    }

    @Override
    public boolean removeAllAssociations(String arg0) throws NotDefinedInModelException {
        return false;

    }

    @Override
    public boolean removeAssociation(String arg0, PersistenceObject arg1) throws ModelConstraintViolationException, NotDefinedInModelException {
        return false;

    }

    @Override
    public PersistenceObject getEntityAddressInfo() {
        return null;
    }

    @Override
    public Object performAction(String arg0, Map<String, Object> arg1) throws NotDefinedInModelException, ModelConstraintViolationException, DelegateFailureException {
        return null;
    }

    @Override
    public void setEntityAddressInfo(PersistenceObject arg0) {

    }

}