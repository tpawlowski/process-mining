#!/bin/sh
CP='./dist/Workshop.jar'
MAIN=org.processmining.contexts.cli.CLI
for libdir in lib stdlib ivy; do
  for lib in ./$libdir/*.jar; do
	  CP=${CP}:$lib
  done
done  
#echo "$CP"

started='0'

java -da -Xmx4G -XX:MaxPermSize=256m -classpath ${CP} -Djava.util.Arrays.useLegacyMergeSort=true -DsuppressSwingDropSupport=false ${MAIN} $1 $2 2>/dev/null | while read line
do
  if [[ "$line" =~ '>>> Scanning for plugins took'* ]]; then
    started='1'
  fi
  if [[ "$started" = "1" ]]; then
    echo "$line"
  fi
  if [[ "$line" =~ '>>> Total startup took'* ]]; then
    started='0'
    pkill -P $$
  fi
done
