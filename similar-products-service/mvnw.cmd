:::

@echo off
REM Maven wrapper script for Windows

SET MAVEN_HOME=%~dp0.mvn\wrapper
SET MAVEN_OPTS=-Xmx512m

IF NOT EXIST "%MAVEN_HOME%\maven-wrapper.jar" (
    echo Maven wrapper not found. Please run 'mvn -N io.takari:maven:wrapper' to generate it.
    exit /b 1
)

java -cp "%MAVEN_HOME%\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*