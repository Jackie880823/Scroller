/*
 *    Copyright 2016 The Open Source Project of Jackie Zhu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *             $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
 *             $                                                   $
 *             $                       _oo0oo_                     $
 *             $                      o8888888o                    $
 *             $                      88" . "88                    $
 *             $                      (| -_- |)                    $
 *             $                      0\  =  /0                    $
 *             $                    ___/`---'\___                  $
 *             $                  .' \\|     |$ '.                 $
 *             $                 / \\|||  :  |||$ \                $
 *             $                / _||||| -:- |||||- \              $
 *             $               |   | \\\  -  $/ |   |              $
 *             $               | \_|  ''\---/''  |_/ |             $
 *             $               \  .-\__  '-'  ___/-. /             $
 *             $             ___'. .'  /--.--\  `. .'___           $
 *             $          ."" '<  `.___\_<|>_/___.' >' "".         $
 *             $         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       $
 *             $         \  \ `_.   \_ __\ /__ _/   .-` /  /       $
 *             $     =====`-.____`.___ \_____/___.-`___.-'=====    $
 *             $                       `=---='                     $
 *             $     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   $
 *             $                                                   $
 *             $          Buddha bless         Never BUG           $
 *             $                                                   $
 *             $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
 */

package com.jackie.refresh;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.*;
import com.jackie.refresh.listener.OnLoadListener;
import com.jackie.refresh.listener.OnRefreshListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by on 16/5/11.
 *
 * @param <T>
 * @author Jackie Zhu
 * @version 1.0
 */
public abstract class RefreshLayoutBase<T extends View> extends ViewGroup implements AbsListView
        .OnScrollListener {
    private static final String TAG = "RefreshLayoutBase";

    /**
     * 内容视图,即用户触摸导致下拉刷新,上拉加载的主视图,比如ListView, GridView等。
     */
    protected T mContentView;

    /**
     * 滚动控制器
     */
    protected Scroller mScroller;
    /**
     * 下拉刷新时显示Header View
     */
    protected View mHeaderView;

    /**
     * 屏幕的高度
     */
    protected int mScreenHeight;
    /**
     * Header View中的箭头图标
     */
    protected ImageView mArrowImg;
    /**
     * Header View 中的进度条
     */
    protected ProgressBar mRefreshProgress;
    /**
     * Header View中的文本标签
     */
    protected TextView mTipsTxt;
    /**
     * Header View中的时间标签
     */
    protected TextView mTimeTxt;
    /**
     * Header View的高度
     */
    protected int mHeaderHeight;
    /**
     * 底部上拉加载更多的视图
     */
    protected View mFooterView;

    /**
     * 最初滚动位置,第一次布局时滚动Header View高度的距离
     */
    protected int mInitScrollY;

    /**
     * 最后一次触摸事件的Y轴坐标
     */
    protected int mLastY;
    /**
     * 触摸滑动Y坐标上的偏移量
     */
    protected int mYOffset;

    /**
     * 箭头是否向上
     */
    private boolean isArrowUp;
    /**
     * 空闲状态
     */
    public static final int STATUS_IDLE = 0;
    /**
     * 下拉或者上拉还没有到达可刷新的状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 1;
    /**
     * 上拉或者下拉状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 2;
    /**
     * 刷新中
     */
    public static final int STATUS_REFRESHING = 3;
    /**
     * 加载中
     */
    public static final int STATUS_LOADING = 4;
    /**
     * 当前状态
     */
    protected int mCurrentStatus = STATUS_IDLE;

    /**
     * 刷新监听
     */
    protected OnRefreshListener mOnRefreshListener;
    /**
     * 加载更多监听
     */
    protected OnLoadListener mOnLoadListener;

    public RefreshLayoutBase(Context context) {
        this(context, null);
    }

    /**
     * @param context
     * @param attrs
     */
    public RefreshLayoutBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 初始化滚动控制器
        mScroller = new Scroller(context);
        // 获取屏幕高度
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;
        // header的高度为屏幕高度的1/4
        mHeaderHeight = mScreenHeight / 4;

        // 初始化整个布局
        initLayout(context);
    }

    /**
     * 初始化布局
     *
     * @param context
     */
    private void initLayout(Context context) {
        // 设置header view
        setupHeaderView(context);
        // 设置内容视图
        setupContentView(context);
        // 设置内容视图的布局参数
        setDefaultContentLayoutParam();
        addView(mContentView);
        // 设置Footer View
        setupFooterView(context);
    }

    /**
     * 初始化设置 Header View
     *
     * @param context
     */
    private void setupHeaderView(Context context) {
        Log.d(TAG, "setupHeaderView: ");
        mHeaderView = LayoutInflater.from(context).inflate(R.layout.header_refresh, this, false);
        mHeaderView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                mHeaderHeight));
        mHeaderView.setBackgroundColor(Color.RED);
        // Header View的高度为屏幕高度的1/4,但是,它只有100px是有效的显示区域, 其余为paddingTop,
        // 这样是为达到下拉的效果
        mHeaderView.setPadding(0, mHeaderHeight - 100, 0, 0);
        addView(mHeaderView);

        // 初始化Header View中的子视图
        mArrowImg = (ImageView) mHeaderView.findViewById(R.id.img_arrow_image);
        mRefreshProgress = (ProgressBar) mHeaderView.findViewById(R.id.progress_refresh);
        mTipsTxt = (TextView) mHeaderView.findViewById(R.id.txt_pull_to_refresh);
        mTimeTxt = (TextView) mHeaderView.findViewById(R.id.txt_update_at);
    }

    /**
     * 初始化设置内容视图,需要子类覆写
     *
     * @param context
     */
    protected abstract void setupContentView(Context context);

    /**
     * 设置内容视图的默认布局参数
     */
    protected void setDefaultContentLayoutParam() {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        mContentView.setLayoutParams(params);
    }

    /**
     * 初始化设置底部视图
     *
     * @param context
     */
    private void setupFooterView(Context context) {
        mFooterView = LayoutInflater.from(context).inflate(R.layout.footer_refresh, this, false);
        addView(mFooterView);
    }

    public T getContentView() {
        return mContentView;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public View getFooterView() {
        return mFooterView;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    public void setOnLoadListener(OnLoadListener onLoadListener) {
        this.mOnLoadListener = onLoadListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // MeasureSpec中的宽度值
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // 初始化组件的总高度
        int finalHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            // 测量没有子视图
            measureChild(childView, widthMeasureSpec, heightMeasureSpec);
            // 累加子视图的测量出来的高度,以得到本组件的总高度
            finalHeight += childView.getMeasuredHeight();
        }

        // 设置该下拉组件的尺寸
        setMeasuredDimension(width, finalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(left, top, child.getMeasuredWidth(), child.getMeasuredHeight() + top);
            top += child.getMeasuredHeight();
        }

        // 计算初始化滑动的y轴距离
        mInitScrollY = mHeaderView.getMeasuredHeight() + getPaddingTop();
        // 滑动到Header View高度的位置,从面达到隐藏header View的效果
        scrollTo(0, mInitScrollY);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int
            totalItemCount) {
        // 用户设置了加载更多监听器,且到了最底部,并且是上拉操作,那么执行加载更多操作
        if (mOnLoadListener != null && isBottom()
                && mScroller.getCurrY() <= mInitScrollY
                && mYOffset < 0
                && mCurrentStatus == STATUS_IDLE) {
            // 显示Footer View
            showFooterView();
            doLoadMore();
            mYOffset = 0;
        }
    }

    @Override
    public void computeScroll() {
        // 滚动还没有完成需要继续滚动,直到computeScrollOffset()返回为false滚动完成
        if (mScroller.computeScrollOffset()) {
            // View滚动到当前滚动控制器的(x, y)坐标上
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            // 请求
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent() called with: " + "ev = [" + ev.toString() + "]");
        // 获取触摸事件的类型
        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = (int) ev.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                mYOffset = (int) ev.getRawX() - mLastY;
                // 如果拉到了顶部,并且是下拉,则拦截触摸事件,并转到onTouchEvent()来处理下拉刷新事件
                if (isTop() && mYOffset > 0) {
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent() called with: " + "event = [" + event.toString() + "]");
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:   // 滑动事件
                // 获取当前触摸的y轴坐标
                int currentY = (int) event.getRawY();
                // 移动的偏移量
                mYOffset = currentY - mLastY;
                if (mCurrentStatus != STATUS_LOADING) {
                    // 在Y轴方向移动该控件
                    changeScrollY(mYOffset);
                }

                // 旋转Header View中的箭头图标
                rotateHeaderArrow();
                // 修改Header View中的文本信息
                changeTips();
                // mLastY设置为这次的Y轴坐标
                mLastY = currentY;
                break;

            case MotionEvent.ACTION_UP:
                if (isTop()) {
                    // 下拉刷新的具体操作
                    doRefresh();
                }

                break;
            default:
                break;
        }
//        return super.onTouchEvent(event);
        return true;
    }

    /**
     * 修改y轴上的滚动值,从而实现Header View被下拉的效果
     *
     * @param distance 滚动的偏移量(偏移距离)
     */
    private void changeScrollY(int distance) {
        int currentY = getScrollY();
        if (distance > 0 && currentY - distance > getPaddingTop()) {
            scrollBy(0, -distance);
        } else if (distance < 0 && currentY - distance <= mInitScrollY) {
            // 上拉过程
            scrollBy(0, -distance);
        }

        currentY = getScrollY();
        int slop = mInitScrollY / 2;
        if (currentY > 0 && currentY < slop) {
            mCurrentStatus = STATUS_RELEASE_TO_REFRESH;
        } else if (currentY > slop && currentY < mInitScrollY) {
            mCurrentStatus = STATUS_PULL_TO_REFRESH;
        } else {
            mCurrentStatus = STATUS_IDLE;
        }
    }

    private void rotateHeaderArrow() {
        Log.d(TAG, "rotateHeaderArrow() called with: mCurrentStatus = " + mCurrentStatus);
        if (mCurrentStatus == STATUS_REFRESHING) {
            return;
        } else if (mCurrentStatus == STATUS_PULL_TO_REFRESH && !isArrowUp) {
            return;
        } else if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH && isArrowUp) {
            return;
        }

        mRefreshProgress.setVisibility(View.GONE);
        mArrowImg.setVisibility(View.VISIBLE);

        float pivotX = mArrowImg.getWidth() / 2f;
        float pivotY = mArrowImg.getHeight() / 2f;
        float fromDegrees = 0f;
        float toDegrees = 0f;
        if (mCurrentStatus == STATUS_PULL_TO_REFRESH) {
            fromDegrees = 180f;
            toDegrees = 360f;
        } else if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
            fromDegrees = 0f;
            toDegrees = 180f;
        }
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(100);
        animation.setFillAfter(true);
        mArrowImg.startAnimation(animation);

        if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
            isArrowUp = true;
        } else {
            isArrowUp = false;
        }
    }

    private void changeTips() {
        if (mCurrentStatus == STATUS_PULL_TO_REFRESH) {
            mTipsTxt.setText(R.string.txt_pull_to_refresh);
        } else if (mCurrentStatus == STATUS_RELEASE_TO_REFRESH) {
            mTipsTxt.setText(R.string.up_to_refresh);
        }
    }

    /**
     * 执行下拉刷新操作
     */
    private void doRefresh() {
        changeHeaderViewStatus();
        // 执行刷新操作
        if (mCurrentStatus == STATUS_REFRESHING && mOnRefreshListener != null) {
            // 刷新回调
            mOnRefreshListener.onRefresh();
        }
    }


    /**
     * 手指抬起时,根据用户下拉的高度来判断是否是有效的下拉刷新操作,如果下的是距离超过Header View的一半(1/2), 那么
     * 则认为是有效的下拉刷新操作,否则恢复原来的视图状态
     */
    private void changeHeaderViewStatus() {
        Log.d(TAG, "changeHeaderViewStatus: ");
        int curScrollY = getScrollY();
        // 超过1/2则认为是有效的下拉刷新,否则还原
        if (curScrollY < mInitScrollY / 2) {
            mRefreshProgress.setVisibility(VISIBLE);
            mArrowImg.setVisibility(View.GONE);
            // 滚动到能够正常显示Header View的位置
            mScroller.startScroll(getScrollX(), curScrollY, 0, mHeaderView.getPaddingTop() -
                    curScrollY);
            mCurrentStatus = STATUS_REFRESHING;
            mTipsTxt.setText(R.string.txt_tip_refreshing);
        } else {
            mScroller.startScroll(getScrollX(), curScrollY, 0, mInitScrollY - curScrollY);
            mCurrentStatus = STATUS_IDLE;
        }

        invalidate();
    }

    /**
     * 刷新结束,恢复状态
     */
    public void refreshComplete() {
        mCurrentStatus = STATUS_IDLE;

        // 隐藏Header View
        mScroller.startScroll(getScrollX(), getScrollY(), 0, mInitScrollY - getScrollY());
        invalidate();

        updateHeaderTimeStamp();

        // 200毫秒后处理arrow和progressBar,免得太突兀
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRefreshProgress.setVisibility(View.GONE);
                mArrowImg.setVisibility(View.VISIBLE);
            }
        }, 100);
    }

    /**
     * 修改Header View上的最新更新时间
     */
    private void updateHeaderTimeStamp() {
        // 设置更新时间
        mTimeTxt.setText(R.string.txt_last_update_time);
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getInstance();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        mTimeTxt.append(sdf.format(new Date()));
    }

    public void loadCompute() {
        // 隐藏Footer View
        startScroll(mInitScrollY - getScrollY());
        mCurrentStatus = STATUS_IDLE;
    }

    /**
     * 显示Footer View
     */
    private void showFooterView() {
        startScroll(mFooterView.getMeasuredHeight());
        mCurrentStatus = STATUS_LOADING;
    }

    /**
     * 执行下拉加载更多的操作
     */
    private void doLoadMore() {
        if (mOnLoadListener != null) {
            mOnLoadListener.onLoadMore();
        }
    }

    private void startScroll(int offSetY) {
        mScroller.startScroll(getScrollX(), getScrollY(), 0, offSetY);
        invalidate();
    }

    /**
     * 判断是否到了最顶端,子类需要覆写该函数,当内容视图滑动到顶端是返回为{@code true}
     *
     * @return <b>
     * true:    到了最顶端<br/>
     * false:   没有到最顶端
     * </b>
     */
    protected abstract boolean isTop();

    /**
     * 判断是否到了最底部,子类需要覆写该函数,当内容视图滑动到最底端时返回为{@code true}
     *
     * @return <b>
     * true:    到了最底部<br/>
     * false:   没有到最底部
     * </b>
     */
    protected abstract boolean isBottom();
}
