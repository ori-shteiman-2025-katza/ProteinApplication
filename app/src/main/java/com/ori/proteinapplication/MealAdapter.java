package com.ori.proteinapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
        MealItem meal = mealList.get(position);
        holder.tvMealInfo.setText("חלבון: " + meal.getProtein() + "g | קלוריות: " + meal.getCalories());
        Glide.with(holder.imgMealItem.getContext()).load(meal.getImageUrl()).into(holder.imgMealItem);
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMealItem;
        TextView tvMealInfo;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealItem = itemView.findViewById(R.id.imgMealItem);
            tvMealInfo = itemView.findViewById(R.id.tvMealInfo);
        }
    }
}
