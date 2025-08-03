cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()


add_subdirectory(SDL)
add_subdirectory(SDL_mixer)

add_subdirectory(boost)
add_subdirectory(miniupnpc)
add_subdirectory(lua)
add_subdirectory(bzip2)

add_subdirectory(s25client)
