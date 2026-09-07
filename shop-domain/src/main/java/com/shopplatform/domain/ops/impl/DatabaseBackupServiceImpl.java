package com.shopplatform.domain.ops.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;
import com.shopplatform.domain.ops.DatabaseBackupService;
import com.shopplatform.domain.ops.entity.OpsBackup;
import com.shopplatform.domain.ops.mapper.OpsBackupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class DatabaseBackupServiceImpl extends ServiceImpl<OpsBackupMapper, OpsBackup>
        implements DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupServiceImpl.class);

    private final DataSource dataSource;
    private final Path backupDir;

    public DatabaseBackupServiceImpl(DataSource dataSource,
                                     @Value("${shop.backup.dir:./data/backups}") String backupDir) {
        this.dataSource = dataSource;
        this.backupDir = Path.of(backupDir).toAbsolutePath().normalize();
    }

    @Override
    public OpsBackup create() {
        String filename = "shop_platform_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        Path file = backupDir.resolve(filename);
        OpsBackup row = new OpsBackup();
        row.setFilename(filename);
        row.setFilePath(file.toString());
        row.setSizeBytes(0L);
        row.setStatus("failed");
        try {
            Files.createDirectories(backupDir);
            dump(file);
            row.setSizeBytes(Files.size(file));
            row.setStatus("success");
            row.setMessage("逻辑备份完成");
        } catch (Exception e) {
            log.error("数据库备份失败", e);
            String detail = e.getMessage() == null ? "" : (": " + e.getMessage());
            row.setMessage(e.getClass().getSimpleName() + detail);
        }
        this.save(row);
        return row;
    }

    @Override
    public List<OpsBackup> listRecent(int limit) {
        return this.list(Wrappers.<OpsBackup>lambdaQuery()
                .orderByDesc(OpsBackup::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    @Override
    public OpsBackup get(Long id) {
        OpsBackup row = this.getOne(Wrappers.<OpsBackup>lambdaQuery().eq(OpsBackup::getId, id));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份不存在");
        }
        return row;
    }

    @Override
    public Path resolveFile(OpsBackup row) {
        Path file = Path.of(row.getFilePath()).toAbsolutePath().normalize();
        if (!file.startsWith(backupDir)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "备份文件路径非法");
        }
        return file;
    }

    private void dump(Path file) throws Exception {
        try (Connection conn = dataSource.getConnection();
             BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("-- shop_platform logical backup\nSET NAMES utf8mb4;\n");
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (name != null && !name.equalsIgnoreCase("flyway_schema_history")) {
                        tables.add(name);
                    }
                }
            }
            try (Statement stmt = conn.createStatement()) {
                for (String table : tables) {
                    out.write("\n-- " + table + "\n");
                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table.replace("`", "") + "`")) {
                        ResultSetMetaData cols = rs.getMetaData();
                        int n = cols.getColumnCount();
                        while (rs.next()) {
                            StringBuilder sql = new StringBuilder("INSERT INTO `").append(table).append("` (");
                            for (int i = 1; i <= n; i++) {
                                if (i > 1) {
                                    sql.append(", ");
                                }
                                sql.append('`').append(cols.getColumnName(i)).append('`');
                            }
                            sql.append(") VALUES (");
                            for (int i = 1; i <= n; i++) {
                                if (i > 1) {
                                    sql.append(", ");
                                }
                                sql.append(literal(rs.getObject(i)));
                            }
                            sql.append(");\n");
                            out.write(sql.toString());
                        }
                    }
                }
            }
        }
    }

    static String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return "'" + ldt.toString().replace('T', ' ') + "'";
        }
        if (value instanceof java.time.LocalDate d) {
            return "'" + d + "'";
        }
        if (value instanceof java.sql.Timestamp ts) {
            return "'" + ts.toLocalDateTime().toString().replace('T', ' ') + "'";
        }
        if (value instanceof byte[] bytes) {
            return "X'" + HexFormat.of().formatHex(bytes) + "'";
        }
        String text = value.toString().replace("\\", "\\\\").replace("'", "''");
        return "'" + text + "'";
    }
}
