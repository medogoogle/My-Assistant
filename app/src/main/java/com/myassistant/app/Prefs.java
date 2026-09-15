package com.myassistant.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;

public class Prefs {
    private static final String P="assistant_prefs";
    private final SharedPreferences sp;
    public Prefs(Context c){ sp=c.getSharedPreferences(P,Context.MODE_PRIVATE); }

    public boolean isDark(){ return sp.getBoolean("dark",true); }
    public void setDark(boolean v){ sp.edit().putBoolean("dark",v).apply(); }

    public int getAccent(){ return sp.getInt("accent",0xFF38BDF8); }
    public void setAccent(int c){ sp.edit().putInt("accent",c).apply(); }

    public boolean hasPin(){ return sp.contains("pin"); }
    public void setPin(String pin){ sp.edit().putString("pin",hash(pin)).apply(); }
    public void clearPin(){ sp.edit().remove("pin").putBoolean("biometric",false).apply(); }
    public boolean checkPin(String pin){ String h=sp.getString("pin",null); return h!=null && h.equals(hash(pin)); }

    public boolean isBiometricEnabled(){ return sp.getBoolean("biometric",false); }
    public void setBiometricEnabled(boolean v){ sp.edit().putBoolean("biometric",v).apply(); }

    private static String hash(String s){
        try{
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] d=md.digest(s.getBytes("UTF-8"));
            StringBuilder sb=new StringBuilder();
            for(byte b:d) sb.append(String.format("%02x",b));
            return sb.toString();
        }catch(Exception e){ return String.valueOf(s.hashCode()); }
    }
}
