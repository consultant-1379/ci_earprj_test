/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.vran.constants;

/**
 * This class provides the templates for logging the VRAN jobs.
 * 
 * @author xindkag
 */
public class VranJobLogMessageTemplate {

    // Software Upgrade job log templates
    public static final String JOB_TRIGGERED = "\"%s\" percent \"%s\" activity has been completed on the node.";
    public static final String ACTIVITY_ABOUT_TO_START = "\"%s\" activity is going to be triggered for \"%s\".";
    public static final String REQUEST_FOR_JOB_STATUS = "\"%s\" activity is currently in progress on node and requesting for status again.";
    public static final String JOB_FAILED = "\"%s\" activity is failed on the node.";
    public static final String ACTION_ABOUT_TO_TRIGGER = "\"%s\" action is going to be triggered for VNF ID \"%s\" on VNFM";
    public static final String WORKFLOW_SERVICE_INVOCATION_FAILED = "\"%s\" completed, notifying workflow service has failed. Waiting until timeout.";
    public static final String PROGRESS_INFORMATION = "Progress Info : Action Name= \"%s\" ProgressPercentage=\"%s\" State= \"%s\"  ";
    public static final String PROGRESS_INFORMATION_WITH_RESULT = "Progress Info : Action Name= \"%s\" ProgressPercentage=\"%s\" ProgressDetail=\"%s\" State= \"%s\" Result= \"%s\" AdditionalInfo= \"%s\" RequestedTime= \"%s\" FinishedTime= \"%s\" ";
    public static final String CANCEL_NOT_SUPPORT = "Node will not support for cancel in \"%s\" Activity";
    public static final String ACTIVITY_FAILED_WITH_REASON = "\"%s\" activity has failed. Because of : \"%s\"";
    public static final String ACTIVITY_FAILED_WITH_ERROR = "\"%s\" activity has failed. Because of Error : \"%s\", Error Code : \"%s\", Error Time : \"%s\"";
    public static final String ACTIVITY_FAILED_WITH_ADDITIONAL_INFO = "\"%s\" activity has failed. Additional Info : \"%s\" ProgressDetail=\"%s\"";
    public static final String ACTIVITY_IN_TIMEOUT = "Notifications not received for the \"%s\" activity";
    public static final String SOFTWARE_PACKAGE_IN_TIMEOUT = "Notifications not received for the \"%s\" activity, SoftwarePackage : \"%s\" ";
    public static final String PERCENTAGE_OF_JOB_COMPLETED = "\"%s\" percent \"%s\" activity for \"%s\" has been completed.";

    //Onboard Software package job log templates

    public static final String JOB_PROGRESS_INFORMATION_WITH_RESULT = "Progress Info for \"%s\" : Action Name= \"%s\"  ProgressPercentage=\"%s\"  State= \"%s\"  Result= \"%s\"  StatusDescription= \"%s\".";
    public static final String JOB_PROGRESS_INFORMATION_WITH_JOB_ID = " \"%s\" activity for Package:\"%s\"  is started with Job Id=\"%s\"  on the NFVO.";
    public static final String ONBOARD_SWPACKAGE_RESULT = "Total number of packages requested for onboard: \"%s\" ; successful packages: \"%s\" ; failed packages: \"%s\" ; ";
    public static final String ONBOARD_SWPACKAGE_JOB_IN_TIMEOUT = "\"%s\" action for the \"%s\" has failed. Handling in timeout.";
    public static final String SWPACKAGE_LOCATION_UPDATE = "Package:\"%s\" location has been updated as \"%s\". ";
    public static final String SWPACKAGE_INFO_FROM_ENM = " %s : \"%s\" information from ENM.";
    public static final String SWPACKAGE_NOT_IN_ENM_LOCATION = "Softwarepackage is not available in ENM location : \"%s\". ";
    public static final String SWPACKAGE_UPDATE_RESULT = " Package:\"%s\" entry has been updated successfully in ENM.";
    public static final String ACTIVITY_FAILED = "\"%s\" activity has failed";
    public static final String SWPACKAGE_NOTFOUND_IN_ENM = " Package:\"%s\" unavailable in ENM";

    //Delete Software package job log templates
    public static final String DELETING_SWPACKAGE_FROM_ENM = "Deleting package from ENM. ENM Location: \"%s\".";
    public static final String SWPACKAGE_DELETED_FROM_ENM_LOCATION = " Package deleted successfully from ENM Location :\"%s\". ";
    public static final String DELETE_SWPACKAGENAMES_FROM_ENM_LOCATION = "Total number of packages requested for deletion from ENM: \"%s\" ; Successful Packages : \"%s\" ; Failed Packages: \"%s\" ; Failed Package Names: \"%s\". ";
    public static final String DELETE_SWPACKAGENAMES_FROM_NFVO_LOCATION = "Total number of packages requested for deletion from NFVO: \"%s\" ; Successful Packages : \"%s\" ; Failed Packages: \"%s\" ; Failed Package Names: \"%s\". ";
    public static final String DELETE_PACKAGES_RESULT_FROM_ENM = "Total number of packages requested for deletion from ENM : \"%s\" ; Successful Packages : \"%s\" ; Failed Packages: \"%s\". ";
    public static final String DELETE_PACKAGES_RESULT_FROM_NFVO = "Total number of packages requested for deletion from NFVO : \"%s\" ; Successful Packages : \"%s\" ; Failed Packages: \"%s\". ";
    public static final String SWPACKAGE_DELETED_FROM_ENM = " Package:\"%s\" information removed successfully from ENM.";
    public static final String SWPACKAGE_DELETED_FROM_NFVO = "Package:\"%s\" information removed successfully from NFVO.";
    public static final String SWPACKAGE_NOT_IN_NFVO = "\"%s\" does not exists in NFVO. Delete operation is not applicable.";
    public static final String SWPACKAGE_IN_USE = "Package:\"%s\" is in use by upgrade jobs.";
    public static final String FOUND_NO_UPGRADE_JOBS = "There are no upgrade jobs on VNFM: \"%s\" ";
    public static final String FOUND_NO_UPGRADE_JOBS_FOR_VNFID = "There are no upgrade jobs for VNFID: \"%s\" on VNFM: \"%s\" ";
    public static final String NO_FROM_VNFID = "Unable to find fromVnfId for toVnfId : \"%s\" NetworkElement : \"%s\" ";

    public static final String ACTIVITY_FAILED_FOR_PACKAGE_WITH_REASON = "\"%s\" activity has failed for software package \"%s\". Because of : \"%s\"";

}
