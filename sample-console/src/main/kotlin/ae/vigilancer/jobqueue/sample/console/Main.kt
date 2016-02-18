package ae.vigilancer.jobqueue.sample.console

import ae.vigilancer.jobqueue.lib.Job
import ae.vigilancer.jobqueue.lib.RequestsManager
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args : Array<String>) {

    println("waiting for response")

    val rm = RequestsManager()
    rm.init(LogJobsBeforeTransformers, PreventDoubleFiring, LogJobsAfterTransformers)

    var specialJobId: String? = null

    rm.toObservable()
        .subscribe(
            {s ->
                println("result: \t$s")
            },
            {e -> println("error: $e")},
            { println ("first onComplete") }
    )

    rm.toObservable().subscribe(
        { s ->
            if (s is GetUserJob) {
                println("getting user!: ${s.result}")
            }
            if (s is Job<*> && s.uuid == specialJobId) {
                println("found special!1: $specialJobId")
            }

        },
        {e -> println("error: $e")},
        { println ("second onComplete") }
    )

    Observable.interval(3, TimeUnit.SECONDS).timeInterval().take(5).toBlocking().subscribe(
        {
            rm.request(GetUserJob())
            val specialJob = UpdatePostJob()
            specialJobId = specialJob.uuid
            rm.request(specialJob)
            rm.request(UpdatePostJob())
            rm.request(UpdatePostJob())
            rm.request(UpdatePostJob())
        },
        { println(it)},
        { Thread.sleep(6000)}
    )

}

class GetUserJob() : Job<String>() {
    override fun run(): Observable<String?> {
        return Observable.just("_get_user_ ($uuid)").delay(1, TimeUnit.SECONDS)
    }
}

class UpdatePostJob() : Job<String>(), IPreventDoubleFiring {
    override fun run(): Observable<String?> {
        return Observable.just("_update_post_ ($uuid)").delay(1L + Random().nextInt((3 - 1) + 1), TimeUnit.SECONDS)
    }
}

/**
 * Example of extension for [RequestsManager]
 * Adding capability to filter out adjacent duplicate jobs
 */
val PreventDoubleFiring: (Observable<Job<*>>) -> Observable<Job<*>> = { o ->
    o.distinctUntilChanged{ if (it is IPreventDoubleFiring) it.javaClass.canonicalName else it.uuid }
}

interface IPreventDoubleFiring {}

val LogJobsBeforeTransformers : (Observable<Job<*>>) -> Observable<Job<*>> = { o ->
    o.map { println("before job: ${it.javaClass.simpleName} \t\t${it.uuid}"); it }
}

val LogJobsAfterTransformers : (Observable<Job<*>>) -> Observable<Job<*>> = { o ->
    o.map { println("after job: ${it.javaClass.simpleName} \t\t${it.uuid}"); it }
}
