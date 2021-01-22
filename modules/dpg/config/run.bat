@echo off
set DIR=%~dp0

set LOCALCLASSPATH="%DIR%\dpg.jar"

for %%i in ("%DIR%\lib\*.jar") do call "%DIR%\lcp.bat" "%%i"

if not "%JAVA%" == "" goto done
if not "%JAVA_HOME%" == "" goto :javaHome 
echo JAVA and JAVA_HOME is not set
exit /B 1
:javaHome
set JAVA=%JAVA_HOME%\bin\java
:done

%JAVA% %JAVA_OPTS% -cp %LOCALCLASSPATH% org.javelus.dpg.DynamicPatchGenerator  %*

