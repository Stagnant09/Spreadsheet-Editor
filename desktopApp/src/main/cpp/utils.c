#include <stdbool.h>
#include <string.h>

bool contains(char* arr, int size, char c) {
    for (int i = 0; i < size; i++) if (arr[i] == c) return true;
    return false;
}
char* containsAny(char* arr, int arrSize, char* chars, int charsSize) {
    for (int i = 0; i < arrSize; i++)
        if (contains(chars, charsSize, arr[i])) return &arr[i];
    return NULL;
}


