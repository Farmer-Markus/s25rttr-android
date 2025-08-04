cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()

# Deletes cmake flags that causes building boost to fail (I really don't want to fork hundreds of boost repos)
function(remove_clang_flag flag)
  foreach(f CMAKE_CXX_FLAGS
  		CMAKE_CXX_FLAGS_DEBUG
  		CMAKE_CXX_FLAGS_RELEASE
		CMAKE_CXX_FLAGS_RELWITHDEBINFO
		CMAKE_CXX_FLAGS_MINSIZEREL)
  
    if(DEFINED ${f})
      string(REPLACE "${flag}" "" "${f}" "${${f}}")
      set(${f} "${${f}}" CACHE STRING "Deleted ${f}" FORCE)
    endif()
  endforeach()
endfunction()

add_subdirectory(SDL)
add_subdirectory(SDL_mixer)
add_library(SDL_mixer::SDL_mixer ALIAS SDL2_mixer)

remove_clang_flag("-Werror")
remove_clang_flag("-Werror=deprecated-copy")
remove_clang_flag("-Wdeprecated-copy")
add_compile_options(-Wno-error=deprecated-copy)
add_subdirectory(boost)

add_subdirectory(miniupnpc)
set(INCLUDE_DEST "${CMAKE_BINARY_DIR}/miniupnpc/include/miniupnpc")
file(MAKE_DIRECTORY "${INCLUDE_DEST}")
file(GLOB INCLUDE_FILES "${CMAKE_CURRENT_SOURCE_DIR}/miniupnpc/include/*")
file(COPY ${INCLUDE_FILES} DESTINATION "${INCLUDE_DEST}")

set(MINIUPNPC_LIBRARY libminiupnpc-static CACHE PATH "Miniupnpc library" FORCE)
set(MINIUPNPC_INCLUDE_DIR "${CMAKE_BINARY_DIR}/miniupnpc/include" CACHE PATH "Miniupnpc include dir" FORCE)

add_subdirectory(lua)

add_subdirectory(bzip2)
add_library(BZip2::BZip2 ALIAS bz2)

add_subdirectory(s25client)
