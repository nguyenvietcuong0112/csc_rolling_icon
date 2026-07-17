package com.mandg.funny.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mandg.funny.R
import com.mandg.funny.data.PreferenceRepository
import kotlinx.coroutines.launch

class EmojiPickerActivity : AppCompatActivity() {

    private lateinit var tabEmoji: ImageView
    private lateinit var tabAnimal: ImageView
    private lateinit var tabLove: ImageView
    private lateinit var tabJoke: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSave: Button

    private lateinit var preferenceRepository: PreferenceRepository
    private val selectedEmojisSet = HashSet<String>()
    private val currentList = ArrayList<String>()
    private lateinit var adapter: EmojiAdapter

    // Định nghĩa trước danh sách tên của các drawable tương ứng trong cơ sở dữ liệu gốc
    private val emojiGroup = (1..83).map { String.format("emoji_emoji_%02d", it) }
    private val animalGroup = (1..28).map { String.format("emoji_animal_%02d", it) }
    private val loveGroup = (1..20).map { String.format("emoji_love_%02d", it) }
    private val jokeGroup = (1..22).map { String.format("emoji_joke_%02d", it) }

    private var activeGroup = 1 // 1: Emoji, 2: Animal, 3: Love, 4: Joke

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emoji_picker)

        preferenceRepository = PreferenceRepository(this)

        tabEmoji = findViewById(R.id.emoji_group_emoji)
        tabAnimal = findViewById(R.id.emoji_group_animal)
        tabLove = findViewById(R.id.emoji_group_love)
        tabJoke = findViewById(R.id.emoji_group_joke)
        recyclerView = findViewById(R.id.emoji_recycler_view)
        btnSave = findViewById(R.id.btnSaveEmojis)

        recyclerView.layoutManager = GridLayoutManager(this, 4)
        adapter = EmojiAdapter()
        recyclerView.adapter = adapter

        setupTabs()

        // Tải các emojis đã lựa chọn trước đó
        lifecycleScope.launch {
            val saved = preferenceRepository.getSelectedEmojis()
            selectedEmojisSet.addAll(saved)
            selectTab(1)
        }

        btnSave.setOnClickListener {
            lifecycleScope.launch {
                preferenceRepository.setSelectedEmojis(selectedEmojisSet)
                Toast.makeText(this@EmojiPickerActivity, "Đã lưu cài đặt Emojis!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupTabs() {
        tabEmoji.setOnClickListener { selectTab(1) }
        tabAnimal.setOnClickListener { selectTab(2) }
        tabLove.setOnClickListener { selectTab(3) }
        tabJoke.setOnClickListener { selectTab(4) }
    }

    private fun selectTab(group: Int) {
        activeGroup = group

        // Cập nhật trạng thái Selected của các Tab icon
        tabEmoji.isSelected = (group == 1)
        tabAnimal.isSelected = (group == 2)
        tabLove.isSelected = (group == 3)
        tabJoke.isSelected = (group == 4)

        // Đổi màu nền của Tab để làm nổi bật (sử dụng alpha/background)
        tabEmoji.setBackgroundColor(if (group == 1) 0x33FFFFFF else 0)
        tabAnimal.setBackgroundColor(if (group == 2) 0x33FFFFFF else 0)
        tabLove.setBackgroundColor(if (group == 3) 0x33FFFFFF else 0)
        tabJoke.setBackgroundColor(if (group == 4) 0x33FFFFFF else 0)

        // Cập nhật danh sách hiển thị
        currentList.clear()
        when (group) {
            1 -> currentList.addAll(emojiGroup)
            2 -> currentList.addAll(animalGroup)
            3 -> currentList.addAll(loveGroup)
            4 -> currentList.addAll(jokeGroup)
        }
        adapter.notifyDataSetChanged()
    }

    private inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.emoji_item_icon_view)
            val checkedView: ImageView = view.findViewById(R.id.emoji_item_icon_checked_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emojiName = currentList[position]
            val resId = resources.getIdentifier(emojiName, "drawable", packageName)

            if (resId != 0) {
                holder.iconView.setImageResource(resId)
            } else {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            val isSelected = selectedEmojisSet.contains(emojiName)
            holder.checkedView.setImageResource(if (isSelected) R.drawable.app_picker_checked else R.drawable.checkbox_unselected)

            holder.itemView.setOnClickListener {
                if (selectedEmojisSet.contains(emojiName)) {
                    selectedEmojisSet.remove(emojiName)
                    holder.checkedView.setImageResource(R.drawable.checkbox_unselected)
                } else {
                    selectedEmojisSet.add(emojiName)
                    holder.checkedView.setImageResource(R.drawable.app_picker_checked)
                }
            }
        }

        override fun getItemCount(): Int = currentList.size
    }
}
