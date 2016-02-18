package ae.vigilancer.jobqueue.lib

import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import rx.Observable

@RunWith(JUnit4::class)
class JobContractTest {

    class EmptyJob : Job<Int>() {
        override fun run(): Observable<Int?> {
            return Observable.just(0)
        }
    }

    @Suppress("unused")
    class WithPrimitiveFieldsJob(var field1: String, val field2: Int) : Job<Long>() {
        override fun run(): Observable<Long?> {
            return Observable.just(0L)
        }
    }

    data class ArgumentDataClass(val field: String)

    @Suppress("unused")
    class WithClassFieldsJob(val field1: String, var field2: ArgumentDataClass) : Job<Long>() {
        override fun run(): Observable<Long?> {
            return Observable.just(0L)
        }
    }

    @Test
    fun `Job subtypes has valid hashCode() and equals()`() {
        EqualsVerifier.forClass(EmptyJob::class.java).verify()
        EqualsVerifier.forClass(WithPrimitiveFieldsJob::class.java).verify()
        EqualsVerifier.forClass(WithClassFieldsJob::class.java).verify()
    }
}
