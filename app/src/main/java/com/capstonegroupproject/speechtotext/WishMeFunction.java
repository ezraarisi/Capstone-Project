package com.capstonegroupproject.speechtotext;

import java.util.Calendar;

public class WishMeFunction {
    static String wishMe() {
        String s = "";
        Calendar c = Calendar.getInstance();
        int time = c.get(Calendar.HOUR_OF_DAY);
        if (time >= 6 && time < 12) {
            s = "Good Morning";
        } else if (time >= 12 && time < 17) {
            s = "Good Afternoon";
        } else if (time >= 17 && time < 22) {
            s = "Good Evening";
        } else {
            s = "Good Night";
        }
        return s;
    }

    // Returns the current hour (0-23)
    public static int getCurrentHour() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY);
    }

    // Returns time period of day (Morning/Afternoon/Evening/Night)
    public static String getTimePeriod() {
        int time = getCurrentHour();
        if (time >= 6 && time < 12) {
            return "Morning";
        } else if (time >= 12 && time < 17) {
            return "Afternoon";
        } else if (time >= 17 && time < 22) {
            return "Evening";
        } else {
            return "Night";
        }
    }

    // Returns a custom greeting with the user's name
    public static String personalizedGreeting(String name) {
        return wishMe() + ", " + name + "!";
    }
}
