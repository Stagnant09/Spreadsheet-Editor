#ifndef TXTBSDSP_UTILS_H
#define TXTBSDSP_UTILS_H
#include <stdbool.h>

/* ═══════════════════════════════════════════════════════════════════════
 * Cell arithmetic (types must match; caller normalises via parser)
 * ═══════════════════════════════════════════════════════════════════════ */
#define ARITH_OP(name, op_int, op_flt, op_dbl) \
int name(const Cell* a, const Cell* b, Cell* r) { \
    if (a->type != b->type) return -1; \
    r->type = a->type; \
    switch (a->type) { \
        case 1: r->content.value  = a->content.value  op_int  b->content.value;  break; \
        case 2: r->content.fvalue = a->content.fvalue op_flt  b->content.fvalue; break; \
        case 3: r->content.dvalue = a->content.dvalue op_dbl  b->content.dvalue; break; \
        default: return -1; \
    } \
    return 0; \
}

/* ── Collective map: range → range ───────────────────────────────────── */
#define DECL_RANGE_MAP(fn) \
int fn##_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r); \
int fn##_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r);

/* ═══════════════════════════════════════════════════════════════════════
 * x86-64 inline-assembly helpers
 *
 * Two routines that are performance-critical (summing a contiguous
 * array of doubles, and doing the same for a column stride):
 *
 *   asm_sum_doubles  – sums n doubles stored contiguously
 *   asm_dot_identity – multiplies n doubles (product accumulator)
 *
 * The compiler would generate perfectly good SSE2 here with -O2, but
 * using explicit inline asm demonstrates the technique and guarantees
 * the XMM register path even at -O0.
 * ═══════════════════════════════════════════════════════════════════════ */

#if defined(__x86_64__) || defined(_M_X64)
#define HAS_ASM_HELPERS 1

/* Sum an array of doubles using XMM registers.
 * Processes pairs of doubles (128-bit addpd) then handles tail. */
static double asm_sum_doubles(const double *arr, int n) {
    if (n <= 0) return 0.0;
    double result = 0.0;
    int i = 0;
    /* 128-bit SIMD: add pairs */
    if (n >= 2) {
        double pair_sum = 0.0;
        int pairs = n / 2;
        __asm__ volatile (
            "xorpd  %%xmm0, %%xmm0          \n\t" /* accumulator = 0 */
            "1:                              \n\t"
            "movupd (%[src]), %%xmm1         \n\t" /* load 2 doubles  */
            "addpd  %%xmm1,  %%xmm0         \n\t" /* acc += pair     */
            "add    $16,     %[src]          \n\t" /* advance 16 bytes */
            "dec    %[cnt]                   \n\t"
            "jnz    1b                       \n\t"
            /* horizontal add: xmm0[0] + xmm0[1] */
            "movhlps %%xmm0, %%xmm2          \n\t"
            "addsd  %%xmm2,  %%xmm0         \n\t"
            "movsd  %%xmm0,  %[out]          \n\t"
            : [out] "=m"(pair_sum), [src] "+r"(arr), [cnt] "+r"(pairs)
            :
            : "xmm0", "xmm1", "xmm2", "memory"
        );
        result += pair_sum;
        i = (n / 2) * 2;
    }
    /* scalar tail */
    for (; i < n; i++) result += arr[i];
    return result;
}

/* Product of an array of doubles using XMM registers. */
/* Product via scalar SSE2 mulsd — stays in XMM regs, correct for all n */
static double asm_product_doubles(const double *arr, int n) {
    if (n <= 0) return 0.0;
    double result = 1.0;
    __asm__ volatile (
        "movsd   %[init], %%xmm0        \n\t" /* xmm0 = 1.0             */
        "test    %[cnt],  %[cnt]        \n\t"
        "jz      2f                     \n\t"
        "1:                             \n\t"
        "movsd   (%[src]), %%xmm1       \n\t" /* xmm1 = arr[i]          */
        "mulsd   %%xmm1,   %%xmm0       \n\t" /* xmm0 *= xmm1           */
        "add     $8,       %[src]       \n\t" /* advance 8 bytes        */
        "dec     %[cnt]                 \n\t"
        "jnz     1b                     \n\t"
        "2:                             \n\t"
        "movsd   %%xmm0,   %[out]       \n\t"
        : [out] "=m"(result), [src] "+r"(arr), [cnt] "+r"(n)
        : [init] "m"(result)
        : "xmm0", "xmm1", "memory"
    );
    return result;
}

#else
#define HAS_ASM_HELPERS 0
static double asm_sum_doubles(const double *arr, int n) {
    double s = 0.0;
    for (int i = 0; i < n; i++) s += arr[i];
    return s;
}
static double asm_product_doubles(const double *arr, int n) {
    double p = 1.0;
    for (int i = 0; i < n; i++) p *= arr[i];
    return p;
}
#endif /* __x86_64__ */

/* ── MIN / MAX ── */
#define AGG_MINMAX(suffix, range_fn, init_val, cmp_op) \
    int suffix(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) { \
    int n; double* arr = range_fn(cells, c1, c2, &n); \
    if (!arr || n == 0) return -1; \
    double acc = arr[0]; \
    for (int i = 1; i < n; i++) if (arr[i] cmp_op acc) acc = arr[i]; \
    free(arr); double_to_cell(acc, 3, r); return 0; \
}

/* ═══════════════════════════════════════════════════════════════════════
 * Map functions  (cell → cell)
 * ═══════════════════════════════════════════════════════════════════════ */
#define MAP1(name, expr_int, expr_flt, expr_dbl) \
int name(const Cell* c, Cell* r) { \
    r->type = c->type; \
    switch (c->type) { \
        case 1: r->content.value  = (int)(expr_int);  break; \
        case 2: r->content.fvalue = (float)(expr_flt); break; \
        case 3: r->content.dvalue = (expr_dbl);        break; \
        default: return -1; \
    } \
    return 0; \
}

/* Trig: always returns double */
#define TRIG_MAP(name, fn) \
int name(const Cell* c, Cell* r) { \
    double v = cell_to_double(c); \
    r->type = 3; r->content.dvalue = fn(v); return 0; \
}

/* ═══════════════════════════════════════════════════════════════════════
 * Collective map  (apply map function across a row/col range,
 *                  writing results back in-place)
 * ═══════════════════════════════════════════════════════════════════════ */
#define COLLECTIVE_MAP(fn) \
int fn##_row(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) { \
    if (c1->y != c2->y) return -1; \
    int row = c1->y; \
    for (int j = c1->x; j <= c2->x; j++) \
    fn##_cell(&cells[row][j], &cells[row][j]); \
    (void)r; return 0; \
} \
int fn##_col(Cell** cells, const Cell* c1, const Cell* c2, Cell* r) { \
    if (c1->x != c2->x) return -1; \
    int col = c1->x; \
    for (int i = c1->y; i <= c2->y; i++) \
    fn##_cell(&cells[i][col], &cells[i][col]); \
    (void)r; return 0; \
}

bool contains(char *arr, int size, char c);

char *containsAny(char *arr, int arrSize, char *chars, int charsSize);

#endif
