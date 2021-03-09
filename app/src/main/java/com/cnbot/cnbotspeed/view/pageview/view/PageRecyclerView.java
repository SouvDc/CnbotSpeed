package com.cnbot.cnbotspeed.view.pageview.view;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;


import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cnbot.cnbotspeed.view.pageview.adapter.OnPageDataListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class PageRecyclerView extends RecyclerView {
	private static final String TAG = "PageRecyclerView";
	private static final String ARGS_SCROLL_OFFSET = "mScrollOffset";
	private static final String ARGS_PAGE = "mCurrentPage";
	private static final String ARGS_SUPER = "super";
	private static final String ARGS_WIDTH = "mWidth";
	private static final String ARGS_HEIGHT = "mHeight";
	private static final int MAX_SETTLE_DURATION = 600; // ms
	private static final int DEFAULT_VELOCITY = 4000;
	private int mVelocity = DEFAULT_VELOCITY;
	private int mWidth;
	private int mHeight;
	private int mOrientation;
	private Method smoothScrollBy = null;
	private Field mViewFlingerField = null;
	private static final Interpolator mInterpolator = new Interpolator() {
		@Override
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};
	private int mScrollOffset;//滚动偏移量
	private int mDragOffset;//拖动时偏移量
	private int mScrollState;
	private int mCurrentPage;
	private boolean mFirstLayout = true;
	private boolean mIsLooping = false;
	private boolean isSliding;//是否是滑动
	private boolean forwardDirection;//滑动方向
	private OnPageChangeListener listener;
	private DecimalFormat decimalFormat;

	public PageRecyclerView(Context context) {
		this(context, null);
	}

	public PageRecyclerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PageRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		decimalFormat = new DecimalFormat("0.00");
		decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
		getScrollerByReflection();
		setOnFlingListener(new OnPageFlingListener());
		addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				removeOnLayoutChangeListener(this);
				mWidth = getWidth();
				mHeight = getHeight();
				if (mFirstLayout) {
					if (mOrientation == OnPageDataListener.HORIZONTAL) {
						mScrollOffset = mCurrentPage * mWidth;
					} else {
						mScrollOffset = mCurrentPage * mHeight;
					}
					if (mIsLooping) {
						PageRecyclerView.super.scrollToPosition(mCurrentPage);
					}
				}
				mFirstLayout = false;
			}
		});
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public void setOrientation(@OnPageDataListener.LayoutOrientation int mOrientation) {
		this.mOrientation = mOrientation;
	}

	public void setLooping(boolean isLooping) {
		this.mIsLooping = isLooping;
	}

	public void setVelocity(@IntRange(from = 0) int mVelocity) {
		this.mVelocity = mVelocity;
	}

	/**
	 * {@link ViewFlinger}
	 */
	private void getScrollerByReflection() {
		Class<?> c = null;
		Class<?> ViewFlingerClass = null;
		try {
			c = Class.forName("android.support.v7.widget.RecyclerView");
			mViewFlingerField = c.getDeclaredField("mViewFlinger");
			mViewFlingerField.setAccessible(true);
			ViewFlingerClass = Class.forName(mViewFlingerField.getType().getName());
			smoothScrollBy = ViewFlingerClass.getDeclaredMethod("smoothScrollBy",
					int.class, int.class, int.class, Interpolator.class);
			smoothScrollBy.setAccessible(true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private void smoothScrollBy(int dx, int dy, int duration) {
		try {
			smoothScrollBy.invoke(mViewFlingerField.get(this), dx, dy, duration, mInterpolator);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onScrollStateChanged(int state) {
		this.mScrollState = state;
		if (listener != null) {
			listener.onPageScrollStateChanged(state);
		}
		switch (state) {
			case SCROLL_STATE_IDLE://0
				if (mOrientation == OnPageDataListener.HORIZONTAL) {
					if (mWidth != 0) {
						if (isSliding) {//放手后的滑动
							int t = mScrollOffset / mWidth;
							int deltaX = mScrollOffset - t * mWidth;
							if (!forwardDirection) {//向前
								deltaX = deltaX - mWidth;
							} else {//向后
							}
							moveX(deltaX);
						} else {//用手拖动
							moveX(mDragOffset);
						}
					}
				} else {
					if (mHeight != 0) {
						if (isSliding) {//放手后的滑动
							int t = mScrollOffset / mHeight;
							int deltaY = mScrollOffset - t * mHeight;
							if (!forwardDirection) {
								deltaY = deltaY - mHeight;
							} else {
							}
							moveY(deltaY);
						} else {//用手拖动
							moveY(mDragOffset);
						}
					}
				}
				mDragOffset = 0;
				isSliding = false;
				break;
			case SCROLL_STATE_DRAGGING://1
				break;
			case SCROLL_STATE_SETTLING://2
				isSliding = true;
				break;
		}
	}

	private void moveX(int deltaX) {
		if (Math.abs(deltaX) == 0 || Math.abs(deltaX) == mWidth) {
			return;
		}
		int itemWidth = mWidth / 2;
		if (deltaX >= itemWidth) {//下一页
			int moveX = mWidth - deltaX;
			smoothScrollBy(moveX, 0, calculateTimeForHorizontalScrolling(mVelocity, Math.abs(moveX)));
		} else if (deltaX <= -itemWidth) {//上一页
			int moveX = -(mWidth + deltaX);
			smoothScrollBy(moveX, 0, calculateTimeForHorizontalScrolling(mVelocity, Math.abs(moveX)));
		} else {//回弹
			smoothScrollBy(-deltaX, 0, calculateTimeForHorizontalScrolling(mVelocity, Math.abs(deltaX)));
		}
	}

	private void moveY(int deltaY) {
		if (Math.abs(deltaY) == 0 || Math.abs(deltaY) == mHeight) {
			return;
		}
		int itemHeight = mHeight / 2;
		if (deltaY >= itemHeight) {//下一页
			int moveY = mHeight - deltaY;
			smoothScrollBy(0, moveY, calculateTimeForVerticalScrolling(mVelocity, Math.abs(moveY)));
		} else if (deltaY <= -itemHeight) {//上一页
			int moveY = -(mHeight + deltaY);
			smoothScrollBy(0, moveY, calculateTimeForVerticalScrolling(mVelocity, Math.abs(moveY)));
		} else {//回弹
			smoothScrollBy(0, -deltaY, calculateTimeForVerticalScrolling(mVelocity, Math.abs(deltaY)));
		}
	}

	@Override
	public void onScrolled(int dx, int dy) {
		if (mOrientation == OnPageDataListener.HORIZONTAL) {
			mScrollOffset += dx;
			if (mScrollState == SCROLL_STATE_DRAGGING) {
				mDragOffset += dx;
			}
			if (dx < 0) {
				forwardDirection = false;
			} else {
				forwardDirection = true;
			}
			Log.d(TAG, "onScrolled:%! mScrollOffset:" + mScrollOffset + ",mCurrentPage:" + mCurrentPage +
					",mDragOffset:" + mDragOffset + ",forwardDirection:" + forwardDirection + ",mWidth：" + mWidth);
			if (mWidth == 0) {
				return;
			}
			if (dx < 0 && mScrollOffset % mWidth != 0) {
				int targetPage = mScrollOffset / mWidth + 1;
				mCurrentPage = targetPage;
			} else {
				int targetPage = mScrollOffset / mWidth;
				mCurrentPage = targetPage;
			}
			if (listener != null) {
				int positionOffsetPixels = mScrollOffset % mWidth;
				float positionOffset = Float.parseFloat(decimalFormat.format(mScrollOffset % mWidth / (double) mWidth));
				listener.onPageScrolled(mCurrentPage, positionOffset, positionOffsetPixels);
				if (positionOffsetPixels == 0) {
					listener.onPageSelected(mCurrentPage);
				}
			}
		} else {
			mScrollOffset += dy;
			if (mScrollState == SCROLL_STATE_DRAGGING) {
				mDragOffset += dy;
			}
			if (dy < 0) {
				forwardDirection = false;
			} else {
				forwardDirection = true;
			}
			if (mHeight == 0) {
				return;
			}
			if (dy < 0 && mScrollOffset % mHeight != 0) {
				int targetPage = mScrollOffset / mHeight + 1;
				mCurrentPage = targetPage;
			} else {
				int targetPage = mScrollOffset / mHeight;
				mCurrentPage = targetPage;
			}
			if (listener != null) {
				int positionOffsetPixels = mScrollOffset % mHeight;
				float positionOffset = Float.parseFloat(decimalFormat.format(mScrollOffset % mHeight / (double) mHeight));
				listener.onPageScrolled(mCurrentPage, positionOffset, positionOffsetPixels);
				if (positionOffsetPixels == 0) {
					listener.onPageSelected(mCurrentPage);
				}
			}
		}
	}

	void scrollToEndPage(@IntRange(from = 0) int page) {
		if (mOrientation == OnPageDataListener.HORIZONTAL) {
			mScrollOffset = page * mWidth;
		} else {
			mScrollOffset = page * mHeight;
		}
		super.scrollToPosition(getAdapter().getItemCount() - 1);
	}

	void scrollToBeginPage() {
		mScrollOffset = 0;
		super.scrollToPosition(0);
	}

	void scrollToPage(@IntRange(from = 0) int page, boolean smoothScroll) {
		Log.d(TAG, "scrollToPage:%!" + page + ", currentPage: " + mCurrentPage + ", mScrollOffset: " + mScrollOffset);
		if (mCurrentPage == page) {
			return;
		}
		if (mOrientation == OnPageDataListener.HORIZONTAL) {
			if (mFirstLayout) {
				mCurrentPage = page;
			} else {
				int scrollX = page * mWidth;
				int moveX = scrollX - mScrollOffset;
				if (smoothScroll) {
					smoothScrollBy(moveX, 0, calculateTimeForHorizontalScrolling(mVelocity, moveX));
				} else {
					smoothScrollBy(moveX, 0, 0);
				}
			}
		} else {
			if (mFirstLayout) {
				mCurrentPage = page;
			} else {
				int scrollY = page * mHeight;
				int moveY = scrollY - mScrollOffset;
				if (smoothScroll) {
					smoothScrollBy(0, moveY, calculateTimeForVerticalScrolling(mVelocity, moveY));
				} else {
					smoothScrollBy(0, moveY, 0);
				}
			}
		}
	}

	private class OnPageFlingListener extends OnFlingListener {
		/**
		 * {@link android.support.v7.widget.PagerSnapHelper}
		 *
		 * @param velocityX
		 * @param velocityY
		 * @return
		 */
		@Override
		public boolean onFling(int velocityX, int velocityY) {
			LayoutManager layoutManager = getLayoutManager();
			if (layoutManager == null) {
				return false;
			}
			Adapter adapter = getAdapter();
			if (adapter == null) {
				return false;
			}
			int minFlingVelocity = getMinFlingVelocity();
			boolean fling = (Math.abs(velocityY) > minFlingVelocity || Math.abs(velocityX) > minFlingVelocity)
					&& snapFromFling(layoutManager, velocityX, velocityY);
			return fling;
		}
	}

	private boolean snapFromFling(@NonNull LayoutManager layoutManager, int velocityX,
								  int velocityY) {
		final int itemCount = layoutManager.getItemCount();
		if (itemCount == 0) {
			return false;
		}
		if (SCROLL_STATE_DRAGGING == this.mScrollState) {
			if (mOrientation == OnPageDataListener.HORIZONTAL) {
				int moveX = getMoveDistance(mScrollOffset, mWidth);
				if (Math.abs(moveX) != 0 && Math.abs(moveX) != mWidth) {
					smoothScrollBy(moveX, 0, calculateTimeForHorizontalScrolling(velocityX, moveX));
				}
			} else {
				int moveY = getMoveDistance(mScrollOffset, mHeight);
				if (Math.abs(moveY) != 0 && Math.abs(moveY) != mHeight) {
					smoothScrollBy(0, moveY, calculateTimeForVerticalScrolling(velocityY, moveY));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private int getMoveDistance(int scrollDistance, int length) {
		int moveDistance;
		if (forwardDirection) {
			int targetPage = scrollDistance / length + 1;
			moveDistance = targetPage * length - scrollDistance;
		} else {
			int targetPage = scrollDistance / length;
			moveDistance = targetPage * length - scrollDistance;
		}
		return moveDistance;
	}

	/**
	 * {@link android.support.v4.view.ViewPager}的smoothScrollTo(int,int ,int)
	 *
	 * @param velocity
	 * @param dx
	 * @return
	 */
	private int calculateTimeForHorizontalScrolling(int velocity, int dx) {
		final int width = mWidth;
		final int halfWidth = width / 2;
		final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
		final float distance = halfWidth + halfWidth
				* distanceInfluenceForSnapDuration(distanceRatio);
		int duration;
		velocity = Math.abs(velocity);
		if (velocity > 0) {
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		} else {
			final float pageWidth = width * 1.0f;
			final float pageDelta = (float) Math.abs(dx) / (pageWidth);
			duration = (int) ((pageDelta + 1) * 100);
		}
		duration = Math.min(duration, MAX_SETTLE_DURATION);
		return duration;
	}

	private int calculateTimeForVerticalScrolling(int velocity, int dy) {
		final int height = mHeight;
		final int halfHeight = height / 2;
		final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dy) / height);
		final float distance = halfHeight + halfHeight
				* distanceInfluenceForSnapDuration(distanceRatio);
		int duration;
		velocity = Math.abs(velocity);
		if (velocity > 0) {
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		} else {
			final float pageWidth = height * 1.0f;
			final float pageDelta = (float) Math.abs(dy) / (pageWidth);
			duration = (int) ((pageDelta + 1) * 100);
		}
		duration = Math.min(duration, MAX_SETTLE_DURATION);
		return duration;
	}

	private float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		mScrollOffset = bundle.getInt(ARGS_SCROLL_OFFSET, 0);
		mCurrentPage = bundle.getInt(ARGS_PAGE, 0);
		mWidth = bundle.getInt(ARGS_WIDTH, 0);
		mHeight = bundle.getInt(ARGS_HEIGHT, 0);
		Parcelable parcelable = bundle.getParcelable(ARGS_SUPER);
		super.onRestoreInstanceState(parcelable);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putInt(ARGS_SCROLL_OFFSET, mScrollOffset);
		bundle.putInt(ARGS_PAGE, mCurrentPage);
		bundle.putInt(ARGS_WIDTH, mWidth);
		bundle.putInt(ARGS_HEIGHT, mHeight);
		bundle.putParcelable(ARGS_SUPER, super.onSaveInstanceState());
		return bundle;
	}

	public void addOnPageChangeListener(OnPageChangeListener listener) {
		this.listener = listener;
	}

	public interface OnPageChangeListener {
		void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

		void onPageSelected(int position);

		void onPageScrollStateChanged(int state);
	}
}
