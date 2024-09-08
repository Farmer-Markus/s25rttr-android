# s25rttr-android
Unofficial Android port of [s25client](https://github.com/Return-To-The-Roots/s25client).

I don't have any experience with android/java programming and much of the project is just a "temporary" solution.

Please DO NOT report bugs of the android port to the original [s25client](https://github.com/Return-To-The-Roots/s25client) github page or discord.

I've used the [sdl2 android example app](https://github.com/libsdl-org/SDL/tree/SDL2/android-project) as base of my rttr port.

I'm using android ndk r27 ndk r23(some dependencies are not compatible with ndk r17 :/) cmake 3.22.1 and openjdk 17.

Currently you need to put the settlers 2 data into the `android-project/app/src/main/assets/share/s25rttr/S2` folder

# How to build

First you will need linux! Ive not tested it on Windows/mac or in wsl. You can try to compile it in wls but please don't report errors if it don't work.
You java 17(in my case openkjdk), android cmake 3.22.1(or newer), android sdk 34, Platform-Tools v.35.0.2, ndk r23 and r27. Why? Sadly some dependencies like boost won't compile with ndk 27 so we need some to compile with r23 and the others with r27.

If possible place the sdk, ndk's, cmake ... under `~/Android`. (for example `/home/username/Android/ndk/r27`, `/home/username/Android/platforms/android-34`)

After that clone this repository with the git command

	git clone --recursive https://github.com/Farmer-Markus/s25rttr-android

Then run the build_dependencies.sh script to prebuild all dependencies. (This could take a while)
If it cannot find your ndk installation please set the NDK23 and NDK27 enviroment variables to the location.
Another example:

	NDK23="~/Android/ndk/r23" NDK27="~/Android/ndk/r27" ./build_dependencies.sh

After compiling all stuff it should show this message:
`All dependencies have been build!(I hope :D)`
If not the build failed! You can open an [issue](https://github.com/Farmer-Markus/s25rttr-android/issues) here if that happen to you.

Now you only need to patch s25client. Run:

	./patches.sh --apply-patch

And it should ONLY show `patched`. If it shows something else, the files I've edited were changed in an update of rttr. Please also tell me in an [issue](https://github.com/Farmer-Markus/s25rttr-android/issues).

And now you should put the path to your ndk r27 into the `android-project/local.properties` file to make gradle use ndkr27, put the path to your java 17 installation into the `build.sh` file (e.g. `/usr/lib/jvm/java-1.17.0-openjdk-amd64`) and finally also the sdk installation path into the `build.sh` file.

Now you should be able to run:

	./build.sh buildDebug

To build debug or `buildRel` to build release. I you want to build the release you need to look a bit deeper into the signing system of android apps good luck.

And please if you have errors -> [Open an Issue](https://github.com/Farmer-Markus/s25rttr-android/issues)
