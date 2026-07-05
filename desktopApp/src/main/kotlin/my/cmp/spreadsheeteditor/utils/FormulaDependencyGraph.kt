package my.cmp.spreadsheeteditor.utils

/**
 * Tracks formula dependencies between cells so that:
 *  - editing a cell automatically recalculates every other cell whose
 *    formula (directly or transitively) reads it, and
 *  - assigning a formula that would create a reference cycle is rejected
 *    up front with a `#CIRCULAR!` error instead of being sent to the
 *    native engine (which has no cycle detection of its own).
 *
 * Cell addresses are represented as (row, col) pairs. The C engine has no
 * notion of "this formula depends on that cell" — it just re-evaluates an
 * expression once, on assignment — so this bookkeeping lives entirely on
 * the Kotlin side, alongside the formula text that the UI already keeps
 * for display purposes.
 */
class FormulaDependencyGraph {

    /** cell -> set of cells that ITS formula reads (its dependencies) */
    private val dependsOn = mutableMapOf<Pair<Int, Int>, Set<Pair<Int, Int>>>()

    /** cell -> set of cells that read IT (the reverse edges, for recalculation) */
    private val dependents = mutableMapOf<Pair<Int, Int>, MutableSet<Pair<Int, Int>>>()

    /**
     * Extracts every cell reference (e.g. "A1", "AA12") mentioned in a
     * formula's source text (without the leading '=').
     */
    fun parseReferences(formula: String): Set<Pair<Int, Int>> {
        val refs = mutableSetOf<Pair<Int, Int>>()
        for (match in CELL_REF_REGEX.findAll(formula)) {
            // A match immediately followed by '(' is a function call whose
            // name happens to end in digits (e.g. ATAN2(...)), not a cell
            // reference — cell references are never callable.
            val followedByParen = formula.getOrNull(match.range.last + 1) == '('
            if (followedByParen) continue
            val letters = match.groupValues[1]
            val digits = match.groupValues[2]
            val col = columnIndex(letters)
            val row = digits.toIntOrNull()
            if (col >= 0 && row != null) refs.add(row to col)
        }
        return refs
    }

    /**
     * Returns true if giving [cell] the dependency set [newDeps] would
     * create a reference cycle (including a cell referencing itself).
     */
    fun wouldCreateCycle(cell: Pair<Int, Int>, newDeps: Set<Pair<Int, Int>>): Boolean {
        if (cell in newDeps) return true
        val visited = mutableSetOf<Pair<Int, Int>>()
        val stack = ArrayDeque(newDeps)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            if (current == cell) return true
            if (!visited.add(current)) continue
            stack.addAll(dependsOn[current].orEmpty())
        }
        return false
    }

    /** Records that [cell]'s formula now depends on exactly [newDeps]. */
    fun setDependencies(cell: Pair<Int, Int>, newDeps: Set<Pair<Int, Int>>) {
        // Remove old reverse edges for this cell
        dependsOn[cell]?.forEach { old -> dependents[old]?.remove(cell) }
        dependsOn[cell] = newDeps
        newDeps.forEach { dep -> dependents.getOrPut(dep) { mutableSetOf() }.add(cell) }
    }

    /** Clears any dependency bookkeeping for [cell] (e.g. it no longer holds a formula). */
    fun clearDependencies(cell: Pair<Int, Int>) {
        setDependencies(cell, emptySet())
    }

    /** Cells whose formulas directly read [cell]. */
    fun getDirectDependents(cell: Pair<Int, Int>): Set<Pair<Int, Int>> =
        dependents[cell].orEmpty().toSet()

    /** Wipes all dependency bookkeeping, e.g. before a full engine resync. */
    fun clearAll() {
        dependsOn.clear()
        dependents.clear()
    }

    companion object {
        private val CELL_REF_REGEX = Regex("([A-Za-z]+)([0-9]+)")
    }
}
