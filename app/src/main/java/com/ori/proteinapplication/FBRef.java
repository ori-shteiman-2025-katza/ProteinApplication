package com.ori.proteinapplication;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;

public class FBRef {
    // מאתחל את המופע של FirebaseAuth, אחראי לניהול האימות של המשתמשים (sign-in, sign-out וכו')
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // מאתחל את המופע של FirebaseDatabase, מאפשר גישה לנתוני Firebase Realtime Database
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();

    //
    public static DatabaseReference refUsers = FirebaseDatabase.getInstance().getReference("Users");

    // מפנה ל-reference של "addItem" ב- Firebase Database. כאן יאוחסנו פריטים שנוספים למערכת (אפשר לשנות את השם בהתאם לצורך).

    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static CollectionReference refImages = FBFS.collection("Images");
    //public static FirebaseStorage FBStorage = FirebaseStorage.getInstance();
    //public static StorageReference refMealImages = FBStorage.getReference("meal_images");
}

