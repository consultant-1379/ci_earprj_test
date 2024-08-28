package com.ericsson.oss.services.shm.job.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsIllegalStateException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;

@RunWith(MockitoJUnitRunner.class)
public class PoAttributesHolderTest {

    @InjectMocks
    PoAttributesHolder poAttributesHolderMock;

    @Mock
    DataPersistenceService dataPersistenceService;

    @Mock
    QueryExecutor queryExecutor;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    PersistenceObject poMock;

    @Mock
    Map<String, Object> attributesMapMock;

    @Mock
    Iterator<Object> iteratorMock;

    @Mock
    Map<String, Object> mapMock;

    @Mock
    Map<Long, Map<String, Object>> mapMock1;

    @Mock
    Restriction restrictionMock;

    @Mock
    DataBucket dataBucket;

    @Mock
    private List<Map<Long, Map<String, Object>>> resultMock;

    List<PersistenceObject> persistenceObjects = null;
    private static final String NAMESPACE = "CPP_NRM_OSS_DEF";
    private static final String TYPE = "HardwareItem";
    private List<Long> po_IDs = null;
    private static final int MAX_PERSISTENCE_OBJECTS = 5;
    private static final String VERSION = "2.1.0";

    @Before
    public void setup() {
        createPOIds();
        createPersistenceObjects();
    }

    private void createPOIds() {
        po_IDs = new ArrayList<Long>();
        po_IDs.add(12345L);
        po_IDs.add(23456L);
        po_IDs.add(34567L);
        po_IDs.add(45678L);
        po_IDs.add(56789L);
    }

    private void createPersistenceObjects() {
        persistenceObjects = new ArrayList<PersistenceObject>();
        for (int index = 1; index < MAX_PERSISTENCE_OBJECTS + 1; index++) {
            final PersistenceObject persistenceObject = new AbstractPersistenceObject(NAMESPACE, TYPE + "-" + index, VERSION, new HashMap<String, Object>()) {

                @Override
                public String getVersion() {
                    return version;
                }

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public String getNamespace() {
                    return namespace;
                }

                @Override
                public Map<String, Object> getAllAttributes() {
                    return attributesMap;
                }

                @Override
                public PersistenceObject getTarget() {
                    return null;
                }

                @Override
                public void setTarget(final PersistenceObject arg0) {

                }

                @Override
                public int getAssociatedObjectCount(final String arg0) throws NotDefinedInModelException {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public Map<String, Object> readAttributesFromDelegate(final String... arg0) throws DpsIllegalStateException {
                    // TODO Auto-generated method stub
                    return null;
                }
            };
            persistenceObjects.add(persistenceObject);
        }
    }

    @Test
    public void findPOsTest() {
        final long poId = 12345l;
        final String nodeName = "neName";
        final Object[] element = { poId, nodeName };
        final List<Object[]> datbaseEntries = new ArrayList<Object[]>();
        datbaseEntries.add(element);
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(dataPersistenceService.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(NAMESPACE, TYPE)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeRestrictionBuilder.equalTo("attributeName", "attrbuteValue")).thenReturn(restrictionMock);
        when(poMock.getPoId()).thenReturn(111l, 222l, 333l, 444l);
        when(queryExecutor.execute(typeQuery)).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true, false);
        when(iteratorMock.next()).thenReturn(poMock);
        when(
                queryExecutor.executeProjection(eq(typeQuery), any(Projection.class), any(Projection.class), any(Projection.class), any(Projection.class), any(Projection.class),
                        any(Projection.class), any(Projection.class), any(Projection.class))).thenReturn(datbaseEntries);
        final List<Long> jobTypeTemplatePoIds = new ArrayList<Long>();
        poAttributesHolderMock.getMainJobDetails(NAMESPACE, TYPE, jobTypeTemplatePoIds);
    }

    @Test
    public void findPOsByPoIds() {
        mapMock = new HashMap<String, Object>();
        mapMock.put("attributeName", "attrbuteValue");
        when(dataPersistenceService.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.findPosByIds(po_IDs)).thenReturn(persistenceObjects);
        when(poMock.getPoId()).thenReturn(111l, 222l, 333l, 444l);
        when(poMock.getAllAttributes()).thenReturn(mapMock);
        final Map<Long, Map<String, Object>> polist = poAttributesHolderMock.findPOsByPoIds(po_IDs);
        assertTrue(polist != null);
    }

}
