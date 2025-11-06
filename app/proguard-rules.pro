# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# Keep Location Services
-keep class com.google.android.gms.location.** { *; }

# Keep Geocoder
-keep class android.location.Address { *; }
-keep class android.location.Geocoder { *; }

# Keep Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep your Location entity
-keep class com.example.sleepytrip.Location { *; }
-keep class com.example.sleepytrip.LocationDao { *; }
-keep class com.example.sleepytrip.AppDatabase { *; }

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**