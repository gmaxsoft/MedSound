@ECHO OFF
@rem Medsound Gradle startup script for Windows.
@rem Prefer opening the project in Android Studio, which will sync and fetch the wrapper JAR if missing.

SETLOCAL

SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%

SET DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

IF DEFINED JAVA_HOME (
  SET JAVA_EXE=%JAVA_HOME%/bin/java.exe
) ELSE (
  SET JAVA_EXE=java.exe
)

SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO gradle-wrapper.jar is missing.
  ECHO Open this project in Android Studio once to generate the Gradle wrapper, or run:
  ECHO   gradle wrapper --gradle-version 8.12.1
  EXIT /B 1
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
EXIT /B %ERRORLEVEL%
