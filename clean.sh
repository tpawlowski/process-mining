#!/bin/bash

rm -rf kafka_2.11-1.0.0 kafka_2.11-1.0.0.tgz


cd Workshop
./clean.sh
cd -

cd kafkapngconsumer
mvn clean
cd -
