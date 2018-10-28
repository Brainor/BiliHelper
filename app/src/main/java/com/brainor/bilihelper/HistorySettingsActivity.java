package com.brainor.bilihelper;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

public class HistorySettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        RecyclerView HistoryListView = findViewById(R.id.HistoryList);
        HistoryListView.setLayoutManager(new LinearLayoutManager(this));
        MyAdapter adapter = new MyAdapter();
        HistoryListView.setAdapter(adapter);
        (new ItemTouchHelper(new SwipeToDeleteCallback(adapter))).attachToRecyclerView(HistoryListView);
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
            TypedValue outValue = new TypedValue();
            getApplicationContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            v.setBackgroundResource(outValue.resourceId);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.mTextView.setText(MainActivity.HistoryList.get(position).title);
            holder.mTextView.setOnClickListener(v ->
                    getApplication().startActivity(new Intent(getApplicationContext(), MainActivity.class).putExtra("position", holder.getAdapterPosition()))
            );
        }

        @Override
        public int getItemCount() {
            return MainActivity.HistoryList.size();
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;

            MyViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }

        }

        void deleteItem(int position) {
            MainActivity.HistoryList.remove(position);
            notifyItemRemoved(position);
            MainActivity.StoreHistory(getApplicationContext());
        }
    }

    class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private MyAdapter mAdapter;
        private Drawable icon;
        private final ColorDrawable background;
        SwipeToDeleteCallback(MyAdapter adapter) {
            super(0, ItemTouchHelper.LEFT);
            mAdapter = adapter;
            icon = ContextCompat.getDrawable(getApplicationContext(), android.R.drawable.ic_menu_delete);
            background = new ColorDrawable(Color.RED);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            int position = viewHolder.getAdapterPosition();
            mAdapter.deleteItem(position);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            View itemView = viewHolder.itemView;
            int backgroundCornerOffset = 0;
            if (dX < 0) {
                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else background.setBounds(0, 0, 0, 0);
            background.draw(c);
            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            if (dX < 0) { // Swiping to the left
                int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                        itemView.getTop(), itemView.getRight(), itemView.getBottom());
            } else { // view is unSwiped
                background.setBounds(0, 0, 0, 0);
            }
            background.draw(c);
            icon.draw(c);
        }

    }
}