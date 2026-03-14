package com.rafex.housedb.services;

import com.rafex.housedb.service.models.MetadataTemplate;

import java.sql.SQLException;
import java.util.List;

public interface MetadataTemplateService {

    List<MetadataTemplate> listMetadataTemplates(String metadataTarget, Boolean includeDisabled, Integer limit, Integer offset)
            throws SQLException;

    MetadataTemplate createMetadataTemplate(String metadataTarget, String code, String name, String description,
            String definitionJson, Boolean enabled) throws SQLException;
}
