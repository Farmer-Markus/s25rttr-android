#!/bin/bash

export PATH=$HOME/Android/Sdk:$HOME/Android/Sdk/ndk/27.2.12479018:$HOME/Android/Sdk/platform-tools:$PATH
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"

./gradlew $1 $2 $3 $4 $5
