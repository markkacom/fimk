mkdir classes

%JAVA_HOME%/bin/javac -sourcepath "src/java/" -cp "lib/*;conf/;classes/" -d classes/ src/java/nxt/*.java

del fim.jar
%JAVA_HOME%/bin/jar cf fim.jar -C classes .
rmdir /s /q classes

echo "fim.jar generated successfully"

rmdir /s /q dist
mkdir dist\conf
xcopy html dist\html /E /I
xcopy lib dist\lib /E /I
copy fim.jar dist /y
copy keystore dist /y
copy LICENSE.txt dist /y
copy README.txt dist /y
copy run.bat dist /y
copy run.sh dist /y
copy conf\nxt-default.properties dist\conf /y
copy conf\logging-default.properties dist\conf /y

echo "dist created successfully"