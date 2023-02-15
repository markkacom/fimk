javac -sourcepath "./src/java/" -cp "lib/*:conf/:classes/" -d classes/ ./src/java/fimk/*.java

del fim.jar

jar cf fim.jar -C classes .
