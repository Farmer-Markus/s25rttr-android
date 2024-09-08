#!/bin/bash

export PATH=$HOME/Android/Sdk:$HOME/Android/Sdk/ndk/r26d:$HOME/Android/Sdk/platform-tools:$PATH
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/usr/lib/jvm/java-1.17.0-openjdk-amd64"

./gradlew $1 $2 $3 $4 $5
