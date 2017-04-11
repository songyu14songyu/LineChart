package sunrain.linechartlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * 自定义折线图
 * Created by songyu on 17/4/3.
 */
public class LineChartView extends View {
    //xy坐标轴颜色
    private int xylinecolor = 0xffe2e2e2;
    //xy坐标轴宽度
    private int xylinewidth = dpToPx(1);
    //xy坐标轴文字颜色
    private int xytextcolor = 0xff7e7e7e;
    //当前时间以前的字体颜色
    private int pastxtextcolor = 0x4dffffff;
    //xy坐标轴文字大小
    private int xytextsize = spToPx(12);
    //折线图中折线的颜色
    private int linecolor = 0xff02bbb7;
    //x轴各个坐标点水平间距
    private int interval = dpToPx(50);
    //背景颜色
    private int bgcolor = 0xffffffff;
    //y对应文字颜色
    private int ystringcolor = 0xffffffff;
    //y对应文字大小
    private int ytextsize = spToPx(14);
    //是否在ACTION_UP时，根据速度进行自滑动，没有要求，建议关闭，过于占用GPU
    private boolean isScroll = false;
    //绘制XY轴坐标对应的画笔
    private Paint xyPaint;
    //绘制XY轴的文本对应的画笔
    private Paint xyTextPaint;
    //画折线对应的画笔
    private Paint linePaint;
    private int width;
    private int height;
    //x轴的原点坐标
    private int xOri;//设置初始显示
    //y轴的原点坐标
    private int yOri;
    //第一个点X的坐标
    private float xInit;
    //第一个点对应的最大Y坐标
    private float maxXInit;
    //第一个点对应的最小X坐标
    private float minXInit;
    //x轴坐标对应的数据
    private List<String> xValue = new ArrayList<>();
    private List<String> xString = new ArrayList<>();
    //y轴坐标对应的数据
    private List<Integer> yValue = new ArrayList<>();
    //折线对应的数据
    private Map<String, Integer> value = new HashMap<>();
    //点击的点对应的X轴的第几个点，默认1
    private int selectIndex = 1;
    //X轴刻度文本对应的最大矩形，为了选中时，在x轴文本画的框框大小一致
    private Rect xValueRect;
    //速度检测器
    private VelocityTracker velocityTracker;
    //当前位置
    private int position;


    private List<String> yDegreeString = new ArrayList<String>();
    private List<Integer> yDegreeValue = new ArrayList<>();
    private List<Integer> pointColor = new ArrayList<>();

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
        initPaint();
    }

    /**
     * 初始化(外漏属性)
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.chartView, defStyleAttr, 0);
        int count = array.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = array.getIndex(i);
            if (attr == R.styleable.chartView_xylinecolor) {
                xylinecolor = array.getColor(attr, xylinecolor);//xy坐标轴颜色
            } else if (attr == R.styleable.chartView_xylinewidth) {//xy坐标轴宽度
                xylinewidth = (int) array.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, xylinewidth, getResources().getDisplayMetrics()));
            }else if (attr == R.styleable.chartView_xytextcolor) {//过去时间坐标轴文字颜色
                xytextcolor = array.getColor(attr, xytextcolor);
            }else if (attr == R.styleable.chartView_pastxtextcolor) {//xy坐标轴文字颜色
                pastxtextcolor = array.getColor(attr, pastxtextcolor);
            }else if (attr == R.styleable.chartView_xytextsize) {//xy坐标轴文字大小
                xytextsize = (int) array.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, xytextsize, getResources().getDisplayMetrics()));
            }else if (attr == R.styleable.chartView_ytextsize) {//xy坐标轴文字大小
                ytextsize = (int) array.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, ytextsize, getResources().getDisplayMetrics()));
            }else if (attr == R.styleable.chartView_linecolor) {//折线图中折线的颜色
                linecolor = array.getColor(attr, linecolor);
            }else if (attr == R.styleable.chartView_interval) {//x轴各个坐标点水平间距
                interval = (int) array.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, interval, getResources().getDisplayMetrics()));
            }else if (attr == R.styleable.chartView_bgcolor) { //背景颜色
                bgcolor = array.getColor(attr, bgcolor);
            }else if (attr == R.styleable.chartView_ystringcolor) {//y值对应的字体颜色
                ystringcolor = array.getColor(attr, ystringcolor);
            }else if (attr == R.styleable.chartView_isScroll) {//是否在ACTION_UP时，根据速度进行自滑动
                isScroll = array.getBoolean(attr, isScroll);
            }
        }
        array.recycle();

    }

    /**
     * 初始化画笔
     */
    private void initPaint() {
        xyPaint = new Paint();
        xyPaint.setAntiAlias(true);
        xyPaint.setStrokeWidth(xylinewidth);
        xyPaint.setStrokeCap(Paint.Cap.ROUND);
        xyPaint.setColor(xylinecolor);

        xyTextPaint = new Paint();
        xyTextPaint.setAntiAlias(true);
        xyTextPaint.setTextSize(xytextsize);
        xyTextPaint.setStrokeCap(Paint.Cap.ROUND);
        xyTextPaint.setColor(xytextcolor);
        xyTextPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth(xylinewidth);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(linecolor);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            //这里需要确定几个基本点，只有确定了xy轴原点坐标，第一个点的X坐标值及其最大最小值
            width = getWidth();
            height = getHeight();
            //Y轴文本最大宽度
            float textYWdith = getTextBounds("000", xyTextPaint).width();
            for (int i = 0; i < yValue.size(); i++) {//求取y轴文本最大的宽度
                float temp = getTextBounds(yValue.get(i) + "", xyTextPaint).width();
                if (temp > textYWdith)
                    textYWdith = temp;
            }
            int dp2 = dpToPx(2);
            int dp3 = dpToPx(3);
            xOri = 0;
           // xOri = (int) (dp2 + textYWdith + dp2 + xylinewidth);//dp2是y轴文本距离左边，以及距离y轴的距离
//            //X轴文本最大高度
            xValueRect = getTextBounds("现在", xyTextPaint);
            float textXHeight = xValueRect.height();
            for (int i = 0; i < xValue.size(); i++) {//求取x轴文本最大的高度
                Rect rect = getTextBounds(xValue.get(i) + "", xyTextPaint);
                if (rect.height() > textXHeight)
                    textXHeight = rect.height();
                if (rect.width() > xValueRect.width())
                    xValueRect = rect;
            }
            yOri = (int) (height - dp2 - textXHeight - dp3 - xylinewidth);//dp3是x轴文本距离底边，dp2是x轴文本距离x轴的距离
            xInit = interval + xOri;
            minXInit = width - (width - xOri) * 0.1f - interval * (xValue.size() - 1);//减去0.1f是因为最后一个X周刻度距离右边的长度为X轴可见长度的10%
            maxXInit = xInit;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        canvas.drawColor(bgcolor);
        drawXY(canvas);
        drawBrokenLineAndPoint(canvas);
    }

    /**
     * 绘制折线和折线交点处对应的点
     *
     * @param canvas
     */
    private void drawBrokenLineAndPoint(Canvas canvas) {
        if (xValue.size() <= 0)
            return;
        //重新开一个图层
        int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);

        drawScrollLine(canvas);
        drawBrokenPoint(canvas);

        // 将折线超出x轴坐标的部分截取掉
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setColor(bgcolor);
        linePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        RectF rectF = new RectF(0, 0, xOri, height);
        canvas.drawRect(rectF, linePaint);
        linePaint.setXfermode(null);
        //保存图层
        canvas.restoreToCount(layerId);
    }

    /**
     * 绘制折线对应的点
     *
     * @param canvas
     */
    private void drawBrokenPoint(Canvas canvas) {
        float dp2 = dpToPx(2);
        float dp4 = dpToPx(4);
        float dp7 = dpToPx(7);
        //绘制节点对应的原点
        for (int i = 0; i < xValue.size(); i++) {
            float x = xInit + interval * i;
            float y = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(i)) / yValue.get(yValue.size() - 1);
/*            //绘制选中的点
            if (i == selectIndex - 1) {
                linePaint.setStyle(Paint.Style.FILL);
                linePaint.setColor(0xffd0f3f2);
                canvas.drawCircle(x, y, dp7, linePaint);
                linePaint.setColor(0xff81dddb);
                canvas.drawCircle(x, y, dp4, linePaint);
                //drawFloatTextBox(canvas, x, y - dp7, value.get(xValue.get(i)));
            }*/
            //绘制普通的节点
/*            linePaint.setStyle(Paint.Style.FILL);
            if(value.get(xValue.get(i))>=30){
                linePaint.setColor(Color.RED);
            }else {
                linePaint.setColor(Color.BLUE);
            }

            canvas.drawCircle(x, y, dp2, linePaint);*/

            linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
/*            for(int j = 0; j < yDegreeValue.size(); j++){
                if(value.get(xValue.get(i))>=yDegreeValue.get(j)){
                    linePaint.setColor(pointColor.get(j));
                    break;
                }
            }*/
            linePaint.setColor(Color.WHITE);
/*            if(value.get(xValue.get(i))>=30){
                linePaint.setColor(Color.RED);
            }else {
                linePaint.setColor(Color.BLUE);
            }*/
            canvas.drawCircle(x, y, dp2, linePaint);
            drawFloatTextBox(canvas, x, y - dp7, changeFloatToText(value.get(xValue.get(i))));

        }
    }
    /**
     * Y值转换为文字
     */
    private String changeFloatToText(float text){
        Log.e("YValue",""+text);
        String airDegree = "";

/*        if(text >= 39){
            airDegree = "严重";
        }else if(text >= 36){
            airDegree = "重度";
        }else if(text >= 33){
            airDegree = "中度";
        }else if(text >= 30){
            airDegree = "轻度";
        }else if(text >= 27){
            airDegree = "良";
        }else{
            airDegree = "优";
        }*/

        for (int i = 0; i < yDegreeValue.size(); i++) {
            if(text >= yDegreeValue.get(i)){
               airDegree = yDegreeString.get(i);
                break;
            }
        }

        return  airDegree;
    }
    /**
     * 绘制显示Y值的浮动框
     *
     * @param canvas
     * @param x
     * @param y
     * @param text
     */
    private void drawFloatTextBox(Canvas canvas, float x, float y, String text) {
        int dp6 = dpToPx(6);
        int dp18 = dpToPx(18);
/*        //p1
        Path path = new Path();
        path.moveTo(x, y);
        //p2
        path.lineTo(x - dp6, y - dp6);
        //p3
        path.lineTo(x - dp18, y - dp6);
        //p4
        path.lineTo(x - dp18, y - dp6 - dp18);
        //p5
        path.lineTo(x + dp18, y - dp6 - dp18);
        //p6
        path.lineTo(x + dp18, y - dp6);
        //p7
        path.lineTo(x + dp6, y - dp6);
        //p1
        path.lineTo(x, y);
        canvas.drawPath(path, linePaint);*/
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);// 设置画笔
        textPaint.setTextSize(ytextsize);// 字体大小
        textPaint.setTypeface(Typeface.SANS_SERIF);// 采用sans_serif(细)
        textPaint.setColor(ystringcolor);// 采用的颜色

        linePaint.setColor(Color.BLUE);
        linePaint.setTextSize(ytextsize);
        Rect rect = getTextBounds(text + "", linePaint);
        canvas.drawText(text + "", x - rect.width() / 2, y - dp6 - (dp18 - rect.height()) / 2, textPaint);
    }

    /**
     * 绘制曲线
     * @param canvas
     */
    private void drawScrollLine(Canvas canvas)
    {
        float xs = xInit + interval * 0;
        float ys = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(0)) / yValue.get(yValue.size() - 1);
        float xe = xInit + interval * 1;
        float ye = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(1)) / yValue.get(yValue.size() - 1);
        int yLength = (int) (yOri * (1 - 0.1f) / (yValue.size() - 1));

        float ymax = yOri - yOri * (1 - 0.1f) * yDegreeValue.get(0) / yValue.get(yValue.size() - 1);
        float ymin = yOri - yOri * (1 - 0.1f) * yDegreeValue.get(yDegreeValue.size()-2) / yValue.get(yValue.size() - 1);

        for (int i = 0; i < xValue.size() - 1; i++)
        {
            xs = xInit + interval * i;
            ys = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(i)) / yValue.get(yValue.size() - 1);
            xe = xInit + interval * (i+1);
            ye = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(i+1)) / yValue.get(yValue.size() - 1);
            float wt = (xs + xe) / 2;
            Point p3 = new Point();
            Point p4 = new Point();
            p3.y = (int)ys;
            p3.x = (int)wt;
            p4.y = (int)ye;
            p4.x = (int)wt;

            Shader mShader = new LinearGradient(xs, ymax, xs,ymin,

                    integerListTointArray(pointColor),

                    null, Shader.TileMode.CLAMP);

/*             Shader.TileMode三种模式

             REPEAT:沿着渐变方向循环重复

             CLAMP:如果在预先定义的范围外画的话，就重复边界的颜色

             MIRROR:与REPEAT一样都是循环重复，但这个会对称重复*/

            linePaint.setShader(mShader);// 用Shader中定义定义的颜色来话
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setColor(linecolor);


            Path path = new Path();
            path.moveTo(xs, ys);
            path.cubicTo(p3.x, p3.y, p4.x, p4.y, xe, ye);
            //canvas.drawRect(0, 0, yLength, yOri + xylinewidth / 2, linePaint);
            canvas.drawPath(path, linePaint);
        }
    }

    /**
     * 绘制折线（直线）
     *
     * @param canvas
     */
    private void drawBrokenLine(Canvas canvas) {


        //绘制折线
        Path path = new Path();
        float x = xInit + interval * 0;
        float y = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(0)) / yValue.get(yValue.size() - 1);
        path.moveTo(x, y);
        for (int i = 0; i < xValue.size(); i++) {
            x = xInit + interval * i;
            y = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(i)) / yValue.get(yValue.size() - 1);
            path.lineTo(x, y);

        }

        Shader mShader = new LinearGradient(x, y, 400+x, 400+y,

                new int[] { Color.RED,  Color.YELLOW,Color.GREEN, Color.BLUE},

                null, Shader.TileMode.MIRROR);

        // Shader.TileMode三种模式

        // REPEAT:沿着渐变方向循环重复

        // CLAMP:如果在预先定义的范围外画的话，就重复边界的颜色

        // MIRROR:与REPEAT一样都是循环重复，但这个会对称重复

        linePaint.setShader(mShader);// 用Shader中定义定义的颜色来话
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(linecolor);


        canvas.drawPath(path, linePaint);
    }

    /**
     * 绘制XY坐标
     *
     * @param canvas
     */
    private void drawXY(Canvas canvas) {
        int length = dpToPx(4);//刻度的长度
/*        //绘制Y坐标
        canvas.drawLine(xOri - xylinewidth / 2, 0, xOri - xylinewidth / 2, yOri, xyPaint);
        //绘制y轴箭头
        xyPaint.setStyle(Paint.Style.STROKE);*/
        Path path = new Path();
/*        path.moveTo(xOri - xylinewidth / 2 - dpToPx(5), dpToPx(12));
        path.lineTo(xOri - xylinewidth / 2, xylinewidth / 2);
        path.lineTo(xOri - xylinewidth / 2 + dpToPx(5), dpToPx(12));
        canvas.drawPath(path, xyPaint);
        //绘制y轴刻度
        int yLength = (int) (yOri * (1 - 0.1f) / (yValue.size() - 1));//y轴上面空出10%,计算出y轴刻度间距
        for (int i = 0; i < yValue.size(); i++) {
            //绘制Y轴刻度
            canvas.drawLine(xOri, yOri - yLength * i + xylinewidth / 2, xOri + length, yOri - yLength * i + xylinewidth / 2, xyPaint);
            xyTextPaint.setColor(xytextcolor);
            //绘制Y轴文本
            String text = yValue.get(i) + "";
            Rect rect = getTextBounds(text, xyTextPaint);
            canvas.drawText(text, 0, text.length(), xOri - xylinewidth - dpToPx(2) - rect.width(), yOri - yLength * i + rect.height() / 2, xyTextPaint);
        }*/

        //绘制X轴坐标
        canvas.drawLine(xOri, yOri + xylinewidth / 2, width, yOri + xylinewidth / 2, xyPaint);
/*        //绘制x轴箭头
        xyPaint.setStyle(Paint.Style.STROKE);
        path = new Path();
        //整个X轴的长度
        float xLength = xInit + interval * (xValue.size() - 1) + (width - xOri) * 0.1f;
        if (xLength < width)
            xLength = width;
        path.moveTo(xLength - dpToPx(12), yOri + xylinewidth / 2 - dpToPx(5));
        path.lineTo(xLength - xylinewidth / 2, yOri + xylinewidth / 2);
        path.lineTo(xLength - dpToPx(12), yOri + xylinewidth / 2 + dpToPx(5));
        canvas.drawPath(path, xyPaint);*/
        //绘制x轴刻度
        for (int i = 0; i < xValue.size(); i++) {
            float x = xInit + interval * i;
            if (x >= xOri) {//只绘制从原点开始的区域
                if(i < position){
                    xyTextPaint.setColor(pastxtextcolor);
                }else {
                    xyTextPaint.setColor(xytextcolor);
                }
                canvas.drawLine(x, yOri, x, yOri - length, xyPaint);
                //绘制X轴文本
                String text = xString.get(i);
                Rect rect = getTextBounds(text, xyTextPaint);
//                if (i == selectIndex - 1) {
//                    xyTextPaint.setColor(linecolor);
//                    canvas.drawText(text, 0, text.length(), x - rect.width() / 2, yOri + xylinewidth + dpToPx(2) + rect.height(), xyTextPaint);
//                    canvas.drawRoundRect(x - xValueRect.width() / 2 - dpToPx(3), yOri + xylinewidth + dpToPx(1), x + xValueRect.width() / 2 + dpToPx(3), yOri + xylinewidth + dpToPx(2) + xValueRect.height() + dpToPx(2), dpToPx(2), dpToPx(2), xyTextPaint);
//                } else {
                    canvas.drawText(text, 0, text.length(), x - rect.width() / 2, yOri + xylinewidth + dpToPx(2) + rect.height(), xyTextPaint);
  //              }
            }
        }
    }

    private float startX;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isScrolling)
            return super.onTouchEvent(event);
        this.getParent().requestDisallowInterceptTouchEvent(true);//当该view获得点击事件，就请求父控件不拦截事件
        obtainVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (interval * xValue.size() > width - xOri) {//当期的宽度不足以呈现全部数据
                    float dis = event.getX() - startX;
                    startX = event.getX();
                    if (xInit + dis < minXInit) {
                        xInit = minXInit;
                    } else if (xInit + dis > maxXInit) {
                        xInit = maxXInit;
                    } else {
                        xInit = xInit + dis;
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                clickAction(event);
                scrollAfterActionUp();
                this.getParent().requestDisallowInterceptTouchEvent(false);
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_CANCEL:
                this.getParent().requestDisallowInterceptTouchEvent(false);
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    //是否正在滑动
    private boolean isScrolling = false;

    /**
     * 手指抬起后的滑动处理
     */
    private void scrollAfterActionUp() {
        if (!isScroll)
            return;
        final float velocity = getVelocity();
        float scrollLength = maxXInit - minXInit;
        if (Math.abs(velocity) < 10000)//10000是一个速度临界值，如果速度达到10000，最大可以滑动(maxXInit - minXInit)
            scrollLength = (maxXInit - minXInit) * Math.abs(velocity) / 10000;
        ValueAnimator animator = ValueAnimator.ofFloat(0, scrollLength);
        animator.setDuration((long) (scrollLength / (maxXInit - minXInit) * 1000));//时间最大为1000毫秒，此处使用比例进行换算
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                if (velocity < 0&& xInit > minXInit) {//向左滑动
                    if (xInit - value <= minXInit)
                        xInit = minXInit;
                    else
                        xInit = xInit - value;
                } else if (velocity > 0 && xInit < maxXInit) {//向右滑动
                    if (xInit + value >= maxXInit)
                        xInit = maxXInit;
                    else
                        xInit = xInit + value;
                }
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isScrolling = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isScrolling = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isScrolling = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();

    }

    /**
     * 获取速度
     *
     * @return
     */
    private float getVelocity() {
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000);
            return velocityTracker.getXVelocity();
        }
        return 0;
    }

    /**
     * 点击X轴坐标或者折线节点
     *
     * @param event
     */
    private void clickAction(MotionEvent event) {
        int dp8 = dpToPx(8);
        float eventX = event.getX();
        float eventY = event.getY();
        for (int i = 0; i < xValue.size(); i++) {
            //节点
            float x = xInit + interval * i;
            float y = yOri - yOri * (1 - 0.1f) * value.get(xValue.get(i)) / yValue.get(yValue.size() - 1);
            if (eventX >= x - dp8 && eventX <= x + dp8 &&
                    eventY >= y - dp8 && eventY <= y + dp8 && selectIndex != i + 1) {//每个节点周围8dp都是可点击区域
                selectIndex = i + 1;
                invalidate();
                return;
            }
            //X轴刻度
            String text = xValue.get(i);
            Rect rect = getTextBounds(text, xyTextPaint);
            x = xInit + interval * i;
            y = yOri + xylinewidth + dpToPx(2);
            if (eventX >= x - rect.width() / 2 - dp8 && eventX <= x + rect.width() + dp8 / 2 &&
                    eventY >= y - dp8 && eventY <= y + rect.height() + dp8 && selectIndex != i + 1) {
                selectIndex = i + 1;
                invalidate();
                return;
            }
        }
    }


    /**
     * 获取速度跟踪器
     *
     * @param event
     */
    private void obtainVelocityTracker(MotionEvent event) {
        if (!isScroll)
            return;
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    /**
     * 回收速度跟踪器
     */
    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    public int getSelectIndex() {
        return selectIndex;
    }

    public void setSelectIndex(int selectIndex) {
        this.selectIndex = selectIndex;
        invalidate();
    }

    public void setxValue(List<String> xValue) {
        this.xValue = xValue;
    }

    public void setyValue(List<Integer> yValue) {
        this.yValue = yValue;
        invalidate();
    }

    public void setValue(Map<String, Integer> value) {
        this.value = value;
        invalidate();
    }

    public void setValue(Map<String, Integer> value, List<String> xValue, List<Integer> yValue, List<String> xString,List<Integer> yDegreeValue,List<String> yDegreeString,List<Integer> pointColor) {
        this.value = value;
        this.xValue = xValue;
        this.yValue = yValue;
        this.xString = xString;
        this.yDegreeValue = yDegreeValue;
        this.yDegreeString = yDegreeString;
        this.pointColor = pointColor;
        invalidate();
    }

    public List<String> getxValue() {
        return xValue;
    }

    public List<Integer> getyValue() {
        return yValue;
    }

    public Map<String, Integer> getValue() {
        return value;
    }

    /**
     * 获取丈量文本的矩形
     *
     * @param text
     * @param paint
     * @return
     */
    private Rect getTextBounds(String text, Paint paint) {
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * dp转化成为px
     *
     * @param dp
     * @return
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f * (dp >= 0 ? 1 : -1));
    }
    /**
     * Integer[]转换为int[]
     */
    private int[] integerListTointArray(List<Integer> pointColor){
        Integer[] integers = (Integer[])pointColor.toArray(new Integer[pointColor.size()]);
        int[] intArray = new int[integers.length];
        for(int i=0; i < integers.length; i ++)
        {
            intArray[i] = integers[i].intValue();
        }
        return intArray;
    }
    /**
     * sp转化为px
     *
     * @param sp
     * @return
     */
    private int spToPx(int sp) {
        float scaledDensity = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (scaledDensity * sp + 0.5f * (sp >= 0 ? 1 : -1));
    }

    /**
     * 设置当前位置
     * @param position
     */
    public void setCurrentPosition(int position){
        this.position = position;
        xInit = - interval * (position - 4);
        invalidate();
    }

    /**
     * 设置x轴间距
     * @param interval
     */
    public void setInterval(int interval){
        this.interval = interval;
    }
}

