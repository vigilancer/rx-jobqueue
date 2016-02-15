package ae.vigilancer.jobqueue.lib

import rx.Observable
import rx.observers.Observers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject
import java.util.*

object RequestsManager {

    private val _manager: Subject<Any, Any> = SerializedSubject(PublishSubject.create())
    private val _queue: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())


    fun init(vararg transformers: (Observable<Job<*>>) -> Observable<Job<*>>) {
        _queue.compose(transformers.asIterable())
            .subscribe(Observers.create(
                { j ->
                    j.run()
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

    fun toObservable(): Observable<Any>  {
        return _manager
    }

    fun <T> Observable<T>.compose(ts: Iterable<(Observable<T>) -> Observable<T>>): Observable<T> {
        var o = this@compose
        ts.forEach { t -> o = o.compose(t) }
        return o
    }
}

abstract class Job<T>() {
    val uuid: String by lazy { UUID.randomUUID().toString() }

    abstract fun run(): Observable<T>
}
