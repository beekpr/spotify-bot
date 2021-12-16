FROM quay.io/beekeeper/java:11
LABEL maintainer="Beekeeper AG <admin@beekeeper.io>"

LABEL Description="Meeting Bot"

ENV "BKPR_SERVER_DOCKER_MODE"="true"

ADD build/distributions/bkpr-spotify-to-youtube-bot-*.tar /opt/bkpr/
RUN mv /opt/bkpr/bkpr-spotify-to-youtube-bot-* /opt/bkpr/service/

EXPOSE 9000
CMD ["server"]
ENTRYPOINT ["/opt/bkpr/service/bin/bkpr-spotify-to-youtube-bot"]
