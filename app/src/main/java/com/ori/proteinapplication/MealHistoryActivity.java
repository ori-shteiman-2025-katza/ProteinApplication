package com.ori.proteinapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MealHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerMeals;
    private MealAdapter adapter;
    private List<Meal> mealList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);

        // בדיקה שהמשתמש מחובר
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerMeals = findViewById(R.id.recyclerMeals);
        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));

        mealList = new ArrayList<>();
        adapter = new MealAdapter(mealList);
        recyclerMeals.setAdapter(adapter);

        loadMealsFromFirestore();
    }

    private void loadMealsFromFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .document(uid)
                .collection("Meals")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mealList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;

                        String base64Image = (String) data.get("image");

                        Number proteinNum = (Number) data.get("protein");
                        Number caloriesNum = (Number) data.get("calories");

                        int protein = proteinNum != null ? proteinNum.intValue() : 0;
                        int calories = caloriesNum != null ? caloriesNum.intValue() : 0;

                        Map<String, Double> components = (Map<String, Double>) data.get("components");

                        mealList.add(new Meal(base64Image, protein, calories, components));
                    }

                    if (mealList.isEmpty()) {
                        Toast.makeText(this, "אין ארוחות עדיין", Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת הארוחות", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    // Adapter
    static class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

        private List<Meal> meals;

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
            holder.tvMealTotals.setText("חלבון: " + meal.getProtein() + " גרם | קלוריות: " + meal.getCalories());

            // הצגת התמונה
            Bitmap bitmap = decodeBase64(meal.getBase64Image());
            if (bitmap != null) {
                holder.imgMealItem.setImageBitmap(bitmap);
            }

            // הצגת רכיבים + משקל לכל רכיב
            holder.layoutComponents.removeAllViews();
            if (meal.getComponents() != null) {
                for (Map.Entry<String, Double> entry : meal.getComponents().entrySet()) {
                    TextView tv = new TextView(holder.itemView.getContext());
                    tv.setText(entry.getKey() + ": " + entry.getValue() + " גרם");
                    holder.layoutComponents.addView(tv);
                }
            }
        }

        @Override
        public int getItemCount() {
            return meals.size();
        }

        private Bitmap decodeBase64(String base64) {
            if (base64 == null) return null;
            try {
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        static class MealViewHolder extends RecyclerView.ViewHolder {
            ImageView imgMealItem;
            TextView tvMealName, tvMealTotals;
            LinearLayout layoutComponents;

            public MealViewHolder(@NonNull View itemView) {
                super(itemView);
                imgMealItem = itemView.findViewById(R.id.imgMealItem);
                tvMealName = itemView.findViewById(R.id.tvMealName);
                tvMealTotals = itemView.findViewById(R.id.tvMealInfo);
                layoutComponents = itemView.findViewById(R.id.layoutComponents);
            }
        }
    }

    // Meal model
    static class Meal {
        private String base64Image;
        private int protein;
        private int calories;
        private Map<String, Double> components; // רכיב -> משקל בגרם

        public Meal(String base64Image, int protein, int calories, Map<String, Double> components) {
            this.base64Image = base64Image;
            this.protein = protein;
            this.calories = calories;
            this.components = components;
        }

        public String getBase64Image() {
            return base64Image;
        }

        public int getProtein() {
            return protein;
        }

        public int getCalories() {
            return calories;
        }

        public Map<String, Double> getComponents() {
            return components;
        }
    }
}
