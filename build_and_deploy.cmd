@echo off

:: This is a simple script to build and copy the JAR file to the right target.
:: This was really only intended to be run on my own machine.

:: Use "--offline" so that it doesn't fetch a million dependencies for no reason
call mvn clean install --offline
copy /y .\target\animalroyale-1.0-SNAPSHOT.jar ..\PaperSpigot\plugins\

echo "Don't forget to do '/rl confirm' from Minecraft!"
