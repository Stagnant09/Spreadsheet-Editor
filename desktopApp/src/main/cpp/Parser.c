/*
 * Parser.c  –  Recursive-descent expression evaluator
 *
 * Supported command syntax
 * ─────────────────────────────────────────────────────────────────────
 *  Assignment     A2 = <expr>
 *  Expression     <expr>  ::=  <term>  { ('+' | '-') <term> }
 *  Term           <term>  ::=  <power> { ('*' | '/') <power> }
 *  Power          <power> ::=  <unary> { '^' <unary> }       (right-assoc)
 *  Unary          <unary> ::=  '-' <unary> | <primary>
 *  Primary        <primary> ::= '(' <expr> ')'
 *                             | FUNC1( <primary> )
 *                             | AGGFUNC( <range> )            e.g. ADD(A0:A4)
 *                             | AGGFUNC( <cell1>, <cell2> )   e.g. SUB(A0,B0)
 *                             | <cell-ref>                    e.g. A3
 *                             | <number>
 *
 *  Examples:
 *    A2 = A1 + A0 + 4
 *    A3 = (A2 + A1) / 2 + A5
 *    A4 = A0 * A1 - A2 * 3 + (A3 ^ 2)
 *    A5 = SUM(A0:A4)
 *    A6 = SQRT(A2)
 */

#include "Parser.h"
#include "Functions.h"
#include "utils.h"
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <stdlib.h>
#include <math.h>

static inline double _cell_val(const Cell* c) {
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

/* ───────────────────────────── token stream ───────────────────────────── */

typedef enum {
    TOK_EOF, TOK_NUM, TOK_CELL, TOK_IDENT,
    TOK_PLUS, TOK_MINUS, TOK_STAR, TOK_SLASH, TOK_CARET,
    TOK_LPAREN, TOK_RPAREN, TOK_COLON, TOK_COMMA
} TokKind;

typedef struct {
    TokKind kind;
    double  num;          /* TOK_NUM  */
    char    ident[32];    /* TOK_IDENT | TOK_CELL */
    int     cx, cy;       /* TOK_CELL  */
} Token;

typedef struct {
    const char* src;
    int         pos;
    Token       cur;
    Matrix*     matrix;
    int         error;      /* set on any parse/eval error */
    int         err_code;   /* ERR_GENERIC / ERR_DIV0 / ERR_REF, see Cell.h */
} Parser;

/* Raise an error on the parser, keeping the most specific code already set */
static void raise_error(Parser* p, int code) {
    p->error = 1;
    if (p->err_code == 0) p->err_code = code;
}

/* ─── multi-letter column decoding ──────────────────────────────────────
 * Bijective base-26 decoding so that A=0 .. Z=25, AA=26, AB=27, ...
 * This matches the historical single-letter behaviour (A-Z => 0-25)
 * while allowing arbitrarily wide sheets (AA, AB, ..., ZZ, AAA, ...).
 */
static int col_letters_to_index(const char* letters, int len) {
    long idx = 0;
    for (int i = 0; i < len; i++) {
        idx = idx * 26 + (toupper((unsigned char)letters[i]) - 'A' + 1);
    }
    return (int)(idx - 1);
}

/* ─── lexer ─────────────────────────────────────────────────────────────── */

static void skip_ws(Parser* p) {
    while (p->src[p->pos] && isspace((unsigned char)p->src[p->pos]))
        p->pos++;
}

static void next_token(Parser* p) {
    skip_ws(p);
    char c = p->src[p->pos];

    if (c == '\0') { p->cur.kind = TOK_EOF; return; }

    switch (c) {
        case '+': p->cur.kind = TOK_PLUS;   p->pos++; return;
        case '-': p->cur.kind = TOK_MINUS;  p->pos++; return;
        case '*': p->cur.kind = TOK_STAR;   p->pos++; return;
        case '/': p->cur.kind = TOK_SLASH;  p->pos++; return;
        case '^': p->cur.kind = TOK_CARET;  p->pos++; return;
        case '(': p->cur.kind = TOK_LPAREN; p->pos++; return;
        case ')': p->cur.kind = TOK_RPAREN; p->pos++; return;
        case ':': p->cur.kind = TOK_COLON;  p->pos++; return;
        case ',': p->cur.kind = TOK_COMMA;  p->pos++; return;
    }

    /* Number (int or float) */
    if (isdigit((unsigned char)c) || (c == '.' && isdigit((unsigned char)p->src[p->pos+1]))) {
        char* end;
        p->cur.num  = strtod(p->src + p->pos, &end);
        p->cur.kind = TOK_NUM;
        p->pos = (int)(end - p->src);
        return;
    }

    /* Identifier or cell reference  e.g.  A3  B12  SUM  AVG */
    if (isalpha((unsigned char)c)) {
        int start = p->pos;
        while (p->src[p->pos] && (isalnum((unsigned char)p->src[p->pos]) || p->src[p->pos] == '_'))
            p->pos++;
        int len = p->pos - start;
        if (len >= (int)sizeof(p->cur.ident)) len = sizeof(p->cur.ident) - 1;
        strncpy(p->cur.ident, p->src + start, len);
        p->cur.ident[len] = '\0';

        /* Is it a cell ref? Letter(s) followed by digits */
        int col_chars = 0;
        while (col_chars < len && isalpha((unsigned char)p->cur.ident[col_chars]))
            col_chars++;
        if (col_chars > 0 && col_chars < len &&
            isdigit((unsigned char)p->cur.ident[col_chars])) {
            /* Treat as cell: any number of leading letters (A, Z, AA, AB, ...) */
            p->cur.kind = TOK_CELL;
            p->cur.cx   = col_letters_to_index(p->cur.ident, col_chars);
            p->cur.cy   = atoi(p->cur.ident + col_chars);
        } else {
            p->cur.kind = TOK_IDENT;
            /* upper-case for case-insensitive function names */
            for (int i = 0; p->cur.ident[i]; i++)
                p->cur.ident[i] = (char)toupper((unsigned char)p->cur.ident[i]);
        }
        return;
    }

    printf("Unexpected character: '%c'\n", c);
    raise_error(p, ERR_GENERIC);
    p->cur.kind = TOK_EOF;
}

static int peek(Parser* p) { return p->cur.kind; }
static int eat(Parser* p, TokKind k) {
    if (p->cur.kind != k) {
        printf("Parse error: unexpected token '%s'\n", p->cur.ident);
        raise_error(p, ERR_GENERIC);
        return 0;
    }
    next_token(p);
    return 1;
}

/* ─── ensure cell exists (expand matrix if needed) ─────────────────────── */

static double read_cell(Parser* p, int cx, int cy) {
    if (cx < 0 || cy < 0) return 0.0;
    if (cx >= p->matrix->cols || cy >= p->matrix->rows)
        cell_create(p->matrix, cx, cy,
                    p->matrix->cells[cy < p->matrix->rows ? cy : 0]
                                     [cx < p->matrix->cols ? cx : 0].content,
                    0);
    /* expand if still needed */
    if (cx >= p->matrix->cols || cy >= p->matrix->rows)
        expand(p->matrix, cy + 1, cx + 1);
    Cell* cell = &p->matrix->cells[cy][cx];
    if (cell->type == 5) {
        /* Reading a cell that already holds an error cascades the error
         * up to whatever formula references it. */
        raise_error(p, ERR_REF);
        return 0.0;
    }
    return _cell_val(cell);
}

/* ─── aggregate dispatcher  AGG_FUNC(range, result_cell) ──────────────── */

typedef int (*AggFn)(Cell**, const Cell*, const Cell*, Cell*);

typedef struct { const char* name; AggFn row_fn; AggFn col_fn; } AggEntry;

static const AggEntry AGG_TABLE[] = {
    {"SUM",     add_row,     add_col    },
    {"ADD",     add_row,     add_col    },
    {"SUB",     sub_row,     sub_col    },
    {"AVG",     avg_row,     avg_col    },
    {"AVERAGE", avg_row,     avg_col    },
    {"MIN",     min_row,     min_col    },
    {"MAX",     max_row,     max_col    },
    {"PRODUCT", product_row, product_col},
    {"PROD",    product_row, product_col},
    {"COUNT",   count_row,   count_col  },
    {"STDEV",   stdev_row,   stdev_col  },
    {"MEDIAN",  median_row,  median_col },
};
#define AGG_TABLE_LEN ((int)(sizeof(AGG_TABLE)/sizeof(AGG_TABLE[0])))

typedef int (*MapFn)(const Cell*, Cell*);
typedef struct { const char* name; MapFn fn; } MapEntry;

static const MapEntry MAP_TABLE[] = {
    {"ABS",    abs_cell   },
    {"DOUBLE", double_cell},
    {"SQ",     square_cell},
    {"SQUARE", square_cell},
    {"CUBE",   cube_cell  },
    {"LOG",    log_cell   },
    {"LN",     log_cell   },
    {"EXP",    exp_cell   },
    {"SQRT",   sqrt_cell  },
    {"CEIL",   ceil_cell  },
    {"FLOOR",  floor_cell },
    {"NEG",    negate_cell},
    {"NEGATE", negate_cell},
    {"INV",    inv_cell   },
    {"SIN",    sin_cell   },
    {"COS",    cos_cell   },
    {"TAN",    tan_cell   },
};
#define MAP_TABLE_LEN ((int)(sizeof(MAP_TABLE)/sizeof(MAP_TABLE[0])))

/* Collective map (row/col range) */
typedef struct {
    const char* name;
    AggFn row_fn; AggFn col_fn;
} CollMapEntry;

static const CollMapEntry COLLMAP_TABLE[] = {
    {"ABS_RANGE",    abs_row,    abs_col   },
    {"DOUBLE_RANGE", double_row, double_col},
    {"SQUARE_RANGE", square_row, square_col},
    {"CUBE_RANGE",   cube_row,   cube_col  },
    {"LOG_RANGE",    log_row,    log_col   },
    {"EXP_RANGE",    exp_row,    exp_col   },
    {"SQRT_RANGE",   sqrt_row,   sqrt_col  },
    {"NEG_RANGE",    negate_row, negate_col},
};
#define COLLMAP_TABLE_LEN ((int)(sizeof(COLLMAP_TABLE)/sizeof(COLLMAP_TABLE[0])))

/* Forward declaration */
static double parse_expr(Parser* p);

/* ─── primary ───────────────────────────────────────────────────────────── */

static double parse_primary(Parser* p) {
    if (p->error) return 0.0;

    /* Parenthesised sub-expression */
    if (peek(p) == TOK_LPAREN) {
        next_token(p);
        double v = parse_expr(p);
        eat(p, TOK_RPAREN);
        return v;
    }

    /* Number literal */
    if (peek(p) == TOK_NUM) {
        double v = p->cur.num;
        next_token(p);
        return v;
    }

    /* Cell reference */
    if (peek(p) == TOK_CELL) {
        int cx = p->cur.cx, cy = p->cur.cy;
        next_token(p);
        return read_cell(p, cx, cy);
    }

    /* Function call */
    if (peek(p) == TOK_IDENT) {
        char fname[32];
        strncpy(fname, p->cur.ident, sizeof(fname)-1);
        fname[sizeof(fname)-1] = '\0';
        next_token(p); /* consume function name */

        if (!eat(p, TOK_LPAREN)) return 0.0;

        /* ── Aggregate functions that take a range or two cells ─── */
        for (int a = 0; a < AGG_TABLE_LEN; a++) {
            if (strcmp(fname, AGG_TABLE[a].name) != 0) continue;

            /* First arg must be a cell */
            if (peek(p) != TOK_CELL) { raise_error(p, ERR_GENERIC); return 0.0; }
            int sx = p->cur.cx, sy = p->cur.cy;
            next_token(p);

            /* Range  A0:A4 */
            if (peek(p) == TOK_COLON) {
                next_token(p);
                if (peek(p) != TOK_CELL) { raise_error(p, ERR_GENERIC); return 0.0; }
                int ex = p->cur.cx, ey = p->cur.cy;
                next_token(p);
                eat(p, TOK_RPAREN);

                /* Standardize range: (sx, sy) is top-left, (ex, ey) is bottom-right */
                if (sx > ex) { int t = sx; sx = ex; ex = t; }
                if (sy > ey) { int t = sy; sy = ey; ey = t; }

                /* Ensure cells exist */
                expand(p->matrix, ey+1, ex+1);
                expand(p->matrix, sy+1, sx+1);
                Cell* c1 = &p->matrix->cells[sy][sx];
                Cell* c2 = &p->matrix->cells[ey][ex];

                /* Temp result cell */
                Cell tmp = {0};
                tmp.type = 3;
                AggFn fn = (sy == ey) ? AGG_TABLE[a].row_fn : AGG_TABLE[a].col_fn;
                fn(p->matrix->cells, c1, c2, &tmp);
                return _cell_val(&tmp);
            }

            /* Two-cell  SUB(A0, B0) */
            if (peek(p) == TOK_COMMA) {
                next_token(p);
                if (peek(p) != TOK_CELL) { raise_error(p, ERR_GENERIC); return 0.0; }
                int ex = p->cur.cx, ey = p->cur.cy;
                next_token(p);
                eat(p, TOK_RPAREN);

                expand(p->matrix, sy+1, sx+1);
                expand(p->matrix, ey+1, ex+1);
                Cell* c1 = &p->matrix->cells[sy][sx];
                Cell* c2 = &p->matrix->cells[ey][ex];

                /* Temp result cell */
                Cell tmp = {0};
                tmp.type = 3;
                /* For two-cell: row_fn works if row is the same; col_fn otherwise */
                AggFn fn = (sy == ey) ? AGG_TABLE[a].row_fn : AGG_TABLE[a].col_fn;
                fn(p->matrix->cells, c1, c2, &tmp);
                return _cell_val(&tmp);
            }

            eat(p, TOK_RPAREN);
            return 0.0;
        }

        /* ── Map functions (single cell or expression) ─── */
        for (int m = 0; m < MAP_TABLE_LEN; m++) {
            if (strcmp(fname, MAP_TABLE[m].name) != 0) continue;
            double arg = parse_expr(p);
            eat(p, TOK_RPAREN);
            Cell tmp_in  = {.type=3, .content={.dvalue=arg}};
            Cell tmp_out = {.type=3, .content={.dvalue=0}};
            MAP_TABLE[m].fn(&tmp_in, &tmp_out);
            return tmp_out.content.dvalue;
        }

        /* ── Collective map  (range → in-place transform) ─── */
        for (int cm = 0; cm < COLLMAP_TABLE_LEN; cm++) {
            if (strcmp(fname, COLLMAP_TABLE[cm].name) != 0) continue;
            if (peek(p) != TOK_CELL) { raise_error(p, ERR_GENERIC); return 0.0; }
            int sx = p->cur.cx, sy = p->cur.cy; next_token(p);
            eat(p, TOK_COLON);
            if (peek(p) != TOK_CELL) { raise_error(p, ERR_GENERIC); return 0.0; }
            int ex = p->cur.cx, ey = p->cur.cy; next_token(p);
            eat(p, TOK_RPAREN);

            /* Standardize range */
            if (sx > ex) { int t = sx; sx = ex; ex = t; }
            if (sy > ey) { int t = sy; sy = ey; ey = t; }

            expand(p->matrix, ey+1, ex+1);
            expand(p->matrix, sy+1, sx+1);
            Cell* c1 = &p->matrix->cells[sy][sx];
            Cell* c2 = &p->matrix->cells[ey][ex];
            AggFn fn = (sy == ey) ? COLLMAP_TABLE[cm].row_fn : COLLMAP_TABLE[cm].col_fn;
            fn(p->matrix->cells, c1, c2, NULL);
            return 0.0; /* side-effect only */
        }

        printf("Unknown function: %s\n", fname);
        raise_error(p, ERR_GENERIC);
        return 0.0;
    }

    printf("Parse error: unexpected token kind %d\n", peek(p));
    raise_error(p, ERR_GENERIC);
    return 0.0;
}

/* ─── unary ─────────────────────────────────────────────────────────────── */

static double parse_unary(Parser* p) {
    if (peek(p) == TOK_MINUS) {
        next_token(p);
        return -parse_unary(p);
    }
    if (peek(p) == TOK_PLUS) {
        next_token(p);
        return parse_unary(p);
    }
    return parse_primary(p);
}

/* ─── power  (right-associative) ────────────────────────────────────────── */

static double parse_power(Parser* p) {
    double base = parse_unary(p);
    if (peek(p) == TOK_CARET) {
        next_token(p);
        double exp = parse_power(p);   /* right-assoc */
        return pow(base, exp);
    }
    return base;
}

/* ─── term  (*, /) ──────────────────────────────────────────────────────── */

static double parse_term(Parser* p) {
    double v = parse_power(p);
    while (!p->error && (peek(p) == TOK_STAR || peek(p) == TOK_SLASH)) {
        TokKind op = peek(p);
        next_token(p);
        double rhs = parse_power(p);
        if (op == TOK_STAR) {
            v *= rhs;
        } else {
            if (rhs == 0.0) { printf("Division by zero\n"); raise_error(p, ERR_DIV0); return 0.0; }
            v /= rhs;
        }
    }
    return v;
}

/* ─── expression  (+, -) ─────────────────────────────────────────────────── */

static double parse_expr(Parser* p) {
    double v = parse_term(p);
    while (!p->error && (peek(p) == TOK_PLUS || peek(p) == TOK_MINUS)) {
        TokKind op = peek(p);
        next_token(p);
        double rhs = parse_term(p);
        v = (op == TOK_PLUS) ? v + rhs : v - rhs;
    }
    return v;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * Public entry point
 * ═══════════════════════════════════════════════════════════════════════════ */

void process_command(Matrix* matrix, char* command) {
    /* ── locate '=' separator ── */
    char* eq = strchr(command, '=');
    if (!eq) {
        printf("Invalid command: expected TARGET = EXPRESSION\n");
        return;
    }

    /* Target cell name */
    char target_buf[32];
    int tlen = (int)(eq - command);
    if (tlen <= 0 || tlen >= (int)sizeof(target_buf)) {
        printf("Invalid target cell\n"); return;
    }
    strncpy(target_buf, command, tlen);
    target_buf[tlen] = '\0';
    /* trim */
    char* t = target_buf;
    while (isspace((unsigned char)*t)) t++;
    char* te = t + strlen(t) - 1;
    while (te > t && isspace((unsigned char)*te)) *te-- = '\0';

    /* Parse column letter + row number */
    if (!isalpha((unsigned char)*t)) {
        printf("Invalid target cell: %s\n", t);
        return;
    }
    int col_chars = 0;
    while (isalpha((unsigned char)t[col_chars])) col_chars++;
    int tx = col_letters_to_index(t, col_chars);
    int ty = atoi(t + col_chars);
    if (tx < 0 || ty < 0) {
        printf("Invalid target cell: %s\n", t);
        return;
    }

    /* Expression string (after '=') */
    const char* expr = eq + 1;
    while (isspace((unsigned char)*expr)) expr++;

    /* ── Run the recursive-descent parser ── */
    Parser p = {
        .src      = expr,
        .pos      = 0,
        .matrix   = matrix,
        .error    = 0,
        .err_code = 0,
    };
    next_token(&p);   /* prime the token stream */
    double result = parse_expr(&p);

    /* Ensure target cell exists */
    expand(matrix, ty + 1, tx + 1);

    if (p.error) {
        printf("Expression evaluation failed.\n");
        /* Write a typed error into the cell instead of leaving it silently
         * unchanged, so the UI can surface #DIV/0!, #REF!, #ERR!, etc. */
        matrix->cells[ty][tx].x    = tx;
        matrix->cells[ty][tx].y    = ty;
        matrix->cells[ty][tx].type = 5;
        matrix->cells[ty][tx].content.value = p.err_code ? p.err_code : ERR_GENERIC;
        return;
    }
    matrix->cells[ty][tx].x    = tx;
    matrix->cells[ty][tx].y    = ty;
    matrix->cells[ty][tx].type = 3;
    matrix->cells[ty][tx].content.dvalue = result;
}
