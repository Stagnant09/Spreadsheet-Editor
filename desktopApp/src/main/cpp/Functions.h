#ifndef TXTBSDSP_FUNCTIONS_H
#define TXTBSDSP_FUNCTIONS_H

#include "Cell.h"
#include "utils.h"

/* ── Initialization / memory ─────────────────────────────────────────── */
int  cell_create(Matrix* matrix, int x, int y, CellContent content, short type);
int  cell_destroy(Matrix* matrix, Cell* cell);
int  cell_set_content(Cell* cell, CellContent content);
int  cell_get_content(const Cell* cell, CellContent* content);
int  cell_set_type(Cell* cell, short type);
int  cell_get_type(const Cell* cell, short* type);
int  set_matrix(Matrix* matrix, Cell** cells);
int  get_matrix(const Matrix* matrix, Cell** cells);
int  set_rows(Matrix* matrix, int rows);
int  get_rows(const Matrix* matrix, int* rows);
int  set_cols(Matrix* matrix, int cols);
int  get_cols(const Matrix* matrix, int* cols);
int  init(Matrix* matrix, int rows, int cols);
int  matrix_destroy(Matrix* matrix);
void expand(Matrix* matrix, int rows, int cols);

/* ── Cell-to-cell arithmetic ─────────────────────────────────────────── */
int add (const Cell* a, const Cell* b, Cell* r);
int sub (const Cell* a, const Cell* b, Cell* r);
int mul (const Cell* a, const Cell* b, Cell* r);
int divc(const Cell* a, const Cell* b, Cell* r);
int mod (const Cell* a, const Cell* b, Cell* r);
int powc(const Cell* a, const Cell* b, Cell* r);

/* ── I/O ─────────────────────────────────────────────────────────────── */
int print(const Cell* cell);
int print_matrix(const Matrix* matrix);

/* ── Aggregate: range → scalar  (ASM-accelerated inner loops) ───────── */
int add_row    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int add_col    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int avg_row    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int avg_col    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int sub_row    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int sub_col    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int min_row    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int min_col    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int max_row    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int max_col    (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int product_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int product_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int count_row  (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int count_col  (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int stdev_row  (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int stdev_col  (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int median_row (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);
int median_col (Cell** cells, const Cell* c1, const Cell* c2, Cell* r);

/* ── Map: cell → cell ────────────────────────────────────────────────── */
int abs_cell   (const Cell* c, Cell* r);
int double_cell(const Cell* c, Cell* r);
int square_cell(const Cell* c, Cell* r);
int cube_cell  (const Cell* c, Cell* r);
int log_cell   (const Cell* c, Cell* r);
int exp_cell   (const Cell* c, Cell* r);
int sqrt_cell  (const Cell* c, Cell* r);
int ceil_cell  (const Cell* c, Cell* r);
int floor_cell (const Cell* c, Cell* r);
int negate_cell(const Cell* c, Cell* r);
int inv_cell   (const Cell* c, Cell* r);
int sin_cell   (const Cell* c, Cell* r);
int cos_cell   (const Cell* c, Cell* r);
int tan_cell   (const Cell* c, Cell* r);

DECL_RANGE_MAP(abs)
DECL_RANGE_MAP(double)
DECL_RANGE_MAP(square)
DECL_RANGE_MAP(cube)
DECL_RANGE_MAP(log)
DECL_RANGE_MAP(exp)
DECL_RANGE_MAP(sqrt)
DECL_RANGE_MAP(negate)

#undef DECL_RANGE_MAP

#endif /* TXTBSDSP_FUNCTIONS_H */
