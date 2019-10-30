package app.coinverse.contentviewmodel.tests

import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentRepository.getContent
import app.coinverse.content.ContentRepository.getMainFeedList
import app.coinverse.content.ContentRepository.queryLabeledContentList
import app.coinverse.content.ContentViewModel
import app.coinverse.content.models.ContentEffectType.OpenContentSourceIntentEffect
import app.coinverse.content.models.ContentEffectType.UpdateAdsEffect
import app.coinverse.content.models.ContentViewEvents.*
import app.coinverse.contentviewmodel.*
import app.coinverse.utils.CoroutineExtension
import app.coinverse.utils.FeedType.*
import app.coinverse.utils.LCE_STATE.CONTENT
import app.coinverse.utils.LiveDataExecutorExtension
import app.coinverse.utils.observe
import app.coinverse.utils.viewEffects
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(LiveDataExecutorExtension::class)
class NavigateContentTests {

    companion object {
        @JvmField
        @RegisterExtension
        val coroutineExtension = CoroutineExtension()
    }

    private val contentViewModel = ContentViewModel()

    private fun NavigateContent() = navigateContentTestCases()

    @BeforeAll
    fun beforeAll() {
        mockkObject(ContentRepository)
    }

    @AfterAll
    fun afterAll() {
        unmockkAll() // Re-assigns transformation of object to original state prior to mock.
    }

    @ParameterizedTest
    @MethodSource("NavigateContent")
    fun `Navigate Content`(test: NavigateContentTest) =
            coroutineExtension.testDispatcher.runBlockingTest {
        mockComponents(test)
        FeedLoad(test.feedType, test.timeframe, false).also { event ->
            contentViewModel.processEvent(event)
        }
        ContentShared(test.mockContent).also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().shareContentIntent.observe()
                    .contentRequest.observe())
                    .isEqualTo(test.mockContent)
        }
        ContentSourceOpened(test.mockContent.url).also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().openContentSourceIntent.observe())
                    .isEqualTo(OpenContentSourceIntentEffect(test.mockContent.url))
        }
        // Occurs on Fragment 'onViewStateRestored'
        UpdateAds().also { event ->
            contentViewModel.processEvent(event)
            assertThat(contentViewModel.viewEffects().updateAds.observe().javaClass)
                    .isEqualTo(UpdateAdsEffect::class.java)
        }
        verifyTests(test)
    }

    private fun mockComponents(test: NavigateContentTest) {
        // Coinverse - ContentRepository
        coEvery { getMainFeedList(any(), test.isRealtime, any()) } returns mockGetMainFeedList(
                test.mockFeedList, CONTENT)
        every {
            queryLabeledContentList(test.feedType)
        } returns mockQueryMainContentList(test.mockFeedList)
        every { getContent(test.mockContent.id) } returns mockGetContent(test)
    }

    private fun verifyTests(test: NavigateContentTest) {
        coVerify {
            when (test.feedType) {
                MAIN -> getMainFeedList(any(), test.isRealtime, any())
                SAVED, DISMISSED -> queryLabeledContentList(test.feedType)
            }
        }
    }
}
