cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()


add_subdirectory(SDL)
add_subdirectory(SDL_mixer)
if(NOT TARGET SDL_mixer::SDL_mixer)
  add_library(SDL_mixer::SDL_mixer INTERFACE IMPORTED GLOBAL)
  target_include_directories(SDL_mixer::SDL_mixer INTERFACE
	${CMAKE_CURRENT_SOURCE_DIR}/SDL_mixer/include
  )
endif()

add_subdirectory(boost)
if(NOT TARGET Boost::boost)
  add_library(Boost::boost INTERFACE IMPORTED GLOBAL)
  target_include_directories(Boost::boost INTERFACE
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/optional/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/math/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/atomic/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/random/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/nowide/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/timer/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/chrono/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/container/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/date_time/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/charconv/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/iostreams/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/filesystem/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/thread/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/program_options/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/locale/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/endian/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/move/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/intrusive/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/ptr_container/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/format/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/config/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/range/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/algorithm/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/numeric/conversion/include
	${CMAKE_CURRENT_SOURCE_DIR}/boost/libs/lexical_cast/include
  )
  
  add_library(Boost::disable_autolinking INTERFACE IMPORTED)
endif()

add_subdirectory(miniupnpc)
set(MINIUPNPC_LIBRARY miniupnpc-static CACHE PATH "Miniupnpc library")
set(MINIUPNPC_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/miniupnpc" CACHE PATH "Miniupnpc include dir")

add_subdirectory(lua)
add_subdirectory(bzip2)
if(NOT TARGET BZip2::BZip2)
  add_library(BZip2::BZip2 INTERFACE IMPORTED GLOBAL)
  target_include_directories(BZip2::BZip2 INTERFACE
 	${CMAKE_CURRENT_SOURCE_DIR}/bzip2
  )
endif()

add_subdirectory(s25client)
