package com.brainor.bilihelper

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.history_settings.*

class HistorySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        historyListView.layoutManager = LinearLayoutManager(this)
        val adapter = MyAdapter()
        historyListView.adapter = adapter
        ItemTouchHelper(SwipeToDeleteCallback(adapter)).attachToRecyclerView(historyListView)
    }

    internal inner class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.support_simple_spinner_dropdown_item, parent, false) as TextView
            val outValue = TypedValue()
            applicationContext.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            v.setBackgroundResource(outValue.resourceId)
            return MyViewHolder(v)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.mTextView.text = MainActivity.HistoryList[position].title
            holder.mTextView.setOnClickListener {  application.startActivity(Intent(applicationContext, MainActivity::class.java).putExtra("position", holder.adapterPosition)) }
        }

        override fun getItemCount(): Int {
            return MainActivity.HistoryList.size
        }


        internal inner class MyViewHolder(var mTextView: TextView) : RecyclerView.ViewHolder(mTextView)

        fun deleteItem(position: Int) {
            MainActivity.HistoryList.removeAt(position)
            notifyItemRemoved(position)
            MainActivity.storeHistory(applicationContext)
        }
    }

    internal inner class SwipeToDeleteCallback(private val mAdapter: MyAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val icon: Drawable = ContextCompat.getDrawable(applicationContext, android.R.drawable.ic_menu_delete)!!
        private val background: ColorDrawable = ColorDrawable(Color.RED)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
            val position = viewHolder.adapterPosition
            mAdapter.deleteItem(position)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, viewHolder1: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            val itemView = viewHolder.itemView
            val backgroundCornerOffset = 0
            if (dX < 0) {
                background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset, itemView.top, itemView.right, itemView.bottom)
            } else
                background.setBounds(0, 0, 0, 0)
            background.draw(c)
            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight
            if (dX < 0) { // Swiping to the left
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                        itemView.top, itemView.right, itemView.bottom)
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0)
            }
            background.draw(c)
            icon.draw(c)
        }

    }
}