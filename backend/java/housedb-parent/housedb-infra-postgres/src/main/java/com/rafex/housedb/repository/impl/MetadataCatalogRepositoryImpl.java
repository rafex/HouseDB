package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.MetadataCatalogRepository;
import com.rafex.housedb.repository.models.MetadataCatalogEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

public final class MetadataCatalogRepositoryImpl implements MetadataCatalogRepository {

    private static final String SQL_LIST_METADATA_CATALOGS = """
            SELECT metadata_catalog_id,
                   metadata_target,
                   code,
                   name,
                   description,
                   payload::text AS payload_json,
                   enabled
              FROM api_list_metadata_catalogs(?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    public MetadataCatalogRepositoryImpl(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<MetadataCatalogEntity> listMetadataCatalogs(final String metadataTarget, final Boolean includeDisabled,
            final int limit, final int offset) throws SQLException {
        final var result = new ArrayList<MetadataCatalogEntity>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_LIST_METADATA_CATALOGS)) {
            ps.setString(1, metadataTarget);
            ps.setBoolean(2, includeDisabled);
            ps.setInt(3, limit);
            ps.setInt(4, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MetadataCatalogEntity(
                            rs.getObject("metadata_catalog_id", UUID.class),
                            rs.getString("metadata_target"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("payload_json"),
                            rs.getBoolean("enabled")));
                }
            }
        }
        return result;
    }
}
