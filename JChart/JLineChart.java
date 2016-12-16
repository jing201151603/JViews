package com.dxhj.tianlang.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.dxhj.tianlang.utils.LogUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * author: 陈永镜 .
 * date: 2016/11/10 .
 * email: jing20071201@qq.com
 * <p>
 * introduce:线性统计图表
 */

public class JLineChart extends View {

    private Paint paint;
    private Paint paintShade;//阴影画笔
    private Paint paintEffect;//虚线画笔
    private Path path;//折线图的路径
    private boolean isShowShade = true;//默认是显示阴影
    private float width = dpToPx(300);
    private float height = dpToPx(200);
    private float padding = dpToPx(40);
    private int countX = 4, countY = 5;//x轴平均分为6等份，y轴平均分为5等份
    private List<JPoint> listData = new ArrayList<>();
    private float minY = 3, maxY = 0;//y轴的最小值和最大值
    private float unitY = 0.05f;//y轴上的间隔单位
    private String labelX = "月";
    private String labelY = "%";
    private Map<Integer, Integer> months = new TreeMap<>();
    private int lineColor = Color.BLACK;//线的颜色
    private int lineColorGray = Color.GRAY;//线的颜色
    private int lineChartColor = Color.RED;//折线的颜色、字体的颜色
    private int shadeColor = Color.parseColor("#55E92020");//阴影的颜色
    private boolean isShowInt = false;//Y轴是否显示整形

    public JLineChart(Context context) {
        this(context, null);
    }

    public JLineChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JLineChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //整体的画笔
        paint = new Paint();
        paint.setColor(lineColor);
        paint.setStrokeWidth(dpToPx(1));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setTextSize(dpToPx(10));
        paint.setTextAlign(Paint.Align.CENTER);

        //阴影画笔
        paintShade = new Paint();
        paintShade.setColor(shadeColor);
        paintShade.setStrokeWidth(dpToPx(1));
        paintShade.setStyle(Paint.Style.FILL);
        paintShade.setAntiAlias(true);

        //折线的路径
        path = new Path();

        //虚线画笔
        paintEffect = new Paint(paintShade);
        paintEffect.setColor(lineColorGray);
        paintEffect.setStyle(Paint.Style.STROKE);
        PathEffect effects = new DashPathEffect(new float[]{5, 5, 5, 5}, 1);
        paintEffect.setPathEffect(effects);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = measure(widthMeasureSpec);
        float height = measure(heightMeasureSpec);
        if (width <= 0) width = dpToPx(50);
        if (height <= 0) height = dpToPx(50);
        this.width = width - padding;
        this.height = height - padding;
        //调用该方法将测量后的宽和高设置进去，完成测量工作，
        setMeasuredDimension((int) width, (int) height);
    }

    /**
     * 测量宽和高
     *
     * @param widthMeasureSpec
     * @return      
     */
    private float measure(int widthMeasureSpec) {
        float result = 0;
        //从MeasureSpec对象中提取出来具体的测量模式和大小
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            //测量的模式，精确
            result = size;
        } else {
            result = width;
        }
        return result;
    }

    public void setDatas(List<JPoint> listData) {
        setDatas(listData, false);
    }

    public void setDatas(List<JPoint> listData, boolean isShowInt) {
        setDatas(listData, isShowInt, false);
    }

    /**
     * 默认显示月份
     *
     * @param listData
     * @param isShowInt
     * @param isYear    true 显示年份
     */
    public void setDatas(List<JPoint> listData, boolean isShowInt, boolean isYear) {
        if (isShowInt) LogUtils.w(getClass().getName(), "累计收益率：" + listData);
        else LogUtils.w(getClass().getName(), "净值走势：" + listData);
        if (listData.size() <= 0) return;
        maxY = minY = listData.get(0).getPointY();
        this.listData.clear();
        this.isShowInt = isShowInt;
        LogUtils.w(getClass().getName(), "listdata=" + listData.size());
        for (int i = listData.size() - 1; i >= 0; i--) {
            JPoint jPoint = listData.get(i);
            int key = 0;

            if (isYear) {//显示年份
                key = getYear(jPoint.getPointX());
                labelX = "年";
            } else {//显示月份
                key = getMonth(jPoint.getPointX());
                labelX = "月";
            }
            if (months.containsKey(key))//添加月份的个数
                months.put(key, months.get(key).intValue() + 1);
            else
                months.put(key, 1);
            float temp = jPoint.getPointY();
            if (temp > maxY) maxY = temp;
            if (temp < minY) minY = temp;
            this.listData.add(jPoint);
        }
        invalidate();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            paint.setStyle(Paint.Style.FILL);
            if (listData.size() == 0) {
                canvas.drawText("还没有任何数据哦", (width + padding) / 2, (height + padding) / 2, paint);
                return;
            }

            //绘画X轴和Y轴
            paint.setColor(lineColor);
            canvas.drawLine(padding, height, width, height, paint);//画X轴
            canvas.drawLine(padding, height, padding, padding, paint);//画Y轴
            paint.setColor(lineColorGray);
            int temp = dpToPx(3);
            //画Y轴上的箭头
            path.moveTo(padding, padding - temp);
            path.lineTo(temp + padding, temp + padding);
            path.lineTo(padding - temp, padding + temp);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            //画X轴上的箭头
            path.moveTo(width + temp, height);
            path.lineTo(width - temp, height + temp);
            path.lineTo(width - temp, height - temp);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();

            //设置最大值和最小值
            LogUtils.w(getClass().getName(), "max=" + maxY + ",min=" + minY);
            float maxY = this.maxY + (this.maxY - this.minY) / 16;//上线和下线空出4/16空间
            float minY = this.minY - (this.maxY - this.minY) / 16;
            countY = 4;//固定设置Y轴为4份
            String[] values = new String[5];
            values[0] = format2(minY + "");
            values[4] = format2(maxY + "");
            values[2] = format2((maxY + minY) / 2 + "");
            values[1] = format2((Float.valueOf(values[0]) + Float.valueOf(values[2])) / 2 + "");
            values[3] = format2((Float.valueOf(values[4]) + Float.valueOf(values[2])) / 2 + "");
            unitY = (maxY - minY) / countY;//计算item的高度对应的数值

            //Y坐标label、Y轴平分
            paint.setColor(lineChartColor);
            float itemY = (height - padding * 3 / 2) / (countY);//每等份的高度
            int location = 0;
            for (int item = 0; item < values.length; item++) {
                float pointX = padding / 2;
                float pointY = height - itemY * (location++);

                //文字
                if (isShowInt)
                    canvas.drawText(format(values[item] + "") + labelY, pointX, pointY, paint);
                else
                    canvas.drawText(format2(values[item] + "") + "", pointX, pointY, paint);

                //画线
                if (item > 0) {
                    Path path = new Path();
                    path.moveTo(padding, height - itemY * (item));
                    path.lineTo(width, height - itemY * (item));
                    canvas.drawPath(path, paintEffect);
                    path.reset();
                }
            }

            //绘画X轴的线和文字
            int total = 0;
            for (Integer value : months.values())
                total += value;
            countX = months.values().size();//X轴份几份
            float tempWidth = padding;
            Iterator<Integer> iterator = months.keySet().iterator();
            for (int i = 0; i < countX; i++) {
                int key = iterator.next();
                int value = months.get(key);
                float destance = (width - padding * 3 / 2) * value / total;
                tempWidth += destance;
                LogUtils.w(getClass().getName(), key + "月的个数是：" + value + ",共" + total + "个");

                //画线
                Path path = new Path();
                path.moveTo(tempWidth, height);
                path.lineTo(tempWidth, padding);
                canvas.drawPath(path, paintEffect);
                path.reset();

                //画文字
                canvas.drawText(key + labelX, (tempWidth - destance / 2), height + padding / 3, paint);
            }

            //添加折线图的路径
            float itemWidth = (width - padding * 3 / 2) / listData.size();
            for (int i = 0; i < listData.size(); i++) {
                JPoint jPoint = listData.get(i);

                float endX = padding + (i + 1) * itemWidth;
                float endY = (height - (jPoint.getPointY() - minY) / (unitY) * ((height - padding * 3 / 2) / countY));

                if (i + 1 < listData.size()) {
                    listData.get(i + 1).setLastY(endY);
                    listData.get(i + 1).setLastX(endX);
                }

                if (jPoint.getLastX() == 0 && jPoint.lastY == 0) {
                    path.moveTo(padding, endY);//该点是虚拟的点
                    path.lineTo(endX, endY);
                    continue;
                }
                path.lineTo(endX, endY);
            }
            //画折线图
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);

            //画阴影
            if (isShowShade) {
                path.lineTo(padding + (listData.size()) * itemWidth, height);//添加最后一点到垂直向下的线
                path.lineTo(padding, height);//添加原点
                path.close();//闭合折线
                canvas.drawPath(path, paintShade);//绘画阴影
            }
            path.reset();//重置


        } catch (NumberFormatException e) {
            e.printStackTrace();
        }


    }

    /**
     * Convert Dp to Pixel
     */
    private int dpToPx(float dp) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        return (int) px;
    }

    public class JPoint {
        private float pointY;
        private long pointX;
        private float lastX, lastY;

        public JPoint(long pointX, float pointY) {
            this.pointX = pointX;
            this.pointY = pointY;
        }

        public long getPointX() {
            return pointX;
        }

        public float getPointY() {
            return pointY;
        }

        public float getLastX() {
            return lastX;
        }

        public void setLastX(float lastX) {
            this.lastX = lastX;
        }

        public float getLastY() {
            return lastY;
        }

        public void setLastY(float lastY) {
            this.lastY = lastY;
        }


        public void setPointX(long pointX) {
            this.pointX = pointX;
        }

        public void setPointY(float pointY) {
            this.pointY = pointY;
        }


        @Override
        public String toString() {
            return "JPoint{" +
                    "pointX=" + pointX +
                    ", pointY=" + pointY +
                    ", lastX=" + lastX +
                    ", lastY=" + lastY +
                    '}';
        }
    }


    /**
     * 小数点后两位
     *
     * @param value
     * @return
     */
    private String format2(String value) {
        try {
            DecimalFormat decimalFormat = new DecimalFormat("##0.000");//构造方法的字符格式这里如果小数不足2位,会以0补足.
            return decimalFormat.format(Float.valueOf(value));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "0";
    }

    /**
     * 小数点后两位
     *
     * @param value
     * @return
     */
    public String format(String value) {
        try {
            DecimalFormat decimalFormat = new DecimalFormat("##0.0");//构造方法的字符格式这里如果小数不足2位,会以0补足.
            return decimalFormat.format(Float.valueOf(value));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "0";
    }

    /**
     * 将字符串转化成时间戳
     *
     * @param formatedStr
     * @return
     */
    private long getDate(String formatedStr) {
        long time = 0;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = formatter.parse(formatedStr);
            time = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    /**
     * 根据已格式化过的字符串获取月份
     *
     * @param date
     * @return
     */
    private int getMonth(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(date));
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取年份
     *
     * @param date
     * @return
     */
    private int getYear(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(date));
        return calendar.get(Calendar.YEAR);
    }

}
