# The Avian Factions server as one image: Paper, the pinned plugin stack, our plugin and the tracked
# config. Everything that changes while the game runs (worlds, plugin data, logs) lives in /data, a
# volume, so the same image runs on this machine, another machine or Kubernetes.
# Build context: build/image, staged by `./gradlew stageImage`; `./dev image` does both steps.
# Guide: docs/DEPLOYMENT.md.
FROM eclipse-temurin:25-jre@sha256:8da0490fa9a3c26867012019565948eef0ee69438f5c75ac28146967bae984b5

# restic (pinned, checksum checked) for the backup sidecar, bin/avian-backup.
ADD --checksum=sha256:f415415624dcc452f2a02b8c33641791a8c6d6d3b65bbb3543fcf9a25151585c \
    https://github.com/restic/restic/releases/download/v0.19.1/restic_0.19.1_linux_amd64.bz2 /tmp/restic.bz2

# unzip: tools/sync-config unpacks CraftEngine's bundled packs on a fresh server.
# mariadb-client: the backup sidecar's database dump. bzip2: unpacks restic.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip mariadb-client bzip2 \
    && bunzip2 -c /tmp/restic.bz2 > /usr/local/bin/restic && chmod 0755 /usr/local/bin/restic && rm /tmp/restic.bz2 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 1001 --user-group --home-dir /data --no-create-home avian \
    && mkdir /data && chown avian:avian /data

COPY --chown=root:root . /opt/avian/
# Readable by the server user whatever mode a file had in the build context (a 0600 file once broke start-up).
RUN chmod -R a+rX /opt/avian && chmod 0755 /opt/avian/bin/* && ln -s /opt/avian/bin/avian-console /usr/local/bin/avian-console \
    && ln -s /opt/avian/bin/avian-backup /usr/local/bin/avian-backup

# Never root: a Paper or plugin exploit stays inside this user and this container.
USER avian
WORKDIR /data
VOLUME /data
# Java on TCP, Bedrock (Geyser) on UDP.
EXPOSE 25565/tcp 19132/udp
ENV MEMORY=4G
ENTRYPOINT ["/opt/avian/bin/entrypoint"]
