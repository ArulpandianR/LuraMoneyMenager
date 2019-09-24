package com.lura.moneymanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lura.moneymanager.R
import com.lura.moneymanager.model.MessageData
import kotlinx.android.synthetic.main.card_item.view.*

class MessageAdapter(var list: MutableList<MessageData>, var context: Context) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewtype: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.card_item, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.day_title.text = list.get(position).number
        holder.day_temp.text = list.get(position).body
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var day_title = view.heading
        var day_temp = view.desc
    }
}