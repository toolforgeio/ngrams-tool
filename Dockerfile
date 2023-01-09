FROM public.ecr.aws/lts/ubuntu:latest

RUN apt-get update \
    && apt-get -y -q --no-install-recommends install openjdk-17-jre-headless

WORKDIR /root

COPY target/toolforge-ngrams-tool.jar /root/
COPY manifest.yml /toolforge/manifest.yml

ENTRYPOINT [ "/usr/bin/java", "-jar", "toolforge-ngrams-tool.jar" ]
