@echo off
setlocal

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6\bin

if not exist "%MAVEN_HOME%\mvn.cmd" (
    echo Downloading Maven 3.9.6...
    if not exist "%USERPROFILE%\.m2\wrapper" mkdir "%USERPROFILE%\.m2\wrapper"
    
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%USERPROFILE%\.m2\wrapper\maven.zip'}"
    
    powershell -Command "& {Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force}"
    
    del "%USERPROFILE%\.m2\wrapper\maven.zip"
)

set JAVA_HOME=C:\Users\Admin\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%;%PATH%

mvn.cmd %*
