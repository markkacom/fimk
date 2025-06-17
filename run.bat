@ECHO OFF
IF EXIST %JAVA_HOME% (
    %JAVA_HOME%\bin\java.exe -server -cp %~dp0\lib\*;%~dp0\conf fimk.Starter -Dnxt.runtime.mode=desktop fimk.Starter
) ELSE (
    IF EXIST java (
        java -server -cp lib\*;conf fimk.Starter -Dnxt.runtime.mode=desktop fimk.Starter
    ) ELSE (
        ECHO Java software not found on your system, environment variable JAVA_HOME is not defined.
        PAUSE
    )
)
