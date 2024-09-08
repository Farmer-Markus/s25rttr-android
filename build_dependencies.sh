#!/bin/bash

if [ "$NDK23" == "" ]
   then
	if [ -e "$HOME/Android/Sdk/ndk/r23" ]
	   then
	   	export export NDK23="$HOME/Android/Sdk/ndk/r23"
	   	echo "(AUTO) NDK23 location set to: '"$NDK23"'"

	   else
		echo "FATAL-ERROR: Please set the 'NDK23' enviroment variable to your ndk r23 installation"
		exit
	fi
   
   else
	echo "NDK23 location set to: '"$NDK23"'"
	export NDK23="$NDK23"
fi

if [ "$NDK27" == "" ]
   then
	if [ -e "$HOME/Android/Sdk/ndk/r27" ]
	   then
	   	export export NDK="$HOME/Android/Sdk/ndk/r27"
	   	echo "(AUTO) NDK27 location set to: '"$NDK"'"

	   else
		echo "FATAL-ERROR: Please set the 'NDK27' enviroment variable to your ndk r27 installation"
		exit
	fi
   
   else
	echo "NDK27 location set to: '"$NDK27"'"
	export NDK="$NDK27"
fi


currDir=$(cd $(dirname $0);pwd)
export PATH=$NDK23:$PATH
abis="armeabi-v7a arm64-v8a x86 x86_64"


universal_builder() {
 local project="$1"
 local NDK="$2"
 
 echo "INFO: Using NDK: "$NDK""
 
 for i in $abis
 do

 echo "Building $project for $i"
 
 mkdir -p build/lib/$i

 cd build/lib/$i

 echo "Configuring for "$i""

 if [ "$project" == "SDL_mixer" ]
    then
 	cmake -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DSDL2_LIBRARY="$currDir/SDL/lib/$i/libSDL2.so" -DSDL2_INCLUDE_DIR="$currDir/SDL/include" -DSDL2MIXER_WAVPACK=OFF -DANDROID_ABI=$i -DBUILD_SHARED_LIBS=TRUE -DCMAKE_C_COMPILER=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang -DCMAKE_CXX_COMPILER=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++ ../../../

    else
    	cmake -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=$i -DBUILD_SHARED_LIBS=TRUE -DCMAKE_C_COMPILER=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang -DCMAKE_CXX_COMPILER=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++ ../../../
 fi

 if [ $? -eq 0 ]; then
     echo "CMake configuration successful."
     cmake --build .
 else
     echo "CMake configuration failed while processing $project."
     exit
 fi
 echo "Build successfull for $project on platform $i."
 cd ../../../
 
 done
 cd $currDir
}

build_boost () {
 cd "$currDir/Boost-for-Android"
 
 if [ ! -e "Modified" ]
    then
 	echo "Modifying boost build script"
 	sed -i -e 's/link=static/link=shared/g' build-android.sh 
 	sed -i -e 's/--layout=${LAYOUT}/--layout=system/g' build-android.sh #--no-suffix \
 	sed -i -e 's/pelya/Farmer-Markus/g' build-android.sh
 	sed -i '564s/libiconv-libicu-android/libiconv-libicu-android\/\$ARCH/' build-android.sh
 	sed -i '622i sed -i '\''s/result = \$(result).\$(BOOST_VERSION)  ;/result = \$(result) ;/'\'' boostcpp.jam' build-android.sh

 	echo "Build script modified" > Modified
 fi
 
 ./build-android.sh --with-iconv
 
 if [ -e "build/out/arm64-v8a/lib/libboost_locale.so" ] && [ -e "build/out/armeabi-v7a/lib/libboost_locale.so" ] && [ -e "build/out/x86/lib/libboost_locale.so" ] &&[ -e "build/out/x86_64/lib/libboost_locale.so" ]
    then
    	echo "SUCCESS: Finished building boost!"
    	cd "$currDir"

    	echo "Copying binaries..."

    	mkdir -p "$currDir/boost/lib/arm64-v8a"
    	mkdir -p "$currDir/boost/lib/armeabi-v7a"
    	mkdir -p "$currDir/boost/lib/x86"
    	mkdir -p "$currDir/boost/lib/x86_64"
    	
    	cp -r "$currDir/Boost-for-Android/build/out/arm64-v8a/include"  "$currDir/boost/"
    	
    	echo "INFO: Applying patch to boost include dir..."
    	cd "$currDir/boost"
    	patch -s -p0 < "$currDir/patch/boost.patch"
    	cd "$currDir"
    	echo "INFO: Boost patched!"

    	cp -r "$currDir/Boost-for-Android/build/out/arm64-v8a/lib/"*  "$currDir/boost/lib/arm64-v8a/"
    	cp -r "$currDir/Boost-for-Android/build/out/armeabi-v7a/lib/"*  "$currDir/boost/lib/armeabi-v7a/"
    	cp -r "$currDir/Boost-for-Android/build/out/x86/lib/"*  "$currDir/boost/lib/x86/"
    	cp -r "$currDir/Boost-for-Android/build/out/x86_64/lib/"*  "$currDir/boost/lib/x86_64/"

    else
    	echo "FATAL-ERROR: Could not build boost!"
    	exit
 fi
 
 if [ -e "$currDir/boost/lib/arm64-v8a/libboost_locale.so" ] && [ -e "$currDir/boost/lib/armeabi-v7a/libboost_locale.so" ] && [ -e "$currDir/boost/lib/x86/libboost_locale.so" ] && [ -e "$currDir/boost/lib/x86_64/libboost_locale.so" ]
    then
    	echo "SUCCESS: Boost completed!"

    else

    	echo "FATAL-ERROR: Failed to copy boost libraries from: "$currDir/Boost-for-Android/build/out/" to "$currDir/boost/lib/"!"
 fi
}

build_bzip2() {
 cd "$currDir/bzip2_sources"
 
 universal_builder "bzip2" "$NDK23"
 
 mkdir -p "$currDir/bzip2/include/bzip2"
 
 mkdir -p "$currDir/bzip2/lib/arm64-v8a"
 mkdir -p "$currDir/bzip2/lib/armeabi-v7a"
 mkdir -p "$currDir/bzip2/lib/x86"
 mkdir -p "$currDir/bzip2/lib/x86_64"
 
 cp -r "$currDir/bzip2_sources/"*".h" "$currDir/bzip2/include/bzip2/"
 
 for i in $abis
 do
   
   cp "$currDir/bzip2_sources/build/lib/$i/libbz2.so" "$currDir/bzip2/lib/$i/"
   
   if [ ! -e "$currDir/bzip2/lib/$i/libbz2.so" ]
      then
	   echo "FATAL-ERROR: Failed to copy bzip2 libraries from: "$currDir/bzip2_sources/build" to "$currDir/bzip2/lib/"!"
	   exit
   fi
   echo "SUCCESS: bzip2 completed!"
   
 done
}

build_iconv() {
 cd "$currDir/libiconv-libicu-android"
 ./build.sh
 
 if [ -e "arm64-v8a" ] && [ -e "armeabi-v7a" ] && [ -e "x86" ] && [ -e "x86_64" ]
    then
    	echo "SUCCESS: Finished building iconv!"
    	
    	cd "$currDir"
    	
    	echo "Copying binaries..."
    	
    	for i in $abis
 	do
 	
    	mkdir -p "$currDir/iconv/lib/$i"
 	
 	cp "$currDir/libiconv-libicu-android/$i/"*".so" "$currDir/iconv/lib/$i/"
 	cp "$currDir/libiconv-libicu-android/$i/"*".a" "$currDir/iconv/lib/$i/"
 	
 	if [ ! -e "$currDir/iconv/lib/$i/libiconv.so" ]
    	    then
    		echo "FATAL-ERROR: Failed to copy iconv libraries from: "$currDir/libiconv-libicu-android/build" to "$currDir/iconv/lib/"!"
 	fi
 	done
 	
 	cp -r "$currDir/libiconv-libicu-android/arm64-v8a/include" "$currDir/iconv/"

    else
    	echo "FATAL-ERROR: Could not build iconv!"
    	exit
 fi
 echo "SUCCESS: iconv completed!"
}

build_lua() {
 cd "$currDir/lua_sources"
 
 universal_builder "lua" "$NDK"
 
 mkdir -p "$currDir/lua/lib/arm64-v8a"
 mkdir -p "$currDir/lua/lib/armeabi-v7a"
 mkdir -p "$currDir/lua/lib/x86"
 mkdir -p "$currDir/lua/lib/x86_64"
 
 cp -r "$currDir/lua_sources/include" "$currDir/lua/"
 
 for i in $abis
 do
   
   cp "$currDir/lua_sources/build/lib/$i/liblua.so" "$currDir/lua/lib/$i/"
   
   if [ ! -e "$currDir/lua/lib/$i/liblua.so" ]
      then
	   echo "FATAL-ERROR: Failed to copy lua libraries from: "$currDir/lua_sources/build" to "$currDir/lua/lib/"!"
	   exit
   fi
 done
 echo "SUCCESS: lua completed!"
}

build_miniupnpc() {
 cd "$currDir/miniupnpc_sources"
 
 universal_builder "miniupnpc" "$NDK"
 
 mkdir -p "$currDir/miniupnpc/lib/arm64-v8a"
 mkdir -p "$currDir/miniupnpc/lib/armeabi-v7a"
 mkdir -p "$currDir/miniupnpc/lib/x86"
 mkdir -p "$currDir/miniupnpc/lib/x86_64"
 
 cp -r "$currDir/miniupnpc_sources/include" "$currDir/miniupnpc/"
 
 cd "$currDir/miniupnpc/include"
 ln -s . "miniupnpc"
 cd $currDir
 
 
 for i in $abis
 do
   
   cp "$currDir/miniupnpc_sources/build/lib/$i/libminiupnpc.so" "$currDir/miniupnpc/lib/$i/"
   
   if [ ! -e "$currDir/miniupnpc/lib/$i/libminiupnpc.so" ]
      then
	   echo "FATAL-ERROR: Failed to copy miniupnpc libraries from: "$currDir/miniupnpc_sources/build" to "$currDir/miniupnpc/lib/"!"
	   exit
   fi
 done
 echo "SUCCESS: miniupnpc completed!"
}

build_SDL() {
 cd "$currDir/SDL_sources"
 
 universal_builder "SDL" "$NDK"

 mkdir -p "$currDir/SDL/lib/arm64-v8a"
 mkdir -p "$currDir/SDL/lib/armeabi-v7a"
 mkdir -p "$currDir/SDL/lib/x86"
 mkdir -p "$currDir/SDL/lib/x86_64"
 
 cp -r "$currDir/SDL_sources/include" "$currDir/SDL/"
 
 for i in $abis
 do
   local sourceDir="$currDir/SDL_sources/build/lib/$i"
   
   cp "$sourceDir/libSDL2.so" "$sourceDir/libSDL2main.a" "$currDir/SDL/lib/$i/"
   
   if [ ! -e "$currDir/SDL/lib/$i/libSDL2.so" ]
      then
	   echo "FATAL-ERROR: Failed to copy SDL libraries from: "$currDir/SDL_sources/build" to "$currDir/SDL/lib/"!"
	   exit
   fi
 done
 echo "SUCCESS: SDL completed!"
}

build_SDL_mixer() {
 cd "$currDir/SDL_mixer_sources"
 
 universal_builder "SDL_mixer" "$NDK"

 mkdir -p "$currDir/SDL_mixer/lib/arm64-v8a"
 mkdir -p "$currDir/SDL_mixer/lib/armeabi-v7a"
 mkdir -p "$currDir/SDL_mixer/lib/x86"
 mkdir -p "$currDir/SDL_mixer/lib/x86_64"
 
 cp -r "$currDir/SDL_mixer_sources/include" "$currDir/SDL_mixer/"
 
 for i in $abis
 do
   local sourceDir="$currDir/SDL_mixer_sources/build/lib/$i"
   
   cp "$sourceDir/libSDL2_mixer.so" "$currDir/SDL_mixer/lib/$i/"
   
   if [ ! -e "$currDir/SDL_mixer/lib/$i/libSDL2_mixer.so" ]
      then
	   echo "FATAL-ERROR: Failed to copy SDL_mixer libraries from: "$currDir/SDL_mixer_sources/build" to "$currDir/SDL_mixer/lib/"!"
	   exit
   fi
 done
 echo "SUCCESS: SDL_mixer completed!"
}


if [ ! -e "$currDir/boost" ]
   then
	echo "INFO: Could not find boost libraries in '"$currDir/boost"'!"
	echo "Preparing to build boost..."
	if [ ! -e "$currDir/Boost-for-Android" ]
	   then
		echo "FATAL-ERROR: could not find project Boost-for-Android("$currDir/Boost-for-Android")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_boost

   else
   	echo "Boost folder ("$currDir/boost") already exists! Skipping boost"
fi

if [ ! -e "$currDir/bzip2" ]
   then
   	echo "INFO: Could not find bzip2 libraries in '"$currDir/bzip2"'!"
   	echo "Preparing to build bzip2..."
   	if [ ! -e "$currDir/bzip2_sources" ]
	   then
		echo "FATAL-ERROR: could not find project bzip2("$currDir/bzip2_sources")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_bzip2
   else
   	echo "Bzip2 folder ("$currDir/bzip2") already exists! Skipping bzip2"
fi

if [ ! -e "$currDir/iconv" ]
   then
   	echo "INFO: Could not find iconv libraries in '"$currDir/iconv"'!"
   	echo "Preparing to build iconv..."
   	if [ ! -e "$currDir/libiconv-libicu-android" ]
	   then
		echo "FATAL-ERROR: could not find project iconv("$currDir/libiconv-libicu-android")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_iconv
   else
   	echo "Iconv folder ("$currDir/iconv") already exists! Skipping iconv"
fi

if [ ! -e "$currDir/lua" ]
   then
   	echo "INFO: Could not find lua libraries in '"$currDir/lua"'!"
   	echo "Preparing to build lua..."
   	if [ ! -e "$currDir/lua_sources" ]
	   then
		echo "FATAL-ERROR: could not find project lua("$currDir/lua_sources")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_lua
   else
   	echo "Lua folder ("$currDir/lua") already exists! Skipping lua"
fi

if [ ! -e "$currDir/miniupnpc" ]
   then
   	echo "INFO: Could not find miniupnpc libraries in '"$currDir/miniupnpc"'!"
   	echo "Preparing to build miniupnpc..."
   	if [ ! -e "$currDir/miniupnpc_sources" ]
	   then
		echo "FATAL-ERROR: could not find project miniupnpc("$currDir/miniupnpc_sources")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_miniupnpc
   else
   	echo "Miniupnpc folder ("$currDir/miniupnpc") already exists! Skipping miniupnpc"
fi

if [ ! -e "$currDir/SDL" ]
   then
   	echo "INFO: Could not find SDL libraries in '"$currDir/SDL"'!"
   	echo "Preparing to build SDL..."
   	if [ ! -e "$currDir/SDL_sources" ]
	   then
		echo "FATAL-ERROR: could not find project SDL("$currDir/SDL_sources")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_SDL
   else
   	echo "SDL folder ("$currDir/SDL") already exists! Skipping SDL"
fi

if [ ! -e "$currDir/SDL_mixer" ]
   then
   	echo "INFO: Could not find SDL_mixer libraries in '"$currDir/SDL_mixer"'!"
   	echo "Preparing to build SDL_mixer..."
   	if [ ! -e "$currDir/SDL_mixer_sources" ]
	   then
		echo "FATAL-ERROR: could not find project SDL_mixer("$currDir/SDL_mixer_sources")!"
		echo "try 'git submodule update --init --recursive'"
		exit
	fi
	build_SDL_mixer
   else
   	echo "SDL_mixer folder ("$currDir/SDL_mixer") already exists! Skipping SDL_mixer"
fi

if [ ! -e "$currDir/gl4es/Patched" ]
   then
   	echo "Patching gl4es..."
   	patch -s -p0 < patch/gl4es.patch
   	echo "Patched gl4es"
   	
   	echo "Patched" > "$currDir/gl4es/Patched"

   else
   	echo "Gl4es already Patched! Skipping gl4es"
fi

echo "All dependencies have been build!(I hope :D)"
