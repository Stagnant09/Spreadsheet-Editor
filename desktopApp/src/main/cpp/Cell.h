#ifndef TXTBSDSP_CELL_H
#define TXTBSDSP_CELL_H

typedef union {
    int    value;
    float  fvalue;
    double dvalue;
    char*  string;
} CellContent;

/* type codes:
 * 0 = empty
 * 1 = int
 * 2 = float
 * 3 = double
 * 4 = string
 * 5 = error   (content.value holds an ERR_* code, see below)
 */

/* Error codes stored in content.value when type == 5 */
#define ERR_GENERIC  1   /* #ERR!    generic parse/eval failure          */
#define ERR_DIV0     2   /* #DIV/0!  division by zero                    */
#define ERR_REF      3   /* #REF!    formula reads a cell that is itself
                             an error (propagated/cascading error)       */
typedef struct {
    int   x;
    int   y;
    CellContent content;
    short type;
} Cell;

typedef struct {
    Cell** cells;
    int    rows;
    int    cols;
} Matrix;

#endif /* TXTBSDSP_CELL_H */
