cmake_minimum_required(VERSION 3.9)

project(GAME)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-deprecated-declarations")

#set(BOOST_ROOT "${CMAKE_SOURCE_DIR}/boost/lib/${CMAKE_ANDROID_ARCH_ABI}")
#set(BOOST_LIBRARYDIR "${CMAKE_SOURCE_DIR}/boost/lib/${CMAKE_ANDROID_ARCH_ABI}")
set(Boost_LIBRARY_DIR "${CMAKE_SOURCE_DIR}/boost/lib/${CMAKE_ANDROID_ARCH_ABI}")
#set(BOOST_INCLUDEDIR "${CMAKE_SOURCE_DIR}/boost/include")
set(Boost_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/boost/include")

set(MINIUPNPC_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/miniupnpc/include/miniupnpc" CACHE INTERNAL "" FORCE)
set(MINIUPNPC_LIBRARY "${CMAKE_SOURCE_DIR}/miniupnpc/lib/${CMAKE_ANDROID_ARCH_ABI}/libminiupnpc.so" CACHE INTERNAL "" FORCE) #Overwriting FindMiniupnpc.cmake variables

set(SDL2_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/SDL/include/SDL" CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY_DIR "${CMAKE_SOURCE_DIR}/SDL/lib/${CMAKE_ANDROID_ARCH_ABI}" CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY "${CMAKE_SOURCE_DIR}/SDL/lib/${CMAKE_ANDROID_ARCH_ABI}/libSDL2.so" CACHE INTERNAL "" FORCE)
set(SDL2_SDL2MAIN_LIBRARY "${CMAKE_SOURCE_DIR}/SDL/lib/${CMAKE_ANDROID_ARCH_ABI}/libSDL2main.a" CACHE INTERNAL "" FORCE)

set(SDL_MIXER_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/SDL_mixer/include")
set(SDL_MIXER_LIBRARY_DIR "${CMAKE_SOURCE_DIR}/SDL_mixer/lib/${CMAKE_ANDROID_ARCH_ABI}")
set(SDL_MIXER_LIBRARIES "${CMAKE_SOURCE_DIR}/SDL_mixer/lib/${CMAKE_ANDROID_ARCH_ABI}")
set(SDL_MIXER_LIBRARY "${CMAKE_SOURCE_DIR}/SDL_mixer/lib/${CMAKE_ANDROID_ARCH_ABI}/libSDL2_mixer.so")

set(LUA_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/lua/include" CACHE INTERNAL "" FORCE)
set(LUA_LIBRARY "${CMAKE_SOURCE_DIR}/lua/lib/${CMAKE_ANDROID_ARCH_ABI}/liblua.so")
set(LUA_LIBRARIES "${CMAKE_SOURCE_DIR}/lua/lib/${CMAKE_ANDROID_ARCH_ABI}")

set(BZIP2_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/bzip2/include/bzip2" CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARY "${CMAKE_SOURCE_DIR}/bzip2/lib/${CMAKE_ANDROID_ARCH_ABI}/libbz2.so" CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARIES "${CMAKE_SOURCE_DIR}/bzip2/lib/${CMAKE_ANDROID_ARCH_ABI}" CACHE INTERNAL "" FORCE)

set(ICONV_LIBRARY_DIR "${CMAKE_SOURCE_DIR}/iconv/lib/${CMAKE_ANDROID_ARCH_ABI}")

add_library(iconv SHARED IMPORTED)
set_target_properties(iconv PROPERTIES IMPORTED_LOCATION "${ICONV_LIBRARY_DIR}/libiconv.so")

set(GL4ES_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/gl4es/include")
include_directories(${GL4ES_INCLUDE_DIR})

set(LIBGL_VSYNC 1)
#set(RTTR_OPENGL "GLES3.2" CACHE INTERNAL "" FORCE)
#set(CMAKE_MATCH_1 "GLES" CACHE INTERNAL "" FORCE)

#add_library(Boost::thread ALIAS boost_thread)


#set(CMAKE_PREFIX_PATH ${CMAKE_SOURCE_DIR}/miniupnpc/:${CMAKE_PREFIX_PATH})

#set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
#set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE BOTH)
#set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE BOTH)


# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()

# SDL sources are in a subfolder named "SDL"
add_subdirectory(SDL)

# Compilation of companion libraries
#add_subdirectory(SDL_image)
add_subdirectory(SDL_mixer)
#add_subdirectory(SDL_ttf)

add_subdirectory(gl4es)

# Your game and its CMakeLists.txt are in a subfolder named "src"
#add_subdirectory(src)
add_subdirectory(src)
