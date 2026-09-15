package com.myassistant.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.List;

public class PieChartView extends View {
    private List<Float> values; private int[] colors;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean drawHole=false; private int holeColor=0;

    public PieChartView(Context c){ super(c); }

    public void setData(List<Float> values, int[] colors){ this.values=values; this.colors=colors; invalidate(); }
    public void setHoleColor(int c){ holeColor=c; drawHole=true; invalidate(); }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(values==null || values.isEmpty()) return;
        float total=0; for(float v:values) total+=v;
        if(total<=0) return;
        int size=Math.min(getWidth(), getHeight());
        float pad=8*getResources().getDisplayMetrics().density;
        RectF rect=new RectF((getWidth()-size)/2f+pad, (getHeight()-size)/2f+pad, (getWidth()+size)/2f-pad, (getHeight()+size)/2f-pad);
        float start=-90;
        for(int i=0;i<values.size();i++){
            float sweep=values.get(i)/total*360f;
            paint.setColor(colors[i%colors.length]);
            canvas.drawArc(rect, start, sweep, true, paint);
            start+=sweep;
        }
        if(drawHole){
            paint.setColor(holeColor);
            float holeR=size*0.28f;
            canvas.drawCircle(getWidth()/2f, getHeight()/2f, holeR, paint);
        }
    }
}
