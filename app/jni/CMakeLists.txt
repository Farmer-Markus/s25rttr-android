cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()

# 16kb alignment
if(ANDROID_ABI STREQUAL "arm64-v8a")
    add_link_options(
            "-Wl,-z,max-page-size=16384"
            "-Wl,-z,common-page-size=16384"
    )
endif()

set(LIB_DIR "${CMAKE_SOURCE_DIR}/../../libs")

# Set variables for find_package in s25client
set(Boost_LIBRARY_DIR "${LIB_DIR}/boost/${ANDROID_ABI}" CACHE INTERNAL "" FORCE)
set(Boost_INCLUDE_DIR "${LIB_DIR}/boost/include" CACHE INTERNAL "" FORCE)

set(MINIUPNPC_INCLUDE_DIR "${LIB_DIR}/miniupnpc/include/" CACHE INTERNAL "" FORCE)
set(MINIUPNPC_LIBRARY "${LIB_DIR}/miniupnpc/${ANDROID_ABI}/libminiupnpc.so" CACHE INTERNAL "" FORCE)

set(SDL2_INCLUDE_DIR "${LIB_DIR}/SDL/include" CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY_DIR "${LIB_DIR}/SDL/${ANDROID_ABI}" CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY "${LIB_DIR}/SDL/${ANDROID_ABI}/libSDL2.so" CACHE INTERNAL "" FORCE)
set(SDL2_SDL2MAIN_LIBRARY "${LIB_DIR}/SDL/${ANDROID_ABI}/libSDL2main.a" CACHE INTERNAL "" FORCE)

set(SDL_MIXER_INCLUDE_DIR "${LIB_DIR}/SDL_mixer/include" CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARY_DIR "${LIB_DIR}/SDL_mixer/${ANDROID_ABI}" CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARIES "${LIB_DIR}/SDL_mixer/${ANDROID_ABI}" CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARY "${LIB_DIR}/SDL_mixer/${ANDROID_ABI}/libSDL2_mixer.so" CACHE INTERNAL "" FORCE)

set(LUA_INCLUDE_DIR "${LIB_DIR}/lua/include" CACHE INTERNAL "" FORCE)
set(LUA_LIBRARY "${LIB_DIR}/lua/${ANDROID_ABI}/liblua.so" CACHE INTERNAL "" FORCE)
set(LUA_LIBRARIES "${LIB_DIR}/lua/${ANDROID_ABI}"	CACHE INTERNAL "" FORCE)

set(BZIP2_INCLUDE_DIR "${LIB_DIR}/bzip2/include" CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARY "${LIB_DIR}/bzip2/${ANDROID_ABI}/libbz2.so" CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARIES "${LIB_DIR}/bzip2/${ANDROID_ABI}" CACHE INTERNAL "" FORCE)

set(GL4ES_INCLUDE_DIR "${LIB_DIR}/gl4es/include" CACHE INTERNAL "" FORCE)
set(GL4ES_LIBRARY "${LIB_DIR}/gl4es/${ANDROID_ABI}/libGL.so" CACHE INTERNAL "" FORCE)


add_library(GL SHARED IMPORTED GLOBAL)
set_target_properties(GL PROPERTIES
    IMPORTED_LOCATION "${GL4ES_LIBRARY}"
    INTERFACE_INCLUDE_DIRECTORIES "${GL4ES_INCLUDE_DIR}"
)

set(RTTR_BUILD_UPDATER OFF CACHE INTERNAL "Disabled" FORCE)
set(BUILD_TESTING OFF CACHE INTERNAL "Disabled" FORCE)
set(RTTR_BUILD_LIB ON CACHE INTERNAL "Enabled" FORCE)
set(RTTR_OPENGL GL4ES CACHE INTERNAL "GL4ES" FORCE)

# assets/RTTR = gamedata
get_filename_component(RTTR_DATADIR "${CMAKE_CURRENT_SOURCE_DIR}/../src/main/assets" ABSOLUTE)
set(RTTR_DATADIR "${RTTR_DATADIR}" CACHE INTERNAL "" FORCE)

find_package(Git QUIET)

if(GIT_FOUND)
    message(STATUS "Using ${GIT_EXECUTABLE} to get 'RTTR_REVISION'")

    # Set s25client commit id
    # RTTR get-git-revision function won't work with s25client as submodule
    execute_process(
            COMMAND git rev-parse HEAD
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/s25client
            RESULT_VARIABLE GIT_RESULT
            OUTPUT_VARIABLE RTTR_REVISION
            OUTPUT_STRIP_TRAILING_WHITESPACE
    )
else()
    message(STATUS "Git NOT found! Using 'RTTR_REVISION = 1' !")
    set(RTTR_REVISION 1)
endif()

add_subdirectory(s25client)
