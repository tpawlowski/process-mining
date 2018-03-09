#!/bin/bash
# Generates random logs based on description from files in example-logs.
# Sleeps for given delay time (seconds) between each printed line.

delay=$1
if [[ "$delay" == "" ]]; then 
  delay="1"
fi

file_id=0
while true; do
  INPUT_FILE="example-logs/L$((file_id%12+1)).txt"
  beginning_of_day="$(date -j -v+${file_id}d -f '%Y-%m-%d %H:%M:%S' '2018-01-01 00:00:00' '+%s')"
  date_string="$(date -j -v+${file_id}d -f '%Y-%m-%d %H:%M:%S' '2018-01-01 00:00:00' '+%Y-%m-%d')"

  echo >&2 -n "Generating log from file $INPUT_FILE with date $date_string"
  set -o pipefail
  awk -F'[[:space:]]+' '
    BEGIN{
      srand()
    }
    {
      for (i = 1; i <= int($1); i++) {
        p=0;
        for (e = 3; e <= NF; ++e) {
          if ($e != "") {
            p = p + rand() + 0.00001;
            k = p / (NF - 2);
            s = int(k * 86399.0);
            printf("%f%03d,%d_%s:%s.%d.%d,%d000,%s\n", k, e, '$file_id', "'"L$((file_id%12+1)).txt"'", $2, i, '$file_id', '$beginning_of_day' + s, $e);
          }
        }
      }
    }' "$INPUT_FILE" | sort | cut -d',' -f2- | while read line; do
    sleep $delay
    echo "$line"
  done
  if [[ "$?" != "0" ]]; then
    echo >&2 " interrupted"
    break
  fi
  echo >&2 " done"
  file_id=$((file_id+1))
done | ./kafka_2.11-1.0.0/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic logs-input --property "parse.key=true" --property "key.separator=:"  >/dev/null

