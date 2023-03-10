FROM docker.toolforge.io/public/ubuntu:22.04

RUN apt-get update \
    && apt-get -y -q --no-install-recommends install openjdk-17-jre-headless

WORKDIR /root

COPY target/ngrams-tool.jar /root/
COPY manifest.yml /toolforge/manifest.yml

ENTRYPOINT [ "/usr/bin/java", "-jar", "ngrams-tool.jar" ]
