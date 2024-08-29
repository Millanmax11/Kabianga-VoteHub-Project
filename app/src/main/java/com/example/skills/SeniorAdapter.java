/* THIS IS AN ADAPTER FOR SENIOR FRAGMENT. */
package com.example.skills;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeniorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_POSITION = 0;
    private static final int VIEW_TYPE_SENIOR = 1;
    private List<Object> itemList;
    private Map<String, Integer> selectedPositionsMap = new HashMap<>();
    private Map<String, SeniorViewHolder> viewHolderMap = new HashMap<>();

    public SeniorAdapter(List<Object> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_POSITION) {
            View itemView = inflater.inflate(R.layout.item_position, parent, false);
            return new PositionViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.item_user, parent, false);
            return new SeniorViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = itemList.get(position);

        if (holder instanceof PositionViewHolder) {
            ((PositionViewHolder) holder).bind((String) item);
        } else if (holder instanceof SeniorViewHolder) {
            ((SeniorViewHolder) holder).bind((Senior) item);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = itemList.get(position);
        return item instanceof String ? VIEW_TYPE_POSITION : VIEW_TYPE_SENIOR;
    }

    public Map<String, Integer> getSelectedPositionsMap() {
        return selectedPositionsMap;
    }

    private class PositionViewHolder extends RecyclerView.ViewHolder {
        private final TextView positionTextView;

        public PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            positionTextView = itemView.findViewById(R.id.textViewPosition);
        }

        public void bind(String position) {
            positionTextView.setText(position);
        }
    }

    private class SeniorViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView yearTextView;
        private RadioButton voteRadioButton;
        private String position;
        private ImageView imageView;
        private int adapterPosition;

        public SeniorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewName);
            imageView = itemView.findViewById(R.id.imageView);
            yearTextView = itemView.findViewById(R.id.textViewYear);
            voteRadioButton = itemView.findViewById(R.id.radioButtonVote);
        }

        public void bind(Senior senior) {
            nameTextView.setText(senior.getName());
            yearTextView.setText(senior.getYear());
            position = senior.getPosition();
            adapterPosition = getAdapterPosition();

            // Load image into imageView using Glide (or Picasso)
            Glide.with(itemView.getContext())
                    .load(senior.getImage()) // Use getImage() method from Senior class
                    .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                    .error(R.drawable.error_image) // Error image if loading fails
                    .circleCrop() // Apply the circular crop transformation
                    .into(imageView);

            voteRadioButton.setOnClickListener(v -> {
                for (Map.Entry<String, SeniorViewHolder> entry : viewHolderMap.entrySet()) {
                    if (entry.getKey().equals(position)) {
                        SeniorViewHolder viewHolder = entry.getValue();
                        if (viewHolder != this) {
                            viewHolder.voteRadioButton.setChecked(false);
                        }
                    }
                }
                selectedPositionsMap.put(position, adapterPosition);
                viewHolderMap.put(position, this);
            });

            voteRadioButton.setChecked(selectedPositionsMap.get(position) != null && selectedPositionsMap.get(position) == adapterPosition);
        }
    }
}
