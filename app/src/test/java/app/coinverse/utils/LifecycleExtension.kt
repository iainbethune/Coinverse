package app.coinverse.utils

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import app.coinverse.content.ContentRepository
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.*

class LifecycleExtension(val contentRepository: ContentRepository)
    : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    val testDispatcher = TestCoroutineDispatcher()

    override fun beforeAll(context: ExtensionContext?) {
        // Repository is used across all the tests.
        mockkObject(contentRepository)
    }

    override fun afterAll(context: ExtensionContext?) {
        unmockkAll()
    }


    override fun beforeEach(context: ExtensionContext?) {
        // Set Coroutine Dispatcher.
        Dispatchers.setMain(testDispatcher)

        // Set LiveData Executor.
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    override fun afterEach(context: ExtensionContext?) {
        // Reset Coroutine Dispatcher.
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()

        // Clear LiveData Executor
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}