package com.ori.proteinapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private List<Meal> mealList;

    public MealAdapter(List<Meal> mealList) {
        this.mealList = mealList;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_meal_item, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal current = mealList.get(position);

        // ---------- Meal Name ----------
        // במקום current.getName(), פשוט נשים טקסט קבוע בינתיים:
        holder.tvMealName.setText("Logged Meal");

        // ---------- Date Logic (Show only if date changed) ----------
        if (position == 0 || !current.getDate().equals(mealList.get(position - 1).getDate())) {
            holder.tvMealDate.setVisibility(View.VISIBLE);
            holder.tvMealDate.setText(current.getDate());
        } else {
            holder.tvMealDate.setVisibility(View.GONE);
        }

        // ---------- Protein & Calories (Separated for Design) ----------
        holder.tvProteinInfo.setText("Protein: " + current.getProtein() + "g");
        holder.tvCaloriesInfo.setText("Calories: " + current.getCalories());

        // ---------- Image Handling ----------
        if (current.getBase64Image() != null && !current.getBase64Image().isEmpty()) {
            byte[] decodedBytes = Base64.decode(current.getBase64Image(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.imgMealItem.setImageBitmap(bitmap);
        } else {
            // תמונת ברירת מחדל אם אין תמונה
            holder.imgMealItem.setImageResource(android.R.color.transparent);
        }

        // ---------- Favorite (Apple Style Alpha) ----------
        // אם זה מועדף - צבע מלא, אם לא - שקיפות נמוכה מאוד
        holder.imgFavorite.setAlpha(current.isFavorite() ? 1f : 0.2f);

        holder.imgFavorite.setOnClickListener(v -> {
            boolean newFav = !current.isFavorite();
            current.setFavorite(newFav);
            holder.imgFavorite.setAlpha(newFav ? 1f : 0.2f);

            // Update Firestore
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null && current.getId() != null) {
                FirebaseFirestore.getInstance()
                        .collection("Users")
                        .document(uid)
                        .collection("Meals")
                        .document(current.getId())
                        .update("favorite", newFav);
            }
        });

        // ---------- Relog Button (Add meal again) ----------
        if (holder.btnRelog != null) {
            holder.btnRelog.setOnClickListener(v -> {
                // כאן תוכל להוסיף לוגיקה שמוסיפה את אותה ארוחה שוב להיום
                // למשל: openRelogDialog(current);
            });
        }
    }

    // ✅ מתודה לעדכון הרשימה
    public void updateList(List<Meal> newList) {
        this.mealList.clear();
        this.mealList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        // 1. הגדרת המשתנים החדשים
        TextView tvMealName, tvMealDate, tvProteinInfo, tvCaloriesInfo;
        ImageView imgMealItem, imgFavorite, btnRelog;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            // 2. קישור ל-IDs החדשים מה-XML
            tvMealName = itemView.findViewById(R.id.tvMealName);
            tvMealDate = itemView.findViewById(R.id.tvMealDate);
            tvProteinInfo = itemView.findViewById(R.id.tvProteinInfo);
            tvCaloriesInfo = itemView.findViewById(R.id.tvCaloriesInfo);
            imgMealItem = itemView.findViewById(R.id.imgMealItem);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            btnRelog = itemView.findViewById(R.id.btnRelog);
        }
    }
}
