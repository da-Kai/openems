package io.openems.edge.core.host;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Worker constantly checks if the disk is full. It may be extended in
 * future to check more Host related states.
 */
public class DiskSpaceWorker extends AbstractWorker {

	private static final int CYCLE_TIME = 300_000; // in ms
	private static final long MINIMUM_FREE_DISK_SPACE = 50 /* MB */ * 1024 /* kB */ * 1024 /* bytes */; // in bytes

	private final Logger log;

	private final HostImpl parent;

	public DiskSpaceWorker(HostImpl parent) {
		this.parent = parent;
		this.log = OpenemsComponent.getComponentLogger(DiskSpaceWorker.class, parent);
	}

	@Override
	protected void forever() {
		var totalUsableSpace = 0L;
		for (Path root : FileSystems.getDefault().getRootDirectories()) {
			try {
				var store = Files.getFileStore(root);
				totalUsableSpace += store.getUsableSpace();
			} catch (IOException e) {
				this.log.info("Unable to query disk space: {}", e.getMessage());
			}
		}

		this.parent._setDiskIsFull(totalUsableSpace < MINIMUM_FREE_DISK_SPACE);
	}

	@Override
	protected int getCycleTime() {
		return CYCLE_TIME;
	}

}
