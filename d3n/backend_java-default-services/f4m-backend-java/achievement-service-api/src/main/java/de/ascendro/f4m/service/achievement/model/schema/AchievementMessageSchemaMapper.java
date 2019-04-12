package de.ascendro.f4m.service.achievement.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class AchievementMessageSchemaMapper extends DefaultMessageSchemaMapper {
    private static final long serialVersionUID = -4575812985735172619L;
    private static final String ACHIEVEMENT_SCHEMA_PATH = "achievement.json";
    @Override
    protected void init() {
        this.register(AchievementMessageSchemaMapper.class, "achievement", ACHIEVEMENT_SCHEMA_PATH);
        super.init();
    }
}