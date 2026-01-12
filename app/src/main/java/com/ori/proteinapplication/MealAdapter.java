package com.ori.proteinapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private List<MealItem> mealList;

    public MealAdapter(List<MealItem> mealList) {
        this.mealList = mealList;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_item_layout, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {

            MealItem current = mealList.get(position);

            // ---------- תאריך ----------
            if (position == 0 ||
                    !current.getDate().equals(mealList.get(position - 1).getDate())) {

                holder.tvMealDate.setVisibility(View.VISIBLE);
                holder.tvMealDate.setText(current.getDate());

            } else {
                holder.tvMealDate.setVisibility(View.GONE);
            }

            // ---------- חלבון וקלוריות ----------
            holder.tvMealInfo.setText(
                    "חלבון: " + current.getProtein() + "g | קלוריות: " + current.getCalories()
            );

            // ---------- תמונה ----------
            Glide.with(holder.imgMealItem.getContext())
                    .load(current.getImageUrl())
                    .into(holder.imgMealItem);

            // ---------- Favorite ----------
            holder.imgFavorite.setImageResource(
                    current.isFavorite() ? R.drawable.img : R.drawable.img
            );

            holder.imgFavorite.setOnClickListener(v -> {
                boolean newFav = !current.isFavorite();
                current.setFavorite(newFav);
                holder.imgFavorite.setImageResource(newFav ? R.drawable.img : R.drawable.img);

                // שמירה ב-Firestore
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null && current.getId() != null) {
                    FirebaseFirestore.getInstance()
                            .collection("Users").document(uid)
                            .collection("Meals").document(current.getId())
                            .update("favorite", newFav);
                }
            });
        }




    @Override
    public int getItemCount() {
        return mealList.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMealItem, imgFavorite;
        TextView tvMealInfo, tvMealDate;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealItem = itemView.findViewById(R.id.imgMealItem);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            tvMealInfo = itemView.findViewById(R.id.tvMealInfo);
            tvMealDate = itemView.findViewById(R.id.tvMealDate);
        }
    } }
