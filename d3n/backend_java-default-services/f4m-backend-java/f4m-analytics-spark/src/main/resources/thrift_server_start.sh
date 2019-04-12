#!/bin/bash
echo "Starting ThriftServerApplication"
echo "User is ${USER}"
echo "Username is ${USERNAME}"

spark-submit --class de.ascendro.f4m.spark.analytics.ThriftServerApplication \
--packages org.apache.bahir:spark-sql-streaming-mqtt_2.11:2.1.0,com.google.code.gson:gson:2.8.0 \
--properties-file F4M.conf   \
--master local[4] \
--driver-java-options "-Djava.library.path=/usr/lib/hadoop-lzo/lib/native" \
--driver-library-path /usr/lib/hadoop-lzo/lib/hadoop-lzo-0.4.19.jar \
--conf spark.executor.extraLibraryPath=/usr/lib/hadoop-lzo/lib/hadoop-lzo-0.4.19.jar \
--jars f4m-analytics-lib-0.0.1-SNAPSHOT.jar \
f4m-analytics-spark-0.0.1-SNAPSHOT.jar
