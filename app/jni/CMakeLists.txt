cmake_minimum_required(VERSION 3.6)

project(s25rttr)

# armeabi-v7a requires cpufeatures library
# include(AndroidNdkModules)
# android_ndk_import_module_cpufeatures()

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-deprecated-declarations")
set(LIB_DIR "${CMAKE_SOURCE_DIR}/../../libs")

set(Boost_LIBRARY_DIR 		"${LIB_DIR}/boost/${ANDROID_ABI}"			CACHE INTERNAL "" FORCE)
set(Boost_INCLUDE_DIR 		"${LIB_DIR}/boost/include"				CACHE INTERNAL "" FORCE)

set(MINIUPNPC_INCLUDE_DIR 	"${LIB_DIR}/miniupnpc/include/" 			CACHE INTERNAL "" FORCE)
set(MINIUPNPC_LIBRARY	 	"${LIB_DIR}/miniupnpc/${ANDROID_ABI}/libminiupnpc.so" 	CACHE INTERNAL "" FORCE)

set(SDL2_INCLUDE_DIR 		"${LIB_DIR}/SDL/include" 				CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY_DIR 		"${LIB_DIR}/SDL/${ANDROID_ABI}" 			CACHE INTERNAL "" FORCE)
set(SDL2_LIBRARY 		"${LIB_DIR}/SDL/${ANDROID_ABI}/libSDL2.so" 		CACHE INTERNAL "" FORCE)
set(SDL2_SDL2MAIN_LIBRARY 	"${LIB_DIR}/SDL/${ANDROID_ABI}/libSDL2main.a" 		CACHE INTERNAL "" FORCE)

set(SDL_MIXER_INCLUDE_DIR 	"${LIB_DIR}/SDL_mixer/include"				CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARY_DIR 	"${LIB_DIR}/SDL_mixer/${ANDROID_ABI}"			CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARIES 	"${LIB_DIR}/SDL_mixer/${ANDROID_ABI}"			CACHE INTERNAL "" FORCE)
set(SDL_MIXER_LIBRARY 		"${LIB_DIR}/SDL_mixer/${ANDROID_ABI}/libSDL2_mixer.so"	CACHE INTERNAL "" FORCE)

set(LUA_INCLUDE_DIR 		"${LIB_DIR}/lua/include" 				CACHE INTERNAL "" FORCE)
set(LUA_LIBRARY 		"${LIB_DIR}/lua/${ANDROID_ABI}/liblua.so"		CACHE INTERNAL "" FORCE)
set(LUA_LIBRARIES 		"${LIB_DIR}/lua/${ANDROID_ABI}"				CACHE INTERNAL "" FORCE)

set(BZIP2_INCLUDE_DIR 		"${LIB_DIR}/bzip2/include" 				CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARY 		"${LIB_DIR}/bzip2/${ANDROID_ABI}/libbz2.so" 		CACHE INTERNAL "" FORCE)
set(BZIP2_LIBRARIES 		"${LIB_DIR}/bzip2/${ANDROID_ABI}" 			CACHE INTERNAL "" FORCE)

set(GL4ES_INCLUDE_DIR 		"${LIB_DIR}/gl4es/include"				CACHE INTERNAL "" FORCE)

include_directories(${GL4ES_INCLUDE_DIR})
set(LIBGL_VSYNC 1)



set(BoostSharedLibs
    atomic
    chrono
    container
    date_time
    filesystem
    iostreams
    locale
    nowide
    program_options
    random
    regex
    thread
)

foreach(LIB IN LISTS BoostSharedLibs)
    add_library(Boost::${LIB} SHARED IMPORTED GLOBAL)
    set_target_properties(Boost::${LIB} PROPERTIES
        IMPORTED_LOCATION "${Boost_LIBRARY_DIR}/libboost_${LIB}.so"
        INTERFACE_INCLUDE_DIRECTORIES "${Boost_INCLUDE_DIR}"
    )
endforeach()

# some libs are not bundlen into the apk by default but needed for another lib
target_link_libraries(Boost::program_options INTERFACE Boost::container)
target_link_libraries(Boost::filesystem INTERFACE Boost::atomic)
target_link_libraries(Boost::iostreams INTERFACE Boost::random Boost::regex)
target_link_libraries(Boost::locale INTERFACE Boost::thread Boost::chrono Boost::date_time)



list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/s25client/cmake/Modules" "${CMAKE_CURRENT_SOURCE_DIR}/s25client/external/libutil/cmake")
set(RTTR_BUILD_UPDATER OFF CACHE INTERNAL "Disabled" FORCE)
set(BUILD_TESTING OFF CACHE INTERNAL "" FORCE)
set(RTTR_BUILD_LIB ON CACHE INTERNAL "" FORCE)
add_subdirectory(s25client)
