#!/bin/sh
java -cp bin nxt.tools.ManifestGenerator
/bin/rm -f fim.jar
jar cfm fim.jar resource/nxt.manifest.mf -C bin . || exit 1
/bin/rm -f fimservice.jar
jar cfm fimservice.jar resource/nxtservice.manifest.mf -C bin . || exit 1

echo "jar files generated successfully"