/**
 * The observable interface. Don't worry about it.
 */
interface IObservable<O> {
    /**
     * List of this object's observers
     * Implementers have to provide this property
     */
    val observers: MutableList<O>
    /**
     * Adds an observer
     * @param observer The new observer
     */
    fun addObserver(observer: O) {
        observers.add(observer)
    }
    /**
     * Removes an observer
     * @param observer The observer to the removed.
     */
    fun removeObserver(observer: O) {
        observers.remove(observer)
    }
    /**
     * Notifies all the observers
     * @param handler Makes the observer handle it. (nice)
     */
    fun notifyObservers(handler: (O) -> Unit) {
        observers.toList().forEach { handler(it) }
    }
}