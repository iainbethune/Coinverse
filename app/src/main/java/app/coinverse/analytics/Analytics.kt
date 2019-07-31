package app.coinverse.analytics

import android.os.Bundle
import android.util.Log
import app.coinverse.analytics.models.UserAction
import app.coinverse.content.ContentRepository
import app.coinverse.content.models.Content
import app.coinverse.content.room.ContentDao
import app.coinverse.firebase.*
import app.coinverse.utils.*
import app.coinverse.utils.Enums.UserActionType.*
import com.google.android.gms.measurement.module.Analytics
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = Analytics::class.java.simpleName

//TODO - Use coroutine
fun updateActionsAndAnalytics(content: Content, contentDao: ContentDao, analytics: FirebaseAnalytics,
                              watchPercent: Double) {
    Thread(Runnable { run { contentDao.updateContentItem(content) } }).start()
    if (watchPercent >= FINISH_THRESHOLD)
        Bundle().also { bundle ->
            FirebaseAuth.getInstance().currentUser.also { user ->
                bundle.putString(Param.ITEM_NAME, content.title)
                if (user != null && !user.isAnonymous) {
                    updateActionAnalytics(FINISH, content, user)
                    bundle.putString(USER_ID_PARAM, user.uid)
                }
            }
            bundle.putString(CREATOR_PARAM, content.creator)
            analytics.logEvent(FINISH_CONTENT_EVENT, bundle)
        }
    else if (watchPercent >= CONSUME_THRESHOLD)
        Bundle().also { bundle ->
            FirebaseAuth.getInstance().currentUser.also { user ->
                bundle.putString(Param.ITEM_NAME, content.title)
                if (user != null && !user.isAnonymous) {
                    updateActionAnalytics(CONSUME, content, user)
                    bundle.putString(USER_ID_PARAM, user.uid)
                }
            }
            bundle.putString(CREATOR_PARAM, content.creator)
            analytics.logEvent(CONSUME_CONTENT_EVENT, bundle)
        }
}

fun updateStartActionsAndAnalytics(content: Content, analytics: FirebaseAnalytics) {
    analytics.logEvent(START_CONTENT_EVENT, Bundle().apply {
        this.putString(Param.ITEM_NAME, content.title)
        this.putString(CREATOR_PARAM, content.creator)
        FirebaseAuth.getInstance().currentUser.let { user ->
            if (user != null && !user.isAnonymous) {
                updateActionAnalytics(START, content, user)
                this.putString(USER_ID_PARAM, user.uid)
            }
        }
    })
}


fun updateStartActionsAndAnalytics(savedInstanceState: Bundle?, content: Content,
                                   analytics: FirebaseAnalytics) {
    if (savedInstanceState == null)
        analytics.logEvent(START_CONTENT_EVENT, Bundle().apply {
            this.putString(Param.ITEM_NAME, content.title)
            this.putString(CREATOR_PARAM, content.creator)
            FirebaseAuth.getInstance().currentUser.let { user ->
                if (user != null && !user.isAnonymous) {
                    updateActionAnalytics(START, content, user)
                    this.putString(USER_ID_PARAM, user.uid)
                }
            }
        })
}

fun getWatchPercent(currentPosition: Double, seekToPositionMillis: Double, duration: Double) =
        (currentPosition - seekToPositionMillis) / duration

//TODO - Move to Cloud Function
fun updateActionAnalytics(actionType: Enums.UserActionType, content: Content, user: FirebaseUser) {
    var actionCollection = ""
    var score = INVALID_SCORE
    var countType = ""
    when (actionType) {
        START -> {
            actionCollection = START_ACTION_COLLECTION
            score = START_SCORE
            countType = START_COUNT
        }
        CONSUME -> {
            actionCollection = CONSUME_ACTION_COLLECTION
            score = CONSUME_SCORE
            countType = CONSUME_COUNT
        }
        FINISH -> {
            actionCollection = FINISH_ACTION_COLLECTION
            score = FINISH_SCORE
            countType = FINISH_COUNT
        }
        SAVE -> {
            actionCollection = SAVE_ACTION_COLLECTION
            score = SAVE_SCORE
            countType = ORGANIZE_COUNT
        }
        DISMISS -> {
            actionCollection = DISMISS_ACTION_COLLECTION
            score = DISMISS_SCORE
            countType = DISMISS_COUNT
        }
    }
    contentEnCollection.document(content.id).collection(actionCollection)
            .document(user.email!!).set(UserAction(Timestamp.now(), user.email!!))
            .addOnSuccessListener { status ->
                ContentRepository.updateContentActionCounter(content.id, countType)
                ContentRepository.updateUserActions(user.uid, actionCollection, content, countType)
                ContentRepository.updateQualityScore(score, content.id)
            }.addOnFailureListener { e ->
                Log.e(LOG_TAG,
                        "Transaction failure update action $actionCollection ${countType}.", e)
            }
}