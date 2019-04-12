package de.ascendro.f4m.spark.analytics;

import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.RuntimeConfig;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import java.lang.reflect.InvocationTargetException;

import de.ascendro.f4m.spark.analytics.config.AppConfig;

public class ThriftServerApplicationTest {

    @Mock
    SQLContext sqlContext;

    @Mock
    SparkContext sparkContext;

    @Mock
    SparkSession sparkSession;

    @Mock
    RuntimeConfig runtimeConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadStream() throws AnalysisException,
            NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        Configuration configuration = new Configuration();
        when(sqlContext.sparkContext()).thenReturn(sparkContext);
        when(sparkSession.conf()).thenReturn(runtimeConfig);
        when(runtimeConfig.contains(anyString())).thenReturn(false);
        when(sparkContext.hadoopConfiguration()).thenReturn(configuration);
        ThriftServerApplication application = new ThriftServerApplication(new AppConfig(sparkSession));
        application.loadStreamQueries(sqlContext);

        assertEquals(application.getStreamingQueriesCount(),3);
    }
}
