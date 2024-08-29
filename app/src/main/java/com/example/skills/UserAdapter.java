/* THIS IS AN ADAPTER TO FOR DELEGATE FRAGMENT */
package com.example.skills;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.bumptech.glide.Glide;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private int selectedPosition = -1; // No selection by default

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewYear.setText(user.getYear());

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(user.getImage()) // Image URL from the User object
                .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                .error(R.drawable.error_image) // Error image if loading fails
                .circleCrop() // Apply the circular crop transformation
                .into(holder.imageView); // Target ImageView

        holder.radioButtonVote.setChecked(position == selectedPosition);

        holder.radioButtonVote.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
        });

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewYear;
        ImageView imageView;
        RadioButton radioButtonVote;

        public UserViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewYear = itemView.findViewById(R.id.textViewYear);
            imageView = itemView.findViewById(R.id.imageView);
            radioButtonVote = itemView.findViewById(R.id.radioButtonVote);
        }
    }
}
