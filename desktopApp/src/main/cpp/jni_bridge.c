#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "Functions.h"
#include "Parser.h"

/* One global matrix for now — later you can pass a pointer handle */
static Matrix g_matrix = {0};
static int    g_initialized = 0;

/* my.cmp.spreadsheeteditor.NativeBridge.init(rows, cols) */
JNIEXPORT void JNICALL
Java_my_cmp_spreadsheeteditor_NativeBridge_init(JNIEnv* env, jobject obj,
                                                  jint rows, jint cols) {
    (void)env; (void)obj;
    if (g_initialized) matrix_destroy(&g_matrix);
    init(&g_matrix, (int)rows, (int)cols);
    g_initialized = 1;
}

/* NativeBridge.processCommand(command: String) */
JNIEXPORT void JNICALL
Java_my_cmp_spreadsheeteditor_NativeBridge_processCommand(JNIEnv* env, jobject obj,
                                                            jstring command) {
    (void)obj;
    const char* cmd = (*env)->GetStringUTFChars(env, command, NULL);
    char buf[512];
    strncpy(buf, cmd, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';
    (*env)->ReleaseStringUTFChars(env, command, cmd);
    process_command(&g_matrix, buf);
}

/* NativeBridge.getCellValue(row: Int, col: Int): String */
JNIEXPORT jstring JNICALL
Java_my_cmp_spreadsheeteditor_NativeBridge_getCellValue(JNIEnv* env, jobject obj,
                                                          jint row, jint col) {
    (void)obj;
    if (!g_initialized ||
        (int)row >= g_matrix.rows ||
        (int)col >= g_matrix.cols) {
        return (*env)->NewStringUTF(env, "");
    }
    Cell* cell = &g_matrix.cells[(int)row][(int)col];
    char buf[64]; buf[0] = '\0';
    switch (cell->type) {
        case 1: snprintf(buf, sizeof(buf), "%d",    cell->content.value);  break;
        case 2: snprintf(buf, sizeof(buf), "%.4g",  cell->content.fvalue); break;
        case 3: snprintf(buf, sizeof(buf), "%.4g",  cell->content.dvalue); break;
        case 4:
            if (cell->content.string != NULL)
                snprintf(buf, sizeof(buf), "%s", cell->content.string);
            break;
        case 5:
            switch (cell->content.value) {
                case ERR_DIV0: snprintf(buf, sizeof(buf), "#DIV/0!"); break;
                case ERR_REF:  snprintf(buf, sizeof(buf), "#REF!");   break;
                default:       snprintf(buf, sizeof(buf), "#ERR!");  break;
            }
            break;
        default: break;
    }
    return (*env)->NewStringUTF(env, buf);
}

/* NativeBridge.getRows(): Int */
JNIEXPORT jint JNICALL
Java_my_cmp_spreadsheeteditor_NativeBridge_getRows(JNIEnv* env, jobject obj) {
    (void)env; (void)obj;
    return (jint)(g_initialized ? g_matrix.rows : 0);
}

/* NativeBridge.getCols(): Int */
JNIEXPORT jint JNICALL
Java_my_cmp_spreadsheeteditor_NativeBridge_getCols(JNIEnv* env, jobject obj) {
    (void)env; (void)obj;
    return (jint)(g_initialized ? g_matrix.cols : 0);
}