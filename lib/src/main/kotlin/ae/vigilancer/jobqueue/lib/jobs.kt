package ae.vigilancer.jobqueue.lib

import rx.Observable
import java.util.*


abstract class Job<R> {
    val uuid: String = UUID.randomUUID().toString()

    var result: R? = null
    abstract fun run(): Observable<R?>

    fun <T: Job<R>> T.wrapResult(): Observable<T> {
        return run().map { r -> this.result = r; this }
    }

    fun runForResult(): Observable<Job<R>> {
        return wrapResult()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Job<*>) return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}
