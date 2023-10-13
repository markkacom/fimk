#!/bin/bash
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=fimk-client-${VERSION}
echo PACKAGE="${PACKAGE}"
CHANGELOG=fimk-client-${VERSION}.changelog.txt
OBFUSCATE=$2

FILES="changelogs conf html lib resource contrib logs"
FILES="${FILES} fimk.exe fimkservice.exe"
#FILES="${FILES} 3RD-PARTY-LICENSES.txt AUTHORS.txt COPYING.txt DEVELOPER-AGREEMENT.txt LICENSE.txt"
#FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh"
#FILES="${FILES} fimk_Wallet.url"

# unix2dos *.bat
echo compile
./win-compile.sh
rm -rf html/doc/*
rm -rf fimk
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p fimk/
mkdir -p fimk/logs

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo obfuscate
proguard.bat @fimk.pro
mv ../fimk.map ../fimk.map.${VERSION}
mkdir -p fimk/src/
else
FILES="${FILES} classes src"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
#echo javadoc
#./win-javadoc.sh
fi
echo copy resources
cp installer/lib/JavaExe.exe fimk.exe
cp installer/lib/JavaExe.exe fimkservice.exe
cp -a ${FILES} fimk
echo gzip
for f in `find fimk/html -name *.html -o -name *.js -o -name *.css -o -name *.json  -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
cd fimk
echo generate jar files
../jar.sh
echo package installer Jar
../installer/build-installer.sh ../${PACKAGE}
echo create installer exe
../installer/build-exe.bat ${PACKAGE}
echo create installer zip
cd -
zip -q -X -r ${PACKAGE}.zip fimk -x \*/.idea/\* \*/.gitignore \*/.git/\* \*.iml fimk/conf/nxt.properties fimk/conf/logging.properties
rm -rf fimk

echo creating change log ${CHANGELOG}
# echo -e "Release $1\n" > ${CHANGELOG}
# echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.exe\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.exe >> ${CHANGELOG}

# echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.jar\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.jar >> ${CHANGELOG}

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo -e "\n\nThis is a development release for testing only. Source code is not provided." >> ${CHANGELOG}
fi
echo -e "\n\nChange log:\n" >> ${CHANGELOG}

cat changelogs/${CHANGELOG} >> ${CHANGELOG}
echo >> ${CHANGELOG}
