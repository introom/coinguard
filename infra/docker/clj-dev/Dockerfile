FROM debian:testing

ARG DEBIAN_FRONTEND=noninteractive
# see https://qiita.com/haessal/items/0a83fe9fa1ac00ed5ee9
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

# editor
RUN apt-get -y install vim-nox

# utils
RUN apt-get -y install curl git

# install clojure
RUN apt-get -y install clojure
# the shipped cli tool is too old.  use the upstream one
# see https://clojure.org/guides/getting_started#_installation_on_linux
RUN curl https://download.clojure.org/install/linux-install-1.10.3.822.sh | bash

# install bb
RUN curl -sL https://raw.githubusercontent.com/babashka/babashka/master/install | bash
