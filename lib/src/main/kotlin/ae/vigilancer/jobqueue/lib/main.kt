package ae.vigilancer.jobqueue.lib

import rx.Observable
import rx.observers.Observers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject

class RequestsManager {
    private val DEBUG = true

    private val _manager: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())
    private val _queue: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())


    fun init(vararg transformers: (Observable<Job<*>>) -> Observable<Job<*>>) {
        _queue.compose(transformers.asIterable())
            .doOnSubscribe { if (DEBUG) println("onSubscribe [_queue]") }
            .subscribe(Observers.create(
                { j ->
                    j.runForResult()
                        .doOnSubscribe { if (DEBUG) println("onSubscribe [job] (${j.uuid})") }
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

    fun toObservable(): Observable<Job<*>>  {
        return _manager
            .doOnSubscribe { if (DEBUG) println("onSubscribe [_manager]") }
    }

    fun <T> Observable<T>.compose(ts: Iterable<(Observable<T>) -> Observable<T>>): Observable<T> {
        var o = this@compose
        ts.forEach { t -> o = o.compose(t) }
        return o
    }
}
