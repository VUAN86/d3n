package de.ascendro.f4m.service.workflow.config;

import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class WorkflowConfig extends F4MConfigImpl implements Config {

	public static final String DS_MAX_POOL_SIZE = "workflow.dataSource.maxPoolSize";
    public static final String DS_USER = "workflow.dataSource.user";
    public static final String DS_PASSWORD = "workflow.dataSource.password";
    public static final String DS_URL = "workflow.dataSource.url";
    public static final String DS_DRIVER_CLASS_NAME = "workflow.dataSource.driverClassName";
    public static final String DS_CLASS_NAME = "workflow.dataSource.className";
    
    public static final String HIBERNATE_DIALECT = "workflow.hibernate.dialect";
    public static final String HIBERNATE_GENERATE_SCHEMA = "workflow.hibernate.hbm2ddl.auto";
    
    public static final String WORKFLOW_RESOURCE_FOLDER = "workflow.resourceFolder";

	public WorkflowConfig() {
        super(new AerospikeConfigImpl());

        setProperty(DS_MAX_POOL_SIZE, 10);
		setProperty(DS_USER, "sa");
		setProperty(DS_PASSWORD, "");
		setProperty(DS_URL, "jdbc:h2:mem:jbpm-db");
		setProperty(DS_DRIVER_CLASS_NAME, "org.h2.Driver");
		setProperty(DS_CLASS_NAME, LrcXADataSource.class.getName());
		
		setProperty(HIBERNATE_DIALECT, "org.hibernate.dialect.H2Dialect");
		setProperty(HIBERNATE_GENERATE_SCHEMA, "update");
		loadProperties();
	}
}
