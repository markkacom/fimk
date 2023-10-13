#!/bin/sh
$JAVA_HOME/bin/java -cp classes nxt.tools.ManifestGenerator
/bin/rm -f fimk.jar
$JAVA_HOME/bin/jar cfm fimk.jar resource/fimk.manifest.mf -C classes . || exit 1
/bin/rm -f fimkservice.jar
$JAVA_HOME/bin/jar cfm fimkservice.jar resource/fimkservice.manifest.mf -C classes . || exit 1

echo "jar files generated successfully"