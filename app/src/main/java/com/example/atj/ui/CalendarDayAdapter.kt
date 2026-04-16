package com.example.atj.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.google.android.material.card.MaterialCardView

/**
 * Modello UI per una singola cella del calendario mensile.
 */
data class CalendarDayUiModel(
    val dayNumber: String,
    val dayInfo: String,
    val status: Status
) {
    enum class Status {
        EMPTY,
        NONE,
        OPEN,
        WIN,
        LOSS,
        MIXED
    }
}

/**
 * Adapter per il calendario a griglia 7 colonne.
 */
class CalendarDayAdapter(
    private val items: MutableList<CalendarDayUiModel>
) : RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder>() {

    class CalendarDayViewHolder(cardView: MaterialCardView) : RecyclerView.ViewHolder(cardView) {
        val dayNumberText: TextView = cardView.findViewById(R.id.dayNumberText)
        val dayInfoText: TextView = cardView.findViewById(R.id.dayInfoText)
        val rootCard: MaterialCardView = cardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false) as MaterialCardView
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val item = items[position]

        holder.dayNumberText.text = item.dayNumber
        holder.dayInfoText.text = item.dayInfo

        when (item.status) {
            CalendarDayUiModel.Status.EMPTY -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#00000000"))
                holder.rootCard.strokeWidth = 0
                holder.dayNumberText.text = ""
                holder.dayInfoText.text = ""
            }

            CalendarDayUiModel.Status.NONE -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                holder.rootCard.strokeColor = Color.parseColor("#E2E8F0")
                holder.rootCard.strokeWidth = 1
            }

            CalendarDayUiModel.Status.OPEN -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#EEF2FF"))
                holder.rootCard.strokeColor = Color.parseColor("#C7D2FE")
                holder.rootCard.strokeWidth = 1
            }

            CalendarDayUiModel.Status.WIN -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#E8FFF2"))
                holder.rootCard.strokeColor = Color.parseColor("#86EFAC")
                holder.rootCard.strokeWidth = 1
            }

            CalendarDayUiModel.Status.LOSS -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#FFF1F2"))
                holder.rootCard.strokeColor = Color.parseColor("#FDA4AF")
                holder.rootCard.strokeWidth = 1
            }

            CalendarDayUiModel.Status.MIXED -> {
                holder.rootCard.setCardBackgroundColor(Color.parseColor("#FFF7ED"))
                holder.rootCard.strokeColor = Color.parseColor("#FDBA74")
                holder.rootCard.strokeWidth = 1
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceItems(newItems: List<CalendarDayUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}