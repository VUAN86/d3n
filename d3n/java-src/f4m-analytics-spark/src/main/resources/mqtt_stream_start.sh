#!/bin/bash
echo "Starting AnalyticsMQTTStreamApplication"
echo "User is ${USER}"
echo "Username is ${USERNAME}"

spark-submit --class de.ascendro.f4m.spark.analytics.AnalyticsMQTTStreamApplication \
--packages com.google.guava:guava:22.0,org.apache.bahir:spark-sql-streaming-mqtt_2.11:2.1.0,com.google.code.gson:gson:2.8.0 \
--properties-file F4M.conf   \
--master local[4] \
--jars f4m-analytics-lib-0.0.1-SNAPSHOT.jar \
f4m-analytics-spark-0.0.1-SNAPSHOT.jar