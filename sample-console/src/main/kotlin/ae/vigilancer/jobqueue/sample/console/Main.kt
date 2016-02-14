package ae.vigilancer.jobqueue.sample.console

import ae.vigilancer.jobqueue.lib.Job
import ae.vigilancer.jobqueue.lib.PreventDoubleFiring
import ae.vigilancer.jobqueue.lib.RequestsManager
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args : Array<String>) {

    println("waiting for response")

    RequestsManager.toObservable()
        .subscribe(
            {s -> println("result: $s") },
            {e -> println("error: $e")},
            { println ("onComplete") }
    )

    Observable.interval(3, TimeUnit.SECONDS).timeInterval().take(5).toBlocking().subscribe(
        {
            RequestsManager.request(GetUserJob())
            RequestsManager.request(UpdatePostJob())
            RequestsManager.request(UpdatePostJob())
        },
        { println(it)},
        { Thread.sleep(6000)}
    )

}

class GetUserJob() : Job<String>() {

    override fun run(): Observable<String> {
        return Observable.just("_get_user_ ($uuid)").delay(1, TimeUnit.SECONDS)
    }
}

class UpdatePostJob() : Job<String>(), PreventDoubleFiring {
    override fun run(): Observable<String> {
        return Observable.just("_update_post_ ($uuid)").delay(1L + Random().nextInt((3 - 1) + 1), TimeUnit.SECONDS)
    }
}

