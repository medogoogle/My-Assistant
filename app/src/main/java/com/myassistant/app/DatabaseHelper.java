package com.myassistant.app;

import android.content.*;import android.database.Cursor;import android.database.sqlite.*;import java.text.*;import java.time.LocalDate;import java.time.temporal.ChronoUnit;import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB="assistant.db"; private static final int V=4;
    public DatabaseHelper(Context c){super(c,DB,null,V);}
    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,type TEXT, balance REAL DEFAULT 0, currency TEXT DEFAULT 'جنيه سوداني')");
        db.execSQL("CREATE TABLE debts(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,kind TEXT,amount REAL DEFAULT 0,paid REAL DEFAULT 0,due TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE trades(id INTEGER PRIMARY KEY AUTOINCREMENT,broker TEXT,symbol TEXT,side TEXT,entry REAL,exit REAL,sl REAL,tp REAL,lots REAL,profit REAL,status TEXT,opened TEXT,closed TEXT,strategy TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,due TEXT,done INTEGER DEFAULT 0,priority TEXT)");
        db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,account TEXT,kind TEXT,category TEXT,amount REAL,date TEXT,note TEXT)");
        db.execSQL("CREATE TABLE investments(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,type TEXT,quantity REAL DEFAULT 0,buy_price REAL DEFAULT 0,current_price REAL DEFAULT 0,buy_date TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE recurring_transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,account TEXT,kind TEXT,category TEXT,amount REAL DEFAULT 0,day_of_month INTEGER DEFAULT 1,note TEXT,last_generated TEXT)");
    }
    public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        db.execSQL("CREATE TABLE IF NOT EXISTS investments(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,type TEXT,quantity REAL DEFAULT 0,buy_price REAL DEFAULT 0,current_price REAL DEFAULT 0,buy_date TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS recurring_transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,account TEXT,kind TEXT,category TEXT,amount REAL DEFAULT 0,day_of_month INTEGER DEFAULT 1,note TEXT,last_generated TEXT)");
        try{ db.execSQL("ALTER TABLE accounts ADD COLUMN currency TEXT DEFAULT 'جنيه سوداني'"); }catch(Exception ignored){}
    }

    public static class Row{ public long id; public String text; public Row(long id,String text){this.id=id;this.text=text;} }

    // ---- create ----
    public long addAccount(String n,String t,double b,String currency){ContentValues v=new ContentValues();v.put("name",n);v.put("type",t);v.put("balance",b);v.put("currency",currency);return getWritableDatabase().insert("accounts",null,v);}
    public long addDebt(String n,String k,double a,double p,String due,String notes){ContentValues v=new ContentValues();v.put("name",n);v.put("kind",k);v.put("amount",a);v.put("paid",p);v.put("due",due);v.put("notes",notes);return getWritableDatabase().insert("debts",null,v);}
    public long addTrade(String broker,String symbol,String side,double entry,double exit,double sl,double tp,double lots,double profit,String status,String opened,String closed,String strategy,String notes){ContentValues v=new ContentValues();v.put("broker",broker);v.put("symbol",symbol);v.put("side",side);v.put("entry",entry);v.put("exit",exit);v.put("sl",sl);v.put("tp",tp);v.put("lots",lots);v.put("profit",profit);v.put("status",status);v.put("opened",opened);v.put("closed",closed);v.put("strategy",strategy);v.put("notes",notes);return getWritableDatabase().insert("trades",null,v);}
    public long addTask(String title,String due,String priority){ContentValues v=new ContentValues();v.put("title",title);v.put("due",due);v.put("priority",priority);return getWritableDatabase().insert("tasks",null,v);}
    public long addInvestment(String n,String type,double qty,double buyPrice,double curPrice,String buyDate,String notes){ContentValues v=new ContentValues();v.put("name",n);v.put("type",type);v.put("quantity",qty);v.put("buy_price",buyPrice);v.put("current_price",curPrice);v.put("buy_date",buyDate);v.put("notes",notes);return getWritableDatabase().insert("investments",null,v);}
    public long addTransaction(String account,String kind,String category,double amount,String date,String note){ContentValues v=new ContentValues();v.put("account",account);v.put("kind",kind);v.put("category",category);v.put("amount",amount);v.put("date",date);v.put("note",note);return getWritableDatabase().insert("transactions",null,v);}
    public long addRecurring(String account,String kind,String category,double amount,int dayOfMonth,String note){
        ContentValues v=new ContentValues();v.put("account",account);v.put("kind",kind);v.put("category",category);v.put("amount",amount);v.put("day_of_month",dayOfMonth);v.put("note",note);
        v.put("last_generated",new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date()));
        return getWritableDatabase().insert("recurring_transactions",null,v);
    }

    // ---- update ----
    public void updateAccount(long id,String n,String t,double b,String currency){ContentValues v=new ContentValues();v.put("name",n);v.put("type",t);v.put("balance",b);v.put("currency",currency);getWritableDatabase().update("accounts",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateDebt(long id,String n,String k,double a,double p,String due,String notes){ContentValues v=new ContentValues();v.put("name",n);v.put("kind",k);v.put("amount",a);v.put("paid",p);v.put("due",due);v.put("notes",notes);getWritableDatabase().update("debts",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateTrade(long id,String broker,String symbol,String side,double entry,double exit,double sl,double tp,double lots,double profit,String status,String opened,String closed,String strategy,String notes){ContentValues v=new ContentValues();v.put("broker",broker);v.put("symbol",symbol);v.put("side",side);v.put("entry",entry);v.put("exit",exit);v.put("sl",sl);v.put("tp",tp);v.put("lots",lots);v.put("profit",profit);v.put("status",status);v.put("opened",opened);v.put("closed",closed);v.put("strategy",strategy);v.put("notes",notes);getWritableDatabase().update("trades",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateTask(long id,String title,String due,String priority,int done){ContentValues v=new ContentValues();v.put("title",title);v.put("due",due);v.put("priority",priority);v.put("done",done);getWritableDatabase().update("tasks",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateInvestment(long id,String n,String type,double qty,double buyPrice,double curPrice,String buyDate,String notes){ContentValues v=new ContentValues();v.put("name",n);v.put("type",type);v.put("quantity",qty);v.put("buy_price",buyPrice);v.put("current_price",curPrice);v.put("buy_date",buyDate);v.put("notes",notes);getWritableDatabase().update("investments",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateTransaction(long id,String account,String kind,String category,double amount,String date,String note){ContentValues v=new ContentValues();v.put("account",account);v.put("kind",kind);v.put("category",category);v.put("amount",amount);v.put("date",date);v.put("note",note);getWritableDatabase().update("transactions",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateRecurring(long id,String account,String kind,String category,double amount,int dayOfMonth,String note){ContentValues v=new ContentValues();v.put("account",account);v.put("kind",kind);v.put("category",category);v.put("amount",amount);v.put("day_of_month",dayOfMonth);v.put("note",note);getWritableDatabase().update("recurring_transactions",v,"id=?",new String[]{String.valueOf(id)});}
    public int generateDueRecurring(){
        String ym=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
        List<Object[]> toProcess=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,account,kind,category,amount,note,last_generated FROM recurring_transactions",null);
        while(c.moveToNext()){
            String last=c.getString(6);
            if(last==null || !last.equals(ym)) toProcess.add(new Object[]{c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getDouble(4),c.getString(5)});
        }
        c.close();
        int count=0;
        for(Object[] r:toProcess){
            long id=(Long)r[0];
            addTransaction((String)r[1],(String)r[2],(String)r[3],(Double)r[4],new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date()),(String)r[5]);
            ContentValues v=new ContentValues(); v.put("last_generated",ym);
            getWritableDatabase().update("recurring_transactions",v,"id=?",new String[]{String.valueOf(id)});
            count++;
        }
        return count;
    }

    // ---- delete / read one ----
    public void deleteRow(String table,long id){getWritableDatabase().delete(table,"id=?",new String[]{String.valueOf(id)});}
    public Cursor getRow(String table,long id,String[] cols){return getReadableDatabase().query(table,cols,"id=?",new String[]{String.valueOf(id)},null,null,null);}

    // ---- lists with search ----
    public List<Row> listRows(String table,String[] cols,String orderBy,String search,String searchCol){
        List<Row> out=new ArrayList<>();
        String where=null; String[] args=null;
        if(search!=null && !search.trim().isEmpty() && searchCol!=null){ where=searchCol+" LIKE ?"; args=new String[]{"%"+search.trim()+"%"}; }
        String[] allCols=new String[cols.length+1]; allCols[0]="id"; System.arraycopy(cols,0,allCols,1,cols.length);
        Cursor c=getReadableDatabase().query(table,allCols,where,args,null,null,orderBy);
        while(c.moveToNext()){
            long id=c.getLong(0);
            StringBuilder s=new StringBuilder();
            for(int i=0;i<cols.length;i++){ if(i>0)s.append(" • "); s.append(cols[i]).append(": ").append(c.getString(i+1)); }
            out.add(new Row(id,s.toString()));
        }
        c.close();
        return out;
    }

    // ---- aggregates ----
    public double sum(String table,String col){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM("+col+"),0) FROM "+table,null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public double investmentsCurrentValue(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(quantity*current_price),0) FROM investments",null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public double investmentsCost(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(quantity*buy_price),0) FROM investments",null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public int count(String table,String where){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table+(where==null?"":" WHERE "+where),null);int x=0;if(c.moveToFirst())x=c.getInt(0);c.close();return x;}

    // ---- trading stats ----
    public double winRate(){int total=count("trades",null);if(total==0)return 0;int wins=count("trades","profit>0");return 100.0*wins/total;}
    public double avgWin(){Cursor c=getReadableDatabase().rawQuery("SELECT AVG(profit) FROM trades WHERE profit>0",null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public double avgLoss(){Cursor c=getReadableDatabase().rawQuery("SELECT AVG(profit) FROM trades WHERE profit<0",null);double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public List<Double> tradeProfitsChrono(){List<Double> out=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT profit FROM trades ORDER BY closed ASC, id ASC",null);while(c.moveToNext())out.add(c.getDouble(0));c.close();return out;}

    // ---- per-currency balances ----
    public LinkedHashMap<String,Double> balanceByCurrency(){
        LinkedHashMap<String,Double> m=new LinkedHashMap<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(currency,'جنيه سوداني') as cur, SUM(balance) FROM accounts GROUP BY cur ORDER BY cur ASC",null);
        while(c.moveToNext()){ String cur=c.getString(0); if(cur==null||cur.trim().isEmpty())cur="جنيه سوداني"; m.put(cur,c.getDouble(1)); }
        c.close();
        return m;
    }

    // ---- investment allocation ----
    public LinkedHashMap<String,Double> investmentAllocation(){
        LinkedHashMap<String,Double> m=new LinkedHashMap<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT type, SUM(quantity*current_price) FROM investments GROUP BY type ORDER BY type ASC",null);
        while(c.moveToNext()){ String t=c.getString(0); if(t==null||t.trim().isEmpty())t="أخرى"; double v=c.getDouble(1); if(v>0) m.put(t,v); }
        c.close();
        return m;
    }

    // ---- monthly income/expense (last N months, oldest -> newest) ----
    public LinkedHashMap<String,double[]> monthlyIncomeExpense(int months){
        LinkedHashMap<String,double[]> out=new LinkedHashMap<>();
        SimpleDateFormat ym=new SimpleDateFormat("yyyy-MM",Locale.US);
        Calendar tmp=Calendar.getInstance(); tmp.add(Calendar.MONTH,-(months-1));
        for(int i=0;i<months;i++){ out.put(ym.format(tmp.getTime()),new double[]{0,0}); tmp.add(Calendar.MONTH,1); }
        Cursor c=getReadableDatabase().rawQuery("SELECT substr(date,1,7) as m, kind, SUM(amount) FROM transactions GROUP BY m, kind",null);
        while(c.moveToNext()){
            String m=c.getString(0); String kind=c.getString(1); double amt=c.getDouble(2);
            double[] arr=out.get(m);
            if(arr!=null){ if(kind!=null && kind.contains("دخل")) arr[0]+=amt; else arr[1]+=amt; }
        }
        c.close();
        return out;
    }

    // ---- monthly report ----
    public double monthIncome(String ym){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE substr(date,1,7)=? AND kind LIKE '%دخل%'",new String[]{ym});double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public double monthExpense(String ym){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE substr(date,1,7)=? AND (kind IS NULL OR kind NOT LIKE '%دخل%')",new String[]{ym});double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}
    public LinkedHashMap<String,Double> monthCategoryBreakdown(String ym){
        LinkedHashMap<String,Double> m=new LinkedHashMap<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT category, SUM(amount) FROM transactions WHERE substr(date,1,7)=? GROUP BY category ORDER BY category ASC",new String[]{ym});
        while(c.moveToNext()){ String cat=c.getString(0); if(cat==null||cat.trim().isEmpty())cat="غير مصنف"; m.put(cat,c.getDouble(1)); }
        c.close();
        return m;
    }
    public int monthTradeCount(String ym){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM trades WHERE substr(closed,1,7)=?",new String[]{ym});int x=0;if(c.moveToFirst())x=c.getInt(0);c.close();return x;}
    public double monthTradeProfit(String ym){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(profit),0) FROM trades WHERE substr(closed,1,7)=?",new String[]{ym});double x=0;if(c.moveToFirst())x=c.getDouble(0);c.close();return x;}

    // ---- upcoming alerts ----
    public List<Row> upcomingDebts(int days){
        List<Row> out=new ArrayList<>();
        Cursor c=getReadableDatabase().query("debts",new String[]{"id","name","kind","amount","due"},null,null,null,null,"due ASC");
        LocalDate today=LocalDate.now();
        while(c.moveToNext()){
            String due=c.getString(4);
            if(due==null||due.trim().isEmpty())continue;
            try{
                LocalDate d=LocalDate.parse(due.trim().length()>=10?due.trim().substring(0,10):due.trim());
                long diff=ChronoUnit.DAYS.between(today,d);
                if(diff<=days){ out.add(new Row(c.getLong(0), c.getString(1)+" • "+c.getString(2)+" • "+c.getString(3)+" • يستحق: "+due)); }
            }catch(Exception ignored){}
        }
        c.close();
        return out;
    }
    public List<Row> upcomingTasks(int days){
        List<Row> out=new ArrayList<>();
        Cursor c=getReadableDatabase().query("tasks",new String[]{"id","title","due","priority"},"done=0",null,null,null,"due ASC");
        LocalDate today=LocalDate.now();
        while(c.moveToNext()){
            String due=c.getString(2);
            if(due==null||due.trim().isEmpty())continue;
            try{
                LocalDate d=LocalDate.parse(due.trim().length()>=10?due.trim().substring(0,10):due.trim());
                long diff=ChronoUnit.DAYS.between(today,d);
                if(diff<=days){ out.add(new Row(c.getLong(0), c.getString(1)+" • "+c.getString(3)+" • موعدها: "+due)); }
            }catch(Exception ignored){}
        }
        c.close();
        return out;
    }
}
