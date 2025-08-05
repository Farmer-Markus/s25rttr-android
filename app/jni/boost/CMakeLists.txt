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
    numeric_conversion
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
)

set(INCLUDE_DEST "${CMAKE_BINARY_DIR}/boost")
file(MAKE_DIRECTORY ${INCLUDE_DEST}) # Should already exist

add_library(Boost::boost INTERFACE IMPORTED GLOBAL)
target_include_directories(Boost::boost INTERFACE "${INCLUDE_DEST}/include")

add_library(Boost::disable_autolinking INTERFACE IMPORTED GLOBAL)

foreach(LIB IN LISTS BOOST_LIBS)
  add_subdirectory(${LIB})
  file(COPY "${LIB}/include" DESTINATION "${INCLUDE_DEST}")
endforeach()

set(Boost_INCLUDE_DIR "${INCLUDE_DEST}/include" CACHE PATH "Boost include dir" FORCE)

#target_include_directories(boost_ INTERFACE "${INCLUDE_DEST}/include")

# because external/libutil does NOT include Boost::boost but wants a header
target_include_directories(boost_filesystem INTERFACE "${INCLUDE_DEST}/include")
