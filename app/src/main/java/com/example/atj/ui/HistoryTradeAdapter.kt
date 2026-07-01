package com.example.atj.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.example.atj.model.Trade
import com.example.atj.utils.TradeStateHelper

/*
 * Adapter per la schermata storico.
 * Trasforma una lista di Trade in righe grafiche dentro una RecyclerView.
 */
class HistoryTradeAdapter(
    private val trades: MutableList<Trade>,
    private val onItemClick: (Trade) -> Unit,
    private val onItemLongClick: (Trade) -> Unit
) : RecyclerView.Adapter<HistoryTradeAdapter.HistoryTradeViewHolder>() {

    /*
     * ViewHolder della singola riga.
     * Mantiene i riferimenti alle View per evitare findViewById ripetuti.
     */
    class HistoryTradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val biasIconText: TextView = itemView.findViewById(R.id.biasIconText)
        val assetText: TextView = itemView.findViewById(R.id.assetText)
        val resultBadgeText: TextView = itemView.findViewById(R.id.resultBadgeText)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val sessionText: TextView = itemView.findViewById(R.id.sessionText)
    }

    /*
     * Crea la View della riga partendo dal layout XML.
     * LayoutInflater converte la risorsa item_history_trade in oggetti View.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryTradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_trade, parent, false)
        return HistoryTradeViewHolder(view)
    }

    /*
     * Collega il Trade alla riga visibile.
     * RecyclerView richiama questo metodo quando deve mostrare o riusare una cella.
     */
    override fun onBindViewHolder(holder: HistoryTradeViewHolder, position: Int) {
        val trade = trades[position]
        val displayState = TradeStateHelper.displayState(trade)

        holder.assetText.text = trade.asset
        holder.resultBadgeText.text = displayState
        holder.typeText.text = "${trade.type} • ${trade.direction.ifBlank { "N/A" }}"
        holder.dateText.text = trade.date
        holder.sessionText.text = "${trade.session} • PnL ${trade.pnlAmount}"

        // Applica colori e badge in base allo stato del trade.
        applyVisualStyle(holder, trade, displayState)

        // Listener per apertura/dettaglio del trade.
        holder.itemView.setOnClickListener {
            onItemClick(trade)
        }

        // Listener per azioni secondarie, ad esempio modifica o cancellazione.
        holder.itemView.setOnLongClickListener {
            onItemLongClick(trade)
            true
        }
    }

    /*
     * Numero di righe gestite dalla RecyclerView.
     */
    override fun getItemCount(): Int = trades.size

    /*
     * Aggiorna l'intera lista mostrata nello storico.
     */
    fun replaceTrades(newTrades: List<Trade>) {
        trades.clear()
        trades.addAll(newTrades)
        notifyDataSetChanged()
    }

    /*
     * Gestisce lo stile visuale della riga.
     * La UI cambia in base al risultato del trade, senza modificare il dato salvato.
     */
    private fun applyVisualStyle(
        holder: HistoryTradeViewHolder,
        trade: Trade,
        displayState: String
    ) {
        when {
            TradeStateHelper.isWin(trade) -> {
                holder.biasIconText.text = if (trade.direction.equals("Long", true)) "🐂" else "↗"
                styleBadge(
                    textView = holder.resultBadgeText,
                    backgroundColor = "#0F2A1B",
                    strokeColor = "#14532D",
                    textColor = "#86EFAC"
                )
                styleBadge(
                    textView = holder.biasIconText,
                    backgroundColor = "#0B3B22",
                    strokeColor = "#166534",
                    textColor = "#86EFAC"
                )
                holder.sessionText.setTextColor(Color.parseColor("#86EFAC"))
            }

            TradeStateHelper.isLoss(trade) -> {
                holder.biasIconText.text = if (trade.direction.equals("Short", true)) "🐻" else "↘"
                styleBadge(
                    textView = holder.resultBadgeText,
                    backgroundColor = "#2A1014",
                    strokeColor = "#7F1D1D",
                    textColor = "#FDA4AF"
                )
                styleBadge(
                    textView = holder.biasIconText,
                    backgroundColor = "#3A0F14",
                    strokeColor = "#991B1B",
                    textColor = "#FCA5A5"
                )
                holder.sessionText.setTextColor(Color.parseColor("#FCA5A5"))
            }

            TradeStateHelper.isBreakEven(trade) -> {
                holder.biasIconText.text = "◉"
                styleBadge(
                    textView = holder.resultBadgeText,
                    backgroundColor = "#2A1B0F",
                    strokeColor = "#9A3412",
                    textColor = "#FDBA74"
                )
                styleBadge(
                    textView = holder.biasIconText,
                    backgroundColor = "#3A2412",
                    strokeColor = "#C2410C",
                    textColor = "#FDBA74"
                )
                holder.sessionText.setTextColor(Color.parseColor("#FDBA74"))
            }

            else -> {
                holder.biasIconText.text = "◎"
                styleBadge(
                    textView = holder.resultBadgeText,
                    backgroundColor = "#172554",
                    strokeColor = "#1D4ED8",
                    textColor = "#BFDBFE"
                )
                styleBadge(
                    textView = holder.biasIconText,
                    backgroundColor = "#111C33",
                    strokeColor = "#2563EB",
                    textColor = "#93C5FD"
                )
                holder.sessionText.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    /*
     * Crea graficamente un badge tramite GradientDrawable.
     * È una modifica programmatica della View, alternativa agli attributi XML.
     */
    private fun styleBadge(
        textView: TextView,
        backgroundColor: String,
        strokeColor: String,
        textColor: String
    ) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor(backgroundColor))
            setStroke(2, Color.parseColor(strokeColor))
        }

        textView.background = drawable
        textView.setTextColor(Color.parseColor(textColor))
    }
}