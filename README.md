# s25rttr-android
Unofficial Android port of [s25client](https://github.com/Return-To-The-Roots/s25client).

![Screenshot_20240908-234846_s25rttr](https://github.com/user-attachments/assets/12fee08c-5d90-430c-9be0-94597ca69273)

This project is work in progress.

I don't have any experience with android/java programming and much of the project is just a "temporary" solution.

Please DO NOT report bugs of the android port to the original [s25client](https://github.com/Return-To-The-Roots/s25client) github page or discord.

I've used the [sdl2 android example app](https://github.com/libsdl-org/SDL/tree/SDL2/android-project) as base of my rttr port.

I'm using android ndk r27 ndk r23(some dependencies are not compatible with ndk r27 :/) cmake 3.22.1 and openjdk 17.

When starting the App it will ask for file permission. After allowing you'll be able to pick/create a folder on your internal storage or sdcard. All app files will be stored in there. After choosing the folder, the game should give you an error that you need to put the orignal settlers 2 files into a folder(the one you picked). Put the game files into the folder in `share/s25rttr/S2`. After that the game should start normally.

The controls are very ugly at the moment. I'll try to fix that.

What is already working?

- Latest versions
- Sound
- Ai
- Lan multiplayer(currently only if android player hosts game.)
- Touch controls (very tricky but playable if you understand how the controls are "bugging". Currently you sadly need to move the map using the minimap)
- File picker to store app files in selected folder

What is planned?

- Better touch control
- Correct working multiplayer
- Resolution fix (gui scale)
- Better java optimisation
- Fix color
- Make fog working

If you know why e.g. the lan multiplayer only works one way please open an issue to let me know. I've never used pull request so I don't know how they work :D

# How to build

First you need linux! I've not tested it on Windows/mac or in wsl. You can try to compile it in wls but don't be surprised if it doesn't work.
You should use java 17(in my case openkjdk), android cmake 3.22.1(or newer), android sdk 34, Platform-Tools v.35.0.2, ndk r23 and r27. Why? Sadly some dependencies like boost won't compile with ndk 27 so we need some to compile with r23 and the others with r27.

If possible place the sdk, ndk's, cmake ... under `~/Android`. (for example `/home/username/Android/ndk/r27`, `/home/username/Android/platforms/android-34`)

After that clone this repository with the git command

	git clone --recursive https://github.com/Farmer-Markus/s25rttr-android

Then move into the `s25rttr-android` folder and run the build_dependencies.sh script to prebuild all dependencies. (This could take a while)
If it cannot find your ndk installation please set the NDK23 and NDK27 enviroment variables to the location.
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

to build debug or `assembleRel` to build release. If you want to build the release you need to look a bit deeper into the signing system of android apps, good luck.
You may need to accept some licenses using the sdk-manager.

Sadly you can't install debug builds on your real phone but you can enable debug mode in your android settings and connect to your pc with usb cable to directly install the debug build on your phone(installDebug).

And please if you have errors -> [Open an Issue](https://github.com/Farmer-Markus/s25rttr-android/issues)

# Some more screenshots (actual gameplay)
![splashscreen](https://github.com/user-attachments/assets/6f91a771-c61f-4eac-9b38-ea76b4637bdc)

![Screenshot_20240830-221959_s25rttr](https://github.com/user-attachments/assets/c72a0a02-94fb-49fb-bcf1-21ef12a5cab8)

![Screenshot_20240908-234706_s25rttr](https://github.com/user-attachments/assets/8aabc7f4-d591-45e6-9e4b-5d0b92e66356)

![Screenshot_20240831-111408_s25rttr](https://github.com/user-attachments/assets/5e1c24ee-024b-45d8-801f-78af74d43e23)

![mainmenuhori](https://github.com/user-attachments/assets/3070cd40-29a2-426c-8355-58d700ffb179)
