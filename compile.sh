#!/bin/sh
CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath ${SP} -classpath ${CP} -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java || exit 1

/bin/rm -f fim.jar 
jar cf fim.jar -C classes . || exit 1
/bin/rm -rf classes

echo "fim.jar generated successfully"

if [ ! -d "html/fimk" ]; then
  cd ./../mofowallet
  sh ./compile4server.sh
  cd ./../fimk
  cp ./../mofowallet/dist/. ./html/fimk -R
fi

/bin/rm -f fim.zip
zip -qr -9 fim.zip conf/nxt-default.properties conf/logging-default.properties html/ lib/ logs/ fim.jar LICENSE.txt README.txt run.bat run.sh update_fimk.sh

echo "fim.zip generated successfully"

# ==============================================================================
# Package it all up
# ==============================================================================

VERSION=0.6.4
BASE=fim
DATE=`date +%Y-%m-%d`

TARGET="$BASE-$VERSION.zip"
cp fim.zip $TARGET

# The version number is used to pick up the changelog which describes the release
CHANGELOG="./changelogs-fim/fim-$VERSION.changelog.txt"
ANNOUNCEMENT="./changelogs-fim/announcement-fim-$VERSION.txt"

rm -f $ANNOUNCEMENT

SHA_SUM=`sha256sum "$TARGET"`
MD5_SUM=`md5sum "$TARGET"`
SHA_SUM=${SHA_SUM%\ *}
MD5_SUM=${MD5_SUM%\ *}

BANNER=$(cat <<'END_HEREDOC'
███████╗██╗███╗   ███╗██╗  ██╗           Date    : #DATE#
██╔════╝██║████╗ ████║██║ ██╔╝           Release : #VERSION#
█████╗  ██║██╔████╔██║█████╔╝
██╔══╝  ██║██║╚██╔╝██║██╔═██╗            http://fimk.fi
██║     ██║██║ ╚═╝ ██║██║  ██╗           https://github.com/fimkrypto/fimk
╚═╝     ╚═╝╚═╝     ╚═╝╚═╝  ╚═╝           https://lompsa.com [online wallet]
END_HEREDOC
)

cat > $ANNOUNCEMENT <<EOF
$BANNER

`cat $CHANGELOG`

                             ~~~ DOWNLOAD ~~~

https://github.com/fimkrypto/fimk/releases/download/v$VERSION/$TARGET
 
SHA256 $SHA_SUM
MD5    $MD5_SUM

EOF

orig=#VERSION#
sed -i "s/${orig}/${VERSION}/g" $ANNOUNCEMENT

orig=#DATE#
sed -i "s/${orig}/${DATE}/g" $ANNOUNCEMENT

gpg --clearsign --batch --default-key AC34E2D5 $ANNOUNCEMENT
mv $ANNOUNCEMENT.asc $ANNOUNCEMENT

echo "========================================================================="
echo "== Successfully generated new version"
echo "=="
echo "=="
echo "== Checklist.."
echo "=="
echo "== 1. Did you update the version number in README.txt ?"
echo "== 2. Did you update the version number in index.html ?"
echo "=="
echo "== Final actions.."  
echo "=="
echo "== 1. Github release: https://github.com/fimkrypto/mofowallet/releases/tag/v$VERSION"
echo "=="
echo "=="
echo "========================================================================="
