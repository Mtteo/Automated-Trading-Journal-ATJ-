package com.example.atj.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.R
import com.example.atj.model.Trade

/*
 * Adapter base per mostrare una lista di Trade.
 * La RecyclerView usa Adapter e ViewHolder per gestire liste dinamiche in modo efficiente.
 */
class TradeAdapter(
    private val trades: MutableList<Trade>,
    private val onItemClick: (Trade) -> Unit
) : RecyclerView.Adapter<TradeAdapter.TradeViewHolder>() {

    /*
     * ViewHolder della singola riga.
     * Contiene i riferimenti alle View definite nel layout XML item_trade.
     */
    class TradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetText: TextView = itemView.findViewById(R.id.assetText)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    /*
     * Crea una nuova riga quando la RecyclerView ne ha bisogno.
     * Il layout XML viene "inflated" e diventa una View concreta.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trade, parent, false)
        return TradeViewHolder(view)
    }

    /*
     * Associa i dati del Trade alla riga.
     * Qui il model viene trasformato in informazione visibile nella UI.
     */
    override fun onBindViewHolder(holder: TradeViewHolder, position: Int) {
        val trade = trades[position]

        holder.assetText.text = trade.asset
        holder.typeText.text = "${trade.type} | ${trade.result} | ${trade.source}"
        holder.dateText.text = "${trade.date} | ${trade.session}"

        // Listener di click: la reazione viene delegata alla schermata che usa l'adapter.
        holder.itemView.setOnClickListener {
            onItemClick(trade)
        }
    }

    /*
     * Numero totale di elementi presenti nella lista.
     */
    override fun getItemCount(): Int = trades.size

    /*
     * Inserisce un nuovo trade in cima alla lista.
     * notifyItemInserted aggiorna solo la riga aggiunta, non tutta la RecyclerView.
     */
    fun addTrade(trade: Trade) {
        trades.add(0, trade)
        notifyItemInserted(0)
    }

    /*
     * Sostituisce tutti i trade mostrati.
     */
    fun replaceTrades(newTrades: List<Trade>) {
        trades.clear()
        trades.addAll(newTrades)
        notifyDataSetChanged()
    }
}