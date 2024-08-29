/* THIS IS AN ADAPTER FOR RESULTSENIOR FRAGMENT */
package com.example.skills;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ResultseniorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_POSITION = 0;
    private static final int VIEW_TYPE_RESULT = 1;
    private List<Object> resultList;

    public ResultseniorAdapter(List<Object> resultList) {
        this.resultList = resultList;
    }

    public void updateResults(List<Object> newResults) {
        this.resultList = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_POSITION) {
            View itemView = inflater.inflate(R.layout.item_position, parent, false);
            return new PositionViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.item_result, parent, false);
            return new ResultViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = resultList.get(position);

        if (holder instanceof PositionViewHolder) {
            ((PositionViewHolder) holder).bind((String) item);
        } else if (holder instanceof ResultViewHolder) {
            ((ResultViewHolder) holder).bind((Result) item);
        }
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = resultList.get(position);
        return item instanceof String ? VIEW_TYPE_POSITION : VIEW_TYPE_RESULT;
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

    private class ResultViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private ImageView imageView;
        private TextView yearTextView;
        private TextView voteCountTextView;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            nameTextView = itemView.findViewById(R.id.textViewName);
            yearTextView = itemView.findViewById(R.id.textViewYear);
            voteCountTextView = itemView.findViewById(R.id.textViewVoteCount);
        }

        public void bind(Result result) {
            nameTextView.setText(result.getName());
            yearTextView.setText(result.getYear());
            voteCountTextView.setText(String.format("%d votes", result.getVoteCount()));
            // Load image into imageView using Glide (or Picasso)
            Glide.with(itemView.getContext())
                    .load(result.getImage()) // Use getImage() method from Senior class
                    .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                    .error(R.drawable.error_image) // Error image if loading fails
                    .circleCrop() // Apply the circular crop transformation
                    .into(imageView);
        }
    }
}

