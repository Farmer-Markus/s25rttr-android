cmake_minimum_required(VERSION 3.16..3.25)

project(lua-android)

file(GLOB LUA_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/lua/*.c")
list(REMOVE_ITEM LUA_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/lua/onelua.c")

add_library(lua SHARED
	${LUA_SOURCES}
)
target_include_directories(lua PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/lua)
