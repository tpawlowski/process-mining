#!/bin/sh
CP='./dist/Workshop.jar'
MAIN=org.processmining.contexts.uitopia.UI
for libdir in stdlib ivy; do
  for lib in ./$libdir/*.jar; do
	  CP=${CP}:$lib
  done
done  
#echo "$CP"
java -da -Xmx4G -XX:MaxPermSize=256m -classpath ${CP} -Djava.library.path=${LIBDIR} -Djava.util.Arrays.useLegacyMergeSort=true -DsuppressSwingDropSupport=false ${MAIN}
