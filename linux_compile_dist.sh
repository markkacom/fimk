#!/bin/sh

mkdir classes

javac -sourcepath "./src/java/" -cp "lib/*:conf/:classes/" -d classes/ ./src/java/fimk/*.java

rm fim.jar

jar cf fim.jar -C classes .

rm -r classes

rm -r ./dist

mkdir dist
mkdir dist/conf
mkdir dist/logs
# create not empty logs dir to force electron builder to copy the dir
touch dist/logs/tmp.1
cp -r html dist/html
cp -r lib dist/lib
cp fim.jar dist
cp keystore dist
cp LICENSE.txt dist
cp README.txt dist
cp run.bat dist
cp run.sh dist
cp conf/fimk-default.properties dist/conf
cp conf/embedded-template.properties dist/conf
cp conf/logging-default.properties dist/conf
