FROM --platform=linux/amd64 debian:testing

ARG DEBIAN_FRONTEND=noninteractive

ARG DEBCONF_NOWARNINGS=yes

RUN echo "deb http://deb.debian.org/debian/ testing main non-free contrib" > /etc/apt/sources.list
RUN apt-get update; apt-get -y upgrade; apt-get -y install locales tzdata vim-nox curl

# locale
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
ENV LANG en_US.UTF-8  
ENV LANGUAGE en_US.UTF-8
ENV LC_ALL en_US.UTF-8     

RUN mkdir -p /opt/fpl
WORKDIR /opt/fpl
RUN curl https://gethstore.blob.core.windows.net/builds/geth-linux-amd64-1.10.16-20356e57.tar.gz -o geth.tar.gz; \
    tar -xf geth.tar.gz --strip-components=1; rm geth.tar.gz;

CMD ["tail", "-f", "/dev/null"]
