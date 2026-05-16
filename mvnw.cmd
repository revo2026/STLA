@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://
@echo off

@REM Set title
title %0

set MAVEN_PROJECTBASEDIR=%~dp0

@REM Find java.exe
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
goto error

:execute
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if exist %WRAPPER_JAR% (
    "%JAVA_EXE%" %MAVEN_OPTS% ^
      "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
      -jar %WRAPPER_JAR% %*
) else (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile %WRAPPER_JAR%"
    "%JAVA_EXE%" %MAVEN_OPTS% ^
      "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
      -jar %WRAPPER_JAR% %*
)

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
exit /b %ERROR_CODE%
