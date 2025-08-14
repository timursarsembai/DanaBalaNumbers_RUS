package com.timursarsembayev.danabalanumbers

import com.timursarsembayev.danabalanumbers.R
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ColorPaletteAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorPaletteAdapter.ColorViewHolder>() {

    private var selectedPosition = 0

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: ImageView = itemView.findViewById(R.id.colorView)
        val selectionIndicator: ImageView = itemView.findViewById(R.id.selectionIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_palette, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]

        // Создаем круглый drawable с цветом
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(4, Color.WHITE)
        }

        holder.colorView.background = drawable

        // Показываем индикатор выбора
        holder.selectionIndicator.visibility = if (position == selectedPosition) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = position

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            onColorSelected(color)
        }
    }

    override fun getItemCount(): Int = colors.size

    fun setSelectedColor(color: Int) {
        val position = colors.indexOf(color)
        if (position >= 0) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    fun clearSelection() {
        val oldPosition = selectedPosition
        selectedPosition = -1
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
    }
}
