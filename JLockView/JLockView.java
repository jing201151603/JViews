package com.dxhj.tianlang.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.dxhj.tianlang.MainApplication;
import com.dxhj.tianlang.dao.UserInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 陈永镜 on 2016/9/1.
 */
public class JLockView extends View {

    final static String ANDROIDXML = "http://schemas.android.com/apk/res/android";

    private int width, height;
    private static final int count = 3;
    private int radiusBig = 30;
    private int radiusSmall = 10;
    /**
     * 存放九宫格
     */
    private Point points[][] = new Point[count][count];
    private Paint smallCirclePaint;
    private Paint bigCirclePaint;

    private int circleWidth = 2;
    private static final int defaultBgColor = Color.parseColor("#ffffff");//默认的背景颜色
    private static final int defaultSelectCircleColor = Color.parseColor("#0EAFEF");//默认选中的圆的颜色
    private static final int defaultNormalColor = Color.parseColor("#757C85");//默认正常的颜色
    private static final int defaultIncorrectColor = Color.RED;//默认手势密码输入错误的颜色

    /**
     * 设置选中的圆的颜色
     */
    private int selectCircleColor = defaultSelectCircleColor;
    /**
     * 整个控件的颜色
     */
    private int backgroundColor = defaultBgColor;
    /**
     * 手滑动的线的颜色
     */
    private int lineColor = defaultSelectCircleColor;
    /**
     * 大圆的颜色
     */
    private int bigCircleColor = defaultNormalColor;
    /**
     * 小圆的颜色
     */
    private int smallCircleColor = defaultNormalColor;
    /**
     * 手势密码输入错误时圆的颜色
     */
    private int incorrectColor = defaultIncorrectColor;

    private Paint linePaint;
    private int lineWidth = 2;
    private float startX;
    private float startY;
    private float currentX;
    private float currentY;
    private List<Point> pointList = new ArrayList<>();//存储所有line的点
    private List<Point> selectPointList = new ArrayList<>();//存储已选中的点
    private SharedPreferences sharedPreferences;
    private static final String length = "length";
    private static final String pointX = "pointX";
    private static final String pointY = "pointY";
    private static final String position = "position";
    private int pwdLength = 0;//存储的密码长度
    private List<Point> pwdPoints = new ArrayList<>();//存放原始密码
    private int requestPwdLength = 4;//密码长度最低为4
    private PwdType pwdType = PwdType.setPwd;//默认为创建密码
    private boolean isEnable = true;//是否可用
    private boolean showLine = true;//是否显示线
    private boolean isRight = true;//判断密码输入是否正确，默认是正确的
    private boolean isUp = false;//判断绘画是否完成，完成后最后选中的点往后的线删除
    float lastX = 0, lastY = 0;
    private Point currentPoints[][];//当密码输入错误时，用于临时放置选中密码

    private int interval;//圆与圆之间的间隔

    public JLockView(Context context) {
        this(context, null);
    }

    public JLockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public JLockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        backgroundColor = attrs.getAttributeIntValue(ANDROIDXML, "background", 0);

        sharedPreferences = MainApplication.getInstance().getSharedPreferences();
        pwdLength = sharedPreferences.getInt(length, 0);
        if (pwdLength > 0) {
            pwdType = PwdType.inputPwd;
            for (int i = 0; i < pwdLength; i++) {
                Point point = new Point(sharedPreferences.getFloat(pointX + i, 0), sharedPreferences.getFloat(pointY + i, 0));
                point.setPosition(sharedPreferences.getInt(position + i, 0));
                pwdPoints.add(point);
            }
        }

        bigCirclePaint = new Paint();
        bigCirclePaint.setColor(bigCircleColor);
        bigCirclePaint.setStyle(Paint.Style.STROKE);
        bigCirclePaint.setAntiAlias(true);
        bigCirclePaint.setStrokeWidth(circleWidth);

        smallCirclePaint = new Paint();
        smallCirclePaint.setColor(smallCircleColor);
        smallCirclePaint.setStyle(Paint.Style.STROKE);
        smallCirclePaint.setAntiAlias(true);
        smallCirclePaint.setStrokeWidth(circleWidth);

        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        lineWidth = dip2px(5);
        linePaint.setStrokeWidth(lineWidth);

        setBackgroundColor(backgroundColor);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            initSelectCircleColor();

            drawCircle(canvas);

            while (pointList.size() > 0) {
                if (getPosition(pointList.get(0).getPointX(), pointList.get(0).getPointY()) > 0)
                    break;
                else pointList.remove(0);
            }

            //绘画线
            drawLines(canvas);

            if (!isRight) handler.sendEmptyMessageDelayed(100, 100);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initSelectCircleColor() {
        if (!isRight) {
            points = currentPoints;
            selectCircleColor = incorrectColor;
        } else
            selectCircleColor = defaultSelectCircleColor;
    }

    private void drawLines(Canvas canvas) {
        int sise = pointList.size();
        if (sise > 1 && showLine && !isUp) {//当绘画未完成
            Point pointNow = pointList.get(sise - 1);
            canvas.drawLine(startX, startY, pointNow.getPointX(), pointNow.getPointY(), linePaint);

            lastX = 0;
            lastY = 0;
            if (selectPointList.size() > 0) {
                chooseLineColor();
                for (int i = 0; i < selectPointList.size(); i++) {
                    Point point = selectPointList.get(i);
                    if (i > 0) {
                        point.setLastPointX(lastX);
                        point.setLastPointY(lastY);
                        canvas.drawLine(point.getLastPointX(), point.getLastPointY(), point.getPointX(), point.getPointY(), linePaint);
                    }
                    lastX = point.getPointX();
                    lastY = point.getPointY();
                    startX = point.getPointX();
                    startY = point.getPointY();
                }
            }
        } else if (sise > 1 && showLine && isUp) {//当绘画已经完成
            if (selectPointList.size() > 0) {
                chooseLineColor();
                for (int i = 1; i < selectPointList.size(); i++) {
                    Point pointLast = selectPointList.get(i - 1);
                    Point point = selectPointList.get(i);
                    canvas.drawLine(pointLast.getPointX(), pointLast.getPointY(), point.getPointX(), point.getPointY(), linePaint);
                }
            }
        }
    }

    private void drawCircle(Canvas canvas) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                Point point = getPoint(i, j);
                if (point.isSelect) {//选中情况下
                    smallCirclePaint.setStyle(Paint.Style.FILL);
                    smallCirclePaint.setColor(selectCircleColor);
                    bigCirclePaint.setColor(selectCircleColor);
                    canvas.drawCircle(point.getPointX(), point.getPointY(), radiusSmall, smallCirclePaint);//画小圆
                    canvas.drawCircle(point.getPointX(), point.getPointY(), radiusBig, bigCirclePaint);//画大圆
                } else {//没选中情况下
                    smallCirclePaint.setStyle(Paint.Style.STROKE);
                    smallCirclePaint.setColor(smallCircleColor);
                    bigCirclePaint.setColor(smallCircleColor);
                    canvas.drawCircle(point.getPointX(), point.getPointY(), radiusSmall, smallCirclePaint);//画小圆
                    canvas.drawCircle(point.getPointX(), point.getPointY(), radiusBig, bigCirclePaint);//画大圆
                }
            }
        }
    }

    private Point getPoint(int i, int j) {
        Point point = points[i][j];
        if (point == null) {
            points[i][j] = new Point(width / 2 + (interval * i - interval), height / 2 + (interval * j - interval));
            point = points[i][j];
        }
        return point;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    isRight = !isRight;
                    clearLines();
                    break;
            }
        }
    };


    private void chooseLineColor() {
        if (!isRight) linePaint.setColor(Color.RED);
        else linePaint.setColor(lineColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
        interval = width / 4;
        radiusBig = interval / 3;
        radiusSmall = radiusBig / 3;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) return true;
        currentX = event.getX();
        currentY = event.getY();
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isUp = false;
                    currentPoints = null;
                    currentPoints = new Point[count][count];
                    break;
                case MotionEvent.ACTION_MOVE:
                    addPointForLine();

                    break;
                case MotionEvent.ACTION_UP:
                    clear();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    clear();
                    break;
            }
            postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 添加选中点
     */
    private void addPointForLine() {
        int position = getPosition(currentX, currentY);
        if (position == 0) return;

        startX = currentX;
        startY = currentY;

        judgeCircleIsSelect(position);
    }

    /**
     * 判断该圆是否被选中
     */
    private void judgeCircleIsSelect(int position) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                Point point = getPoint(i, j);
                if (isIncluding(currentX, currentY, point) && !point.isSelect) {
                    point.setSelect(true);
                    point.setPosition(position);
                    warmLog(getClass().getName(), "currentPoint=" + point);
                    selectPointList.add(point);
                    pointList.add(point);

                }
                currentPoints[i][j] = point;
            }
        }
    }

    private void clear() {
        isUp = true;
        if (onJLockListener == null) {
            clearLines();
            return;
        }
        long delayTime = 300;
        if (isUp) delayTime = 0;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (judgePwd()) return;
                clearLines();
            }
        };

        new Handler() {
        }.postDelayed(runnable, delayTime);

    }

    /**
     * 判断是不是密码
     *
     * @return true:不是密码
     */
    private boolean judgePwd() {
        if (selectPointList.size() <= 0) return true;
        if (selectPointList.size() < requestPwdLength) {//密码长度不足
            onJLockListener.onShort();
            if (pwdType != PwdType.inputPwd)
                pwdType = PwdType.setPwd;
        } else {
            warmLog(getClass().getName(), "selectPointList=" + selectPointList.toString());

            switch (pwdType) {
                case setPwd://设置密码操作

                    if (pwdPoints.size() > 0) pwdPoints.clear();
                    pwdPoints.addAll(selectPointList);
                    pwdType = PwdType.rePwd;
                    onJLockListener.onReset();
                    break;
                case rePwd://确认密码

                    if (pwdPoints.size() != selectPointList.size()) {//两次输入的长度不一致
                        inputIncorrect();
                    } else {//两次输入密码的长度一致
                        if (isSame(pwdPoints, selectPointList)) {//密码一致
                            onJLockListener.onCreateSucceed();//密码创建成功
                            MainApplication.getInstance().getUserInfo().save(UserInfo.Type.lock_enable_time, "");//设置锁住手势密码的时间为空，确保手势密码可用
                            samePwd(pwdPoints);//储存密码
                        } else {
                            inputIncorrect();
                        }
                    }
                    break;
                case inputPwd://输入手势密码

                    if (pwdPoints.size() == selectPointList.size() && isSame(pwdPoints, selectPointList)) {//长度一致并且任意一集合包含另一集合时，密码正确
                        onJLockListener.onInputPwd(true);
                    } else {
                        isRight = false;//密码输入错误，标志绘画红线
                        onJLockListener.onInputPwd(false);
                    }
                    break;
            }

        }
        return false;
    }

    /**
     * 密码输入错误
     */
    private void inputIncorrect() {
        onJLockListener.onDifferent();//两次密码输入不一致
        pwdType = PwdType.setPwd;
        pwdPoints.clear();
    }

    /**
     * 保存密码
     *
     * @param pwdPoints
     */
    private void samePwd(List<Point> pwdPoints) {
        warmLog(getClass().getName(), "pwdPoints=" + pwdPoints.toString());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int size = pwdPoints.size();
        editor.putInt(length, size);
        for (int i = 0; i < size; i++) {
            Point point = pwdPoints.get(i);
            editor.putFloat(pointX + i, point.getPointX());
            editor.putFloat(pointY + i, point.getPointY());
            editor.putInt(position + i, point.getPosition());
        }
        editor.commit();
        pwdType = PwdType.inputPwd;
    }

    /**
     * 显示线
     *
     * @param
     */
    public void showLine() {
        showLine = true;
    }

    /**
     * 隐藏线
     */
    public void hideLine() {
        showLine = false;
    }

    /**
     * 清除线和选中的圆，只留下九宫格
     */
    private void clearLines() {
        if (isRight) {
            pointList.clear();
            selectPointList.clear();
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < count; j++) {

                    Point point = points[i][j];
                    if (point == null) {
                        points[i][j] = getPoint(i, j);
                    }
                    points[i][j].setSelect(false);
                }
            }
        }
        postInvalidate();
    }

    /**
     * 暴露出去的方法，用于创建手势密码
     */
    public void createPwd() {
        if (recoverPwdList.size() > 0) recoverPwdList.clear();
        getPwds(recoverPwdList);//清除前保存在缓存中，方便事件回滚

        sharedPreferences.edit().putInt(length, 0).commit();
        pwdPoints.clear();
        selectPointList.clear();
        pointList.clear();
        pwdType = PwdType.setPwd;
    }

    private void getPwds(List<Point> pwdPoints) {
        for (int i = 0; i < getLength(); i++) {
            Point point = new Point(sharedPreferences.getFloat(pointX + i, 0), sharedPreferences.getFloat(pointY + i, 0));
            point.setPosition(sharedPreferences.getInt(position + i, 0));
            pwdPoints.add(point);
        }
    }

    public int getLength() {
        return sharedPreferences.getInt(length, 0);
    }

    /**
     * 保存需要恢复的密码
     */
    private List<Point> recoverPwdList = new ArrayList<>();

    /**
     * 恢复原来的密码：当创建密码失败后事件回滚
     */
    public void recover() {
        samePwd(recoverPwdList);
    }

    public PwdType getPwdType() {
        return pwdType;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    private boolean isSame(List<Point> points1, List<Point> points2) {
        int size = points1.size();
        for (int i = 0; i < size; i++) {
            if (points1.get(i).getPosition() != points2.get(i).getPosition())
                return false;
        }
        return true;
    }

    public interface OnJLockListener {
        void onShort();//密码长度不足

        void onCreateSucceed();//密码设置成功

        void onDifferent();//两次密码不一致

        void onInputPwd(boolean result);//输入密码

        void onReset();//再次输入手势密码
    }

    private OnJLockListener onJLockListener;

    public void setOnJLockListener(OnJLockListener onJLockListener) {
        this.onJLockListener = onJLockListener;
    }

    public enum PwdType {
        setPwd, rePwd, inputPwd
    }

    /**
     * 判断（currentX，currentY）是否在point点中
     *
     * @param currentX
     * @param currentY
     * @param point
     * @return
     */
    private boolean isIncluding(float currentX, float currentY, Point point) {
        float distanceX = Math.abs(currentX - point.getPointX());
        float distanceY = Math.abs(currentY - point.getPointY());
        int distanceZ = (int) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
        if (distanceZ <= radiusBig) {//在圆内
            this.currentX = point.getPointX();
            this.currentY = point.getPointY();
            return true;
        }
        return false;
    }

    /**
     * @param currentX
     * @param currentY
     * @return 当position的值大于0时，表示当前的点在九宫格中
     */
    private int getPosition(float currentX, float currentY) {
        int position = 0;
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                Point point = getPoint(i, j);
                float distanceX = Math.abs(currentX - point.getPointX());
                float distanceY = Math.abs(currentY - point.getPointY());
                int distanceZ = (int) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
                if (distanceZ <= radiusBig) {//在圆内
                    position = 3 * j + i + 1;
                }
            }

        }

        return position;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void warmLog(String tag, String msg) {
        Log.w(tag, msg);
    }


    class Point {
        float pointX = 0, lastPointX = 0;
        float pointY = 0, lastPointY = 0;
        boolean isSelect = false;
        protected int position;

        public Point(float pointX, float pointY) {
            this.pointX = pointX;
            this.pointY = pointY;
        }

        public float getPointX() {
            return pointX;
        }

        public void setPointX(float pointX) {
            this.pointX = pointX;
        }

        public float getPointY() {
            return pointY;
        }

        public void setPointY(float pointY) {
            this.pointY = pointY;
        }

        public boolean isSelect() {
            return isSelect;
        }

        public void setSelect(boolean select) {
            isSelect = select;
        }

        public float getLastPointX() {
            return lastPointX;
        }

        public void setLastPointX(float lastPointX) {
            this.lastPointX = lastPointX;
        }

        public float getLastPointY() {
            return lastPointY;
        }

        public void setLastPointY(float lastPointY) {
            this.lastPointY = lastPointY;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "pointX=" + pointX +
                    ", lastPointX=" + lastPointX +
                    ", pointY=" + pointY +
                    ", lastPointY=" + lastPointY +
                    ", isSelect=" + isSelect +
                    ", position=" + position +
                    '}';
        }
    }

}
