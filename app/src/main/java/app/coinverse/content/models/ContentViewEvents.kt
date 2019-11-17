package app.coinverse.content.models

import app.coinverse.content.models.ContentViewEventType.FeedLoad
import app.coinverse.utils.FeedType
import app.coinverse.utils.Timeframe
import app.coinverse.utils.UserActionType
import com.google.firebase.auth.FirebaseUser

interface ContentViewEvents {
    fun feedLoad(contentViewEvents: FeedLoad)
}

sealed class ContentViewEventType {
    data class FeedLoad(val feedType: FeedType, val timeframe: Timeframe, val isRealtime: Boolean)
        : ContentViewEventType()

    data class FeedLoadComplete(val hasContent: Boolean) : ContentViewEventType()
    data class AudioPlayerLoad(val contentId: String, val filePath: String, val previewImageUrl: String)
        : ContentViewEventType()

    data class SwipeToRefresh(val feedType: FeedType, val timeframe: Timeframe,
                              val isRealtime: Boolean) : ContentViewEventType()

    data class ContentSelected(val position: Int, val content: Content) : ContentViewEventType()
    data class ContentSwipeDrawed(val isDrawed: Boolean) : ContentViewEventType()
    data class ContentSwiped(val feedType: FeedType, val actionType: UserActionType, val position: Int)
        : ContentViewEventType()

    data class ContentLabeled(val feedType: FeedType, val actionType: UserActionType,
                              val user: FirebaseUser?, val position: Int, val content: Content?,
                              val isMainFeedEmptied: Boolean) : ContentViewEventType()

    data class ContentShared(val content: Content) : ContentViewEventType()
    data class ContentSourceOpened(val url: String) : ContentViewEventType()
    class UpdateAds : ContentViewEventType()
}