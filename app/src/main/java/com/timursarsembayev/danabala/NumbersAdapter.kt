package com.timursarsembayev.danabalanumbers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class NumbersAdapter(
    private var items: MutableList<MatchingItem>,
    private val onItemClick: (MatchingItem, View) -> Unit
) : RecyclerView.Adapter<NumbersAdapter.NumberViewHolder>() {

    class NumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberText: TextView = itemView.findViewById(R.id.numberText)
        val cardView: CardView = itemView.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val item = items[position]
        holder.numberText.text = item.value.toString()

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
