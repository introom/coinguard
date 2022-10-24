FROM debian:testing

ARG DATOMIC_VERSION=1.0.6242	

ARG USERNAME=i@introo.me
ARG PASSWORD=71ff9720-0a6d-424e-ba0c-a113f4064aed

RUN apt-get update

RUN apt-get install -y curl

ARG DATOMIC_DIST=datomic-pro-${DATOMIC_VERSION}
RUN curl -L -u ${USERNAME}:${PASSWORD} \
    https://my.datomic.com/repo/com/datomic/datomic-pro/${DATOMIC_VERSION}/${DATOMIC_DIST}.zip -o /var/tmp/datomic.zip

RUN apt-get install unzip

RUN unzip /var/tmp/datomic.zip -d /opt && mv /opt/${DATOMIC_DIST} /opt/datomic

WORKDIR /opt/datomic

RUN apt-get install -y default-jdk-headless

COPY transactor.properties /opt/datomic/config/

COPY start.sh start.clj /opt/datomic/

CMD bash start.sh
