package com.myassistant.app;

import android.app.*;import android.os.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.content.*;import android.net.Uri;import android.database.Cursor;import android.text.Editable;import android.text.InputType;import android.text.TextWatcher;import android.view.*;import android.view.inputmethod.EditorInfo;import android.view.inputmethod.InputMethodManager;import android.widget.*;import androidx.biometric.BiometricManager;import androidx.biometric.BiometricPrompt;import androidx.core.content.ContextCompat;import androidx.fragment.app.FragmentActivity;import java.text.*;import java.util.*;

public class MainActivity extends FragmentActivity {
    DatabaseHelper db; Prefs prefs; LinearLayout content,drawerPanel; ScrollView sc; TextView title; View scrim; String currentTab="لوحة اليوم"; boolean drawerOpen=false; int drawerWidthPx;
    List<View> allNavRows=new ArrayList<>();
    Map<String,View> sectionAnchors=new HashMap<>();
    int white,muted,card,bg,accent,green,red,amber;
    static final int REQ_EXPORT=1001, REQ_IMPORT=1002;

    static class SubItem{ String label; Runnable action; SubItem(String l,Runnable a){label=l;action=a;} }

    static final String[] ACCOUNT_TYPES={"بنك","كاش","محفظة إلكترونية","حساب تداول","أخرى"};
    static final String[] TX_KINDS={"دخل","مصروف"};
    static final String[] TX_CATEGORIES={"راتب","مبيعات","إيجار","فواتير","طعام وشراب","مواصلات","تسوق","صحة","ترفيه","تحويل بين حسابات","أخرى"};
    static final String[] DEBT_KINDS={"عليّ","لي"};
    static final String[] BROKERS={"Exness","JustMarkets","أخرى"};
    static final String[] TRADE_SIDES={"BUY","SELL"};
    static final String[] INVESTMENT_TYPES={"ذهب فعلي","أسهم","عملات رقمية","عقار","أخرى"};
    static final String[] PRIORITIES={"عالية","متوسطة","منخفضة"};
    static final String[] CURRENCIES={"جنيه سوداني","دولار أمريكي","يورو","ريال سعودي","درهم إماراتي","جنيه مصري","أخرى"};

    public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=new Prefs(this);
        setTheme(prefs.isDark()?R.style.AppTheme:R.style.AppTheme_Light);
        db=new DatabaseHelper(this);
        applyPalette();
        boolean dark=prefs.isDark();
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if(prefs.hasPin()) showLockScreen(); else { build(); showHome(); checkRecurring(); handleShortcutIntent(getIntent()); }
    }
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        if(content!=null) handleShortcutIntent(intent);
    }
    void handleShortcutIntent(Intent intent){
        if(intent==null) return;
        String action=intent.getStringExtra("shortcut_action");
        if(action==null) return;
        intent.removeExtra("shortcut_action");
        if(action.equals("trade")){ navigateTo("📈 التداول"); tradeDialog(); }
        else if(action.equals("transaction")){ navigateTo("المال"); transactionDialog(); }
        else if(action.equals("debt")){ navigateTo("الديون"); debtDialog(); }
        else if(action.equals("task")){ navigateTo("المهام"); taskDialog(); }
    }
    void checkRecurring(){
        int n=db.generateDueRecurring();
        if(n>0) showMsg("معاملات متكررة","تمت إضافة "+n+" معاملة متكررة تلقائيًا لهذا الشهر.");
    }

    void applyPalette(){
        boolean dark=prefs.isDark();
        if(dark){ bg=Color.rgb(15,23,42); card=Color.rgb(30,41,59); white=Color.rgb(248,250,252); muted=Color.rgb(148,163,184); }
        else { bg=Color.rgb(241,245,249); card=Color.WHITE; white=Color.rgb(15,23,42); muted=Color.rgb(100,116,139); }
        accent=prefs.getAccent(); green=Color.rgb(34,197,94); red=Color.rgb(239,68,68); amber=Color.rgb(245,158,11);
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable rounded(int color,int radiusDp){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radiusDp));return d;}
    TextView tv(String s,float size){TextView t=new TextView(this);t.setText(s);t.setTextColor(white);t.setTextSize(size);t.setPadding(dp(18),dp(12),dp(18),dp(12));return t;}
    TextView cardText(String s){TextView t=tv(s,16);t.setBackground(rounded(card,12));t.setPadding(dp(20),dp(20),dp(20),dp(20));return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(white);b.setAllCaps(false);b.setBackground(rounded(card,10));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));return b;}
    Button primaryBtn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(rounded(accent,10));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));return b;}
    EditText e(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(muted);e.setTextColor(white);e.setSingleLine(true);e.setImeOptions(EditorInfo.IME_ACTION_NEXT);e.setMinHeight(dp(44));return e;}
    EditText eNotes(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(muted);e.setTextColor(white);e.setSingleLine(false);e.setMinLines(2);e.setImeOptions(EditorInfo.IME_ACTION_DONE);e.setMinHeight(dp(44));return e;}
    EditText eNum(String h){
        EditText ed=e(h);
        ed.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        addThousandsFormatting(ed);
        return ed;
    }
    EditText eInt(String h){ EditText ed=e(h); ed.setInputType(InputType.TYPE_CLASS_NUMBER); return ed; }
    void addThousandsFormatting(final EditText ed){
        ed.addTextChangedListener(new TextWatcher(){
            boolean editing=false;
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){}
            public void afterTextChanged(Editable s){
                if(editing) return;
                editing=true;
                String raw=s.toString().replace(",","");
                String result=raw;
                if(!(raw.isEmpty()||raw.equals(".")||raw.equals("-")||raw.equals("-."))){
                    boolean neg=raw.startsWith("-");
                    String body=neg?raw.substring(1):raw;
                    String intp,frac;
                    int dot=body.indexOf('.');
                    if(dot>=0){ intp=body.substring(0,dot); frac=body.substring(dot); } else { intp=body; frac=""; }
                    if(!intp.isEmpty()){
                        StringBuilder rev=new StringBuilder(intp).reverse();
                        StringBuilder out=new StringBuilder();
                        for(int i=0;i<rev.length();i++){ out.append(rev.charAt(i)); if((i+1)%3==0 && i!=rev.length()-1) out.append(','); }
                        intp=out.reverse().toString();
                    }
                    result=(neg?"-":"")+intp+frac;
                }
                ed.setText(result);
                ed.setSelection(Math.min(result.length(),ed.getText().length()));
                editing=false;
            }
        });
    }
    EditText dateField(String hint,String initial){
        final EditText ed=e(hint+" 📅");
        ed.setFocusable(false); ed.setFocusableInTouchMode(false); ed.setClickable(true);
        ed.setInputType(InputType.TYPE_NULL);
        if(initial!=null && !initial.trim().isEmpty()) ed.setText(initial);
        ed.setOnClickListener(v->openDatePicker(ed));
        return ed;
    }
    void openDatePicker(EditText target){
        Calendar cal=Calendar.getInstance();
        String cur=target.getText().toString().trim();
        try{
            if(cur.length()>=10){
                String[] parts=cur.substring(0,10).split("-");
                cal.set(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,Integer.parseInt(parts[2]));
            }
        }catch(Exception ignored){}
        new DatePickerDialog(this,(view,y,m,d)->target.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    Spinner spinner(String[] options,String selected){
        List<String> opts=new ArrayList<>(Arrays.asList(options));
        if(selected!=null && !selected.trim().isEmpty() && !opts.contains(selected)) opts.add(0,selected);
        Spinner s=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,opts);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(ad);
        int idx=selected!=null?opts.indexOf(selected):0;
        if(idx>=0) s.setSelection(idx);
        s.setMinimumHeight(dp(44));
        return s;
    }
    EditText searchBox(String h){
        final EditText s=e(h);
        s.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        s.setOnEditorActionListener((tvv,actionId,ev)->{ hideKeyboard(tvv); return true; });
        return s;
    }
    void hideKeyboard(View anyView){
        InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm!=null && anyView!=null) imm.hideSoftInputFromWindow(anyView.getWindowToken(),0);
    }
    LinearLayout box(View... vs){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),dp(8),dp(20),dp(8));
        l.setOnTouchListener((v,ev)->{ hideKeyboard(l); return false; });
        for(int i=0;i<vs.length;i++){
            View v=vs[i];
            v.setMinimumHeight(dp(44));
            if(v instanceof EditText){ EditText ed=(EditText)v; if(ed.isSingleLine() && i==vs.length-1) ed.setImeOptions(EditorInfo.IME_ACTION_DONE); }
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(4),0,dp(4));
            l.addView(v,lp);
        }
        return l;
    }

    View add(View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));content.addView(v,lp);return v;}
    View add(View v,int heightDp){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(heightDp));lp.setMargins(0,0,0,dp(10));content.addView(v,lp);return v;}
    void addTo(LinearLayout container,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));container.addView(v,lp);}
    TextView clickableRow(String text,Runnable onClick){TextView t=cardText(text+"   ✏️");t.setOnClickListener(v->onClick.run());return t;}
    TextWatcher simpleWatcher(final java.util.function.Consumer<String> onChange){
        return new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){}
            public void afterTextChanged(Editable s){ onChange.accept(s.toString()); }
        };
    }
    void confirmDelete(String table,long id,Runnable after){
        new AlertDialog.Builder(this).setTitle("تأكيد الحذف").setMessage("هل أنت متأكد من الحذف؟ لا يمكن التراجع عن هذا.")
            .setPositiveButton("حذف",(d,w)->{ db.deleteRow(table,id); if(after!=null) after.run(); })
            .setNegativeButton("إلغاء",null).show();
    }

    void showLockScreen(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER); l.setBackgroundColor(bg); l.setPadding(dp(30),dp(30),dp(30),dp(30));
        TextView t=tv("🔒 مساعدي",26); t.setGravity(Gravity.CENTER); l.addView(t);
        TextView sub=tv("أدخل رمز الدخول",15); sub.setTextColor(muted); sub.setGravity(Gravity.CENTER); l.addView(sub);
        final EditText pin=new EditText(this); pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD); pin.setGravity(Gravity.CENTER); pin.setTextColor(white); pin.setHintTextColor(muted); pin.setHint("••••"); pin.setTextSize(24);
        LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(dp(180),-2); plp.topMargin=dp(24); plp.bottomMargin=dp(16); l.addView(pin,plp);
        final TextView err=tv("",13); err.setTextColor(red); err.setGravity(Gravity.CENTER); l.addView(err);
        Button ok=primaryBtn("دخول"); LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(dp(180),dp(48)); blp.topMargin=dp(16); l.addView(ok,blp);
        Runnable unlocked=()->{ build(); showHome(); checkRecurring(); handleShortcutIntent(getIntent()); };
        ok.setOnClickListener(v->{
            if(prefs.checkPin(pin.getText().toString().trim())){ unlocked.run(); }
            else { err.setText("رمز غير صحيح، حاول مرة أخرى"); pin.setText(""); }
        });
        if(prefs.isBiometricEnabled() && biometricAvailable()){
            Button bio=btn("🔓 الدخول بالبصمة"); LinearLayout.LayoutParams blp2=new LinearLayout.LayoutParams(dp(180),dp(48)); blp2.topMargin=dp(12); l.addView(bio,blp2);
            bio.setOnClickListener(v->showBiometricPrompt(unlocked));
        }
        setContentView(l);
        if(prefs.isBiometricEnabled() && biometricAvailable()) showBiometricPrompt(unlocked);
    }

    boolean biometricAvailable(){
        try{
            BiometricManager bm=BiometricManager.from(this);
            return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)==BiometricManager.BIOMETRIC_SUCCESS;
        }catch(Exception e){ return false; }
    }
    void showBiometricPrompt(Runnable onSuccess){
        try{
            BiometricPrompt.PromptInfo promptInfo=new BiometricPrompt.PromptInfo.Builder()
                .setTitle("مساعدي")
                .setSubtitle("تحقق من هويتك للدخول")
                .setNegativeButtonText("استخدام الرمز")
                .build();
            BiometricPrompt prompt=new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback(){
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){ super.onAuthenticationSucceeded(result); runOnUiThread(onSuccess); }
                @Override public void onAuthenticationError(int errorCode, CharSequence errString){ super.onAuthenticationError(errorCode,errString); /* المستخدم ألغى أو حدث خطأ — يبقى في شاشة الرمز بدون رسالة */ }
                @Override public void onAuthenticationFailed(){ super.onAuthenticationFailed(); /* محاولة فاشلة واحدة — يترك النظام يعيد المحاولة */ }
            });
            prompt.authenticate(promptInfo);
        }catch(Exception ignored){}
    }

    void build(){
        FrameLayout rootFrame=new FrameLayout(this);
        setContentView(rootFrame);

        LinearLayout mainLayout=new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(bg);

        LinearLayout titleBar=new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setBackgroundColor(card);
        titleBar.setPadding(dp(8),0,dp(8),0);
        titleBar.setElevation(dp(4));
        title=tv("مساعدي",22);
        title.setGravity(Gravity.RIGHT);
        titleBar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button menuBtn=new Button(this);
        menuBtn.setText("☰");
        menuBtn.setTextColor(white);
        menuBtn.setTextSize(20);
        menuBtn.setAllCaps(false);
        menuBtn.setBackground(rounded(bg,10));
        menuBtn.setOnClickListener(v->toggleDrawer());
        titleBar.addView(menuBtn,new LinearLayout.LayoutParams(dp(52),dp(52)));
        mainLayout.addView(titleBar,new LinearLayout.LayoutParams(-1,dp(56)));

        sc=new ScrollView(this);
        mainLayout.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10),dp(10),dp(10),dp(10));
        content.setOnTouchListener((v,ev)->{ hideKeyboard(content); return false; });
        sc.addView(content,new LinearLayout.LayoutParams(-1,-2));

        rootFrame.addView(mainLayout,new FrameLayout.LayoutParams(-1,-1));

        scrim=new View(this);
        scrim.setBackgroundColor(Color.argb(150,0,0,0));
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v->closeDrawer());
        rootFrame.addView(scrim,new FrameLayout.LayoutParams(-1,-1));

        int screenWidthDp=getResources().getConfiguration().screenWidthDp;
        int drawerWidthDp=Math.min(screenWidthDp-56,320);
        drawerWidthDp=Math.max(drawerWidthDp,240);
        drawerWidthPx=dp(drawerWidthDp);
        drawerPanel=new LinearLayout(this);
        drawerPanel.setOrientation(LinearLayout.VERTICAL);
        drawerPanel.setBackgroundColor(card);
        drawerPanel.setElevation(dp(16));
        FrameLayout.LayoutParams drawerLp=new FrameLayout.LayoutParams(drawerWidthPx,-1);
        drawerLp.gravity=Gravity.RIGHT;
        rootFrame.addView(drawerPanel,drawerLp);
        drawerPanel.setTranslationX(drawerWidthPx);
        drawerPanel.setVisibility(View.GONE);

        TextView header=tv("مساعدي 👋",22);
        header.setGravity(Gravity.RIGHT);
        header.setPadding(dp(20),dp(28),dp(20),dp(4));
        drawerPanel.addView(header);
        TextView sub=tv("مساعدك الشخصي اليومي",13);
        sub.setTextColor(muted);
        sub.setGravity(Gravity.RIGHT);
        sub.setPadding(dp(20),0,dp(20),dp(18));
        drawerPanel.addView(sub);
        View divider=new View(this);
        divider.setBackgroundColor(Color.argb(60,255,255,255));
        drawerPanel.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));

        View homeRow=addDrawerGroup("الرئيسية","🏠",()->navigateTo("لوحة اليوم"),null);
        addDrawerGroup("المال","💰",null,new SubItem[]{
            new SubItem("الحسابات", ()->{ navigateTo("المال"); scrollToAnchor("money:accounts"); }),
            new SubItem("الحركات المالية", ()->{ navigateTo("المال"); scrollToAnchor("money:transactions"); }),
            new SubItem("معاملات متكررة", ()->{ navigateTo("المال"); scrollToAnchor("money:recurring"); })
        });
        addDrawerGroup("الديون","🤝",()->navigateTo("الديون"),null);
        addDrawerGroup("التداول","📈",null,new SubItem[]{
            new SubItem("سجل الصفقات", ()->{ navigateTo("📈 التداول"); scrollToAnchor("trading:list"); }),
            new SubItem("حاسبة المخاطرة", ()->{ navigateTo("📈 التداول"); riskDialog(); })
        });
        addDrawerGroup("الاستثمار","💼",()->navigateTo("💼 الاستثمار"),null);
        addDrawerGroup("المهام","✅",()->navigateTo("المهام"),null);
        addDrawerGroup("الإحصائيات","📊",()->navigateTo("📊 الإحصائيات"),null);
        addDrawerGroup("الإعدادات","⚙️",()->navigateTo("⚙️ الإعدادات"),null);
        highlightRow(homeRow);
    }

    View addDrawerGroup(String label,String icon,Runnable mainAction,SubItem[] subs){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20),dp(12),dp(20),dp(12));
        row.setMinimumHeight(dp(52));
        TextView labelV=new TextView(this);
        labelV.setText(label);
        labelV.setTextColor(white);
        labelV.setTextSize(16);
        labelV.setGravity(Gravity.RIGHT);
        row.addView(labelV,new LinearLayout.LayoutParams(0,-2,1));
        final TextView chevron;
        if(subs!=null && subs.length>0){
            chevron=new TextView(this);
            chevron.setText("▸");
            chevron.setTextColor(muted);
            chevron.setTextSize(14);
            chevron.setPadding(0,0,dp(10),0);
            row.addView(chevron,new LinearLayout.LayoutParams(-2,-2));
        } else chevron=null;
        TextView iconV=new TextView(this);
        iconV.setText(icon);
        iconV.setTextSize(18);
        iconV.setPadding(dp(12),0,0,0);
        row.addView(iconV,new LinearLayout.LayoutParams(-2,-2));
        drawerPanel.addView(row,new LinearLayout.LayoutParams(-1,-2));
        allNavRows.add(row);

        if(subs!=null && subs.length>0){
            final LinearLayout subsContainer=new LinearLayout(this);
            subsContainer.setOrientation(LinearLayout.VERTICAL);
            subsContainer.setVisibility(View.GONE);
            for(SubItem si:subs){
                TextView subRow=new TextView(this);
                subRow.setText(si.label);
                subRow.setTextColor(muted);
                subRow.setTextSize(14);
                subRow.setPadding(dp(40),dp(10),dp(20),dp(10));
                subRow.setGravity(Gravity.RIGHT);
                subRow.setMinimumHeight(dp(40));
                subRow.setOnClickListener(v->{ highlightRow(subRow); closeDrawer(); si.action.run(); });
                subsContainer.addView(subRow,new LinearLayout.LayoutParams(-1,-2));
                allNavRows.add(subRow);
            }
            drawerPanel.addView(subsContainer,new LinearLayout.LayoutParams(-1,-2));
            row.setOnClickListener(v->{
                boolean expanded=subsContainer.getVisibility()==View.VISIBLE;
                subsContainer.setVisibility(expanded?View.GONE:View.VISIBLE);
                chevron.setText(expanded?"▸":"▾");
            });
        } else {
            row.setOnClickListener(v->{ highlightRow(row); closeDrawer(); mainAction.run(); });
        }
        return row;
    }

    void toggleDrawer(){if(drawerOpen)closeDrawer();else openDrawer();}
    void openDrawer(){drawerOpen=true;drawerPanel.setVisibility(View.VISIBLE);scrim.setVisibility(View.VISIBLE);scrim.setAlpha(0f);scrim.animate().alpha(1f).setDuration(200).start();drawerPanel.animate().translationX(0).setDuration(220).start();}
    void closeDrawer(){drawerOpen=false;drawerPanel.animate().translationX(drawerWidthPx).setDuration(200).withEndAction(()->drawerPanel.setVisibility(View.GONE)).start();scrim.animate().alpha(0f).setDuration(200).withEndAction(()->scrim.setVisibility(View.GONE)).start();}
    void highlightRow(View row){int hl=Color.argb(80,Color.red(accent),Color.green(accent),Color.blue(accent));for(View v:allNavRows) v.setBackground(v==row?rounded(hl,10):null);}
    void markAnchor(String key,View v){ sectionAnchors.put(key,v); }
    void scrollToAnchor(String key){
        final View v=sectionAnchors.get(key);
        if(v==null || sc==null) return;
        sc.post(()->sc.smoothScrollTo(0,v.getTop()));
    }
    void navigateTo(String key){
        if(key.equals("لوحة اليوم"))showHome();
        else if(key.equals("المال"))showMoney();
        else if(key.equals("الديون"))showDebts();
        else if(key.equals("📈 التداول"))showTrading();
        else if(key.equals("💼 الاستثمار"))showInvestments();
        else if(key.equals("المهام"))showTasks();
        else if(key.equals("📊 الإحصائيات"))showStats();
        else if(key.equals("⚙️ الإعدادات"))showSettings();
    }

    public void onBackPressed(){if(drawerOpen){closeDrawer();}else if(!currentTab.equals("لوحة اليوم")){showHome();}else{super.onBackPressed();}}
    void clear(String t){content.removeAllViews();title.setText(t);currentTab=t;sectionAnchors.clear();}

    // ================= HOME =================
    void showHome(){
        clear("لوحة اليوم");
        List<DatabaseHelper.Row> dueDebts=db.upcomingDebts(3);
        List<DatabaseHelper.Row> dueTasks=db.upcomingTasks(2);
        if(!dueDebts.isEmpty() || !dueTasks.isEmpty()){
            add(tv("🔔 تنبيهات",18));
            for(DatabaseHelper.Row r:dueDebts){ TextView t=cardText("🤝 "+r.text); t.setTextColor(red); t.setOnClickListener(v->navigateTo("الديون")); add(t); }
            for(DatabaseHelper.Row r:dueTasks){ TextView t=cardText("✅ "+r.text); t.setTextColor(amber); t.setOnClickListener(v->navigateTo("المهام")); add(t); }
        }
        double invCost=db.investmentsCost(),invNow=db.investmentsCurrentValue(),invPl=invNow-invCost;
        LinkedHashMap<String,Double> byCur=db.balanceByCurrency();
        StringBuilder balLine=new StringBuilder();
        if(byCur.isEmpty()) balLine.append("لا توجد حسابات بعد");
        else { boolean first=true; for(Map.Entry<String,Double> en:byCur.entrySet()){ if(!first) balLine.append(" | "); balLine.append(money(en.getValue())).append(" ").append(en.getKey()); first=false; } }
        add(cardText("صباح الخير 👋\n\n💰 أرصدة الحسابات: "+balLine.toString()+"\n📈 صفقات التداول: "+db.count("trades",null)+"\n💼 قيمة الاستثمارات الحالية: "+money(invNow)+"\n🤝 الديون عليك/لك: "+db.count("debts",null)+"\n✅ المهام المفتوحة: "+db.count("tasks","done=0")));
        add(tv("ملخص سريع",20));
        add(cardText("💸 إجمالي أرباح التداول: "+money(db.sum("trades","profit"))+"\n📊 ربح/خسارة الاستثمارات غير المحققة: "+money(invPl)+"\n📋 إجمالي قيمة الديون المسجلة: "+money(db.sum("debts","amount"))));
    }

    // ================= MONEY =================
    void showMoney(){
        clear("المال");
        add(primaryBtn("+ إضافة حساب")).setOnClickListener(v->accountDialog());
        add(primaryBtn("+ إضافة حركة مالية")).setOnClickListener(v->transactionDialog());

        TextView accHeader=tv("الحسابات",20); add(accHeader); markAnchor("money:accounts",accHeader);
        EditText searchAcc=searchBox("🔍 بحث في الحسابات (بالاسم)"); add(searchAcc);
        LinearLayout accList=new LinearLayout(this); accList.setOrientation(LinearLayout.VERTICAL); add(accList);
        renderAccounts(accList,"");
        searchAcc.addTextChangedListener(simpleWatcher(s->renderAccounts(accList,s)));

        TextView txHeader=tv("الحركات المالية",20); add(txHeader); markAnchor("money:transactions",txHeader);
        EditText searchTx=searchBox("🔍 بحث في الحركات (بالتصنيف)"); add(searchTx);
        LinearLayout txList=new LinearLayout(this); txList.setOrientation(LinearLayout.VERTICAL); add(txList);
        renderTransactions(txList,"");
        searchTx.addTextChangedListener(simpleWatcher(s->renderTransactions(txList,s)));

        TextView recHeader=tv("معاملات متكررة (شهرية)",20); add(recHeader); markAnchor("money:recurring",recHeader);
        TextView recHint=tv("تُضاف تلقائيًا مرة كل شهر عند فتح التطبيق.",13); recHint.setTextColor(muted); add(recHint);
        add(primaryBtn("+ إضافة معاملة متكررة")).setOnClickListener(v->recurringDialog());
        LinearLayout recList=new LinearLayout(this); recList.setOrientation(LinearLayout.VERTICAL); add(recList);
        renderRecurring(recList);
    }
    void renderRecurring(LinearLayout container){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("recurring_transactions",new String[]{"account","kind","category","amount","day_of_month"},"id DESC",null,null);
        if(rows.isEmpty()){ TextView t=cardText("لا توجد معاملات متكررة بعد."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editRecurringDialog(r.id,container)));
    }
    void recurringDialog(){
        EditText acc=e("الحساب"); Spinner kind=spinner(TX_KINDS,null); Spinner cat=spinner(TX_CATEGORIES,null); EditText amt=eNum("المبلغ"); EditText day=eInt("يوم الشهر (1-28)"); EditText note=eNotes("ملاحظة");
        new AlertDialog.Builder(this).setTitle("معاملة متكررة جديدة").setView(box(acc,kind,cat,amt,day,note)).setPositiveButton("حفظ",(d,w)->{
            int dom=(int)Math.max(1,Math.min(28,num(day)));
            db.addRecurring(acc.getText().toString(),(String)kind.getSelectedItem(),(String)cat.getSelectedItem(),num(amt),dom,note.getText().toString());
            showMoney();
        }).setNegativeButton("إلغاء",null).show();
    }
    void editRecurringDialog(long id,LinearLayout container){
        Cursor c=db.getRow("recurring_transactions",id,new String[]{"account","kind","category","amount","day_of_month","note"}); if(!c.moveToFirst()){c.close();return;}
        EditText acc=e("الحساب"); acc.setText(c.getString(0));
        Spinner kind=spinner(TX_KINDS,c.getString(1));
        Spinner cat=spinner(TX_CATEGORIES,c.getString(2));
        EditText amt=eNum("المبلغ"); amt.setText(String.valueOf(c.getDouble(3)));
        EditText day=eInt("يوم الشهر (1-28)"); day.setText(String.valueOf(c.getInt(4)));
        EditText note=eNotes("ملاحظة"); note.setText(c.getString(5));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل المعاملة المتكررة").setView(box(acc,kind,cat,amt,day,note))
            .setPositiveButton("حفظ",(d,w)->{ int dom=(int)Math.max(1,Math.min(28,num(day))); db.updateRecurring(id,acc.getText().toString(),(String)kind.getSelectedItem(),(String)cat.getSelectedItem(),num(amt),dom,note.getText().toString()); renderRecurring(container); })
            .setNeutralButton("حذف",(d,w)->confirmDelete("recurring_transactions",id,()->renderRecurring(container)))
            .setNegativeButton("إلغاء",null).show();
    }
    void renderAccounts(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("accounts",new String[]{"name","type","balance","currency"},"id DESC",q,"name");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد حسابات بعد — دوس + إضافة حساب عشان تبدأ.":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editAccountDialog(r.id,container,q)));
    }
    void accountDialog(){final EditText n=e("اسم الحساب");final Spinner type=spinner(ACCOUNT_TYPES,null);final EditText bal=eNum("الرصيد");final Spinner cur=spinner(CURRENCIES,null);LinearLayout l=box(n,type,bal,cur);new AlertDialog.Builder(this).setTitle("إضافة حساب").setView(l).setPositiveButton("حفظ",(d,w)->{db.addAccount(n.getText().toString(),(String)type.getSelectedItem(),num(bal),(String)cur.getSelectedItem());showMoney();}).setNegativeButton("إلغاء",null).show();}
    void editAccountDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("accounts",id,new String[]{"name","type","balance","currency"}); if(!c.moveToFirst()){c.close();return;}
        EditText n=e("اسم الحساب"); n.setText(c.getString(0));
        Spinner type=spinner(ACCOUNT_TYPES,c.getString(1));
        EditText bal=eNum("الرصيد"); bal.setText(String.valueOf(c.getDouble(2)));
        Spinner cur=spinner(CURRENCIES,c.getString(3));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل الحساب").setView(box(n,type,bal,cur))
            .setPositiveButton("حفظ",(d,w)->{db.updateAccount(id,n.getText().toString(),(String)type.getSelectedItem(),num(bal),(String)cur.getSelectedItem());renderAccounts(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("accounts",id,()->renderAccounts(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }
    void renderTransactions(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("transactions",new String[]{"account","kind","category","amount","date"},"id DESC",q,"category");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد حركات مالية مسجلة بعد.":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editTransactionDialog(r.id,container,q)));
    }
    void transactionDialog(){EditText acc=e("الحساب");Spinner kind=spinner(TX_KINDS,null);Spinner cat=spinner(TX_CATEGORIES,null);EditText amt=eNum("المبلغ");EditText note=eNotes("ملاحظة");LinearLayout l=box(acc,kind,cat,amt,note);new AlertDialog.Builder(this).setTitle("حركة مالية").setView(l).setPositiveButton("حفظ",(d,w)->{db.addTransaction(acc.getText().toString(),(String)kind.getSelectedItem(),(String)cat.getSelectedItem(),num(amt),today(),note.getText().toString());showMoney();}).setNegativeButton("إلغاء",null).show();}
    void editTransactionDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("transactions",id,new String[]{"account","kind","category","amount","date","note"}); if(!c.moveToFirst()){c.close();return;}
        EditText acc=e("الحساب"); acc.setText(c.getString(0));
        Spinner kind=spinner(TX_KINDS,c.getString(1));
        Spinner cat=spinner(TX_CATEGORIES,c.getString(2));
        EditText amt=eNum("المبلغ"); amt.setText(String.valueOf(c.getDouble(3)));
        String date=c.getString(4);
        EditText note=eNotes("ملاحظة"); note.setText(c.getString(5));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل الحركة").setView(box(acc,kind,cat,amt,note))
            .setPositiveButton("حفظ",(d,w)->{db.updateTransaction(id,acc.getText().toString(),(String)kind.getSelectedItem(),(String)cat.getSelectedItem(),num(amt),date,note.getText().toString());renderTransactions(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("transactions",id,()->renderTransactions(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }

    // ================= DEBTS =================
    void showDebts(){
        clear("الديون");
        add(primaryBtn("+ إضافة دين")).setOnClickListener(v->debtDialog());
        add(cardText("إجمالي المسجل: "+money(db.sum("debts","amount"))+"\nعدد الديون: "+db.count("debts",null)));
        add(tv("مرتّبة حسب أقرب استحقاق",18));
        EditText search=searchBox("🔍 بحث بالاسم"); add(search);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); add(list);
        renderDebts(list,"");
        search.addTextChangedListener(simpleWatcher(s->renderDebts(list,s)));
    }
    void renderDebts(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("debts",new String[]{"name","kind","amount","paid","due"},"due ASC",q,"name");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد ديون مسجلة حاليًا.":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editDebtDialog(r.id,container,q)));
    }
    void debtDialog(){EditText n=e("الاسم");Spinner k=spinner(DEBT_KINDS,null);EditText a=eNum("المبلغ");EditText p=eNum("المدفوع");EditText due=dateField("تاريخ الاستحقاق",null);EditText no=eNotes("ملاحظات");new AlertDialog.Builder(this).setTitle("إضافة دين").setView(box(n,k,a,p,due,no)).setPositiveButton("حفظ",(d,w)->{db.addDebt(n.getText().toString(),(String)k.getSelectedItem(),num(a),num(p),due.getText().toString(),no.getText().toString());showDebts();}).setNegativeButton("إلغاء",null).show();}
    void editDebtDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("debts",id,new String[]{"name","kind","amount","paid","due","notes"}); if(!c.moveToFirst()){c.close();return;}
        EditText n=e("الاسم"); n.setText(c.getString(0));
        Spinner k=spinner(DEBT_KINDS,c.getString(1));
        EditText a=eNum("المبلغ"); a.setText(String.valueOf(c.getDouble(2)));
        EditText p=eNum("المدفوع"); p.setText(String.valueOf(c.getDouble(3)));
        EditText due=dateField("تاريخ الاستحقاق",c.getString(4));
        EditText no=eNotes("ملاحظات"); no.setText(c.getString(5));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل الدين").setView(box(n,k,a,p,due,no))
            .setPositiveButton("حفظ",(d,w)->{db.updateDebt(id,n.getText().toString(),(String)k.getSelectedItem(),num(a),num(p),due.getText().toString(),no.getText().toString());renderDebts(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("debts",id,()->renderDebts(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }

    // ================= TRADING =================
    void showTrading(){
        clear("📈 التداول");
        add(cardText("🥇 XAUUSD Trading Center\n\nإجمالي P/L: "+money(db.sum("trades","profit"))+"\nعدد الصفقات: "+db.count("trades",null)+"\nمعدل الربح: "+String.format(Locale.US,"%.1f",db.winRate())+"%"));
        add(primaryBtn("+ تسجيل صفقة")).setOnClickListener(v->tradeDialog());
        add(btn("🛡️ حاسبة المخاطرة")).setOnClickListener(v->riskDialog());
        TextView tradesHeader=tv("سجل الصفقات",20); add(tradesHeader); markAnchor("trading:list",tradesHeader);
        EditText search=searchBox("🔍 بحث بالبروكر (Exness / JustMarkets)"); add(search);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); add(list);
        renderTrades(list,"");
        search.addTextChangedListener(simpleWatcher(s->renderTrades(list,s)));
    }
    void renderTrades(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("trades",new String[]{"broker","symbol","side","entry","exit","lots","profit","strategy"},"id DESC",q,"broker");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد صفقات مسجلة بعد.":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editTradeDialog(r.id,container,q)));
    }
    void tradeDialog(){Spinner broker=spinner(BROKERS,null);EditText symbol=e("Symbol: XAUUSD");Spinner side=spinner(TRADE_SIDES,null);EditText entry=eNum("Entry");EditText exit=eNum("Exit");EditText sl=eNum("Stop Loss");EditText tp=eNum("Take Profit");EditText lots=eNum("Lots");EditText profit=eNum("Profit / Loss");EditText strategy=e("Strategy");EditText notes=eNotes("Notes");new AlertDialog.Builder(this).setTitle("صفقة جديدة").setView(box(broker,symbol,side,entry,exit,sl,tp,lots,profit,strategy,notes)).setPositiveButton("حفظ",(d,w)->{db.addTrade((String)broker.getSelectedItem(),symbol.getText().toString(),(String)side.getSelectedItem(),num(entry),num(exit),num(sl),num(tp),num(lots),num(profit),"CLOSED",today(),today(),strategy.getText().toString(),notes.getText().toString());showTrading();}).setNegativeButton("إلغاء",null).show();}
    void editTradeDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("trades",id,new String[]{"broker","symbol","side","entry","exit","sl","tp","lots","profit","status","opened","closed","strategy","notes"}); if(!c.moveToFirst()){c.close();return;}
        Spinner broker=spinner(BROKERS,c.getString(0));
        EditText symbol=e("Symbol"); symbol.setText(c.getString(1));
        Spinner side=spinner(TRADE_SIDES,c.getString(2));
        EditText entry=eNum("Entry"); entry.setText(String.valueOf(c.getDouble(3)));
        EditText exit=eNum("Exit"); exit.setText(String.valueOf(c.getDouble(4)));
        EditText sl=eNum("Stop Loss"); sl.setText(String.valueOf(c.getDouble(5)));
        EditText tp=eNum("Take Profit"); tp.setText(String.valueOf(c.getDouble(6)));
        EditText lots=eNum("Lots"); lots.setText(String.valueOf(c.getDouble(7)));
        EditText profit=eNum("Profit / Loss"); profit.setText(String.valueOf(c.getDouble(8)));
        String status=c.getString(9), opened=c.getString(10), closed=c.getString(11);
        EditText strategy=e("Strategy"); strategy.setText(c.getString(12));
        EditText notes=eNotes("Notes"); notes.setText(c.getString(13));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل الصفقة").setView(box(broker,symbol,side,entry,exit,sl,tp,lots,profit,strategy,notes))
            .setPositiveButton("حفظ",(d,w)->{db.updateTrade(id,(String)broker.getSelectedItem(),symbol.getText().toString(),(String)side.getSelectedItem(),num(entry),num(exit),num(sl),num(tp),num(lots),num(profit),status,opened,closed,strategy.getText().toString(),notes.getText().toString());renderTrades(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("trades",id,()->renderTrades(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }
    void riskDialog(){EditText balance=eNum("رأس المال");EditText risk=eNum("نسبة المخاطرة %");EditText sl=eNum("مسافة وقف الخسارة بالنقاط");new AlertDialog.Builder(this).setTitle("حاسبة المخاطرة").setView(box(balance,risk,sl)).setPositiveButton("احسب",(d,w)->{double allowed=num(balance)*num(risk)/100;showMsg("الحد الأقصى للخسارة","المسموح بخسارته = "+money(allowed)+"\nحجم اللوت يحتاج أيضًا إلى قيمة النقطة (Tick Value) الخاصة بالبروكر/الأداة، لذلك لا أخمنها.");}).setNegativeButton("إلغاء",null).show();}

    // ================= INVESTMENTS =================
    void showInvestments(){
        clear("💼 الاستثمار");
        double cost=db.investmentsCost(),now=db.investmentsCurrentValue(),pl=now-cost;String plIcon=pl>=0?"🟢":"🔴";
        add(cardText("القيمة الحالية: "+money(now)+"\nإجمالي التكلفة: "+money(cost)+"\n"+plIcon+" الربح/الخسارة غير المحقق: "+money(pl)));
        add(primaryBtn("+ إضافة استثمار")).setOnClickListener(v->investmentDialog());
        add(tv("محفظة الاستثمار",20));
        EditText search=searchBox("🔍 بحث بالاسم"); add(search);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); add(list);
        renderInvestments(list,"");
        search.addTextChangedListener(simpleWatcher(s->renderInvestments(list,s)));
    }
    void renderInvestments(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("investments",new String[]{"name","type","quantity","buy_price","current_price","buy_date"},"id DESC",q,"name");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد استثمارات مسجلة بعد.":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editInvestmentDialog(r.id,container,q)));
    }
    void investmentDialog(){EditText n=e("اسم الأصل: مثل ذهب فعلي / سهم / عملة رقمية");Spinner type=spinner(INVESTMENT_TYPES,null);EditText qty=eNum("الكمية");EditText buyPrice=eNum("سعر الشراء للوحدة");EditText curPrice=eNum("السعر الحالي للوحدة");EditText buyDate=dateField("تاريخ الشراء",null);EditText notes=eNotes("ملاحظات");new AlertDialog.Builder(this).setTitle("إضافة استثمار").setView(box(n,type,qty,buyPrice,curPrice,buyDate,notes)).setPositiveButton("حفظ",(d,w)->{String bd=buyDate.getText().toString();if(bd.trim().isEmpty())bd=today();db.addInvestment(n.getText().toString(),(String)type.getSelectedItem(),num(qty),num(buyPrice),num(curPrice),bd,notes.getText().toString());showInvestments();}).setNegativeButton("إلغاء",null).show();}
    void editInvestmentDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("investments",id,new String[]{"name","type","quantity","buy_price","current_price","buy_date","notes"}); if(!c.moveToFirst()){c.close();return;}
        EditText n=e("اسم الأصل"); n.setText(c.getString(0));
        Spinner type=spinner(INVESTMENT_TYPES,c.getString(1));
        EditText qty=eNum("الكمية"); qty.setText(String.valueOf(c.getDouble(2)));
        EditText buyPrice=eNum("سعر الشراء للوحدة"); buyPrice.setText(String.valueOf(c.getDouble(3)));
        EditText curPrice=eNum("السعر الحالي للوحدة"); curPrice.setText(String.valueOf(c.getDouble(4)));
        EditText buyDate=dateField("تاريخ الشراء",c.getString(5));
        EditText notes=eNotes("ملاحظات"); notes.setText(c.getString(6));
        c.close();
        new AlertDialog.Builder(this).setTitle("تعديل الاستثمار").setView(box(n,type,qty,buyPrice,curPrice,buyDate,notes))
            .setPositiveButton("حفظ",(d,w)->{String bd=buyDate.getText().toString();if(bd.trim().isEmpty())bd=today();db.updateInvestment(id,n.getText().toString(),(String)type.getSelectedItem(),num(qty),num(buyPrice),num(curPrice),bd,notes.getText().toString());renderInvestments(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("investments",id,()->renderInvestments(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }

    // ================= TASKS =================
    void showTasks(){
        clear("المهام");
        add(primaryBtn("+ مهمة جديدة")).setOnClickListener(v->taskDialog());
        EditText search=searchBox("🔍 بحث بعنوان المهمة"); add(search);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); add(list);
        renderTasks(list,"");
        search.addTextChangedListener(simpleWatcher(s->renderTasks(list,s)));
    }
    void renderTasks(LinearLayout container,String q){
        container.removeAllViews();
        List<DatabaseHelper.Row> rows=db.listRows("tasks",new String[]{"title","due","priority","done"},"id DESC",q,"title");
        if(rows.isEmpty()){ TextView t=cardText(q.isEmpty()?"لا توجد مهام حاليًا 🎉":"لا نتائج."); t.setTextColor(muted); addTo(container,t); return; }
        for(DatabaseHelper.Row r:rows) addTo(container, clickableRow(r.text, ()->editTaskDialog(r.id,container,q)));
    }
    void taskDialog(){EditText n=e("المهمة");EditText due=dateField("الموعد",null);Spinner pri=spinner(PRIORITIES,null);new AlertDialog.Builder(this).setTitle("مهمة جديدة").setView(box(n,due,pri)).setPositiveButton("حفظ",(d,w)->{db.addTask(n.getText().toString(),due.getText().toString(),(String)pri.getSelectedItem());showTasks();}).setNegativeButton("إلغاء",null).show();}
    void editTaskDialog(long id,LinearLayout container,String q){
        Cursor c=db.getRow("tasks",id,new String[]{"title","due","priority","done"}); if(!c.moveToFirst()){c.close();return;}
        EditText n=e("المهمة"); n.setText(c.getString(0));
        EditText due=dateField("الموعد",c.getString(1));
        Spinner pri=spinner(PRIORITIES,c.getString(2));
        boolean isDone=c.getInt(3)!=0;
        c.close();
        LinearLayout doneRow=new LinearLayout(this); doneRow.setOrientation(LinearLayout.HORIZONTAL); doneRow.setGravity(Gravity.CENTER_VERTICAL); doneRow.setPadding(0,dp(6),0,dp(6));
        TextView doneLabel=new TextView(this); doneLabel.setText("تم الإنجاز"); doneLabel.setTextColor(white); doneLabel.setGravity(Gravity.RIGHT);
        final Switch doneSwitch=new Switch(this); doneSwitch.setChecked(isDone);
        doneRow.addView(doneLabel,new LinearLayout.LayoutParams(0,-2,1)); doneRow.addView(doneSwitch,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout l=box(n,due,pri); l.addView(doneRow,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("تعديل المهمة").setView(l)
            .setPositiveButton("حفظ",(d,w)->{db.updateTask(id,n.getText().toString(),due.getText().toString(),(String)pri.getSelectedItem(),doneSwitch.isChecked()?1:0);renderTasks(container,q);})
            .setNeutralButton("حذف",(d,w)->confirmDelete("tasks",id,()->renderTasks(container,q)))
            .setNegativeButton("إلغاء",null).show();
    }

    // ================= STATS =================
    void showStats(){
        clear("📊 الإحصائيات");

        add(tv("أداء التداول",20));
        add(cardText("معدل الصفقات الرابحة: "+String.format(Locale.US,"%.1f",db.winRate())+"%\nإجمالي الربح/الخسارة: "+money(db.sum("trades","profit"))+"\nمتوسط الربح: "+money(db.avgWin())+"\nمتوسط الخسارة: "+money(db.avgLoss())));
        List<Double> profits=db.tradeProfitsChrono();
        if(profits.size()>=2){
            List<Float> cum=new ArrayList<>(); double running=0;
            for(double p:profits){ running+=p; cum.add((float)running); }
            LineChartView lc=new LineChartView(this); lc.setColor(accent); lc.setValues(cum);
            add(lc,160);
        } else {
            TextView t=cardText("سجّل صفقتين على الأقل عشان يظهر منحنى الأداء."); t.setTextColor(muted); add(t);
        }

        add(tv("توزيع محفظة الاستثمار",20));
        LinkedHashMap<String,Double> alloc=db.investmentAllocation();
        if(!alloc.isEmpty()){
            List<Float> vals=new ArrayList<>(); List<String> names=new ArrayList<>();
            for(Map.Entry<String,Double> en:alloc.entrySet()){ vals.add(en.getValue().floatValue()); names.add(en.getKey()); }
            int[] pieColors={accent,green,amber,red,Color.rgb(168,85,247),Color.rgb(236,72,153)};
            PieChartView pc=new PieChartView(this); pc.setData(vals,pieColors); pc.setHoleColor(bg);
            add(pc,180);
            StringBuilder legend=new StringBuilder();
            for(String nm:names) legend.append("● ").append(nm).append("   ");
            add(cardText(legend.toString()));
        } else {
            TextView t=cardText("لا توجد استثمارات مسجلة بعد."); t.setTextColor(muted); add(t);
        }

        add(tv("الدخل والمصروف الشهري",20));
        LinkedHashMap<String,double[]> monthly=db.monthlyIncomeExpense(6);
        List<String> shortLabels=new ArrayList<>(); List<Float> incomeVals=new ArrayList<>(); List<Float> expenseVals=new ArrayList<>();
        boolean anyData=false;
        for(Map.Entry<String,double[]> en:monthly.entrySet()){
            String m=en.getKey(); shortLabels.add(m.length()>=7?m.substring(5):m);
            double[] v=en.getValue(); incomeVals.add((float)v[0]); expenseVals.add((float)v[1]);
            if(v[0]!=0 || v[1]!=0) anyData=true;
        }
        if(anyData){
            BarChartView bc=new BarChartView(this); bc.setTextColor(muted);
            float[] inc=new float[incomeVals.size()]; for(int i=0;i<inc.length;i++) inc[i]=incomeVals.get(i);
            float[] exp=new float[expenseVals.size()]; for(int i=0;i<exp.length;i++) exp[i]=expenseVals.get(i);
            List<float[]> series=new ArrayList<>(); series.add(inc); series.add(exp);
            bc.setData(shortLabels,series,new int[]{green,red});
            add(bc,160);
            add(cardText("🟢 دخل   🔴 مصروف"));
        } else {
            TextView t=cardText("سجّل حركات مالية عشان يظهر الرسم الشهري."); t.setTextColor(muted); add(t);
        }
    }

    // ================= SETTINGS =================
    void showSettings(){
        clear("⚙️ الإعدادات");

        add(tv("المظهر",18));
        LinearLayout themeRow=new LinearLayout(this); themeRow.setOrientation(LinearLayout.HORIZONTAL); themeRow.setGravity(Gravity.CENTER_VERTICAL); themeRow.setBackground(rounded(card,12)); themeRow.setPadding(dp(16),dp(14),dp(16),dp(14));
        TextView themeLabel=new TextView(this); themeLabel.setText(prefs.isDark()?"الوضع الغامق مفعّل":"الوضع الفاتح مفعّل"); themeLabel.setTextColor(white); themeLabel.setTextSize(15); themeLabel.setGravity(Gravity.RIGHT);
        Switch themeSwitch=new Switch(this); themeSwitch.setChecked(prefs.isDark());
        themeRow.addView(themeLabel,new LinearLayout.LayoutParams(0,-2,1)); themeRow.addView(themeSwitch,new LinearLayout.LayoutParams(-2,-2));
        themeSwitch.setOnCheckedChangeListener((btnV,checked)->{ prefs.setDark(checked); recreate(); });
        add(themeRow);

        add(tv("لون التطبيق",18));
        LinearLayout colorsRow=new LinearLayout(this); colorsRow.setOrientation(LinearLayout.HORIZONTAL); colorsRow.setGravity(Gravity.CENTER);
        int[] palette={0xFF38BDF8,0xFF22C55E,0xFFF59E0B,0xFFEF4444,0xFFA855F7,0xFFEC4899};
        for(int col:palette){
            View swatch=new View(this);
            GradientDrawable gd=new GradientDrawable(); gd.setColor(col); gd.setShape(GradientDrawable.OVAL);
            if(col==prefs.getAccent()) gd.setStroke(dp(3),white);
            swatch.setBackground(gd);
            LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(dp(40),dp(40)); slp.setMargins(dp(6),0,dp(6),0);
            final int c=col;
            swatch.setOnClickListener(v->{ prefs.setAccent(c); recreate(); });
            colorsRow.addView(swatch,slp);
        }
        add(colorsRow);

        add(tv("الأمان",18));
        if(prefs.hasPin()){
            add(btn("🔑 تغيير رمز القفل")).setOnClickListener(v->setPinDialog());
            add(btn("🚫 إزالة رمز القفل")).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("إزالة القفل").setMessage("هل تريد إزالة رمز القفل؟").setPositiveButton("إزالة",(d,w)->{prefs.clearPin();showMsg("تم","تم إزالة رمز القفل");showSettings();}).setNegativeButton("إلغاء",null).show());
            if(biometricAvailable()){
                LinearLayout bioRow=new LinearLayout(this); bioRow.setOrientation(LinearLayout.HORIZONTAL); bioRow.setGravity(Gravity.CENTER_VERTICAL); bioRow.setBackground(rounded(card,12)); bioRow.setPadding(dp(16),dp(14),dp(16),dp(14));
                TextView bioLabel=new TextView(this); bioLabel.setText("فتح التطبيق بالبصمة"); bioLabel.setTextColor(white); bioLabel.setTextSize(15); bioLabel.setGravity(Gravity.RIGHT);
                Switch bioSwitch=new Switch(this); bioSwitch.setChecked(prefs.isBiometricEnabled());
                bioRow.addView(bioLabel,new LinearLayout.LayoutParams(0,-2,1)); bioRow.addView(bioSwitch,new LinearLayout.LayoutParams(-2,-2));
                bioSwitch.setOnCheckedChangeListener((btnV,checked)->prefs.setBiometricEnabled(checked));
                add(bioRow);
            }
        } else {
            add(primaryBtn("🔒 تفعيل رمز قفل للتطبيق")).setOnClickListener(v->setPinDialog());
        }

        add(tv("النسخ الاحتياطي",18));
        add(btn("⬆️ تصدير نسخة احتياطية")).setOnClickListener(v->exportBackup());
        add(btn("⬇️ استيراد نسخة احتياطية")).setOnClickListener(v->importBackupPrompt());

        add(tv("التقرير الشهري",18));
        final Spinner monthPick=spinner(new String[]{"هذا الشهر","الشهر الماضي","قبل شهرين"},null);
        add(monthPick);
        add(btn("📄 تصدير / مشاركة التقرير")).setOnClickListener(v->{
            int back=monthPick.getSelectedItemPosition();
            Calendar cal=Calendar.getInstance(); cal.add(Calendar.MONTH,-back);
            String ym=new SimpleDateFormat("yyyy-MM",Locale.US).format(cal.getTime());
            String[] arMonths={"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
            String label=arMonths[cal.get(Calendar.MONTH)]+" "+cal.get(Calendar.YEAR);
            exportMonthlyReport(ym,label);
        });

        add(tv("عن التطبيق",18));
        add(cardText("مساعدي — نسخة محلية بالكامل، بياناتك محفوظة على جهازك فقط ولا تُرسل لأي سيرفر خارجي."));
    }
    void setPinDialog(){
        EditText p1=e("رمز جديد (4 أرقام على الأقل)"); p1.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        EditText p2=e("تأكيد الرمز"); p2.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("رمز القفل").setView(box(p1,p2)).setPositiveButton("حفظ",(d,w)->{
            String a=p1.getText().toString().trim(), bb=p2.getText().toString().trim();
            if(a.length()<4){ showMsg("خطأ","الرمز لازم يكون 4 أرقام على الأقل"); return; }
            if(!a.equals(bb)){ showMsg("خطأ","الرمزين غير متطابقين"); return; }
            prefs.setPin(a); showMsg("تم","تم تفعيل رمز القفل"); showSettings();
        }).setNegativeButton("إلغاء",null).show();
    }
    void exportBackup(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"myassistant_backup.json");
        startActivityForResult(i,REQ_EXPORT);
    }
    void importBackupPrompt(){
        new AlertDialog.Builder(this).setTitle("استيراد نسخة احتياطية").setMessage("سيتم استبدال كل البيانات الحالية بالبيانات الموجودة في ملف النسخة الاحتياطية. متابعة؟").setPositiveButton("اختيار الملف",(d,w)->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i,REQ_IMPORT);
        }).setNegativeButton("إلغاء",null).show();
    }
    void exportMonthlyReport(String ym,String label){
        StringBuilder sb=new StringBuilder();
        sb.append("تقرير مساعدي — ").append(label).append("\n\n");
        double inc=db.monthIncome(ym), exp=db.monthExpense(ym);
        sb.append("💰 الدخل والمصروف:\n");
        sb.append("  الدخل: ").append(money(inc)).append("\n");
        sb.append("  المصروف: ").append(money(exp)).append("\n");
        sb.append("  الصافي: ").append(money(inc-exp)).append("\n\n");
        LinkedHashMap<String,Double> cats=db.monthCategoryBreakdown(ym);
        if(!cats.isEmpty()){
            sb.append("📋 حسب التصنيف:\n");
            for(Map.Entry<String,Double> en:cats.entrySet()) sb.append("  ").append(en.getKey()).append(": ").append(money(en.getValue())).append("\n");
            sb.append("\n");
        }
        sb.append("🤝 الديون:\n");
        sb.append("  عدد الديون المسجلة: ").append(db.count("debts",null)).append("\n");
        sb.append("  إجمالي القيمة: ").append(money(db.sum("debts","amount"))).append("\n\n");
        int tc=db.monthTradeCount(ym); double tp=db.monthTradeProfit(ym);
        sb.append("📈 التداول (").append(label).append("):\n");
        sb.append("  عدد الصفقات المغلقة: ").append(tc).append("\n");
        sb.append("  صافي الربح/الخسارة: ").append(money(tp)).append("\n\n");
        sb.append("💼 الاستثمار:\n");
        sb.append("  القيمة الحالية: ").append(money(db.investmentsCurrentValue())).append("\n");
        sb.append("  الربح/الخسارة غير المحقق: ").append(money(db.investmentsCurrentValue()-db.investmentsCost())).append("\n");

        Intent share=new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT,"تقرير مساعدي — "+label);
        share.putExtra(Intent.EXTRA_TEXT,sb.toString());
        startActivity(Intent.createChooser(share,"مشاركة / حفظ التقرير"));
    }

    protected void onActivityResult(int reqCode,int resultCode,Intent data){
        super.onActivityResult(reqCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null) return;
        Uri uri=data.getData();
        try{
            if(reqCode==REQ_EXPORT){ Backup.export(this,db,uri); showMsg("تم","تم حفظ النسخة الاحتياطية بنجاح"); }
            else if(reqCode==REQ_IMPORT){ Backup.restore(this,db,uri); showMsg("تم","تم استرجاع البيانات بنجاح"); showHome(); }
        }catch(Exception ex){ showMsg("خطأ","حصل خطأ: "+ex.getMessage()); }
    }

    // ================= HELPERS =================
    double num(EditText e){try{return Double.parseDouble(e.getText().toString().replace(",","").trim());}catch(Exception x){return 0;}}
    String money(double x){return String.format(Locale.US,"%.2f",x);}
    String today(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
    void showMsg(String t,String m){new AlertDialog.Builder(this).setTitle(t).setMessage(m).setPositiveButton("حسنًا",null).show();}
}
