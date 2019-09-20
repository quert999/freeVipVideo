package com.fvip.cd.fvipplayer.widget;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;

public class DragFloatingActionButton extends FloatingActionButton {

    private float mLastRawX;
    private float mLastRawY;
    private final String TAG = "AttachButton";
    private boolean isDrug = false;
    private int mRootMeasuredWidth;

    public DragFloatingActionButton(Context context) {
        this(context, null);
    }

    public DragFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //当前手指的坐标
        float mRawX = ev.getRawX();
        float mRawY = ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN://手指按下
                isDrug = false;
                //记录按下的位置
                mLastRawX = mRawX;
                mLastRawY = mRawY;
                break;
            case MotionEvent.ACTION_MOVE://手指滑动
                ViewGroup mViewGroup = (ViewGroup) getParent();
                if (mViewGroup != null) {
                    int[] location = new int[2];
                    mViewGroup.getLocationInWindow(location);
                    //获取父布局的高度
                    int mRootMeasuredHeight = mViewGroup.getMeasuredHeight();
                    mRootMeasuredWidth = mViewGroup.getMeasuredWidth();
                    //获取父布局顶点的坐标
                    int mRootTopY = location[1];
                    if (mRawX >= 0 && mRawX <= mRootMeasuredWidth && mRawY >= mRootTopY && mRawY <= (mRootMeasuredHeight + mRootTopY)) {
                        //手指X轴滑动距离
                        float differenceValueX = mRawX - mLastRawX;
                        //手指Y轴滑动距离
                        float differenceValueY = mRawY - mLastRawY;
                        //判断是否为拖动操作
                        if (!isDrug) {
                            if (Math.sqrt(differenceValueX * differenceValueX + differenceValueY * differenceValueY) < 2) {
                                isDrug = false;
                            } else {
                                isDrug = true;
                            }
                        }
                        //获取手指按下的距离与控件本身X轴的距离
                        float ownX = getX();
                        //获取手指按下的距离与控件本身Y轴的距离
                        float ownY = getY();
                        //理论中X轴拖动的距离
                        float endX = ownX + differenceValueX;
                        //理论中Y轴拖动的距离
                        float endY = ownY + differenceValueY;
                        //X轴可以拖动的最大距离
                        float maxX = mRootMeasuredWidth - getWidth();
                        //Y轴可以拖动的最大距离
                        float maxY = mRootMeasuredHeight - getHeight();
                        //X轴边界限制
                        endX = endX < 0 ? 0 : endX > maxX ? maxX : endX;
                        //Y轴边界限制
                        endY = endY < 0 ? 0 : endY > maxY ? maxY : endY;
                        //开始移动
                        setX(endX);
                        setY(endY);
                        //记录位置
                        mLastRawX = mRawX;
                        mLastRawY = mRawY;
                    }
                }
                break;
            case MotionEvent.ACTION_UP://手指离开
                float center = mRootMeasuredWidth / 2;
                //自动贴边
                if (mLastRawX <= center) {
                    //向左贴边
                    DragFloatingActionButton.this.animate()
                            .setInterpolator(new BounceInterpolator())
                            .setDuration(500)
                            .x(0)
                            .start();
                } else {
                    //向右贴边
                    DragFloatingActionButton.this.animate()
                            .setInterpolator(new BounceInterpolator())
                            .setDuration(500)
                            .x(mRootMeasuredWidth - getWidth())
                            .start();
                }
                break;
        }
        //是否拦截事件
        return isDrug ? isDrug : super.onTouchEvent(ev);
    }
}