#!/bin/bash

JAR=$(ls target/traffic-demo-*-web-assembly.jar)

if [ "$JAVA" = "" ]; then
 JAVA=java
fi

if [ ! -f "$JAR" ]; then
  mvn -DskipTests=true install assembly:single
  JAR=$(ls target/traffic-demo-*-web-assembly.jar)
fi

exec "$JAVA" $JAVA_OPTS -jar $JAR "$@"