package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.InviteEvent.FRIENDS_BLOCKED;
import static de.ascendro.f4m.server.analytics.model.InviteEvent.FRIENDS_INVITED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.InviteEvent.FRIENDS_INVITED_TOO;
import static de.ascendro.f4m.server.analytics.model.InviteEvent.FRIENDS_UNBLOCKED;
import static de.ascendro.f4m.server.analytics.model.InviteEvent.INVITED_FROM_FRIENDS;
import static de.ascendro.f4m.server.analytics.model.InviteEvent.BONUS_INVITE_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class InviteMessage extends BaseEventMessage {

    private static final String TABLE_NAME_INVITE = "invite";

	public InviteMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.InviteEvent", TABLE_NAME_INVITE);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(FRIENDS_INVITED_PROPERTY);
        addSelectColumn(BONUS_INVITE_PROPERTY);
        addSelectColumn(INVITED_FROM_FRIENDS);
        addSelectColumn(FRIENDS_INVITED_TOO);
        addSelectColumn(FRIENDS_BLOCKED);
        addSelectColumn(FRIENDS_UNBLOCKED);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(FRIENDS_INVITED_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(BONUS_INVITE_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(INVITED_FROM_FRIENDS, DataTypes.BooleanType, true),
                DataTypes.createStructField(FRIENDS_INVITED_TOO, DataTypes.BooleanType, true),
                DataTypes.createStructField(FRIENDS_BLOCKED, DataTypes.LongType, true),
                DataTypes.createStructField(FRIENDS_UNBLOCKED, DataTypes.LongType, true)
        };
    }
}
