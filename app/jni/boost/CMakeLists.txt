cmake_minimum_required(VERSION 3.6)

project(boost-android)

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
)

# Include boost libs
foreach(LIB IN LISTS BOOST_LIBS)
  add_subdirectory(${LIB})
endforeach()

# Create include folder

