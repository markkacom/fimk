CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java || exit 1

/bin/rm -f fim.jar 
jar cf fim.jar -C classes . || exit 1
/bin/rm -rf classes

echo "fim.jar generated successfully"

/bin/rm -f fim.zip
zip -qr -9 fim.zip conf/nxt-default.properties conf/logging-default.properties html/ lib/ logs/ fim.jar MIT-license.txt README.txt run.bat run.sh

echo "fim.zip generated successfully"
