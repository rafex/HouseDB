package com.rafex.housedb.services;

import com.rafex.housedb.service.models.MetadataCatalog;

import java.sql.SQLException;
import java.util.List;

public interface MetadataCatalogService {

    List<MetadataCatalog> listMetadataCatalogs(String metadataTarget, Boolean includeDisabled, Integer limit, Integer offset)
            throws SQLException;
}
