package ae.vigilancer.jobqueue.lib

import rx.Observable
import rx.observers.Observers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject
import java.util.*

object RequestsManager {

    private val _manager: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())
    private val _queue: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())


    fun init(vararg transformers: (Observable<Job<*>>) -> Observable<Job<*>>) {
        _queue.compose(transformers.asIterable())
            .subscribe(Observers.create(
                { j ->
                    j.runForResult()
                        .subscribe(Observers.create(
                            { a -> _manager.onNext(a) },
                            { e -> _manager.onError(e) }
                        ))
                },
                { e -> _manager.onError(e) }
            ))
    }

    fun <T> request(job: Job<T>) {
        _queue.onNext(job)
    }

    fun toObservable(): Observable<*>  {
        return _manager
    }

    fun <T> Observable<T>.compose(ts: Iterable<(Observable<T>) -> Observable<T>>): Observable<T> {
        var o = this@compose
        ts.forEach { t -> o = o.compose(t) }
        return o
    }
}

abstract class Job<R> {
    val uuid: String by lazy { UUID.randomUUID().toString() }

    var result: R? = null
    abstract fun run(): Observable<R?>

    fun <T: Job<R>> T.`this`() = this

    fun <T: Job<R>> T.wrapResult(): Observable<T> {
        return run().map { r -> this.result = r; this }.map { `this`() }
    }

    fun runForResult(): Observable<Job<R>> {
        return wrapResult()
    }
}
