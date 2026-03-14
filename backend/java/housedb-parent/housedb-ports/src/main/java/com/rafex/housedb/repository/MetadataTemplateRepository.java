package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.MetadataTemplateEntity;

import java.sql.SQLException;
import java.util.List;

public interface MetadataTemplateRepository {

    List<MetadataTemplateEntity> listMetadataTemplates(String metadataTarget, Boolean includeDisabled, int limit, int offset)
            throws SQLException;
}
