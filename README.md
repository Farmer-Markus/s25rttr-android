# s25rttr-android
Unofficial Android port of [s25client](https://github.com/Return-To-The-Roots/s25client).

![new-game](https://github.com/user-attachments/assets/dde1a73c-b66b-4874-9ba1-693fc4798c24)


This project is work in progress.

I don't have any experience with android/java programming and much of the project is just a "temporary" solution.

Please DO NOT report bugs of the android port to the original [s25client](https://github.com/Return-To-The-Roots/s25client) github page or discord.

Please don't upload my app on google play, fdroid. I want to do this later.

I've used the [sdl2 android-project](https://github.com/libsdl-org/SDL/tree/SDL2/android-project) as base of my rttr port.

I'm using android ndk r27 ndk r23.0.7599858(some dependencies are not compatible with ndk r27 :/) cmake 3.22.1 and openjdk 17.

When starting the App it will ask for file permission. After allowing you'll be able to pick/create a folder on your internal storage or sdcard. All app files will be stored in there. After choosing the folder, the game should give you an error that you need to put the orignal settlers 2 files into a folder(the one you picked). Put the game files into the folder in `share/s25rttr/S2`. After that the game should start normally.

If you want to lock the game into horizontal mode enable fullscreen in the rttr settings and restart the app.

You can ask questions and share ideas [here](https://github.com/Farmer-Markus/s25rttr-android/discussions). Feel free to share everything you want to.

What is already working?

- Latest versions
- Sound
- Ai
- Lan multiplayer(currently only if android player hosts game.)
- Touch controls (same controls as on Pc with external mouse)
- File picker to store app files in selected folder

What is planned?

- Correct working multiplayer
- Resolution fix (gui scale)
- Better java optimisation

If you know why e.g. the lan multiplayer only works one way please open an issue to let me know.

Todo:
- Update Boost to [this](https://github.com/moritz-wundke/Boost-for-Android) version and make it compatible with ndk27

# Downloads

You can download the final apk file [here](https://github.com/Farmer-Markus/s25rttr-android/releases/latest)

Or you compile it yourself as described below.

# How to build

First you need linux! I've not tested it on Windows/mac or in wsl. You can try to compile it in wls but don't be surprised if it doesn't work.
You should use java 17(in my case openkjdk), android cmake 3.22.1(or newer), android sdk 34, Platform-Tools v.35.0.2, ndk r23.0.7599858 and r27. Why? Sadly some dependencies like boost won't compile with ndk 27 so we need some to compile with r23 and the others with r27.

If possible place the sdk, ndk's, cmake ... under `~/Android`. (for example `/home/username/Android/ndk/r27`, `/home/username/Android/platforms/android-34`)

You need to install a few system packages, <br />
ARCH:

    sudo pacman -S jdk17-openjdk wget curl gradle cmake

DEBIAN:

    sudo apt-get install openjdk-17-jdk wget curl gradle cmake

After that clone this repository with the git command

	git clone --recursive https://github.com/Farmer-Markus/s25rttr-android

Then move into the `s25rttr-android` folder and run the build_dependencies.sh script to prebuild all dependencies. (This could take a while)
If it cannot find your ndk installation please set the NDK23 and NDK27 enviroment variables to the location. <br />
Another example:

	NDK23="~/Android/ndk/r23" NDK27="~/Android/ndk/r27" ./build_dependencies.sh

After compiling all stuff it should show this message:
`All dependencies have been build!(I hope :D)`
If not the build failed! You can open an [issue](https://github.com/Farmer-Markus/s25rttr-android/issues) here if that happen to you.

Now you only need to patch my changes to s25client. Run:

	./patches.sh --apply-patch

And it should ONLY show `patched`. If it shows something else, the files I've edited were changed in an update of rttr. Please also tell me in an [issue](https://github.com/Farmer-Markus/s25rttr-android/issues).

And now you should put the path of your java 17 installation into the `build.sh` file (e.g. `/usr/lib/jvm/java-1.17.0-openjdk-amd64`) and finally also the sdk installation path into the `build.sh` file.

Now you should be able to go into the `android-project` directory and run:

	./build.sh assembleDebug

to build debug or `assembleRel` to build release. If you want to build the release you need to look a bit deeper into the signing system of android apps, good luck.  <br />
You may need to accept some licenses using the sdk-manager.

Sadly you can't install debug builds on your real phone but you can enable debug mode in your android settings and connect to your pc with usb cable to directly install the debug build on your phone(installDebug).

And please if you have errors -> [Open an Issue](https://github.com/Farmer-Markus/s25rttr-android/issues)

# Special thanks
Special thanks to this libraries used in this project: <br />
[Boost for Android](https://github.com/pelya/Boost-for-Android), <br />
[Lua for Android](https://github.com/pelya/commandergenius/tree/sdl_android/project/jni/lua), <br />
[LibIconv/LibIcu for Android](https://github.com/pelya/libiconv-libicu-android), <br />
[gl4es](https://github.com/ptitSeb/gl4es), <br />
[bzip2-cmake](https://github.com/sergiud/bzip2/)
