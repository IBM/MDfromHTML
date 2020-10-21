FROM openliberty/open-liberty:kernel-java8-openj9-ubi

ARG VERSION=1.0.6
ARG REVISION=RELEASE

LABEL \
  org.opencontainers.image.authors="Nathaniel Mills" \
  org.opencontainers.image.vendor="IBM" \
  org.opencontainers.image.url="local" \
  org.opencontainers.image.source="https://github.com/IBM/MDfromHTML/tree/master/MDfromHTMLWebServices" \
  org.opencontainers.image.version="$VERSION" \
  org.opencontainers.image.revision="$REVISION" \
  vendor="IBM" \
  name="MDfromHTMLWebServices" \
  version="$VERSION-$REVISION" \
  summary="MDfromHTMLWebServices" \
  description="This image contains the Open Source MDfromHTMLWebServices microservice running with the Open Liberty runtime."

COPY --chown=1001:0 MDfromHTMLWebServices/server.xml /config/
COPY --chown=1001:0 MDfromHTMLWebServices/target/*.war /config/dropins/
COPY --chown=1001:0 MDfromHTMLWebServices/properties/  /opt/ol/wlp/output/defaultServer/properties/
COPY --chown=1001:0 MarkdownGenerator/data/  /opt/ol/wlp/output/defaultServer/data/
COPY --chown=1001:0 MarkdownGenerator/properties/  /opt/ol/wlp/output/defaultServer/properties/

ENV AIDEN_HOME=/opt/ol/wlp/output/defaultServer

RUN configure.sh
