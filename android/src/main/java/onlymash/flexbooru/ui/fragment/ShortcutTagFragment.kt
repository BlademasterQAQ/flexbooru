/*
 * Copyright (C) 2020. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import onlymash.flexbooru.R
import onlymash.flexbooru.app.Keys
import onlymash.flexbooru.app.Settings.activatedBooruUid
import onlymash.flexbooru.app.Values.Tags
import onlymash.flexbooru.app.Values.BOORU_TYPE_DAN
import onlymash.flexbooru.app.Values.BOORU_TYPE_SANKAKU
import onlymash.flexbooru.data.database.BooruManager
import onlymash.flexbooru.data.database.TagFilterManager
import onlymash.flexbooru.data.database.dao.PostDao
import onlymash.flexbooru.data.model.common.*
import onlymash.flexbooru.databinding.FragmentBottomSheetTagBinding
import onlymash.flexbooru.databinding.ItemTagBrowseBinding
import onlymash.flexbooru.extension.copyText
import onlymash.flexbooru.ui.activity.SearchActivity
import onlymash.flexbooru.ui.base.BaseBottomSheetDialogFragment
import onlymash.flexbooru.ui.viewbinding.viewBinding
import onlymash.flexbooru.ui.viewmodel.ShortcutViewModel
import onlymash.flexbooru.ui.viewmodel.getShortcutViewModel
import org.kodein.di.erased.instance

class ShortcutTagFragment : BaseBottomSheetDialogFragment() {
    companion object {
        fun create(postId: Int): ShortcutTagFragment {
            return ShortcutTagFragment().apply {
                arguments = Bundle().apply {
                    putInt(Keys.POST_ID, postId)
                }
            }
        }
    }

    private lateinit var behavior: BottomSheetBehavior<View>

    private val postDao by instance<PostDao>()

    private var postId = -1
    private var post: Post? = null

    private lateinit var booru: Booru
    private lateinit var binding: FragmentBottomSheetTagBinding
    private lateinit var shortcutViewModel: ShortcutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            postId = getInt(Keys.POST_ID, -1)
        }
        val booru = BooruManager.getBooruByUid(activatedBooruUid)
        if (booru == null) {
            dismiss()
            return
        }
        this.booru = booru
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        binding = FragmentBottomSheetTagBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        binding.toolbarLayout.toolbar.apply {
            setTitle(R.string.title_tags)
            setOnClickListener {
                dismiss()
            }
        }
        behavior = BottomSheetBehavior.from(binding.root.parent as View)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

        })
        val tagListAdapter = TagListAdapter()
        binding.tagsList.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = tagListAdapter
        }
        shortcutViewModel = getShortcutViewModel(postDao, booru.uid, postId)
        shortcutViewModel.post.observe(this, Observer { post ->
            this.post = post
            tagListAdapter.notifyDataSetChanged()
        })
        return dialog
    }

    override fun onStart() {
        super.onStart()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    inner class TagListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int = post?.tags?.size ?: 0

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val tag = post?.tags?.get(position)
            (holder as TagListViewHolder).bind(tag)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int): RecyclerView.ViewHolder {
            return TagListViewHolder(parent)
        }
    }

    inner class TagListViewHolder(binding: ItemTagBrowseBinding) : RecyclerView.ViewHolder(binding.root) {

        constructor(parent: ViewGroup): this(parent.viewBinding(ItemTagBrowseBinding::inflate))

        private val dot = binding.dot
        private val tagName = binding.tagName
        private val tagExclude = binding.tagExclude
        private val tagInclude = binding.tagInclude

        private var tag: TagBase? = null

        init {
            TooltipCompat.setTooltipText(tagExclude, tagExclude.contentDescription)
            TooltipCompat.setTooltipText(tagInclude, tagInclude.contentDescription)
            itemView.setOnClickListener {
                tag?.name?.let { query ->
                    SearchActivity.startSearch(itemView.context, query)
                    dismiss()
                }
            }
            itemView.setOnLongClickListener {
                itemView.context.copyText(tagName.text)
                true
            }
            tagExclude.setOnClickListener {
                tag?.let {
                    TagFilterManager.createTagFilter(
                        TagFilter(
                            booruUid = booru.uid,
                            name = "-${it.name}",
                            type = it.category
                        )
                    )
                }
            }
            tagInclude.setOnClickListener {
                tag?.let {
                    TagFilterManager.createTagFilter(
                        TagFilter(
                            booruUid = booru.uid,
                            name = it.name,
                            type = it.category
                        )
                    )
                }
            }
        }

        fun bind(tag: TagBase?) {
            this.tag = tag ?: return
            tagName.text = tag.name
            when (booru.type) {
                BOORU_TYPE_DAN -> {
                    when (tag.category) {
                        Tags.TYPE_GENERAL -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_general))
                        Tags.TYPE_ARTIST -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_artist))
                        Tags.TYPE_COPYRIGHT -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_copyright))
                        Tags.TYPE_CHARACTER -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_character))
                        Tags.TYPE_META -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_meta))
                        else -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                    }
                }
                BOORU_TYPE_SANKAKU -> {
                    when (tag.category) {
                        Tags.TYPE_GENERAL -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_general))
                        Tags.TYPE_ARTIST -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_artist))
                        Tags.TYPE_COPYRIGHT -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_copyright))
                        Tags.TYPE_CHARACTER -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_character))
                        Tags.TYPE_META_SANKAKU -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_meta))
                        Tags.TYPE_GENRE -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_genre))
                        Tags.TYPE_MEDIUM -> dot.setColorFilter(ContextCompat.getColor(itemView.context, R.color.tag_type_medium))
                        Tags.TYPE_STUDIO -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_studio))
                        else -> dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                    }
                }
                else -> {
                    dot.setColorFilter(ContextCompat.getColor(itemView.context,  R.color.tag_type_unknown))
                }
            }
        }
    }
}