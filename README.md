# Spreadsheet Editor

A cross-platform spreadsheet application built with Kotlin Multiplatform and a high-performance calculation engine written in C.

## Project Overview

This project is a modern spreadsheet editor that combines the ease of use of a high-level user interface with the raw power of a native calculation engine. It allows users to manage data, perform complex mathematical calculations, and handle spreadsheet formulas in real-time.

### Key Features

- Interactive Data Grid: A responsive interface for entering and editing data across rows and columns.
- Formula Support: Built-in support for common spreadsheet functions like SUM, AVG, MIN, MAX, and more.
- Native Calculation Engine: A custom-built engine written in C to ensure that calculations are processed quickly and accurately.
- Cross-Platform Foundation: Developed using Kotlin Multiplatform, designed to share core logic across different operating systems while maintaining a native feel.

## Technical Highlights

- Hybrid Architecture: Integrates Kotlin/JVM for the desktop user interface with C code via Java Native Interface (JNI).
- Robust Formula Parser: A custom parser in C that handles cell ranges, mathematical precedence, and error checking.
- Optimized Memory Management: Efficient data handling between the user interface and the calculation engine to ensure smooth performance even with many cells.

## Running the Application

To run the desktop application, you can use the following Gradle command:

```bash
./gradlew :desktopApp:run
```

The application requires a C compiler (like GCC or Clang) and CMake to be installed on your system to build the native engine components.