package ae.vigilancer.jobqueue.lib

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import java.util.*
import java.util.concurrent.TimeUnit


@RunWith(JUnit4::class)
class SimpleTest() {

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
        assert(testSubscriber.onNextEvents.size == 1)
        assert(testSubscriber.onNextEvents[0].result == SimpleJob.RESULT)
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

        assert(result.isEmpty() == true)
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        assert(result.size == 1)
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


        assert(result.isEmpty() == true)
        testScheduler.advanceTimeBy(10, TimeUnit.MINUTES)
        assert(result.size == 2)
        assert(result[0].uuid == specialJob.uuid)
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

        assert(thisJobs.isEmpty() == true)
        assert(thatJobs.isEmpty() == true)
        testScheduler.advanceTimeBy(10, TimeUnit.MINUTES)
        assert(thisJobs.size == 3)
        assert(thatJobs.size == 2)
        assert(thisJobs[0] is SimpleJob)
        assert(thatJobs[0] is OtherJob)
    }

    // что задачи выполняются даже когда нет подписчиков

    // что задача выполняется после того как все подписчики отписались

    // что подписчики получают только результаты задач, готовых на момент подписки, а не всех с самого начала работы RequestManager-а


    // что несколько одновременных подписчиков получают одинаковые дубликаты
    // выполненной Job



    // проверка что все трансформеры применяются (больше 2х)   ( .compose(ts) работает )




    // проверки [RequestsManager]
    // проверка что transformer применяются
    // проверка что трансформеры применяются в том же порядке, что и заданы в init()
    // (выполнение контракта) сквозь RequestsManager могут безболезненно проходить null-ы

}


