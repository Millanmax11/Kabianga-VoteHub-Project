package com.example.skills;
// this adapter display cards in the homepage

// CardAdapter.java
// CardAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<CardItem> cardItemList;
    private OnButtonClickListener onButtonClickListener;

    public interface OnButtonClickListener {
        void onButtonClick(int position);
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public Button button;

        public CardViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView = itemView.findViewById(R.id.textView);
            button = itemView.findViewById(R.id.button);
        }
    }

    public CardAdapter(List<CardItem> cardItemList, OnButtonClickListener onButtonClickListener) {
        this.cardItemList = cardItemList;
        this.onButtonClickListener = onButtonClickListener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.horizonatal_cards, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem currentItem = cardItemList.get(position);
        holder.imageView.setImageResource(currentItem.getImageResId());
        holder.textView.setText(currentItem.getStatement());
        holder.button.setText(currentItem.getButtonText());

        holder.button.setOnClickListener(v -> {
            if (onButtonClickListener != null) {
                onButtonClickListener.onButtonClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardItemList.size();
    }
}


