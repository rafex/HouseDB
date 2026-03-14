package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.MetadataCatalogEntity;

import java.sql.SQLException;
import java.util.List;

public interface MetadataCatalogRepository {

    List<MetadataCatalogEntity> listMetadataCatalogs(String metadataTarget, Boolean includeDisabled, int limit, int offset)
            throws SQLException;

    MetadataCatalogEntity createMetadataCatalog(String metadataTarget, String code, String name, String description,
            String payloadJson, boolean enabled) throws SQLException;
}
