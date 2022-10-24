FROM debian:testing

ARG DEBIAN_FRONTEND=noninteractive
ARG DEBCONF_NOWARNINGS=yes

RUN echo "deb http://deb.debian.org/debian/ testing main non-free contrib" > /etc/apt/sources.list
RUN apt-get update; apt-get -y upgrade; apt-get -y install locales tzdata

# locale
RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
ENV LANG en_US.UTF-8  
ENV LANGUAGE en_US.UTF-8
ENV LC_ALL en_US.UTF-8     

# location
ENV TZ 'Asia/Hong_Kong'
RUN echo $TZ > /etc/timezone 

# utils
RUN apt-get -y install vim-tiny curl

# we set the build context as the `backend` directory
ARG PROJ_DIR=.

# install clojure
RUN apt-get -y install openjdk-17-jdk

# the shipped cli tool is too old.  use the upstream one
# see https://clojure.org/guides/getting_started#_installation_on_linux
RUN curl -sL https://download.clojure.org/install/linux-install-1.11.1.1113.sh | bash

# install bb
RUN curl -sL https://raw.githubusercontent.com/babashka/babashka/master/install | bash

WORKDIR /opt/app

COPY $PROJ_DIR/deps.edn /opt/app/deps.edn

# prepare dependency
RUN clojure -P

COPY $PROJ_DIR/src /opt/app/src

CMD ["tail", "-f", "/dev/null"]
