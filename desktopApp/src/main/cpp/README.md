A terminal-based spreadsheet editor written in C11. Cells are addressed by column letter and row number (e.g. `A0`, `B3`). Expressions are evaluated by a recursive-descent parser that supports multi-operand arithmetic, parenthesised sub-expressions, operator precedence, aggregate range functions, and per-cell map functions. On x86-64 the inner accumulation loops use inline SSE2 assembly.

---

## Building

Requires a C11 compiler and CMake 3.15 or later. Links against `libm`.

```bash
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make
./txtbsdsp
```

---

## Cell Addressing

Columns are single uppercase letters (`A`-`Z`). Rows are zero-indexed integers. A cell reference is the column letter followed immediately by the row number: `A0`, `B12`, `Z99`. The matrix expands automatically when a cell outside the current bounds is written.

---

## Expression Syntax

Every command takes the form `TARGET = EXPRESSION`. The expression is evaluated and the result stored in the target cell. All arithmetic is done in double precision internally.

**Operator precedence** (high to low):

| Level | Operators | Associativity |
|-------|-----------|---------------|
| Unary | `-` `+` | right |
| Power | `^` | right |
| Multiplicative | `*` `/` | left |
| Additive | `+` `-` | left |

**Examples:**

```
A0 = 5
A1 = 10
A2 = A1 + A0 + 4
A3 = (A2 + A1) / 2 + A0
A4 = A0 * A1 - A2 * 3 + (A3 ^ 2)
```

---

## Functions

### Aggregate  --  `FUNC(start:end)`

Operate over a contiguous row or column range. The range direction is inferred automatically: same row = row range, same column = column range.

| Function | Aliases | Result |
|----------|---------|--------|
| `SUM` | `ADD` | Sum of all values in range |
| `AVG` | `AVERAGE` | Arithmetic mean |
| `MIN` | | Minimum value |
| `MAX` | | Maximum value |
| `PRODUCT` | `PROD` | Product of all values |
| `COUNT` | | Number of non-empty cells |
| `STDEV` | | Sample standard deviation |
| `MEDIAN` | | Median (non-destructive) |

```
C0 = SUM(A0:A4)
C1 = AVG(A0:A4)
C2 = STDEV(A0:A4)
C3 = MEDIAN(A0:A4)
C4 = PRODUCT(A0:A4)
```

Aggregates can be composed freely inside larger expressions:

```
D0 = SUM(A0:A4) / COUNT(A0:A4) + B0
```

### Map  --  `FUNC(expr)`

Apply a transformation to a single value or sub-expression. The argument is itself a full expression, so nesting is supported.

| Function | Description |
|----------|-------------|
| `ABS` | Absolute value |
| `SQRT` | Square root |
| `SQUARE` / `SQ` | x^2 |
| `CUBE` | x^3 |
| `DOUBLE` | 2x |
| `NEG` / `NEGATE` | Negation |
| `INV` | 1/x |
| `LOG` / `LN` | Natural logarithm |
| `EXP` | e^x |
| `CEIL` | Ceiling |
| `FLOOR` | Floor |
| `SIN` | Sine (radians) |
| `COS` | Cosine (radians) |
| `TAN` | Tangent (radians) |

```
B0 = SQRT(A0)
B1 = SQRT(A0 * A1 + 4)
B2 = SIN(A0) + COS(A1)
B3 = INV(AVG(A0:A4))
```

### Collective Map  --  `FUNC_RANGE(start:end)`

Transform every cell in a range in-place. The target cell of the assignment is unused; the side effect is the operation.

| Function |
|----------|
| `ABS_RANGE` |
| `SQRT_RANGE` |
| `SQUARE_RANGE` |
| `CUBE_RANGE` |
| `DOUBLE_RANGE` |
| `NEG_RANGE` |
| `LOG_RANGE` |
| `EXP_RANGE` |

```
A0 = NEG_RANGE(A0:A4)
A0 = SQRT_RANGE(B0:B5)
```

---

## Special Commands

| Command | Action |
|---------|--------|
| `help` | Print command reference |
| `exit` / `quit` | Exit the program |

---

## Implementation Notes

**Parser.** `Parser.c` implements a hand-written recursive-descent parser. The grammar has five levels (expression, term, power, unary, primary) which naturally encode precedence without a separate precedence-climbing pass. Function calls are resolved inside `parse_primary` by table lookup against the aggregate, map, and collective-map dispatch tables.

**Assembly.** On x86-64, two hot paths use `__asm__ volatile` with SSE2 instructions. `asm_sum_doubles` uses `addpd` to accumulate pairs of doubles in a 128-bit XMM register with a horizontal add at the end. `asm_product_doubles` uses a scalar `mulsd` loop that keeps the running product in `xmm0` throughout, avoiding the x87 FP stack. Both functions are guarded by `#if defined(__x86_64__)` and fall back to plain C loops on other architectures. All aggregate functions (`SUM`, `AVG`, `PRODUCT`, `STDEV`, `MEDIAN`) route through a shared `range_to_doubles_row/col` helper that materialises the range as a contiguous `double[]` before calling the assembly routines, enabling sequential memory access.

**Types.** Cells hold a tagged union (`int`, `float`, `double`, `char*`). The parser evaluates all expressions as `double` and stores the result with type code 3. The `cell_to_double` helper normalises any cell type to `double` for aggregation. Type-preserving arithmetic (`add`, `sub`, `mul`, `divc`, `powc`) is still available for direct cell-to-cell operations.

**Memory.** The matrix is heap-allocated as a `Cell**` array-of-rows. `expand()` reallocates both the row pointer array and each row when a write targets an out-of-bounds cell, copying existing data and zero-initialising new cells. `matrix_destroy()` frees all allocations.

---

## File Structure

```
txtbsdsp/
├── main.c          entry point, REPL loop
├── Cell.h          Cell, CellContent, Matrix type definitions
├── Functions.h     full function declarations
├── Functions.c     arithmetic, aggregates, maps, I/O, ASM helpers
├── Parser.h        process_command declaration
├── Parser.c        recursive-descent expression parser
├── utils.h         contains / containsAny declarations
├── utils.c         character search utilities
└── CMakeLists.txt  build configuration
```

---

## Acknowledgements

Developed with assistance from [Claude](https://claude.ai) (Anthropic).
