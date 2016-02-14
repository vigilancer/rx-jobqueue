package ae.vigilancer.jobqueue.lib

import rx.Observable
import rx.functions.Func1
import rx.observers.Observers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Варианты использования
 *
 *
 *  Запретить повторное последовательные дубликаты:
 *      юскейс: затапывание кнопки Лайк
 *          FIXED: distinctUntilChanged()
 *
 *  Кешировать результаты выполнения Job-ов, на которые никто пока не подписан.
 *      юскейс: запустили обновление Юзера, отписались (Активити была закрыта), снова подписались,
 *      работа была завершена, но нам нужен её результат
 *
 *  Еще юскейс
 *    иногда хочется получить результат выполнения именно этой Job, которую отправили,
 *    а не любой Job такого же типа.
 *      FIXED: Job.uuid
 *
 *  Подписывание на результаты выбранных Job-ов, даже не нами запущенных.
 *
 */


object RequestsManager {

    private val _manager: Subject<Any, Any> = SerializedSubject(PublishSubject.create())
    // todo: внедрять _queue через DI, чтобы можно было расширять функционал при необходимости снаружи
    private val _queue: Subject<Job<*>, Job<*>> = SerializedSubject(PublishSubject.create())

    init {
        _queue.distinctUntilChanged{ if (it is PreventDoubleFiring) it.javaClass.canonicalName else it.uuid }
            .subscribe(Observers.create(
            { j ->
                println("next job: ${j.javaClass.simpleName}")
                j.run()
                    .subscribe(Observers.create(
                        { a -> _manager.onNext(a) },
                        { e -> _manager.onError(e) }
                    ))
            },
            { e -> _manager.onError(e) }
        ))
    }

    fun <O> request(job: Job<O>) {
        println("requested job: ${job.uuid}, ${job.javaClass.simpleName}")

        _queue.onNext(job)
    }

    fun toObservable(): Observable<Any>  {
        return _manager
    }

}

interface PreventDoubleFiring {}

abstract class Job<T>() {
    val uuid: String by lazy { UUID.randomUUID().toString() }

    abstract fun run(): Observable<T>
}


