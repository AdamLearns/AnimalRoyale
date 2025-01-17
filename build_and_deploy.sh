#!/bin/sh

# This is a simple script to build and copy the JAR file to the right target.
# This was really only intended to be run on my own machine.

# Use "--offline" so that it doesn't fetch a million dependencies for no reason
mvn clean install --offline
/bin/cp -f ./target/animalroyale-1.0-SNAPSHOT.jar /Users/adam/Downloads/paper/plugins

echo "Don't forget to do '/rl confirm' from Minecraft!"
