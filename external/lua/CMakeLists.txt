cmake_minimum_required(VERSION 3.6)

project(lua)

file(GLOB LUA_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/lua/*.c")
list(REMOVE_ITEM LUA_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/lua/onelua.c")

add_library(lua SHARED ${LUA_SOURCES})
target_include_directories(lua PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/lua")

add_custom_target(install_lua ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/lua/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/lua/liblua.so" "${INSTALL_PATH}/lua/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E make_directory "${INSTALL_PATH}/lua/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lua/lua.h" "${INSTALL_PATH}/lua/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lua/lualib.h" "${INSTALL_PATH}/lua/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lua/lauxlib.h" "${INSTALL_PATH}/lua/include"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_CURRENT_SOURCE_DIR}/lua/luaconf.h" "${INSTALL_PATH}/lua/include"
	DEPENDS lua
)
