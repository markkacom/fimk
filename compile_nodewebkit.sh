# ##############################################################################
#
# This script compiles the FIM Community Client source code for the various 
# supported nodewebkit platforms.
# 
# Supported platforms:
#
#   1. Windows (fim.win.zip)
#   2. Linux   (fim.lin.zip
#   3. MacOSX  (not implemented)
#
# Requirements:
#
#   1. Compiled FIMK jar files (run compile.sh first)
#   2. Installed grunt
#   3. Installed grunt nodewebkit
#
# Usage:
#
#   Usage is straightforward, just run this script and you'll end up with 
#   executables for all supported platforms. All platforms come with various 
#   helper libraries that for the embedded webkit libraries, these libraries 
#   (dll, so) are packaged together with the FIM Community Client source in 
#   a directory. That directory is then zipper. 
#
# ##############################################################################

cd fimui && grunt nodewebkit
cd ..

echo "nodewebkit generated successfully"

FIM_FILES="conf/nxt-default.properties conf/logging-default.properties html/ lib/ logs/ fim.jar MIT-license.txt README.txt run.bat run.sh"

# ==============================================================================
# Linux
# ==============================================================================

/bin/rm -f fim.zip
zip -qr -9 fim.zip $FIM_FILES

NW_LINUX="fimui/dist/releases/fimkrypto/linux32/fimkrypto"
F1=fimkrypto
F2=libffmpegsumo.so
F3=nw.pak

cp $NW_LINUX/$F1 .
cp $NW_LINUX/$F2 .
cp $NW_LINUX/$F3 .

zip -g fim.zip $F1 $F2 $F3

/bin/rm -f $F1 $F2 $F3

/bin/rm -rf dist
mkdir dist
unzip fim.zip -d dist

cp fim.zip fim.linux.zip
echo "fim.linux.zip generated successfully"
/bin/rm -f fim.zip

# ==============================================================================
# Windows
# ==============================================================================

zip -qr -9 fim.zip $FIM_FILES

NW_WINDOWS="fimui/dist/releases/fimkrypto/win/fimkrypto"
F1=ffmpegsumo.dll
F2=fimkrypto.exe
F3=icudt.dll
F4=libEGL.dll
F5=libGLESv2.dll
F6=nw.pak

cp $NW_WINDOWS/$F1 .
cp $NW_WINDOWS/$F2 .
cp $NW_WINDOWS/$F3 .
cp $NW_WINDOWS/$F4 .
cp $NW_WINDOWS/$F5 .
cp $NW_WINDOWS/$F6 .

zip -g fim.zip $F1 $F2 $F3 $F4 $F5 $F6

/bin/rm -f $F1 $F1 $F2 $F3 $F4 $F5 $F6

cp fim.zip fim.win.zip
echo "fim.win.zip generated successfully"
/bin/rm -f fim.zip
