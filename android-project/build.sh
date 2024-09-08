#!/bin/bash

export PATH=/home/markus/Android/Sdk:/home/markus/Android/Sdk/ndk/r26d:/home/markus/Android/Sdk/tools:/home/markus/Android/Sdk/platform-tools:$PATH
export ANDROID_HOME="/home/markus/Android/Sdk"
#export ANDROID_NDK_HOME="/home/markus/Android/Sdk/ndk/r26d"
export JAVA_HOME="/usr/lib/jvm/java-1.17.0-openjdk-amd64"

./gradlew $1 $2 $3 $4 $5
