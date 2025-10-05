cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()


# Append cmake prefix path to let find_package find the precompiled libraries
set(DEPENDENCY_ROOT "${CMAKE_SOURCE_DIR}/../libs/${ANDROID_ABI}")
list(APPEND CMAKE_PREFIX_PATH "${DEPS_ROOT}/lib/cmake")




set(RTTR_DATA_DEST_DIR "../../../../src/main/assets/share/s25rttr" CACHE STRING "Android assets dir" FORCE)
set(DEFAULT_GAMEDIR "${RTTR_DATA_DEST_DIR}/S2" CACHE STRING "S2 original game data" FORCE)
#set(RTTR_OGL_ES TRUE CACHE STRING "Use opengl es" FORCE)
add_subdirectory(s25client)
