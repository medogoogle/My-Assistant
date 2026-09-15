package com.myassistant.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.List;

public class LineChartView extends View {
    private List<Float> values;
    private final Paint linePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint=new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context c){
        super(c);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setColor(Color.CYAN);
        fillPaint.setColor(Color.CYAN);
        fillPaint.setAlpha(40);
    }

    public void setColor(int c){ linePaint.setColor(c); fillPaint.setColor(c); fillPaint.setAlpha(40); invalidate(); }
    public void setValues(List<Float> v){ values=v; invalidate(); }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(values==null || values.size()<2) return;
        int w=getWidth(), h=getHeight();
        float pad=8*getResources().getDisplayMetrics().density;
        float min=Float.MAX_VALUE, max=-Float.MAX_VALUE;
        for(float v:values){ min=Math.min(min,v); max=Math.max(max,v); }
        if(min==max){ min-=1; max+=1; }
        int n=values.size();
        float stepX=(w-2*pad)/(n-1);
        Path path=new Path(); Path fillPath=new Path();
        for(int i=0;i<n;i++){
            float x=pad+i*stepX;
            float y=pad+(h-2*pad)*(1-(values.get(i)-min)/(max-min));
            if(i==0){ path.moveTo(x,y); fillPath.moveTo(x,h-pad); fillPath.lineTo(x,y); }
            else { path.lineTo(x,y); fillPath.lineTo(x,y); }
        }
        fillPath.lineTo(pad+(n-1)*stepX, h-pad);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(path, linePaint);
    }
}
