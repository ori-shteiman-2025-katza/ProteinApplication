package com.ori.proteinapplication;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MealHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerMeals;
    private MealAdapter adapter;
    private TextView tvDailySummary;
    private Button btnPickDate, btnToggleFavorites;

    private final List<Meal> allMeals = new ArrayList<>();
    private final List<Meal> filteredMeals = new ArrayList<>();
    private boolean showFavoritesOnly = false;
    private String selectedDate; // yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDailySummary = findViewById(R.id.tvDailySummary);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnToggleFavorites = findViewById(R.id.btnToggleFavorites);

        recyclerMeals = findViewById(R.id.recyclerMeals);
        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MealAdapter(filteredMeals);
        recyclerMeals.setAdapter(adapter);

        selectedDate = LocalDate.now().toString(); // היום כברירת מחדל

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnToggleFavorites.setOnClickListener(v -> {
            showFavoritesOnly = !showFavoritesOnly;
            filterByDate(selectedDate);
        });

        loadMealsFromFirestore();
    }

    // ---------- Firestore ----------
    private void loadMealsFromFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .collection("Meals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allMeals.clear();

                    for (DocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;

                        String image = (String) data.get("image");
                        Long timestamp = (Long) data.get("timestamp");

                        String date = "";
                        if (timestamp != null) {
                            date = Instant.ofEpochMilli(timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .toString();
                        }

                        int protein = data.get("protein") != null
                                ? ((Number) data.get("protein")).intValue() : 0;

                        int calories = data.get("calories") != null
                                ? ((Number) data.get("calories")).intValue() : 0;

                        Map<String, Double> components =
                                (Map<String, Double>) data.get("components");
                        Boolean favorite = (Boolean) data.get("favorite");
                        String docId = doc.getId();

                        allMeals.add(new Meal(image, protein, calories, components, date, favorite != null ? favorite : false, docId));
                    }

                    filterByDate(selectedDate);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בטעינת הארוחות", Toast.LENGTH_SHORT).show()
                );
    }

    // ---------- סינון לפי יום ו-favorite ----------
    private void filterByDate(String date) {
        selectedDate = date;
        filteredMeals.clear();

        for (Meal meal : allMeals) {
            boolean sameDate = date.equals(meal.getDate());
            boolean favoriteOk = !showFavoritesOnly || meal.isFavorite();

            if (sameDate && favoriteOk) {
                filteredMeals.add(meal);
            }
        }


        adapter.notifyDataSetChanged();
        updateDailySummary(filteredMeals);
    }

    // ---------- סיכום יומי ----------
    private void updateDailySummary(List<Meal> meals) {
        int totalProtein = 0;
        int totalCalories = 0;

        for (Meal meal : meals) {
            totalProtein += meal.getProtein();
            totalCalories += meal.getCalories();
        }

        tvDailySummary.setText(
                "חלבון כולל: " + totalProtein +
                        " | קלוריות כוללות: " + totalCalories
        );
    }

    // ---------- DatePicker ----------
    private void showDatePicker() {
        LocalDate now = LocalDate.now();

        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    LocalDate picked = LocalDate.of(year, month + 1, day);
                    filterByDate(picked.toString());
                },
                now.getYear(),
                now.getMonthValue() - 1,
                now.getDayOfMonth()
        ).show();
    }

    // ================== Adapter ==================
    static class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

        private final List<Meal> meals;

        public MealAdapter(List<Meal> meals) {
            this.meals = meals;
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
            Meal meal = meals.get(position);

            holder.tvMealName.setText("ארוחה " + (position + 1));
            holder.tvMealTotals.setText(
                    "חלבון: " + meal.getProtein() +
                            " גרם | קלוריות: " + meal.getCalories()
            );

            Bitmap bitmap = decodeBase64(meal.getBase64Image());
            if (bitmap != null) holder.imgMealItem.setImageBitmap(bitmap);

            holder.layoutComponents.removeAllViews();
            if (meal.getComponents() != null) {
                for (Map.Entry<String, Double> entry : meal.getComponents().entrySet()) {
                    TextView tv = new TextView(holder.itemView.getContext());
                    tv.setText(entry.getKey() + ": " + entry.getValue() + " גרם");
                    holder.layoutComponents.addView(tv);
                }
            }

            // ---------- Favorite ----------
            holder.imgFavorite.setAlpha(meal.isFavorite() ? 1f : 0.3f);
            holder.imgFavorite.setOnClickListener(v -> {
                boolean newFav = !meal.isFavorite();
                meal.setFavorite(newFav);
                holder.imgFavorite.setAlpha(newFav ? 1f : 0.3f);

                // שמירה ב-Firestore
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null && meal.getId() != null) {
                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(uid)
                            .collection("Meals")
                            .document(meal.getId())
                            .update("favorite", newFav);
                }
            });
        }

        @Override
        public int getItemCount() {
            return meals.size();
        }

        private Bitmap decodeBase64(String base64) {
            if (base64 == null) return null;
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        static class MealViewHolder extends RecyclerView.ViewHolder {
            ImageView imgMealItem, imgFavorite;
            TextView tvMealName, tvMealTotals;
            LinearLayout layoutComponents;

            public MealViewHolder(@NonNull View itemView) {
                super(itemView);
                imgMealItem = itemView.findViewById(R.id.imgMealItem);
                imgFavorite = itemView.findViewById(R.id.imgFavorite);
                tvMealName = itemView.findViewById(R.id.tvMealName);
                tvMealTotals = itemView.findViewById(R.id.tvMealInfo);
                layoutComponents = itemView.findViewById(R.id.layoutComponents);
            }
        }
    }
}
