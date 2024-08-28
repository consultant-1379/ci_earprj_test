package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

public enum UpgradePackageInvokedAction {

    UNKNOWN(-1, "Unknown Invoked Action"), VERIFY_UPGRADE(1, "The verifyUpgrade action has been invoked."), UPGRADE(2, "An upgrade action (one of the variants) has been invoked."), INSTALL(3,
            "An install action (one of the variants) has been invoked."), CANCEL_INSTALL(4, "The cancelInstall action has been invoked."), SHRINK(5,
                    "The shrink action has been invoked."), READREFERRINGCVINFORMATION(6, "The readReferringCvInformation action has been invoked."), RESTORE_SU(7,
                            "The restoreSu action has been invoked.");

    private int actionIdentifier;
    private String message;

    private UpgradePackageInvokedAction(final int progressId, final String progressMessage) {
        this.actionIdentifier = progressId;
        this.message = progressMessage;
    }

    public int getProgressId() {
        return actionIdentifier;
    }

    public String getProgressMessage() {
        return message;
    }

    public static UpgradePackageInvokedAction getInvokedAction(final String header) {

        for (final UpgradePackageInvokedAction s : UpgradePackageInvokedAction.values()) {
            if (s.name().equalsIgnoreCase(header)) {
                return s;
            }
        }
        return UpgradePackageInvokedAction.UNKNOWN;
    }

}
