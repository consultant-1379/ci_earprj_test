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
package com.ericsson.oss.services.shm.es.instrumentation.impl;

/**
 * Class Defines the Job Instrumentation Constants
 * 
 * @author zkummad
 * 
 */
public class JobInstrumentationConstants {

    public static final String ERBS = "erbs";
    public static final String SGSN = "sgsn-mme";
    public static final String DUSG2 = "radionode";
    public static final String MINILINKINDOOR = "MINI-LINK-Indoor";
    public static final String MINILINK669X = "MINI-LINK-669x";
    public static final String MINILINK665X = "MINI-LINK-665x";
    public static final String MINILINK6366 = "MINI-LINK-6366";
    public static final String MINILINKINDOOR_CN510R2 = "MINI-LINK-CN510R2";
    public static final String MINILINKINDOOR_CN210 = "MINI-LINK-CN210";
    public static final String MINILINKINDOOR_CN510R1 = "MINI-LINK-CN510R1";
    public static final String MINILINKINDOOR_CN810R1 = "MINI-LINK-CN810R1";
    public static final String MINILINKINDOOR_CN810R2 = "MINI-LINK-CN810R2";

    public static final String DELIMITER_UNDERSCORE = "_";

    public static final String BSC = "BSC";
    public static final String MSC_BC_BSP = "MSC-BC-BSP";
    public static final String MSC_BC_IS = "MSC-BC-IS";
    public static final String MSC_DB_BSP = "MSC-DB-BSP";
    public static final String MSC_DB = "MSC-DB";
    public static final String VMSC_HC = "vMSC-HC";
    public static final String VMSC = "vMSC";
    public static final String IP_STP = "IP-STP";
    public static final String IP_STP_BSP = "IP-STP-BSP";
    public static final String VIP_STP = "vIP-STP";
    public static final String HLR_FE = "HLR-FE";
    public static final String HLR_FE_IS = "HLR-FE-IS";
    public static final String HLR_FE_BSP = "HLR-FE-BSP";
    public static final String VHLR_FE = "vHLR-FE";

    /* START - Constants related to Delete Upgrade Package Job. */

    public static final String DELETE_UPGRADE_PACKAGE_ECIM_ACTIVITIES_COUNT = "ECIM delete upgrade package activities count";
    public static final String DELETE_UPGRADE_PACKAGE_CPP_ACTIVITIES_COUNT = "CPP delete upgrade package activities count";
    public static final String DELETE_UPGRADE_PACKAGE_MAIN_JOB_COUNT = "DeleteUpgradePackage  MainJobs count";
    public static final String DELETE_UPGRADE_PACKAGE_NE_JOB_COUNT = "Running Delete Upgrade Package NeJobs count";
    public static final String DELETE_UPGRADE_PACKAGE_ERBS_JOB_COUNT = "ERBS Delete Upgrade Package NeJobs count";
    public static final String DELETE_UPGRADE_PACKAGE_DUSGEN2_JOB_COUNT = "DUSGEN2 Delete Upgrade Package NeJobs count";
    public static final String DELETE_UPGRADE_PACKAGE_SGSN_JOB_COUNT = "SGSN DeleteBackup Waiting NeJobs count";
    public static final String DELETE_UPGRADE_ACTIVITY_NAME = "deleteupgradepackage";

    /* END - Constants related to Delete Upgrade Package Job. */

    /* START - Constants for AXE License Job */

    public static final String AXE_LICENSE_INSTALL_NEJOB_COUNT = "Running AXE License NeJobs count";
    public static final String AXE_LICENSE_INSTALL_ACTIVITIES_COUNT = "Running AXE install license activities count";
    public static final String AXE_LICENSE_ACTIVITY_NAME = "install";

    /* END - Constants for AXE License Job */
}
