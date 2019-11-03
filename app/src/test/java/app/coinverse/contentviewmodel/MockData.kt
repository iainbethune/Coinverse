package app.coinverse.contentviewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.paging.PagedList
import app.coinverse.content.models.*
import app.coinverse.utils.*
import app.coinverse.utils.ContentType.ARTICLE
import app.coinverse.utils.ContentType.YOUTUBE
import app.coinverse.utils.LCE_STATE.*
import app.coinverse.utils.livedata.Event
import app.coinverse.utils.models.Lce
import app.coinverse.utils.models.Lce.Error
import app.coinverse.utils.models.Lce.Loading

val mockArticleContent = Content(id = "1", contentType = ARTICLE, url = MOCK_URL)
val mockYouTubeContent = Content(id = "1", contentType = YOUTUBE, url = MOCK_URL)
val mockDbContentListForDay = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"))
val mockDbContentListForAll = listOf(Content(id = "1"), Content(id = "2"),
        Content(id = "3"), Content(id = "4"), Content(id = "5"), Content(id = "6"))

// TODO - Test liveData coroutine builder

fun mockGetMainFeedList(mockFeedList: List<Content>, lceState: LCE_STATE) =
        MutableLiveData<Lce<PagedListResult>>().also { lce ->
            when (lceState) {
                LOADING -> lce.value = Loading()
                CONTENT -> {
                    lce.value = Lce.Content(PagedListResult(null, ""))
                    lce.value = Lce.Content(PagedListResult(
                            pagedList = mockQueryMainContentList(mockFeedList),
                            errorMessage = ""))
                }
                ERROR -> lce.value = Error(PagedListResult(
                        pagedList = null,
                        errorMessage = MOCK_GET_MAIN_FEED_LIST_ERROR))
            }
        }

fun mockQueryMainContentList(mockFeedList: List<Content>) =
        MutableLiveData<PagedList<Content>>().also { pagedList ->
            pagedList.value = mockFeedList.asPagedList(PagedList.Config.Builder()
                    .setEnablePlaceholders(false)
                    .setPrefetchDistance(24)
                    .setPageSize(12)
                    .build())
        }

fun mockGetAudiocast(test: PlayContentTest) =
        MutableLiveData<Lce<ContentToPlay>>().also { lce ->
            when (test.lceState) {
                LOADING -> lce.value = Loading()
                CONTENT -> lce.value = Lce.Content(ContentToPlay(
                        position = test.mockPosition,
                        content = test.mockContent,
                        filePath = test.mockFilePath,
                        errorMessage = ""
                ))
                ERROR -> lce.value = Error(ContentToPlay(
                        position = test.mockPosition,
                        content = test.mockContent,
                        filePath = test.mockFilePath,
                        errorMessage = test.mockGetAudiocastError))
            }
        }

fun mockGetContentUri(test: PlayContentTest) =
        MutableLiveData<Lce<ContentPlayer>>().apply {
            when (test.lceState) {
                LOADING -> value = Loading()
                CONTENT -> value = Lce.Content(ContentPlayer(
                        uri = Uri.parse(""),
                        image = test.mockPreviewImageByteArray,
                        errorMessage = ""))
                ERROR -> value = Error(ContentPlayer(
                        uri = Uri.parse(""),
                        image = ByteArray(0),
                        errorMessage = MOCK_GET_CONTENT_URI_ERROR))
            }
        }

fun mockBitmapToByteArray(test: PlayContentTest) =
        MutableLiveData<Lce<ContentBitmap>>().apply {
            when (test.lceState) {
                LOADING -> value = Loading()
                CONTENT -> value = Lce.Content(ContentBitmap(
                        image = test.mockPreviewImageByteArray,
                        errorMessage = ""))
                ERROR -> value = Error(ContentBitmap(
                        image = ByteArray(0),
                        errorMessage = MOCK_GET_BITMAP_TO_BYTEARRAY_ERROR))
            }
        }

fun mockEditContentLabels(test: LabelContentTest) = liveData {
    emit(when (test.lceState) {
        LOADING -> Loading()
        CONTENT -> Lce.Content(ContentLabeled(test.adapterPosition, ""))
        ERROR -> Error(ContentLabeled(test.adapterPosition, MOCK_CONTENT_LABEL_ERROR))
    })
}

fun mockGetContent(test: NavigateContentTest) =
        MutableLiveData<Event<Content>>().apply { value = Event(test.mockContent) }
