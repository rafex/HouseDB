FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./housedb-parent/mvnw -q -f housedb-parent/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /opt/housedb
COPY --from=build /app/housedb-parent/housedb-bootstrap/target/housedb-bootstrap-0.1.0-SNAPSHOT.jar /opt/housedb/housedb-bootstrap.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/housedb/housedb-bootstrap.jar"]
