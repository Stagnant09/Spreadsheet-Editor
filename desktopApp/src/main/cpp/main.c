#include <stdio.h>
#include <string.h>
#include "Cell.h"
#include "Functions.h"
#include "Parser.h"

static void print_help(void) {
    printf("\n"
           "  ┌──────────────────────────────────────────────────────────────┐\n"
           "  │         Terminal Spreadsheet  –  Command Reference           │\n"
           "  ├──────────────────────────────────────────────────────────────┤\n"
           "  │  ASSIGNMENT                                                  │\n"
           "  │    A0 = 5          literal                                   │\n"
           "  │    A2 = A1 + A0 + 4                                          │\n"
           "  │    A3 = (A2 + A1) / 2 + A5                                   │\n"
           "  │    A4 = A0 * A1 - A2 * 3 + (A3 ^ 2)                          │\n"
           "  │                                                              │\n"
           "  │  AGGREGATE  (range or two cells)                             │\n"
           "  │    A5 = SUM(A0:A4)   AVG(A0:A4)   MIN(A0:A4)   MAX(A0:A4)    │\n"
           "  │    A5 = PRODUCT(A0:A4)   COUNT(A0:A4)                        │\n"
           "  │    A5 = STDEV(A0:A4)     MEDIAN(A0:A4)                       │\n"
           "  │                                                              │\n"
           "  │  MAP  (single cell -> result)                                │\n"
           "  │    B0 = SQRT(A0)   ABS(A0)   LOG(A0)   EXP(A0)               │\n"
           "  │    B0 = CEIL(A0)   FLOOR(A0) NEG(A0)   INV(A0)               │\n"
           "  │    B0 = SIN(A0)    COS(A0)   TAN(A0)   SQUARE(A0)            │\n"
           "  │    B0 = CUBE(A0)   DOUBLE(A0)                                │\n"
           "  │                                                              │\n"
           "  │  COLLECTIVE MAP  (transforms range in-place)                 │\n"
           "  │    A0 = SQRT_RANGE(A0:A4)   ABS_RANGE(A0:A4)                 │\n"
           "  │    A0 = NEG_RANGE(A0:A4)    SQUARE_RANGE(A0:A4)              │\n"
           "  │                                                              │\n"
           "  │  SPECIAL COMMANDS                                            │\n"
           "  │    help   show this message                                  │\n"
           "  │    exit   quit                                               │\n"
           "  └──────────────────────────────────────────────────────────────┘\n\n");
}

int main(void) {
    Matrix matrix;
    init(&matrix, 6, 6);

    print_help();
    char buffer[512];

    while (1) {
        printf("> ");
        fflush(stdout);
        if (fgets(buffer, sizeof(buffer), stdin) == NULL) break;
        buffer[strcspn(buffer, "\n")] = '\0';

        if (strcmp(buffer, "exit") == 0 || strcmp(buffer, "quit") == 0) break;
        if (strcmp(buffer, "help") == 0) { print_help(); continue; }
        if (strlen(buffer) == 0) continue;

        process_command(&matrix, buffer);
        print_matrix(&matrix);
    }

    matrix_destroy(&matrix);
    return 0;
}
