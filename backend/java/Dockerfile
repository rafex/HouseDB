FROM docker.io/eclipse-temurin:25.0.2_10-jre

ENV DEBIAN_FRONTEND=noninteractive

ARG HOUSEDB_UID=10001
ARG HOUSEDB_GID=10001

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd -g ${HOUSEDB_GID} housedb \
  && useradd -u ${HOUSEDB_UID} -g ${HOUSEDB_GID} -m -s /usr/sbin/nologin housedb

WORKDIR /app

RUN mkdir -p /app/glowroot /app/glowroot/tmp /app/glowroot/logs /app/glowroot/data
COPY observability/glowroot /app/glowroot
# Avoid shipping local runtime state from developer machine.
RUN rm -rf /app/glowroot/logs/* /app/glowroot/tmp/* /app/glowroot/data/* || true

COPY housedb-parent/housedb-transport-jetty/target/housedb-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar
COPY start-housedb.sh /usr/local/bin/start-housedb.sh

RUN chmod +x /usr/local/bin/start-housedb.sh \
  && mkdir -p /etc/housedb-env \
  && chown -R housedb:housedb /app /etc/housedb-env /usr/local/bin/start-housedb.sh

ENV DB_USER=""
ENV DB_PASSWORD=""
ENV DB_URL=""

EXPOSE 8080
EXPOSE 4000

USER 10001:10001

ENTRYPOINT ["/usr/local/bin/start-housedb.sh"]
