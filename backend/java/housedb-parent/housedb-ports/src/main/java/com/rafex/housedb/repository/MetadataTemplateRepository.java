package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.MetadataTemplateEntity;

import java.sql.SQLException;
import java.util.List;

public interface MetadataTemplateRepository {

    List<MetadataTemplateEntity> listMetadataTemplates(String metadataTarget, Boolean includeDisabled, int limit, int offset)
            throws SQLException;

    MetadataTemplateEntity createMetadataTemplate(String metadataTarget, String code, String name, String description,
            String definitionJson, boolean enabled) throws SQLException;
}
