JAR=$(ls target/traffic-demo-*-dependencies.jar)

if [ "$JAVA" = "" ]; then
 JAVA=java
fi

if [ ! -f "$JAR" ]; then
  mvn -DskipTests=true install assembly:single
  JAR=$(ls target/traffic-demo-*-dependencies.jar)
fi

exec "$JAVA" $JAVA_OPTS -jar $JAR "$@"