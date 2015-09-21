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

FILES="changelogs changelogs-fim conf html lib resource contrib"
FILES="${FILES} fim.exe fimservice.exe"
FILES="${FILES} 3RD-PARTY-LICENSES.txt COPYING.txt LICENSE.txt"
FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh"
FILES="${FILES} FIM_Wallet.url"

unix2dos *.bat
echo compile
./compile.sh
rm -rf html/doc/*
rm -rf fimk
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p fimk/
mkdir -p fimk/logs

FILES="${FILES} src"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
#echo javadoc
#./javadoc.sh

echo copy resources
cp installer/lib/JavaExe.exe fim.exe
cp installer/lib/JavaExe.exe fimservice.exe
cp -a ${FILES} fimk

echo generate jar files
./jar.sh
echo package installer Jar
cd fimk
../installer/build-installer.sh ../${PACKAGE}
#echo create installer exe
#../installer/build-exe.bat ${PACKAGE}
echo create installer zip
cd -
zip -q -X -r ${PACKAGE}.zip fimk -x \*/.idea/\* \*/.gitignore \*/.git/\* \*/\*.log \*.iml fimk/conf/nxt.properties fimk/conf/logging.properties
rm -rf fimk

echo signing zip package
../jarsigner.sh ${PACKAGE}.zip

echo signing jar package
../jarsigner.sh ${PACKAGE}.jar

echo creating change log ${CHANGELOG}
echo -e "Release $1\n" > ${CHANGELOG}
echo -e "https://github.com/fimkrypto/fimk/releases/download/v${PACKAGE}/fim-${PACKAGE}.zip\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.zip >> ${CHANGELOG}

echo -e "\nhttps://github.com/fimkrypto/fimk/releases/download/v${PACKAGE}/${PACKAGE}.jar\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.jar >> ${CHANGELOG}

echo -e "https://github.com/fimkrypto/fimk/releases/download/v${PACKAGE}/${PACKAGE}.exe\n" >> ${CHANGELOG}
echo -e "sha256:\n" >> ${CHANGELOG}
sha256sum ${PACKAGE}.exe >> ${CHANGELOG}

echo -e "\n\nChange log:\n" >> ${CHANGELOG}

cat changelogs-fim/${CHANGELOG} >> ${CHANGELOG}
echo >> ${CHANGELOG}

#######################
# Skip signing for now
exit 0
#######################

gpg --detach-sign --armour --sign-with jlp666@yandex.ru ${PACKAGE}.zip
gpg --detach-sign --armour --sign-with jlp666@yandex.ru ${PACKAGE}.jar
#gpg --detach-sign --armour --sign-with jlp666@yandex.ru ${PACKAGE}.exe

gpg --clearsign --sign-with jlp666@yandex.ru ${CHANGELOG}
rm -f ${CHANGELOG}
gpgv ${PACKAGE}.zip.asc ${PACKAGE}.zip
gpgv ${PACKAGE}.jar.asc ${PACKAGE}.jar
#gpgv ${PACKAGE}.exe.asc ${PACKAGE}.exe
gpgv ${CHANGELOG}.asc
sha256sum -c ${CHANGELOG}.asc
jarsigner -verify ${PACKAGE}.zip
jarsigner -verify ${PACKAGE}.jar


