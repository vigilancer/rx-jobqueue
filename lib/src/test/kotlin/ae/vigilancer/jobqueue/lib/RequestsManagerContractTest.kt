package ae.vigilancer.jobqueue.lib

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RequestsManagerContractTest {

    @Test
    fun stub() {
    }

    // проверки [RequestsManager]
    // проверка что transformer применяются
    // проверка что трансформеры применяются в том же порядке, что и заданы в init()
    // (выполнение контракта) сквозь RequestsManager могут безболезненно проходить null-ы


    // проверка что все трансформеры применяются (больше 2х)   ( .compose(ts) работает )

    // что задачи выполняются даже когда нет подписчиков

    // что задача выполняется после того как все подписчики отписались

    // что подписчики получают только результаты задач, готовых на момент подписки, а не всех с самого начала работы RequestManager-а


}
