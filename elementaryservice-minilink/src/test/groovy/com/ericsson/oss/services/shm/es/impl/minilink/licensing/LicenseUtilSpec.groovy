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
package com.ericsson.oss.services.shm.es.impl.minilink.licensing


import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.services.shm.common.smrs.SmrsServiceConstants
import com.ericsson.oss.services.shm.es.impl.ActivityUtils
import com.ericsson.oss.services.shm.es.impl.JobEnvironment
import com.ericsson.oss.services.shm.es.impl.minilink.common.constants.MiniLinkConstants
import com.ericsson.oss.services.shm.es.impl.minilink.upgrade.MiniLinkActivityUtil
import com.ericsson.oss.services.shm.minilinkindoor.common.FtpUtil
import com.ericsson.oss.services.shm.es.impl.minilink.common.MiniLinkDps

class LicenseUtilSpec extends CdiSpecification {

    private static final String nodeName = "nodeName";
    private static final String LKF_DOWNLOAD_STARTED = "lkfDownloadStarted";
    private static final long activityJobId = 123l;
    private static final String FDN = "MeContext=CORE82MLTN02,ManagedElement=CORE82MLTN02,ericsson=1,miniLinkXF=1,xfPlatform=1,xfLicenseMIB=1,xfLicenseMIBObjects=1,xfLicenseInstallObjects=1";
    private final String ossModelIdentity ="M11-TN-4.4FP";

    @ObjectUnderTest
    LicenseUtil licenseUtil

    @MockedImplementation
    MiniLinkActivityUtil miniLinkActivityUtil

    @MockedImplementation
    ManagedObject managedObject

    @MockedImplementation
    ActivityUtils activityUtils;

    @MockedImplementation
    JobEnvironment jobEnvironment;

    @MockedImplementation
    FtpUtil ftpUtil

    @MockedImplementation
    MiniLinkDps miniLinkDps;

    def setup() {
        miniLinkActivityUtil.getManagedObject(activityJobId, MiniLinkConstants.XF_LICENSE_INSTALL_OBJECTS) >> managedObject
        managedObject.getFdn() >> FDN
        managedObject.getAttribute(MiniLinkConstants.XF_LICENSE_INSTALL_OPER_STATUS) >> LKF_DOWNLOAD_STARTED
        ftpUtil.setupFtp(nodeName, SmrsServiceConstants.LICENCE_ACCOUNT) >> 2
        activityUtils.getJobEnvironment(activityJobId) >> jobEnvironment;
        jobEnvironment.getNodeName() >> nodeName
    }

    def "TestGetXfLicenseInstallObjectsFdn" () {
        when: "execute"
        String fdn = licenseUtil.getXfLicenseInstallObjectsFdn(activityJobId)
        then: "expect"
        fdn  == FDN
    }

    def "TestGetLicenseInstallOperStatus" () {
        when: "execute"
        String status = licenseUtil.getLicenseInstallOperStatus(activityJobId)
        then: "expect"
        status == LKF_DOWNLOAD_STARTED
    }

    def "TestSetSmrsFtpOnNode" () {
        given:
        miniLinkDps.getOssModelIdentity(nodeName) >> ossModelIdentity
        when: "execute"
        licenseUtil.setSmrsFtpOnNode(activityJobId, nodeName)
        then: "expect nothing"
    }
}
