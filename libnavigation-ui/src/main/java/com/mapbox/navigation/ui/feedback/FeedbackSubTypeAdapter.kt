package com.mapbox.navigation.ui.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.libnavigation.ui.R

/**
 * FeedbackSubTypeAdapter provides a binding from [FeedbackBottomSheet] data set
 * to [FeedbackSubTypeViewHolder] that are displayed within a [RecyclerView].
 */
internal class FeedbackSubTypeAdapter internal constructor(
    private val feedbackSubTypeItems: List<FeedbackSubTypeItem>,
    private val itemClickListener: OnSubTypeItemClickListener
) : RecyclerView.Adapter<FeedbackSubTypeViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedbackSubTypeViewHolder {
        return FeedbackSubTypeViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mapbox_feedback_detail_viewholder_layout, parent, false),
            itemClickListener
        )
    }

    override fun onBindViewHolder(
        holder: FeedbackSubTypeViewHolder,
        position: Int
    ) {
        holder.setFeedbackSubTypeInfo(feedbackSubTypeItems[position])
    }

    override fun getItemCount(): Int {
        return feedbackSubTypeItems.size
    }

    fun getFeedbackSubTypeItem(position: Int): FeedbackSubTypeItem {
        return feedbackSubTypeItems[position]
    }

    internal interface OnSubTypeItemClickListener {
        fun onItemClick(position: Int): Boolean
    }
}
