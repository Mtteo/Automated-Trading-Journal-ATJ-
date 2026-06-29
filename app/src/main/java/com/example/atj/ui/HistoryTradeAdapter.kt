package com.example.atj.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.example.atj.model.Trade

class HistoryTradeAdapter(
    private val trades: MutableList<Trade>,
    private val onItemClick: (Trade) -> Unit,
    private val onItemLongClick: (Trade) -> Unit
) : RecyclerView.Adapter<HistoryTradeAdapter.HistoryTradeViewHolder>() {

    class HistoryTradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetText: TextView = itemView.findViewById(R.id.assetText)
        val resultBadgeText: TextView = itemView.findViewById(R.id.resultBadgeText)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val sessionText: TextView = itemView.findViewById(R.id.sessionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryTradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_trade, parent, false)
        return HistoryTradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryTradeViewHolder, position: Int) {
        val trade = trades[position]

        val result = trade.result.ifBlank { "Open" }

        holder.assetText.text = trade.asset
        holder.resultBadgeText.text = result
        holder.typeText.text = "${trade.type} • ${trade.direction.ifBlank { "N/A" }}"
        holder.dateText.text = trade.date
        holder.sessionText.text = "${trade.session} • PnL ${trade.pnlAmount}"

        holder.itemView.setOnClickListener {
            onItemClick(trade)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(trade)
            true
        }
    }

    override fun getItemCount(): Int = trades.size

    fun replaceTrades(newTrades: List<Trade>) {
        trades.clear()
        trades.addAll(newTrades)
        notifyDataSetChanged()
    }
}