#!/bin/sh

CP="conf/;classes/;lib/*"
SP=src/java/

/bin/mkdir -p classes/

# javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java || exit 1
$JAVA_HOME/bin/javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java

/bin/rm -f fim.jar
# jar cf fim.jar -C classes . || exit 1
$JAVA_HOME/bin/jar cf fim.jar -C classes .
/bin/rm -rf classes

echo "fim.jar generated successfully"

/bin/rm -rf dist
mkdir dist
cp html/ lib/ fim.jar keystore LICENSE.txt README.txt run.bat run.sh dist -R
mkdir dist/conf
cp conf/nxt-default.properties conf/logging-default.properties dist/conf -R
mkdir dist/logs
# create not empty logs dir to force electron builder to copy the dir
touch dist/logs/fim.log
/bin/rm -f fim.zip
zip -qr -9 fim.zip conf/nxt-default.properties conf/logging-default.properties html/ lib/ logs/ fim.jar keystore LICENSE.txt README.txt run.bat run.sh

echo "fim.zip generated successfully"

$SHELL