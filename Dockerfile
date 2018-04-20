FROM openjdk:8-jre-alpine
LABEL maintainer="Beekeeper AG <admin@beekeeper.io>"

LABEL Description="Meeting Bot"
RUN apk add --update bash curl unzip && rm -rf /var/cache/apk/*

ENV "BKPR_SERVER_DOCKER_MODE"="true"

ADD build/distributions/bkpr-meet-o-matic-*.tar /opt/bkpr/
RUN mv /opt/bkpr/bkpr-meet-o-matic-* /opt/bkpr/service/

RUN curl -q -L -C - -b "oraclelicense=accept-securebackup-cookie" -o /tmp/jce_policy-8.zip -O http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
    && unzip -oj -d /usr/lib/jvm/default-jvm/jre/lib/security /tmp/jce_policy-8.zip \*/\*.jar \
    && rm /tmp/jce_policy-8.zip

EXPOSE 9000
CMD ["server"]
ENTRYPOINT ["/opt/bkpr/service/bin/bkpr-meet-o-matic"]
