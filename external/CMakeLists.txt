cmake_minimum_required(VERSION 3.6)

project(dependencies)

set(INSTALL_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../libs" CACHE BOOL "" FORCE)


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


# Opengl2.* to Opengl-es translation library
add_subdirectory(gl4es)
add_custom_target(install_gl4es ALL
	COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/gl4es/include" "${INSTALL_PATH}/gl4es/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lib/libGL.so.1" "${INSTALL_PATH}/gl4es/${ANDROID_ABI}/libGL.so"
	DEPENDS GL
)
