package app.carpecoin.content.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import app.carpecoin.Enums.FeedType.MAIN
import app.carpecoin.Enums.UserActionType
import app.carpecoin.coin.databinding.CellContentBinding
import app.carpecoin.content.ContentViewModel
import app.carpecoin.content.models.Content
import com.google.firebase.auth.FirebaseUser

private val LOG_TAG = ContentAdapter::class.java.simpleName

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent.id == newContent.id

    override fun areContentsTheSame(oldContent: Content, newContent: Content): Boolean =
            oldContent == newContent
}

class ContentAdapter(var contentViewmodel: ContentViewModel) : PagedListAdapter<Content, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CellContentBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        binding.viewmodel = contentViewmodel
        val viewHolder = ViewHolder(binding)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    fun organizeContent(feedType: String, actionType: UserActionType, itemPosition: Int, user: FirebaseUser) {
        var mainFeedEmptied = false
        if (feedType == MAIN.name) {
            mainFeedEmptied = itemCount == 1
        }
        contentViewmodel.organizeContent(feedType, actionType, user, getItem(itemPosition), mainFeedEmptied)
    }

}
