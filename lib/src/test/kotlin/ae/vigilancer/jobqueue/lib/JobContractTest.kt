package ae.vigilancer.jobqueue.lib

import nl.jqno.equalsverifier.EqualsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import rx.Observable

@Test
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

    @Test
    fun `can reassign Job's result`() {
        val j = EmptyJob()
        assertThat(j.result).isNull()
        j.result = 22
        assertThat(j.result!!).isEqualTo(22)
        j.result = 23
        assertThat(j.result!!).isEqualTo(23)
    }
}
