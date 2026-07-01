package com.example.atj.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.google.android.material.card.MaterialCardView

/*
 * Modello usato solo per la UI del calendario.
 * Non rappresenta direttamente una tabella Room, ma i dati già pronti da mostrare.
 */
data class CalendarDayUiModel(
    val dayNumber: String,
    val dayInfo: String,
    val status: Status
) {
    /*
     * Stato grafico della cella.
     * Serve a distinguere visivamente giorni vuoti, senza trade, trade aperti,
     * giorni positivi, negativi o misti.
     */
    enum class Status {
        EMPTY,
        NONE,
        OPEN,
        WIN,
        LOSS,
        MIXED
    }
}

/*
 * Adapter della RecyclerView usata come calendario mensile.
 * La RecyclerView è adatta a liste/griglie dinamiche perché riusa le View
 * invece di ricrearle ogni volta da zero.
 */
class CalendarDayAdapter(
    private val items: MutableList<CalendarDayUiModel>
) : RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder>() {

    /*
     * ViewHolder: mantiene i riferimenti alle View della singola cella.
     * In questo modo onBindViewHolder può aggiornare i dati senza rifare findViewById ogni volta.
     */
    class CalendarDayViewHolder(cardView: MaterialCardView) : RecyclerView.ViewHolder(cardView) {
        val dayNumberText: TextView = cardView.findViewById(R.id.dayNumberText)
        val dayInfoText: TextView = cardView.findViewById(R.id.dayInfoText)
        val rootCard: MaterialCardView = cardView
    }

    /*
     * Crea la View della singola cella partendo dal layout XML item_calendar_day.
     * LayoutInflater trasforma la risorsa dichiarativa XML in oggetti View reali.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false) as MaterialCardView
        return CalendarDayViewHolder(view)
    }

    /*
     * Collega i dati del modello UI alla cella visibile.
     * Questo metodo viene richiamato dalla RecyclerView quando una cella deve essere mostrata o riusata.
     */
    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val item = items[position]

        holder.dayNumberText.text = item.dayNumber
        holder.dayInfoText.text = item.dayInfo

        // Cambia lo stile della cella in base allo stato del giorno.
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

    /*
     * Numero totale di celle gestite dalla RecyclerView.
     */
    override fun getItemCount(): Int = items.size

    /*
     * Aggiorna il contenuto del calendario.
     * notifyDataSetChanged forza la RecyclerView a ridisegnare le celle con i nuovi dati.
     */
    fun replaceItems(newItems: List<CalendarDayUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}