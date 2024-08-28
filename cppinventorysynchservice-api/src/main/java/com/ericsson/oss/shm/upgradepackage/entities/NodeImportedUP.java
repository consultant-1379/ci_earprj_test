package com.ericsson.oss.shm.upgradepackage.entities;

import java.util.List;

public class NodeImportedUP {

	private List<UpgradePackagePO> importedUPInfo;

	public NodeImportedUP() {
	}

	public NodeImportedUP(final List<UpgradePackagePO> importedUPInfo) {
		this.importedUPInfo = importedUPInfo;
	}

	public List<UpgradePackagePO> getImportedUPInfo() {
		return importedUPInfo;
	}

}
