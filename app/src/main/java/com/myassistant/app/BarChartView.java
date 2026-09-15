package com.myassistant.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.List;

public class BarChartView extends View {
    private List<String> labels; private List<float[]> series; private int[] colors;
    private final Paint barPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context c){
        super(c);
        textPaint.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
    }

    public void setTextColor(int c){ textPaint.setColor(c); invalidate(); }
    public void setData(List<String> labels, List<float[]> series, int[] colors){ this.labels=labels; this.series=series; this.colors=colors; invalidate(); }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(labels==null || labels.isEmpty() || series==null || series.isEmpty()) return;
        int w=getWidth(), h=getHeight();
        float density=getResources().getDisplayMetrics().density;
        float padBottom=22*density, padTop=8*density;
        float max=1f;
        for(float[] s:series) for(float v:s) max=Math.max(max, Math.abs(v));
        int n=labels.size();
        float groupW=(float)w/n;
        int seriesCount=series.size();
        float barW=(groupW*0.6f)/Math.max(1,seriesCount);
        float chartH=h-padBottom-padTop;
        for(int i=0;i<n;i++){
            float groupCenter=i*groupW+groupW/2f;
            float startX=groupCenter-(barW*seriesCount)/2f;
            for(int s=0;s<seriesCount;s++){
                float v=series.get(s)[i];
                float barH=Math.abs(v)/max*chartH;
                barPaint.setColor(colors[s%colors.length]);
                float left=startX+s*barW;
                float top=padTop+chartH-barH;
                canvas.drawRect(left, top, left+barW*0.85f, padTop+chartH, barPaint);
            }
            canvas.drawText(labels.get(i), groupCenter, h-4, textPaint);
        }
    }
}
