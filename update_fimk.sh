#!/bin/bash
FIMK_VERSION="$1"
CHECKSUM="$2"
FILE_NAME="fim-$FIMK_VERSION.zip"
ZIP_URL="https://github.com/fimkrypto/fimk/releases/download/v$FIMK_VERSION/$FILE_NAME"

printf "FIMK_VERSION=$FIMK_VERSION\n"
printf "CHECKSUM=$CHECKSUM\n"
printf "\n"
printf "        IMPORTANT! YOU MUST STOP/QUIT FIMK SERVER BEFORE RUNNING THIS SCRIPT\n"
printf "\n"
printf "\n"
printf "                      ███████╗██╗███╗   ███╗██╗  ██╗\n"
printf "                      ██╔════╝██║████╗ ████║██║ ██╔╝\n"
printf "                      █████╗  ██║██╔████╔██║█████╔╝\n"
printf "                      ██╔══╝  ██║██║╚██╔╝██║██╔═██╗\n"
printf "                      ██║     ██║██║ ╚═╝ ██║██║  ██╗\n"
printf "                      ╚═╝     ╚═╝╚═╝     ╚═╝╚═╝  ╚═╝\n"
printf "             ██╗   ██╗██████╗ ██████╗  █████╗ ████████╗███████╗██████╗\n"
printf "             ██║   ██║██╔══██╗██╔══██╗██╔══██╗╚══██╔══╝██╔════╝██╔══██╗\n"
printf "             ██║   ██║██████╔╝██║  ██║███████║   ██║   █████╗  ██████╔╝\n"
printf "             ██║   ██║██╔═══╝ ██║  ██║██╔══██║   ██║   ██╔══╝  ██╔══██╗\n"
printf "             ╚██████╔╝██║     ██████╔╝██║  ██║   ██║   ███████╗██║  ██║\n"
printf "              ╚═════╝ ╚═╝     ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝\n"
printf "\n"
printf "\n"
printf "We will download FIMK version $FIMK_VERSION from Github and verify its integrity\n"
printf "by testing if its SHA256 HASH matches the provided CHECKSUM\n"
printf "\n"
printf "When the integrity is established the downloaded version will be installed, your\n"
printf "blockchain will be left in tact.\n"
printf "\n"
printf "It could be an update requires either a 'rescan' or a 'validation' of the\n"
printf "blockchain this process might take a while.\n\n\n"

printf "Download $ZIP_URL\n\n"

curl -L -k -O "$ZIP_URL"

printf "\nDownload COMPLETE\n\n"

printf "Verifying file integrity ... "

SHA_SUM=`sha256sum "$FILE_NAME" | head -c 64`

if [ "$SHA_SUM" != "$CHECKSUM" ] 
then
    printf "FAILED\n\n"
    printf "################################################################################\n"
    printf "## \n"
    printf "##                    WARNING! COULD NOT VERIFY FILE INTEGRITY\n"
    printf "## \n"
    printf "## Expected checksum\n"
    printf "## $CHECKSUM\n"
    printf "## \n"
    printf "## $FILE_NAME checksum\n"
    printf "## $SHA_SUM\n"
    printf "## \n"
    printf "## It appears the downloaded file does not match the checksum you provided.\n"
    printf "## Please make sure you provided the correct checksum, checksums should always\n"
    printf "## be 64 characters long.\n"
    printf "## \n"
    printf "## To verify you provided the correct checksum visit the following URL and look\n"
    printf "## for the value next to 'SHA256'.\n"
    printf "## \n"
    printf "## https://github.com/fimkrypto/fimk/releases/tag/v$FIMK_VERSION\n"
    printf "## \n"
    printf "################################################################################\n"
    exit 1
fi

printf "VERIFIED!\n\n"

printf "Removing libraries ... "
rm -f -r lib
printf "DONE!\n\n"

printf "Unpacking files ... "
unzip -q -o "$FILE_NAME"
printf "DONE!\n\n\n"

printf "Congratulations you successfully updated to FIMK $FIMK_VERSION.\n\n"