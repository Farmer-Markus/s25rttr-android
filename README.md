## Return to the Roots Android
Android port of the fan-project [Return to the Roots](https://github.com/Return-To-The-Roots/s25client) which aims to renew the original The Settlers 2. <br>

This project is work in progress, please report bugs [here](https://github.com/Farmer-Markus/s25rttr-android/issues). <br>

## Downloads
Download from [github releases](https://github.com/Farmer-Markus/s25rttr-android/releases) or [build it yourself](#Compiling) as described below.

## Compiling
You will need git <br>
Clone this repository and its submodules:

	git clone --recursive https://github.com/Farmer-Markus/s25rttr-android

When using android studio just open the project folder and click on build and everything should work fine. <br>
If you want to use the commandline see steps below.

### Dependencies
#### Linux
You need to install [gradle](https://gradle.org/) and java(for example [openjdk](openjdk.org))
Also you will need the android sdk, ndk, buildtools and android cmake.
These can be installed using the sdkmanager cmdline tool.

##### Archlinux
To install gradle and java run:

	sudo pacman -S gradle jdk17-openjdk
To install the android sdkmanager you can use the [AUR package](https://aur.archlinux.org/packages/android-sdk-cmdline-tools-latest) or download the tools yourself on [Googles website](https://developer.android.com/studio?hl=de#command-tools).

##### Debian
To install gradle, java and the android sdkmanager run:

	sudo apt-get install gradle openjdk-17-jdk sdkmanager

##### Windows
coming soon...

### Installing android SDK/build-tools
To show every available package run:

	sdkmanager --list

You will need at least the following packages

	android sdk 34
	android cmake 3.22.1
	Platform-Tools v.35.0.2
	android build-tools 35.0.0
	android ndk 27.0.12077973

You can install them via the `sdkmanager --install` command:

	sdkmanager --install "platforms;android-34" "cmake;4.1.2" "platform-tools;36.0.2" "ndk;27.0.12077973" "build-tools;35.0.0"

This will install the SDK to a specific location. (Remember the location) <br>
You need to accept the licenses using the sdkmanager

	sdkmanager --licenses
You may want to move the SDK to another folder like ~/Android/Sdk (Default in android studio)

Now you need to give the gradle script the path to the android sdk. <br>
You can create a `local.properties` file containing the following. <br>

	sdk.dir=<path to the sdk>
Or set the enviroment variable `ANDROID_HOME` to the sdk installation.

