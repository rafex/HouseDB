package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.MetadataTemplateRepository;
import com.rafex.housedb.repository.models.MetadataTemplateEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

public final class MetadataTemplateRepositoryImpl implements MetadataTemplateRepository {

    private static final String SQL_LIST_METADATA_TEMPLATES = """
            SELECT metadata_template_id,
                   metadata_target,
                   code,
                   name,
                   description,
                   definition::text AS definition_json,
                   enabled
              FROM api_list_metadata_templates(?, ?, ?, ?)
            """;

    private static final String SQL_CREATE_METADATA_TEMPLATE = """
            SELECT metadata_template_id,
                   metadata_target,
                   code,
                   name,
                   description,
                   definition::text AS definition_json,
                   enabled
              FROM api_create_metadata_template(?, ?, ?, ?, ?::jsonb, ?)
            """;

    private final DataSource dataSource;

    public MetadataTemplateRepositoryImpl(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<MetadataTemplateEntity> listMetadataTemplates(final String metadataTarget, final Boolean includeDisabled,
            final int limit, final int offset) throws SQLException {
        final var result = new ArrayList<MetadataTemplateEntity>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_LIST_METADATA_TEMPLATES)) {
            ps.setString(1, metadataTarget);
            ps.setBoolean(2, includeDisabled);
            ps.setInt(3, limit);
            ps.setInt(4, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MetadataTemplateEntity(
                            rs.getObject("metadata_template_id", UUID.class),
                            rs.getString("metadata_target"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("definition_json"),
                            rs.getBoolean("enabled")));
                }
            }
        }
        return result;
    }

    @Override
    public MetadataTemplateEntity createMetadataTemplate(final String metadataTarget, final String code, final String name,
            final String description, final String definitionJson, final boolean enabled) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_CREATE_METADATA_TEMPLATE)) {
            ps.setString(1, metadataTarget);
            ps.setString(2, code);
            ps.setString(3, name);
            ps.setString(4, description);
            ps.setString(5, definitionJson);
            ps.setBoolean(6, enabled);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_create_metadata_template returned no rows");
                }

                return new MetadataTemplateEntity(
                        rs.getObject("metadata_template_id", UUID.class),
                        rs.getString("metadata_target"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("definition_json"),
                        rs.getBoolean("enabled"));
            }
        }
    }
}
