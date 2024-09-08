cmake_minimum_required(VERSION 3.6)

project(GAME)

execute_process(
    COMMAND git rev-parse HEAD
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/s25client
    RESULT_VARIABLE GIT_RESULT
    OUTPUT_VARIABLE RTTR_REVISION
    OUTPUT_STRIP_TRAILING_WHITESPACE
)

add_subdirectory(s25client)
