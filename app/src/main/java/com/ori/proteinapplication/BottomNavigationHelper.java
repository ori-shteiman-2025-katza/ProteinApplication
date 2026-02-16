package com.ori.proteinapplication;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class BottomNavigationHelper {

    public static void setupBottomNavigation(Activity activity,
                                             BottomNavigationView bottomNavigationView,
                                             int selectedItemId) {

        bottomNavigationView.setSelectedItemId(selectedItemId);

        bottomNavigationView.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        int id = item.getItemId();

                        if (id == R.id.nav_history) {

                            if (!(activity instanceof MealHistoryActivity)) {
                                activity.startActivity(
                                        new Intent(activity, MealHistoryActivity.class));
                                activity.overridePendingTransition(0, 0);
                                activity.finish();
                            }
                            return true;

                        } else if (id == R.id.nav_profile) {

                            if (!(activity instanceof EditInfoActivity)) {
                                activity.startActivity(
                                        new Intent(activity, EditInfoActivity.class));
                                activity.overridePendingTransition(0, 0);
                                activity.finish();
                            }
                            return true;

                        } else if (id == R.id.nav_main) {

                            if (!(activity instanceof MainDashboardActivity)) {
                                activity.startActivity(
                                        new Intent(activity, MainDashboardActivity.class));
                                activity.overridePendingTransition(0, 0);
                                activity.finish();
                            }
                            return true;
                        }

                        return false;
                    }
                });
    }
}

