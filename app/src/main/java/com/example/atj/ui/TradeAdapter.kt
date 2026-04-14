package com.example.atj.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.example.atj.model.Trade

// Adapter della RecyclerView.
// Serve a trasformare la lista di Trade in righe visibili nella UI.
class TradeAdapter(
    private val trades: MutableList<Trade>,
    private val onItemClick: (Trade) -> Unit
) : RecyclerView.Adapter<TradeAdapter.TradeViewHolder>() {

    // ViewHolder = contenitore delle view di una singola riga.
    class TradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetText: TextView = itemView.findViewById(R.id.assetText)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trade, parent, false)
        return TradeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TradeViewHolder, position: Int) {
        val trade = trades[position]

        // Popoliamo i campi della riga.
        holder.assetText.text = trade.asset
        holder.typeText.text = "${trade.type} | ${trade.result} | ${trade.source}"
        holder.dateText.text = "${trade.date} | ${trade.session}"

        // Rende cliccabile la riga.
        holder.itemView.setOnClickListener {
            onItemClick(trade)
        }
    }

    override fun getItemCount(): Int = trades.size

    // Aggiunge un trade in cima alla lista.
    fun addTrade(trade: Trade) {
        trades.add(0, trade)
        notifyItemInserted(0)
    }

    // Sostituisce completamente la lista con una nuova.
    fun replaceTrades(newTrades: List<Trade>) {
        trades.clear()
        trades.addAll(newTrades)
        notifyDataSetChanged()
    }
}