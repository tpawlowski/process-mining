#!/bin/bash
# Kafka quick setup + stream example execution
# Based on: 
#   https://kafka.apache.org/quickstart
#   https://kafka.apache.org/10/documentation/streams/tutorial

SCALA_VERSION="2.11"
KAFKA_VERSION="1.0.0"

directory="kafka_$SCALA_VERSION-$KAFKA_VERSION"
binary_file="$directory.tgz"

if [[ -f "$binary_file" ]]; then
  echo "Using existing $binary_file"
else
  source_url="http://www-eu.apache.org/dist/kafka/$KAFKA_VERSION/$binary_file"
  echo -n "Downloading $binary_file from $source_url"
  curl "$source_url" > "$binary_file" 2>/dev/null
  if [[ "$(echo $?)" = "0" ]]; then
    echo " done"
  else
    echo " fail"
    exit 1
  fi
fi

if [[ -f "$directory" || -d "$directory" ]]; then
  echo -n "Removing existing $directory"
  rm -rf "$directory"
  echo " done"
fi

echo -n "Unpacking $binary_file"
tar -xzf "$binary_file"
echo " done"

cd "$directory"
trap "exit" INT TERM ERR

echo -n "Resetting data directory"
rm -rf "/tmp/zookeeper"
rm -rf "/tmp/kafka-streams"
rm -rf "/tmp/kafka-logs"
echo " done"

echo -n "Starting ZooKeeper"
mkdir -p logs
./bin/zookeeper-server-start.sh config/zookeeper.properties >logs/zookeeper-server.log 2>&1 &
zookeeper_pid=$!
echo " done with pid $zookeeper_pid"

stop_zookeeper () { 
  echo -n "Stopping ZooKeeper($zookeeper_pid)"
  kill -TERM "$zookeeper_pid"
  wait "$zookeeper_pid"
  echo " done"
}
trap stop_zookeeper EXIT

sleep 5
echo -n "Starting Kafka"
./bin/kafka-server-start.sh config/server.properties >logs/kafka-server.log 2>&1 &
kafka_pid=$!
echo " done with pid $kafka_pid"

stop_kafka () {
  echo -n "Stopping kafka($kafka_pid)"
  kill -TERM "$kafka_pid"
  wait "$kafka_pid"
  echo " done"
  stop_zookeeper
} 
trap stop_kafka EXIT

sleep 5
./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic logs-input
./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic logs-debug
./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic logs-petri
./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic logs-images

echo "Press Ctrl+C to stop the server"
wait
