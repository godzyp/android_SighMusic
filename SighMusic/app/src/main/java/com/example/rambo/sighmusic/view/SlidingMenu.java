package com.example.rambo.sighmusic.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

public class SlidingMenu extends ViewGroup {
	private static final String TAG = "SlidingMenu";
	private View mLeftView;
	private View mContentView;
	private int mLeftWidth;
	private float mDownX;
	private float mDownY;

	private Scroller mScroller;

	public static boolean isLeftShow = false;

	public SlidingMenu(Context context) {
		this(context, null);
	}

	public SlidingMenu(Context context, AttributeSet attrs) {
		super(context, attrs);

		mScroller = new Scroller(context);
	}
	//在完成findbyid之后调用的方法
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		//获取子视图
		mLeftView = getChildAt(0);
		mContentView = getChildAt(0);

		LayoutParams params = mLeftView.getLayoutParams();
		mLeftWidth = params.width;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int leftWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mLeftWidth, MeasureSpec.EXACTLY);
		mLeftView.measure(leftWidthMeasureSpec, heightMeasureSpec);

		//TODO  这里BUG貌似是布局位置产生的   right measure
		mContentView.measure(widthMeasureSpec, heightMeasureSpec);

		// self measuring
		int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
		int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
		// setMeasuredDimension(10, 10);
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		// relativity

		int width = mLeftView.getMeasuredWidth();
		int height = mLeftView.getMeasuredHeight();

		Log.d(TAG, "width : " + width);
		Log.d(TAG, "height : " + height);

		// left layout
		int lvLeft = -width;
		int lvTop = 0;
		int lvRight = 0;
		int lvBottom = height;
		mLeftView.layout(lvLeft, lvTop, lvRight, lvBottom);// 有width和height

		//TODO 这里BUG貌似是布局位置产生的       courtesy...
		mContentView.layout(0, 0, mContentView.getMeasuredWidth(), mContentView.getMeasuredHeight());
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownX = ev.getX();
			mDownY = ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			float moveX = ev.getX();
			float moveY = ev.getY();

			if (Math.abs(moveX - mDownX) > Math.abs(moveY - mDownY)) {
				// 水平方向移动进行拦截，自己处理
				return true;
			}

			break;
		case MotionEvent.ACTION_UP:
			break;
		default:
			break;
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownX = event.getX();
			mDownY = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			float moveX = event.getX();
			float moveY = event.getY();

			int diffX = (int) (mDownX - moveX + 0.5f);// 四舍五入

			int scrollX = getScrollX() + diffX;

			if (scrollX < 0 && scrollX < -mLeftView.getMeasuredWidth()) {
				// 从左往右滑动
				scrollTo(-mLeftView.getMeasuredWidth(), 0);
			} else if (scrollX > 0) {
				scrollTo(0, 0);
			} else {
				// 标准滑动
				scrollBy(diffX, 0);
			}
			mDownX = moveX;
			mDownY = moveY;
			break;
		case MotionEvent.ACTION_UP:
			// 松开时的逻辑
			// 判断是要去打开，要去关闭

			int width = mLeftView.getMeasuredWidth();
			int currentX = getScrollX();
			float middle = -width / 2f;
			switchMenu(currentX <= middle);
			break;
		default:
			break;
		}
		return true;
	}

	public void switchMenu(boolean showLeft) {
		isLeftShow = showLeft;
		int width = mLeftView.getMeasuredWidth();
		int currentX = getScrollX();
		if (!showLeft) {
			// 关闭:显示内容区域
			// scrollTo(0, 0);
			// 起始点---》结束点
			// -100------->0 -100,-99,-98.....0

			int startX = currentX;
			int startY = 0;

			int endX = 0;
			int endY = 0;

			int dx = endX - startX;// 增量的值
			int dy = endY - startY;

			int duration = Math.abs(dx) * 10;// 时长
			if (duration >= 600) {
				duration = 600;
			}

			// 模拟数据变化
			mScroller.startScroll(startX, startY, dx, dy, duration);

		} else {
			// 打开：显示左侧菜单
			// scrollTo(-width, 0);

			int startX = currentX;
			int startY = 0;

			int endX = -width;
			int endY = 0;

			int dx = endX - startX;// 增量的值
			int dy = endY - startY;

			int duration = Math.abs(dx) * 10;// 时长
			if (duration >= 600) {
				duration = 600;
			}
			// 模拟数据变化
			mScroller.startScroll(startX, startY, dx, dy, duration);
		}
		invalidate();// UI刷新---> draw() -->drawChild() --> computeScroll()
	}

	/**
	 * 由父视图调用用来请求子视图根据偏移值 mScrollX,mScrollY重新绘制  
	 * 空方法 ，自定义ViewGroup必须实现方法体
	 */
	@Override
	public void computeScroll() {
		// the animation is not yet finished.
		if (mScroller.computeScrollOffset()) {
			// 更新位置
			scrollTo(mScroller.getCurrX(), 0);
			invalidate();
		}
	}

	// 开关：根据当前位置判断开关状态，如果是打开状态就取反关闭，如果是关闭状态就取反打开
	public void toggle() {
		switchMenu(!isLeftShow);
	}
}
