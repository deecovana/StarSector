package roiderUnion.helpers

class LoopingListIterator<E>(
    private val list: List<E>,
    private var index: Int
) : ListIterator<E> {
    private var lastIndex = -1

    override fun hasNext(): Boolean = list.isNotEmpty()
    override fun hasPrevious(): Boolean = list.isNotEmpty()

    override fun previousIndex(): Int = index - 1
    override fun nextIndex(): Int = index

    override fun previous(): E {
        if (list.isEmpty()) throw NoSuchElementException()
        if (index <= 0) index = list.size
        lastIndex = --index
        return list[lastIndex]
    }

    override fun next(): E {
        if (list.isEmpty()) throw NoSuchElementException()
        if (index >= list.size) index = 0
        lastIndex = index++
        return list[lastIndex]
    }
}

fun <E> List<E>.loopingIterator(): LoopingListIterator<E> = LoopingListIterator(this, 0)