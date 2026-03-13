package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.MetadataCatalogEntity;

import java.sql.SQLException;
import java.util.List;

public interface MetadataCatalogRepository {

    List<MetadataCatalogEntity> listMetadataCatalogs(String metadataTarget, Boolean includeDisabled, int limit, int offset)
            throws SQLException;
}
