package com.ericsson.oss.services.shm.es.impl.ecim.polling
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

public class PollingTestConstants {
    public static final String neJobBusinessKey = "1234";
    public static final String ACTIVITY_CREATE = "createbackup";
    public static final String ACTIVITY_UPLOAD = "uploadbackup";
    public static final String ACTIVITY_ACTIVATE = "activate";
    public static final String resultSuccess = "SUCCESS";
    public static final String resultFailure = "FAILURE";
    public static final String backupName = "LTE01dg2ERBS00002_Backup04072018065534";
    public static final String backupType = "backupType";
    public static final String backupDomain = "BACKUP_DOMAIN";
    public static final String domainName = "backupDomain";
    public static final String platform = "ECIM";
    public static final String softwarePackageName = "16ARadioNodePackage1";
    public static final String nodeName = "LTE01dg2ERBS00002";
    public static final String neType = "RadioNode";

    public static final String upMoFdn = "SubNetwork=LTE01dg2ERBS00002,MeContext=LTE01dg2ERBS00002,ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,SwM=1,UpgradePackage=16ARadioNodePackage1";
    public static final String brmBackupMoFdn = "SubNetwork=LTE01dg2ERBS00002,MeContext=LTE01dg2ERBS00002,ManagedElement=LTE01dg2ERBS00002,SystemFunctions=1,BrM=1,BrmBackupManager=1";
}
