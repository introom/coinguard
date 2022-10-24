# refer to this: https://github.com/xtdb/xtdb/tree/master/build/docker

FROM clojure:openjdk-11-tools-deps

WORKDIR /opt/xtdb

ADD deps.edn deps.edn
ADD resources/logback.xml resources/logback.xml

# download libs
RUN clojure -P

ADD xtdb.edn xtdb.edn

# https://stackoverflow.com/a/58248523/855160
# these are anonymous volumes.  they get automatically removed.
VOLUME /opt/xtdb/data

ENV MALLOC_ARENA_MAX 2
CMD clojure -M -m xtdb.main
