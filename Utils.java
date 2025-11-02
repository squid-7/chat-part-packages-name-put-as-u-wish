//my part in utils.java
package com.yourpackagename;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.Arrays;

public class Utils {
    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String MESSAGE_TYPE_IMAGE = "IMAGE";

    // Generates a sorted chat path identifier for the two users
    public static String chatPath(String uid1, String uid2) {
        String[] arr = {uid1, uid2};
        Arrays.sort(arr);
        return arr[0] + "_" + arr[1];
    }

    public static long getTimestamp() {
        return System.currentTimeMillis();
    }

    public static String formatTimestampDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);
        return DateFormat.format("dd/MM/yyyy", calendar).toString();
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
