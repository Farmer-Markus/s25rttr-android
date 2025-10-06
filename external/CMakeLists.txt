cmake_minimum_required(VERSION 3.6)

project(dependencies)

set(INSTALL_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../libs" CACHE BOOL "" FORCE)


# Deletes cmake flags that causes building boost to fail (I really don't want to fork hundreds of boost repos)
#function(remove_clang_flag flag)
#  foreach(f CMAKE_CXX_FLAGS
#  		CMAKE_CXX_FLAGS_DEBUG
#  		CMAKE_CXX_FLAGS_RELEASE
#		CMAKE_CXX_FLAGS_RELWITHDEBINFO
#		CMAKE_CXX_FLAGS_MINSIZEREL)
#  
#    if(DEFINED ${f})
#      string(REPLACE "${flag}" "" "${f}" "${${f}}")
#      set(${f} "${${f}}" CACHE STRING "Deleted ${f}" FORCE)
#    endif()
#  endforeach()
#endfunction()



set(SDL_STATIC OFF CACHE BOOL "" FORCE)
add_subdirectory(SDL)
add_custom_target(install_sdl ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/SDL/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/SDL/libSDL2.so" "${INSTALL_PATH}/SDL/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/SDL/libSDL2main.a" "${INSTALL_PATH}/SDL/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/SDL/include" "${INSTALL_PATH}/SDL/include"
	DEPENDS SDL2 SDL2main
)

add_subdirectory(SDL_mixer)
add_custom_target(install_sdlmixer ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/SDL_mixer/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/SDL_mixer/libSDL2_mixer.so" "${INSTALL_PATH}/SDL_mixer/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/SDL_mixer/include" "${INSTALL_PATH}/SDL_mixer/include"
	DEPENDS SDL2_mixer
)

# RTTR's audio driver does not link to sdl but tries to include the sdl header
#cmake_policy(SET CMP0079 NEW)
#target_link_libraries(SDL2_mixer PUBLIC SDL2)
#add_library(SDL_mixer::SDL_mixer ALIAS SDL2_mixer)

#remove_clang_flag("-Werror")
#remove_clang_flag("-Werror=deprecated-copy")
#remove_clang_flag("-Wdeprecated-copy")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-deprecated-declarations")
#add_compile_options(-Wno-error=deprecated-copy -Wno-error=missing-noreturn -Wno-error=undef)

# Handled in seperate boost cmake file
add_subdirectory(boost)

add_subdirectory(miniupnpc)
add_custom_target(install_miniupnpc ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/miniupnpc/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/miniupnpc/libminiupnpc.so" "${INSTALL_PATH}/miniupnpc/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/miniupnpc/libminiupnpc.a" "${INSTALL_PATH}/miniupnpc/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/miniupnpc/include"
	COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/miniupnpc/include" "${INSTALL_PATH}/miniupnpc/include/miniupnpc"
	DEPENDS libminiupnpc-shared libminiupnpc-static
)


#set(INCLUDE_DEST "${CMAKE_BINARY_DIR}/miniupnpc/include/miniupnpc")
#file(MAKE_DIRECTORY "${INCLUDE_DEST}")
#file(GLOB INCLUDE_FILES "${CMAKE_CURRENT_SOURCE_DIR}/miniupnpc/include/*")
#file(COPY ${INCLUDE_FILES} DESTINATION "${INCLUDE_DEST}")

#set(MINIUPNPC_LIBRARY libminiupnpc-static CACHE PATH "Miniupnpc library" FORCE)
#set(MINIUPNPC_INCLUDE_DIR "${CMAKE_BINARY_DIR}/miniupnpc/include" CACHE STRING "Miniupnpc include dir" FORCE)

# Handled in seperate lua cmake file
add_subdirectory(lua)

add_subdirectory(bzip2)
add_custom_target(install_bz2 ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/bzip2/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/bzip2/libbz2.so" "${INSTALL_PATH}/bzip2/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/bzip2/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/bzip2/bzlib.h" "${INSTALL_PATH}/bzip2/include/"
	DEPENDS bz2
)

#add_library(BZip2::BZip2 ALIAS bz2)
#target_include_directories(bz2 INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/bzip2)

# Opengl2.* to Opengl es translation library set(SDL_STATIC OFF CACHE BOOL "" FORCE)
add_subdirectory(gl4es)
add_custom_target(install_gl4es ALL
	COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/gl4es/include" "${INSTALL_PATH}/gl4es/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lib/libGL.so.1" "${INSTALL_PATH}/gl4es/${ANDROID_ABI}/libGL.so"
	DEPENDS GL
)


#target_include_directories(GL PUBLIC "${CMAKE_SOURCE_DIR}/gl4es/include")
