/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.resources

import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElementIdResponse

/**
 * Class to verify tests on Job Configuration response 
 *
 * @author xkalkil
 *
 */
public class SHMJobResourceImplTest extends AbstractSHMJobServiceImplSpec{

    @ObjectUnderTest
    private SHMJobResourceImpl sHMJobResourceImpl;


    def "Verify Job Summary details for backup job on AXE nodes"() {

        given : "Provide persisted JobTemplate id try to get summary details"
        long jobTemplateId = getJobTemplatePOId(NETYPE, BOTH_ACTIVITIES, COMPONENTS)
        inventoryQueryConfigurationListener.getNeFdnBatchSize() >> 50

        when: "call job config method to get summary details"
        final Response response = sHMJobResourceImpl.getJobConfiguration(jobTemplateId);

        then: "response should be ok"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
    }

    def "Verify No data for Job Summary details for backup job on AXE nodes"() {

        given : "Provide non existing JobTemplate id try to get summary details"

        when: "call job config method to get summary details"
        final Response response = sHMJobResourceImpl.getJobConfiguration(22l);

        then: "response should be not found"
        assert(Status.fromStatusCode(response.getStatus()) == Status.NOT_FOUND)
    }

    def "Verify supported and unsupported rest call response"() {

        given : "Provide neNames list and Job Type"
        final List<String> neNames = new ArrayList<String>();
        neNames.add("MSC07");
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");
        neNames.add("LTE05ERBS00001");
        neNames.add("LTE05ERBS00002");
        final String jobType = "NODE_HEALTH_CHECK";
        final Map<String,List<String>> inputData = new HashMap<>();
        inputData.put(jobType,neNames);

        when: "call getsupportedNes method to get supported and unsupported nes"
        final Response response = sHMJobResourceImpl.getSupportedNes(inputData);

        then: "response should be ok"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
    }

    def "Verify supported and unsupported rest call when no nodes selected" () {
        given : "Provide empty neNames list and Job Type"
        final List<String> neNames = new ArrayList<String>();
        final String jobType = "NODE_HEALTH_CHECK";
        final Map<String,List<String>> inputData = new HashMap<>();
        inputData.put(jobType,neNames);

        when: "call getsupportedNes method to get supported and unsupported nes"
        final Response response = sHMJobResourceImpl.getSupportedNes(inputData);
        final Map<String,Object> supportedUnSupportedMap = (response.getEntity());


        then: "response should be ok and supported and unsupported should be empty"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
        assert(supportedUnSupportedMap.size() == 0)
    }
    def"Get Existing data in dps network element poids based on node names "(){

        given : "Provide node names"
        final List<String> neNames=new ArrayList<String>();
        neNames.add("LTE06dg2ERBS00003");
        neNames.add("LTE06dg2ERBS00006");
        inventoryQueryConfigurationListener.getNeFdnBatchSize()  >> 50
        final NetworkElementIdResponse networkElementIdsResponce=getNetworkElementPoIds(neNames)

        when : "call getPoids method to get Poids"
        final Response response = sHMJobResourceImpl.getNetworkElementPoIds(neNames);
        final NetworkElementIdResponse poIdsList = (response.getEntity());

        then : "responce should be ok and get details"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
        assert(poIdsList.getPoIdList()== [4, 5]);
        assert(poIdsList.getPoIdList().size()== 2);
    }

    def"Get non exsting data in network element poids based on node names"(){

        given : "Provide node names"

        final List<String> neNames=new ArrayList<String>();
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00002");

        inventoryQueryConfigurationListener.getNeFdnBatchSize()  >> 50
        final NetworkElementIdResponse networkElementIdsResponce=getNetworkElementPoidsnotindps(neNames);
        when : "call getPoids method to get Poids"

        final Response response = sHMJobResourceImpl.getNetworkElementPoIds(neNames);
        final NetworkElementIdResponse poIdsList = (response.getEntity());

        then : "responce should be ok and get details"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
        assert(poIdsList.getPoIdList()== []);
        assert(poIdsList.getPoIdList().size() == 0);
    }

    def "Verify supported and unsupported rest call response for duplicate node names scenario"() {

        given : "Provide neNames list and Job Type"
        final List<String> neNames = new ArrayList<String>();
        neNames.add("LTE06dg2ERBS00001");
        neNames.add("LTE06dg2ERBS00001");
        final String jobType = "RESTORE";
        final Map<String,List<String>> inputData = new HashMap<>();
        inputData.put(jobType,neNames);

        when: "call getsupportedNes method to get supported and unsupported nes"
        final Response response = sHMJobResourceImpl.getSupportedNes(inputData);
        final Map<String,Object> supportedUnSupportedMap = (response.getEntity());

        then: "response should be ok"
        assert(Status.fromStatusCode(response.getStatus()) == Status.OK)
        assert(supportedUnSupportedMap.size() == 1)
    }
}

