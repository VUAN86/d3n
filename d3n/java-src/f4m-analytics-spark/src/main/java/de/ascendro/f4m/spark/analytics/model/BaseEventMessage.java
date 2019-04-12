package de.ascendro.f4m.spark.analytics.model;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.from_json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseEventMessage {

    public static final String TIMESTAMP_COLUMN = "timestamp";
    public static final String VALUE_COLUMN = "value";

    private static final String EVENT_TYPE = "eventType";
    private static final String EVENT_USER_ID = "userId";
    private static final String EVENT_TENANT_ID = "tenantId";
    private static final String EVENT_TIMESTAMP = "eventTimestamp";
    private static final String EVENT_APPLICATION_ID = "appId";
    private static final String EVENT_CLIENT_IP = "sessionIp";
    private static final String EVENT_COUNTRY_CODE = "countryCode";
    private static final String EVENT_DATA = "eventData";
    private static final String EVENT_OBJECT = "jsonObject";
    
    private static final String EVENT_TYPE_PATH = VALUE_COLUMN + "." + EVENT_TYPE;

    private static final String SQL_TABLE_SEPARATOR = ".";

    private static final String EVENT_OBJECT_COLUMN = VALUE_COLUMN + SQL_TABLE_SEPARATOR + EVENT_DATA
            + SQL_TABLE_SEPARATOR + EVENT_OBJECT + SQL_TABLE_SEPARATOR;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseEventMessage.class);

    private StructType inputSchema;
    private StructType outputSchema;
    private ArrayList<Column> inputColumns;
    private ArrayList<Column> outputColumns;
    private String format;
    private String path;
    private String eventTableName;
	private String eventClassName;

    protected BaseEventMessage(String format, String path, String eventClassName, String eventTableName) {
        this.format = format;
        this.path = path;
    	this.eventClassName = eventClassName;
        this.eventTableName = eventTableName;
        inputColumns = new ArrayList<>();
        outputColumns = new ArrayList<>();
        initBaseColumns();
        initColumns();

        DataType jsonObjectDataType = DataType.fromJson(DataTypes.createStructType(new StructField[]{DataTypes.createStructField(EVENT_OBJECT,
                getEventDataType(), true)}).json());

        List<StructField> inputFields = new ArrayList<>();

        //generate base message inputSchema
        inputFields.add(DataTypes.createStructField(EVENT_TYPE, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_USER_ID, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_TENANT_ID, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_APPLICATION_ID, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_CLIENT_IP, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_COUNTRY_CODE, DataTypes.StringType, true));
        inputFields.add(DataTypes.createStructField(EVENT_TIMESTAMP, DataTypes.LongType, true));

        List<StructField> outputFields = new ArrayList<>(inputFields);
        outputFields.addAll(Arrays.asList(getEventFields()));
        inputFields.add(DataTypes.createStructField(EVENT_DATA, jsonObjectDataType, true));

        inputSchema = DataTypes.createStructType(inputFields);
        outputSchema = DataTypes.createStructType(outputFields);


        //Print inputSchema for current message handler
        LOGGER.info("Structure for message class: {}", this.getClass().getSimpleName());
        LOGGER.info(inputSchema.simpleString());
        inputSchema.printTreeString();
    }

    private DataType getEventDataType() {
        return DataType.fromJson(DataTypes.createStructType(getEventFields()).json());
    }

    protected abstract StructField[] getEventFields();

    protected abstract void initColumns();

    public String getEventTableName() {
    	return eventTableName;
    }

    public void persist(Dataset<Row> events) {
        try {
            LOGGER.debug("DATA FOR MESSAGE TYPE: {}", getClass().getSimpleName());
            events.show(100, false);

            Dataset<Row> filteredEvents = events.filter(getFilterColumn())
                    .withColumn(VALUE_COLUMN, from_json(col(VALUE_COLUMN), getInputSchema()))
                    .select(getInputSelectColumns().toArray(new Column[getInputSelectColumns().size()]));

            //Debug message to console
            LOGGER.debug("Persisting {} filtered events for message class: {}", filteredEvents.count(), this.getClass().getSimpleName());
            filteredEvents.show(false);

            saveToHDFS(filteredEvents);
        } catch (Exception e) {
            LOGGER.error("Persistence failed", e);
        }
    }

    public StructType getInputSchema() {
        return inputSchema;
    }

    public StructType getOutputSchema() {
        return outputSchema;
    }

    private String getEventName() {
		return eventClassName;
    }

    private void initBaseColumns() {
        addBaseColumn(EVENT_TYPE);
        addBaseColumn(EVENT_USER_ID);
        addBaseColumn(EVENT_TENANT_ID);
        addBaseColumn(EVENT_APPLICATION_ID);
        addBaseColumn(EVENT_CLIENT_IP);
        addBaseColumn(EVENT_COUNTRY_CODE);
        addBaseColumn(EVENT_TIMESTAMP);
    }

    private void addBaseColumn(String columnName) {
        inputColumns.add(col(VALUE_COLUMN + SQL_TABLE_SEPARATOR + columnName));
        outputColumns.add(col(columnName));
    }

    protected Column getFilterColumn() {
        return col(EVENT_TYPE_PATH).cast(DataTypes.StringType).equalTo(getEventName());
    }

    protected void addSelectColumn(String columnName) {
        inputColumns.add(col(EVENT_OBJECT_COLUMN + columnName));
        outputColumns.add(col(columnName));
    }

    public List<Column> getInputSelectColumns() {
        return inputColumns;
    }

    public List<Column> getOutputSelectColumns() {
        return outputColumns;
    }

    protected void saveToHDFS(Dataset<Row> events) {
        events.write()
                .format(format)
                .mode(SaveMode.Append)
                .save(getFilePath());
    }

    public String getFilePath() {
        return path + (path.endsWith("/")?"":"/") + getFileName();
    }

    private String getFileName() {
        return getEventTableName() + "." + format;
    }

    public String getFormat() {
        return format;
    }
}
