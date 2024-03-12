package com.capstonegroupproject.speechtotext;

import java.util.Calendar;

public class WishMeFunction {
    static String wishMe() {
        String s = " ";
        Calendar c = Calendar.getInstance();
        int time = c.get(Calendar.HOUR_OF_DAY);
        if(time >=6 && time < 12){
            s = "Good Morning";
        }
        else if(time >=12 && time < 17){
            s = "Good Afternoon";
        }
        else if(time >=17 && time < 22){
            s= "Good Evening";
        }
        else{
            s ="Good Night";
        }
        return s;
    }

}
