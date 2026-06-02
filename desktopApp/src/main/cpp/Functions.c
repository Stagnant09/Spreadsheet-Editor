#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "Cell.h"
#include "Functions.h"

/* ═══════════════════════════════════════════════════════════════════════
 * Internal helpers
 * ═══════════════════════════════════════════════════════════════════════ */

/* Promote a cell's value to double regardless of type */
static inline double cell_to_double(const Cell* c) {
    if (c->type == 0) return 0.0;
    if (c->type == 4) {
        if (c->content.string == NULL) return 0.0;
        return strtod(c->content.string, NULL);
    }
    switch (c->type) {
        case 1: return (double)c->content.value;
        case 2: return (double)c->content.fvalue;
        case 3: return c->content.dvalue;
        default: return 0.0;
    }
}

/* Write a double back to a result cell, inheriting the dominant type */
static inline void double_to_cell(double v, short type, Cell* r) {
    r->type = (type >= 1 && type <= 3) ? type : 3;
    switch (r->type) {
        case 1: r->content.value  = (int)v;    break;
        case 2: r->content.fvalue = (float)v;  break;
        case 3: r->content.dvalue = v;          break;
    }
}

/* Comparison for qsort (doubles) */
static int cmp_double(const void* a, const void* b) {
    double da = *(const double*)a;
    double db = *(const double*)b;
    return (da > db) - (da < db);
}



/* ═══════════════════════════════════════════════════════════════════════
 * Initialization / memory management
 * ═══════════════════════════════════════════════════════════════════════ */

int cell_create(Matrix* matrix, int x, int y, CellContent content, short type) {
    if (x < 0 || y < 0) return -1;
    if (x >= matrix->cols || y >= matrix->rows)
        expand(matrix, y + 1, x + 1);
    Cell* cell = &matrix->cells[y][x];
    cell->x = x; cell->y = y;
    cell->content = content; cell->type = type;
    return 0;
}

int cell_destroy(Matrix* matrix, Cell* cell) {
    (void)matrix;
    cell->x = cell->y = 0;
    cell->content.value = 0; cell->type = 0;
    return 0;
}

int cell_set_content(Cell* cell, CellContent content) { cell->content = content; return 0; }
int cell_get_content(const Cell* cell, CellContent* content) { *content = cell->content; return 0; }
int cell_set_type(Cell* cell, short type) { cell->type = type; return 0; }
int cell_get_type(const Cell* cell, short* type) { *type = cell->type; return 0; }
int set_matrix(Matrix* matrix, Cell** cells) { matrix->cells = cells; return 0; }
int get_matrix(const Matrix* matrix, Cell** cells) { *cells = *matrix->cells; return 0; }
int set_rows(Matrix* matrix, int rows) { matrix->rows = rows; return 0; }
int get_rows(const Matrix* matrix, int* rows) { *rows = matrix->rows; return 0; }
int set_cols(Matrix* matrix, int cols) { matrix->cols = cols; return 0; }
int get_cols(const Matrix* matrix, int* cols) { *cols = matrix->cols; return 0; }

int init(Matrix* matrix, int rows, int cols) {
    matrix->rows = rows; matrix->cols = cols;
    matrix->cells = malloc(rows * sizeof(Cell*));
    for (int i = 0; i < rows; i++) {
        matrix->cells[i] = calloc(cols, sizeof(Cell));
    }
    return 0;
}

int matrix_destroy(Matrix* matrix) {
    for (int i = 0; i < matrix->rows; i++) free(matrix->cells[i]);
    free(matrix->cells);
    matrix->cells = NULL;
    matrix->rows = matrix->cols = 0;
    return 0;
}

void expand(Matrix* matrix, int rows, int cols) {
    int nr = (rows > matrix->rows) ? rows : matrix->rows;
    int nc = (cols > matrix->cols) ? cols : matrix->cols;
    Cell** nc_arr = malloc(nr * sizeof(Cell*));
    for (int i = 0; i < nr; i++) {
        nc_arr[i] = calloc(nc, sizeof(Cell));
        for (int j = 0; j < nc; j++) {
            if (i < matrix->rows && j < matrix->cols)
                nc_arr[i][j] = matrix->cells[i][j];
        }
    }
    for (int i = 0; i < matrix->rows; i++) free(matrix->cells[i]);
    free(matrix->cells);
    matrix->cells = nc_arr;
    matrix->rows = nr; matrix->cols = nc;
}

ARITH_OP(add, +, +, +)
ARITH_OP(sub, -, -, -)
ARITH_OP(mul, *, *, *)

int divc(const Cell* a, const Cell* b, Cell* r) {
    if (a->type != b->type) return -1;
    r->type = a->type;
    switch (a->type) {
        case 1:
            if (b->content.value == 0) return -1;
            r->type = 3;
            r->content.dvalue = (double)a->content.value / b->content.value;
            break;
        case 2:
            if (b->content.fvalue == 0.0f) return -1;
            r->content.fvalue = a->content.fvalue / b->content.fvalue;
            break;
        case 3:
            if (b->content.dvalue == 0.0) return -1;
            r->content.dvalue = a->content.dvalue / b->content.dvalue;
            break;
        default: return -1;
    }
    return 0;
}

int mod(const Cell* a, const Cell* b, Cell* r) {
    if (a->type != 1 || b->type != 1) return -1;
    if (b->content.value == 0) return -1;
    r->type = 1;
    r->content.value = a->content.value % b->content.value;
    return 0;
}

int powc(const Cell* a, const Cell* b, Cell* r) {
    if (a->type != b->type) return -1;
    r->type = a->type;
    switch (a->type) {
        case 1: r->content.value  = (int)pow(a->content.value,  b->content.value);  break;
        case 2: r->content.fvalue = powf(a->content.fvalue, b->content.fvalue);     break;
        case 3: r->content.dvalue = pow (a->content.dvalue, b->content.dvalue);     break;
        default: return -1;
    }
    return 0;
}

/* ═══════════════════════════════════════════════════════════════════════
 * I/O
 * ═══════════════════════════════════════════════════════════════════════ */

int print(const Cell* c) {
    switch (c->type) {
        case 1: printf("(%d,%d) %d",  c->x, c->y, c->content.value);  break;
        case 2: printf("(%d,%d) %f",  c->x, c->y, c->content.fvalue); break;
        case 3: printf("(%d,%d) %g",  c->x, c->y, c->content.dvalue); break;
        case 4: printf("(%d,%d) %s",  c->x, c->y, c->content.string); break;
        default: return -1;
    }
    return 0;
}

int print_matrix(const Matrix* matrix) {
    if (!matrix || !matrix->cells) return -1;

    /* Column header */
    printf("    ");
    for (int j = 0; j < matrix->cols; j++)
        printf("  %-10c", 'A' + j);
    printf("\n    ");
    for (int j = 0; j < matrix->cols; j++)
        printf("+-----------");
    printf("+\n");

    for (int i = 0; i < matrix->rows; i++) {
        printf("%3d |", i);
        for (int j = 0; j < matrix->cols; j++) {
            const Cell* cell = &matrix->cells[i][j];
            char buf[11]; buf[0] = '\0';
            switch (cell->type) {
                case 1: snprintf(buf, sizeof(buf), "%d",    cell->content.value);  break;
                case 2: snprintf(buf, sizeof(buf), "%.4g",  cell->content.fvalue); break;
                case 3: snprintf(buf, sizeof(buf), "%.4g",  cell->content.dvalue); break;
                case 4:
                    if (cell->content.string)
                        snprintf(buf, sizeof(buf), "%s", cell->content.string);
                    break;
                default: break;
            }
            printf(" %-9s |", buf);
        }
        printf("\n    ");
        for (int j = 0; j < matrix->cols; j++) printf("+-----------");
        printf("+\n");
    }
    return 0;
}

/* ═══════════════════════════════════════════════════════════════════════
 * Aggregate functions  (ASM-accelerated where applicable)
 * ═══════════════════════════════════════════════════════════════════════ */

/* ── ADD ── */
int add_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double sum = 0;
    for (int j = c1->x; j <= c2->x; j++) {
        sum += cell_to_double(&cells[c1->y][j]);
    }
    double_to_cell(sum, 3, r);
    return 0;
}
int add_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double sum = 0;
    for (int i = c1->y; i <= c2->y; i++) {
        sum += cell_to_double(&cells[i][c1->x]);
    }
    double_to_cell(sum, 3, r);
    return 0;
}

/* ── AVG ── */
int avg_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double sum = 0;
    int count = 0;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            sum += cell_to_double(&cells[c1->y][j]);
            count++;
        }
    }
    if (count == 0) return -1;
    double_to_cell(sum / count, 3, r);
    return 0;
}
int avg_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double sum = 0;
    int count = 0;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            sum += cell_to_double(&cells[i][c1->x]);
            count++;
        }
    }
    if (count == 0) return -1;
    double_to_cell(sum / count, 3, r);
    return 0;
}

/* ── SUB ── */
int sub_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double v1 = cell_to_double(c1);
    double v2 = cell_to_double(c2);
    double_to_cell(v1 - v2, 3, r);
    return 0;
}
int sub_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double v1 = cell_to_double(c1);
    double v2 = cell_to_double(c2);
    double_to_cell(v1 - v2, 3, r);
    return 0;
}

/* ── MIN/MAX ── */
int min_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double min_v = 0;
    int first = 1;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            double v = cell_to_double(&cells[c1->y][j]);
            if (first || v < min_v) { min_v = v; first = 0; }
        }
    }
    if (first) return -1;
    double_to_cell(min_v, 3, r);
    return 0;
}
int min_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double min_v = 0;
    int first = 1;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            double v = cell_to_double(&cells[i][c1->x]);
            if (first || v < min_v) { min_v = v; first = 0; }
        }
    }
    if (first) return -1;
    double_to_cell(min_v, 3, r);
    return 0;
}
int max_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double max_v = 0;
    int first = 1;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            double v = cell_to_double(&cells[c1->y][j]);
            if (first || v > max_v) { max_v = v; first = 0; }
        }
    }
    if (first) return -1;
    double_to_cell(max_v, 3, r);
    return 0;
}
int max_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double max_v = 0;
    int first = 1;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            double v = cell_to_double(&cells[i][c1->x]);
            if (first || v > max_v) { max_v = v; first = 0; }
        }
    }
    if (first) return -1;
    double_to_cell(max_v, 3, r);
    return 0;
}

/* ── PRODUCT ── */
int product_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double p = 1.0;
    int count = 0;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            p *= cell_to_double(&cells[c1->y][j]);
            count++;
        }
    }
    double_to_cell(count > 0 ? p : 0.0, 3, r);
    return 0;
}
int product_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double p = 1.0;
    int count = 0;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            p *= cell_to_double(&cells[i][c1->x]);
            count++;
        }
    }
    double_to_cell(count > 0 ? p : 0.0, 3, r);
    return 0;
}

/* ── COUNT ── */
int count_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    int count = 0;
    for (int j = c1->x; j <= c2->x; j++)
        if (cells[c1->y][j].type != 0) count++;
    r->type = 1; r->content.value = count; return 0;
}
int count_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    int count = 0;
    for (int i = c1->y; i <= c2->y; i++)
        if (cells[i][c1->x].type != 0) count++;
    r->type = 1; r->content.value = count; return 0;
}

/* ── STDEV (sample) ── */
int stdev_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    double sum = 0;
    int n = 0;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            sum += cell_to_double(&cells[c1->y][j]);
            n++;
        }
    }
    if (n < 2) return -1;
    double mean = sum / n;
    double sq = 0.0;
    for (int j = c1->x; j <= c2->x; j++) {
        if (cells[c1->y][j].type != 0) {
            double d = cell_to_double(&cells[c1->y][j]) - mean;
            sq += d * d;
        }
    }
    double_to_cell(sqrt(sq / (n - 1)), 3, r);
    return 0;
}
int stdev_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    double sum = 0;
    int n = 0;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            sum += cell_to_double(&cells[i][c1->x]);
            n++;
        }
    }
    if (n < 2) return -1;
    double mean = sum / n;
    double sq = 0.0;
    for (int i = c1->y; i <= c2->y; i++) {
        if (cells[i][c1->x].type != 0) {
            double d = cell_to_double(&cells[i][c1->x]) - mean;
            sq += d * d;
        }
    }
    double_to_cell(sqrt(sq / (n - 1)), 3, r);
    return 0;
}

/* ── MEDIAN ── */
static int _median(double* arr, int n, Cell* r) {
    if (n <= 0) return -1;
    qsort(arr, n, sizeof(double), cmp_double);
    double med = (n % 2 == 1) ? arr[n/2]
                               : (arr[n/2 - 1] + arr[n/2]) / 2.0;
    double_to_cell(med, 3, r);
    return 0;
}
int median_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->y != c2->y) return -1;
    int n = 0;
    for (int j = c1->x; j <= c2->x; j++)
        if (cells[c1->y][j].type != 0) n++;
    if (n <= 0) return -1;
    double* arr = malloc(n * sizeof(double));
    int idx = 0;
    for (int j = c1->x; j <= c2->x; j++)
        if (cells[c1->y][j].type != 0)
            arr[idx++] = cell_to_double(&cells[c1->y][j]);
    int rc = _median(arr, n, r); free(arr); return rc;
}
int median_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) {
    if (c1->x != c2->x) return -1;
    int n = 0;
    for (int i = c1->y; i <= c2->y; i++)
        if (cells[i][c1->x].type != 0) n++;
    if (n <= 0) return -1;
    double* arr = malloc(n * sizeof(double));
    int idx = 0;
    for (int i = c1->y; i <= c2->y; i++)
        if (cells[i][c1->x].type != 0)
            arr[idx++] = cell_to_double(&cells[i][c1->x]);
    int rc = _median(arr, n, r); free(arr); return rc;
}

MAP1(abs_cell,    abs(c->content.value),
                  fabsf(c->content.fvalue),
                  fabs(c->content.dvalue))
MAP1(double_cell, c->content.value  * 2,
                  c->content.fvalue * 2.0f,
                  c->content.dvalue * 2.0)
MAP1(square_cell, c->content.value  * c->content.value,
                  c->content.fvalue * c->content.fvalue,
                  c->content.dvalue * c->content.dvalue)
MAP1(cube_cell,   c->content.value  * c->content.value  * c->content.value,
                  c->content.fvalue * c->content.fvalue * c->content.fvalue,
                  c->content.dvalue * c->content.dvalue * c->content.dvalue)
MAP1(log_cell,    (int)log((double)c->content.value),
                  logf(c->content.fvalue),
                  log(c->content.dvalue))
MAP1(exp_cell,    (int)exp((double)c->content.value),
                  expf(c->content.fvalue),
                  exp(c->content.dvalue))
MAP1(sqrt_cell,   (int)sqrt((double)c->content.value),
                  sqrtf(c->content.fvalue),
                  sqrt(c->content.dvalue))
MAP1(ceil_cell,   c->content.value,
                  ceilf(c->content.fvalue),
                  ceil(c->content.dvalue))
MAP1(floor_cell,  c->content.value,
                  floorf(c->content.fvalue),
                  floor(c->content.dvalue))
MAP1(negate_cell, -c->content.value,
                  -c->content.fvalue,
                  -c->content.dvalue)

int inv_cell(const Cell* c, Cell* r) {
    double v = cell_to_double(c);
    if (v == 0.0) { printf("Division by zero in INV\n"); return -1; }
    double_to_cell(1.0 / v, 3, r);
    return 0;
}

TRIG_MAP(sin_cell, sin)
TRIG_MAP(cos_cell, cos)
TRIG_MAP(tan_cell, tan)

COLLECTIVE_MAP(abs)
COLLECTIVE_MAP(double)
COLLECTIVE_MAP(square)
COLLECTIVE_MAP(cube)
COLLECTIVE_MAP(log)
COLLECTIVE_MAP(exp)
COLLECTIVE_MAP(sqrt)
COLLECTIVE_MAP(negate)
