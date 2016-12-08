package com.dxhj.tianlang.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by 陈永镜 on 2016/9/21.
 */

class JBaseImageView extends ImageView {
    final static String ANDROIDXML = "http://schemas.android.com/apk/res/android";

    public JBaseImageView(Context context) {
        super(context);
    }

    public JBaseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JBaseImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


}
