cmake_minimum_required(VERSION 3.15)
project(spreadsheet_native C)

set(CMAKE_C_STANDARD 11)

# JNI headers — point to your JDK
find_package(JNI REQUIRED)

add_library(spreadsheet_native SHARED
    desktopApp/src/main/cpp/Functions.c
    desktopApp/src/main/cpp/Parser.c
    desktopApp/src/main/cpp/utils.c
    desktopApp/src/main/cpp/jni_bridge.c
)

target_include_directories(spreadsheet_native PRIVATE
    desktopApp/src/main/cpp
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(spreadsheet_native ${JNI_LIBRARIES} m)

if(MINGW)
    target_link_options(spreadsheet_native PRIVATE
        -static-libgcc
        -static-libstdc++
        -static
        -Wl,-Bstatic,--whole-archive -lwinpthread -Wl,--no-whole-archive
    )
endif()