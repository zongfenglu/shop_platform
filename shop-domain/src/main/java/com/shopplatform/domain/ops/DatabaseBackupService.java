package com.shopplatform.domain.ops;

import com.shopplatform.domain.ops.entity.OpsBackup;

import java.nio.file.Path;
import java.util.List;

public interface DatabaseBackupService {

    OpsBackup create();

    List<OpsBackup> listRecent(int limit);

    OpsBackup get(Long id);

    Path resolveFile(OpsBackup row);
}
