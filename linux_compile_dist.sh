javac -sourcepath "./src/java/" -cp "lib/*:conf/:classes/" -d classes/ ./src/java/fimk/*.java

rm fim.jar

jar cf fim.jar -C classes .
