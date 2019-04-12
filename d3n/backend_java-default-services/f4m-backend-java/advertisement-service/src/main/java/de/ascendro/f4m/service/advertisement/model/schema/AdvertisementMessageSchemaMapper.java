package de.ascendro.f4m.service.advertisement.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class AdvertisementMessageSchemaMapper extends DefaultMessageSchemaMapper {
    private static final long serialVersionUID = -4575812985735172612L;


    private static final String ADVERTISEMENT_SCHEMA_PATH = "advertisement.json";


    @Override
    protected void init() {
        this.register(AdvertisementMessageSchemaMapper.class, "advertisement", ADVERTISEMENT_SCHEMA_PATH);
        super.init();
    }
}
