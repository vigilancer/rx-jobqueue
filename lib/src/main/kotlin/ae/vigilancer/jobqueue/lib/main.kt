package ae.vigilancer.jobqueue.lib

import rx.Observable
import rx.observers.Observers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subjects.Subject
import java.util.*

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
 *  Расширение возможностей Queue на клиентской стороне. Добавление своих правил при обработке потока Job-ов
 *      FIXED: fun init(vararg transformers)
 *
 *
 *  Пока неясно как быть с обработкой ошибок. Например, если Job - это запрос через Retrofit, то как быть с ошибками
 *
 *
 */


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
