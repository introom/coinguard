#!/bin/bash

java -cp `bin/classpath` clojure.main -i start.clj

# make the exit status to be 0
true
