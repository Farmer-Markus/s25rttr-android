cmake_minimum_required(VERSION 3.6)

project(boost-android)
add_compile_options(-Wno-error=deprecated-copy)

set(BOOST_LIBS 
    filesystem
    iostreams
    program_options
    test
    nowide
    assert
    any
    algorithm
    config
    bind
    array
    container_hash
    core
    throw_exception
    concept_check
    detail
    type_index
    exception
    static_assert
    describe
    preprocessor
    io
    function
    type_traits
    smart_ptr
    mp11
    iterator
    integer
    tuple
    move
    mpl
    lexical_cast
    function_types
    container
    predef
    fusion
    range
    tokenizer
    random
    system
    variant2
    dynamic_bitset
    conversion
    typeof
    intrusive
    utility
    regex
    optional
    scope
    atomic
    unordered
    functional
    winapi
    align
    locale
    thread
    chrono
    ratio
    date_time
    endian
    ptr_container
    format
    circular_buffer
    dll
    signals2
    variant
    parameter
    interprocess
    numeric_conversion
)

set(BUILD_SHARED_LIBS TRUE)
set(LIB_DEST "${INSTALL_PATH}/boost")
file(MAKE_DIRECTORY ${LIB_DEST})


foreach(LIB IN LISTS BOOST_LIBS)  
  if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/${LIB}/include")
    file(COPY "${CMAKE_CURRENT_SOURCE_DIR}/${LIB}/include" DESTINATION "${LIB_DEST}")
  endif()
endforeach()

set(Boost_INCLUDE_DIR "${LIB_DEST}/include" CACHE PATH "" FORCE)
add_library(Boost::boost INTERFACE IMPORTED GLOBAL)
target_include_directories(Boost::boost INTERFACE "${LIB_DEST}/include")

foreach(LIB IN LISTS BOOST_LIBS)
  add_subdirectory("${LIB}")
endforeach()


add_custom_target(install_boost ALL
	COMMAND ${CMAKE_COMMAND} -E make_directory "${LIB_DEST}/${ANDROID_ABI}"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/atomic/libboost_atomic.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/chrono/libboost_chrono.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/locale/libboost_locale.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/nowide/libboost_nowide.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/random/libboost_random.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/thread/libboost_thread.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/container/libboost_container.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/date_time/libboost_date_time.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/iostreams/libboost_iostreams.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/filesystem/libboost_filesystem.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/program_options/libboost_program_options.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/test/libboost_prg_exec_monitor.so" "${LIB_DEST}/${ANDROID_ABI}/"
	COMMAND ${CMAKE_COMMAND} -E copy "${CMAKE_BINARY_DIR}/boost/test/libboost_unit_test_framework.so" "${LIB_DEST}/${ANDROID_ABI}/"
	DEPENDS boost_atomic boost_chrono boost_locale boost_nowide boost_random boost_thread boost_container boost_date_time boost_iostreams boost_filesystem boost_program_options boost_unit_test_framework boost_prg_exec_monitor
)
