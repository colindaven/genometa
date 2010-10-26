
REM This is an EXAMPLE of a shell script to run igb from jar files
REM on a Windows system.
REM You may need to edit it for your system.
REM IGB REQUIRES JAVA 1.6 or higher

REM This file assumes you have the 'java' program in your PATH.
REM If not, go to http://java.com and verify your installation.

java -mx1024m -classpath "igb_exe.jar" "com.affymetrix.igb.IGB"

