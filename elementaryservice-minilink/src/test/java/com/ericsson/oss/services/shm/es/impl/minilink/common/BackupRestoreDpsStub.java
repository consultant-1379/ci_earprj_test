package com.ericsson.oss.services.shm.es.impl.minilink.common;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeContainmentRestrictionBuilder;
import com.ericsson.oss.services.shm.minilinkindoor.common.ManagedObjectUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants.XF_CONFIG_LOAD_OBJECTS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackupRestoreDpsStub {

    private final Map<Long, ManagedObject> poidToManagedObjects = new HashMap<>();
    private final Map<String, ManagedObject> moTypeToManagedObjects = new HashMap<>();
    private final Map<String, ManagedObject> fdnToManagedObjects = new HashMap<>();
    private final Map<ManagedObject, Map<String, Object>> moAttributes = new HashMap<>();
    private long poidCount = 1;
    private boolean makeDpsFail = false;

    public void reset() {
        poidToManagedObjects.clear();
        moTypeToManagedObjects.clear();
        fdnToManagedObjects.clear();
        moAttributes.clear();
        poidCount = 1;
    }

    public void setDpsFail(boolean makeDpsFail) {
        this.makeDpsFail = makeDpsFail;
    }

    public ManagedObject createManagedObjectMock(final String moType) {
        final ManagedObject mo = mock(ManagedObject.class);

        final long poid = poidCount++;
        final String fdn = moType;

        when(mo.getPoId()).thenReturn(poid);
        when(mo.getFdn()).thenReturn(fdn);

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String attrName = (String) invocationOnMock.getArguments()[0];
                return moAttributes.get(mo).get(attrName);
            }
        }).when(mo).getAttribute(anyString());

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String attrName = (String) invocationOnMock.getArguments()[0];
                Object attrValue = invocationOnMock.getArguments()[1];
                if (!moAttributes.containsKey(mo)) {
                    moAttributes.put(mo, new HashMap<String, Object>());
                }
                moAttributes.get(mo).put(attrName, attrValue);
                return null;
            }
        }).when(mo).setAttribute(anyString(), anyString());

        poidToManagedObjects.put(poid, mo);
        fdnToManagedObjects.put(fdn, mo);
        moTypeToManagedObjects.put(moType, mo);

        return mo;
    }

    public ManagedObject getMoBasedOnMoType(String moType) {
        return moTypeToManagedObjects.get(moType);
    }

    public void makeDpsMock(DataPersistenceService dataPersistenceService) {
        DataBucket liveBucket = mock(DataBucket.class);
        when(dataPersistenceService.getDataBucket(anyString(), anyString(), anyString())).thenReturn(liveBucket);

        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);

        Query<TypeContainmentRestrictionBuilder> typeQuery = mock(Query.class);
        when(queryBuilder.createTypeQuery(anyString(), anyString(), anyString())).thenReturn(typeQuery);

        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);

        List<Object> queryResult = Arrays.<Object>asList(moTypeToManagedObjects.get(XF_CONFIG_LOAD_OBJECTS));
        when(queryExecutor.execute(eq(typeQuery))).thenReturn(queryResult.iterator());
    }

    public void mockManagedObjectUtil(ManagedObjectUtil managedObjectUtil, String nodeName) {
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (makeDpsFail) throw new RuntimeException();
                String moType = (String) invocationOnMock.getArguments()[1];
                return getMoBasedOnMoType(moType);
            }
        }).when(managedObjectUtil).getManagedObject(eq(nodeName), anyString());

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (makeDpsFail) throw new RuntimeException();
                String moType = (String) invocationOnMock.getArguments()[1];
                return getMoBasedOnMoType(moType);
            }
        }).when(managedObjectUtil).getManagedObjectUnderNetworkElement(eq(nodeName), anyString(), anyString());
    }

}
