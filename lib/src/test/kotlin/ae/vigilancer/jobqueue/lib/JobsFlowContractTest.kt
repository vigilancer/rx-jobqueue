package ae.vigilancer.jobqueue.lib

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import java.util.*
import java.util.concurrent.TimeUnit

@Test
class JobsFlowContractTest() {

    val testScheduler = TestScheduler()

    class SimpleJob: Job<String>() {
        companion object {
            @JvmField val RESULT = "simple.job.result"
        }
        override fun run(): Observable<String?> {
            return Observable.just(RESULT)
        }
    }

    // простая проверка что Job проходит сквозь RequestsManager и результат приходит
    @Test
    fun `jobs go through with TestSubscriber`() {
        val testSubscriber = TestSubscriber<Job<*>>()
        val rm = RequestsManager()
        rm.init()

        rm.toObservable().subscribe(testSubscriber)
        rm.request(SimpleJob())

        testSubscriber.assertNoErrors()
        assertThat(testSubscriber.onNextEvents).hasSize(1)
        assertThat(testSubscriber.onNextEvents[0].result).isEqualTo(SimpleJob.RESULT)
    }

    // простая проверка что Job проходит сквозь RequestsManager и результат приходит
    @Test
    fun `jobs go through with TestScheduler`() {
        val rm = RequestsManager()
        rm.init()
        val result: ArrayList<Job<*>> = ArrayList(0)

        rm.toObservable().subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe { j -> result.add(j) }

        Observable.just(SimpleJob()).subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe{ rm.request(it) }

        assertThat(result).isEmpty()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        assertThat(result).hasSize(1)
    }

    // проверка что задача отлавливается по uuid
    @Test
    fun `can catch job by uuid`() {
        val result: ArrayList<Job<*>> = ArrayList(0)

        val rm = RequestsManager()
        rm.init()


        val specialJob = SimpleJob()
        val feed = Observable.just(specialJob, SimpleJob(), SimpleJob(), specialJob)

        rm.toObservable().subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe { j -> if (j.uuid == specialJob.uuid) result.add(j)
            }

        feed.subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe{ rm.request(it) }


        assertThat(result).isEmpty()
        testScheduler.advanceTimeBy(10, TimeUnit.MINUTES)
        assertThat(result).hasSize(2)
        assertThat(result[0].uuid).isEqualTo(specialJob.uuid)
    }

    // что задача отлавливается по своему типу
    @Test
    fun `can catch job by subtype`() {

        class OtherJob : Job<Int>() {
            override fun run(): Observable<Int?> {
                return Observable.just(1)
            }
        }

        val thisJobs: ArrayList<Job<*>> = ArrayList(0)
        val thatJobs: ArrayList<Job<*>> = ArrayList(0)

        val rm = RequestsManager()
        rm.init()

        val feed = Observable.just(SimpleJob(), SimpleJob(), OtherJob(), OtherJob(), SimpleJob())

        rm.toObservable().subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe { j ->
                if (j is SimpleJob) {
                    thisJobs.add(j)
                } else if (j is OtherJob) {
                    thatJobs.add(j)
                }
            }

        feed.subscribeOn(testScheduler).observeOn(testScheduler)
            .subscribe{ rm.request(it) }

        assertThat(thisJobs).isEmpty()
        assertThat(thatJobs).isEmpty()
        testScheduler.advanceTimeBy(10, TimeUnit.MINUTES)
        assertThat(thisJobs).hasSize(3)
        assertThat(thatJobs).hasSize(2)
        assertThat(thisJobs[0]).isExactlyInstanceOf(SimpleJob::class.java)
        assertThat(thatJobs[0]).isExactlyInstanceOf(OtherJob::class.java)
    }


    // что несколько одновременных подписчиков получают
    // одинаковые дубликаты выполненной Job

}
