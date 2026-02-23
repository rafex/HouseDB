FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./housedb-parent/mvnw -q -f housedb-parent/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /opt/housedb
COPY --from=build /app/housedb-parent/housedb-transport-jetty/target/housedb-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar /opt/housedb/housedb-server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/housedb/housedb-server.jar"]
