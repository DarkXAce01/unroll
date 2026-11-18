package io.github.darkxace01.unroll.model

/**
 * Undo and redo as whole-project snapshots (projects are small: a few kB of JSON). A drag that fires a
 * hundred updates is one step: call [commit] when the finger lifts, [preview] while it moves.
 */
class History(initial: Project, private val limit: Int = 200) {
    private val undo = ArrayDeque<Project>()
    private val redo = ArrayDeque<Project>()

    /** The last committed state; the base for the current gesture. */
    var committed: Project = initial
        private set

    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    /** Records [next] as a new step. No-op if nothing changed. */
    fun commit(next: Project): Project {
        if (next == committed) return next
        undo.addLast(committed)
        if (undo.size > limit) undo.removeFirst()
        redo.clear()
        committed = next
        return next
    }

    fun undo(): Project? {
        val prev = undo.removeLastOrNull() ?: return null
        redo.addLast(committed)
        committed = prev
        return prev
    }

    fun redo(): Project? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(committed)
        committed = next
        return next
    }

    /** Swaps the committed state without making a step (for facts found later, like faces). */
    fun replaceCommitted(p: Project) {
        committed = p
    }
}
