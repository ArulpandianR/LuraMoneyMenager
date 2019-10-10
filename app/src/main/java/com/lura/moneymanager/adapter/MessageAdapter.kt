package com.lura.moneymanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lura.moneymanager.R
import com.lura.moneymanager.model.MessageData
import kotlinx.android.synthetic.main.card_item.view.*

class MessageAdapter(var list: MutableList<MessageData>, var context: Context) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewtype: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.card_item, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = list.get(position).title
        if (list.get(position).status.equals("Debited")) {
            holder.status.setTextColor(
                ContextCompat.getColor(
                    context,
                    android.R.color.holo_red_dark
                )
            )
        } else {
            holder.status.setTextColor(
                ContextCompat.getColor(
                    context,
                    android.R.color.holo_green_dark
                )
            )
        }
        holder.status.text = list.get(position).status
        holder.amount.text = list.get(position).amount
        holder.date.text = list.get(position).date
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title = view.heading
        var status = view.status
        var amount = view.amount
        var date = view.date
    }
}