FROM debian:unstable

ARG DEBIAN_FRONTEND=noninteractive

ARG NODE_VERSION=v16.13.0 \
    CLOJURE_VERSION=1.10.3.967 \
    CLJKONDO_VERSION=2021.09.15 \
    BABASHKA_VERSION=0.6.6 

ENV LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8
    
RUN apt-get update

RUN apt-get -y install --no-install-recommends \
        locales \
    ; \
    echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen; \
    locale-gen;

RUN apt-get -y install --no-install-recommends \
        build-essential \
        gnupg2 \
        ca-certificates \
        wget \
        sudo \
        tmux \
        vim-nox \
        curl \
        bash \
        git \
        rlwrap \
        unzip \
        fakeroot \
        socat \
        jq \
        redis-tools \
        python3-pip \
    ;

RUN useradd -m -g users -s /bin/bash firepanda; \
    passwd firepanda -d; \
    echo "firepanda ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# jdk
RUN curl -LfsSo /tmp/openjdk.tar.gz https://github.com/AdoptOpenJDK/openjdk16-binaries/releases/download/jdk-16.0.1%2B9/OpenJDK16U-jdk_x64_linux_hotspot_16.0.1_9.tar.gz; \
    mkdir -p /usr/lib/jvm/openjdk16; \
    cd /usr/lib/jvm/openjdk16; \
    tar -xf /tmp/openjdk.tar.gz --strip-components=1; \
    rm -rf /tmp/openjdk.tar.gz;

ENV PATH="/usr/lib/jvm/openjdk16/bin:$PATH" \
    JAVA_HOME=/usr/lib/jvm/openjdk16

# clojure
RUN curl -LfsSo /tmp/clojure.sh https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh; \
    chmod +x /tmp/clojure.sh; \
    /tmp/clojure.sh; \
    rm -rf /tmp/clojure.sh;

# clj-kondo
RUN curl -LfsSo /tmp/clj-kondo.zip https://github.com/borkdude/clj-kondo/releases/download/v$CLJKONDO_VERSION/clj-kondo-$CLJKONDO_VERSION-linux-amd64.zip; \
    cd /usr/local/bin; \
    unzip /tmp/clj-kondo.zip; \
    rm /tmp/clj-kondo.zip;

# babashka
RUN curl -LfsSo /tmp/babashka.tar.gz https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz; \
    cd /usr/local/bin; \
    tar -xf /tmp/babashka.tar.gz; \
    rm -rf /tmp/babashka.tar.gz;

# nodejs
RUN curl -LfsSo /tmp/nodejs.tar.xz https://nodejs.org/dist/$NODE_VERSION/node-$NODE_VERSION-linux-x64.tar.xz; \
    mkdir -p /usr/local/nodejs; \
    cd /usr/local/nodejs; \
    tar -xf /tmp/nodejs.tar.xz --strip-components=1; \
    chown -R root /usr/local/nodejs; \
    rm -rf /tmp/nodejs.tar.xz;
ENV PATH="/usr/local/nodejs/bin:$PATH"

# postgres
RUN curl https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -; \
    echo "deb http://apt.postgresql.org/pub/repos/apt focal-pgdg main" >> /etc/apt/sources.list.d/postgresql.list; \
    apt-get update; \
    apt-get -y install postgresql-client-13;

WORKDIR /home/firepanda