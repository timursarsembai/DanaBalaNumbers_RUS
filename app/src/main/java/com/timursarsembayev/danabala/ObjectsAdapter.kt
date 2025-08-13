package com.timursarsembayev.danabalanumbers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class ObjectsAdapter(
    private var items: MutableList<MatchingItem>,
    private val onItemClick: (MatchingItem, View) -> Unit
) : RecyclerView.Adapter<ObjectsAdapter.ObjectViewHolder>() {

    class ObjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val objectsText: TextView = itemView.findViewById(R.id.objectsText)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_objects, parent, false)
        return ObjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObjectViewHolder, position: Int) {
        val item = items[position]

        // Создаем строку с повторяющимися эмодзи
        val objectsString = item.emoji.repeat(item.value)
        holder.objectsText.text = objectsString

        // Устанавливаем прозрачность для сопоставленных элементов
        holder.cardView.alpha = if (item.isMatched) 0.3f else 1.0f

        holder.cardView.setOnClickListener {
            if (!item.isMatched) {
                // Простой эффект нажатия
                holder.cardView.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                    .withEndAction {
                        holder.cardView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }.start()

                onItemClick(item, holder.cardView)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<MatchingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(item: MatchingItem) {
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
            // Убираем notifyItemRangeChanged чтобы избежать мерцания
        }
    }
}
