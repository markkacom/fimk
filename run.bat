@ECHO OFF
IF EXIST java (
	start "FIM NRS" java -cp fim.jar;lib\*;conf nxt.Nxt
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\jre7" (
		start "FIM NRS" "%PROGRAMFILES%\Java\jre7\bin\java.exe" -cp fim.jar;lib\*;conf nxt.Nxt
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Java\jre7" (
			start "FIM NRS" "%PROGRAMFILES(X86)%\Java\jre7\bin\java.exe" -cp fim.jar;lib\*;conf nxt.Nxt
		) ELSE (
			ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
			PAUSE
		)
	)
)

