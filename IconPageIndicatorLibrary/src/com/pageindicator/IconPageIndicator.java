/*
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2015  hellosky ye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pageindicator;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class IconPageIndicator extends View implements PageIndicator {
    private static final int INVALID_POINTER = -1;

    private float mRadius;
    private final Paint mPaintPageFill = new Paint(ANTI_ALIAS_FLAG);
    private final Paint mPaintFill = new Paint(ANTI_ALIAS_FLAG);
    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private int mCurrentPage;
    private int mSnapPage;
    private float mPageOffset;
    private int mScrollState;
    private int mOrientation;
    private boolean mSnap;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;
    private int[] mFillColors;
    private float previousOffset;
	private static final float CENTER_POSITION_OFFSET = 0.5f;
	private float mIconWidth;
	private int[] mNormalIcons;
	private Bitmap[] mNormalIconBitmaps;
	private int[] mSelectedIcons;
	private Bitmap[] mSelectedIconBitmaps;

    public IconPageIndicator(Context context) {
        this(context, null);
    }

    public IconPageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.vpiIconPageIndicatorStyle);
    }

    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public IconPageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (isInEditMode()) return;

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultPageColor = res.getColor(R.color.default_circle_indicator_page_color);
        final int defaultFillColor = res.getColor(R.color.default_circle_indicator_fill_color);
        final int defaultOrientation = res.getInteger(R.integer.default_circle_indicator_orientation);
        final float defaultRadius = res.getDimension(R.dimen.default_circle_indicator_radius);
        final boolean defaultSnap = res.getBoolean(R.bool.default_circle_indicator_snap);
        final float defaultIconWidth = res.getDimension(R.dimen.default_circle_indicator_icon_width);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPageIndicator, defStyle, 0);

        mOrientation = a.getInt(R.styleable.IconPageIndicator_android_orientation, defaultOrientation);
        mPaintPageFill.setStyle(Style.FILL);
        mPaintPageFill.setColor(a.getColor(R.styleable.IconPageIndicator_pageColor, defaultPageColor));
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setColor(a.getColor(R.styleable.IconPageIndicator_fillColor, defaultFillColor));
        mRadius = a.getDimension(R.styleable.IconPageIndicator_radius, defaultRadius);
        mSnap = a.getBoolean(R.styleable.IconPageIndicator_snap, defaultSnap);
        mIconWidth = a.getDimension(R.styleable.IconPageIndicator_iconWidth, defaultIconWidth);
        
        Drawable background = a.getDrawable(R.styleable.IconPageIndicator_android_background);
        if (background != null) {
        	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                setBackground(background);
        	}else{
                setBackgroundDrawable(background);
        	}
        }

        a.recycle();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    public void setPageColor(int pageColor) {
        mPaintPageFill.setColor(pageColor);
        invalidate();
    }

    public int getPageColor() {
        return mPaintPageFill.getColor();
    }
    
    public void setFillColors(int[] fillColors){
    	mFillColors = fillColors;
    	setFillColor(fillColors[0]);
    }
    
    public int[] getFillColors(){
    	return mFillColors;
    }

    public void setFillColor(int fillColor) {
        mPaintFill.setColor(fillColor);
        invalidate();
    }

    public int getFillColor() {
        return mPaintFill.getColor();
    }
    
    public int[] getNormalIcons(){
    	return mNormalIcons;
    }

    public void setNormalIcons(int[] icons){
    	clearIconBitmaps(mNormalIconBitmaps);
    	mNormalIcons = icons;
    	mNormalIconBitmaps = fillIconBitmaps(icons);
    	invalidate();
    }
    
    public int[] getSelectedIcons(){
    	return mSelectedIcons;
    }
    
    public void setSelectedIcons(int[] icons){
    	clearIconBitmaps(mSelectedIconBitmaps);
    	mSelectedIcons = icons;
    	mSelectedIconBitmaps = fillIconBitmaps(icons);
    	invalidate();
    }
    
    private void clearIconBitmaps(Bitmap[] bitmaps){
    	if(bitmaps != null && bitmaps.length > 0){
    		for(int i = 0; i < bitmaps.length; i++){
    			Bitmap bp = bitmaps[i];
    			recycleBitmap(bp);
    			bitmaps[i] = null;
    		}
    		bitmaps = null;
    	}
    }
    
    private void recycleBitmap(Bitmap bp){
    	if(bp != null && !bp.isRecycled()){
			bp.recycle();
		}
    }
    
    private Bitmap[] fillIconBitmaps(int[] icons){
    	Bitmap[] bitmaps = null;
    	if(icons != null && icons.length > 0){
    		bitmaps = new Bitmap[icons.length];
    		for(int i = 0; i < icons.length; i++){
    			bitmaps[i] = decodeBitmap(icons[i]);
    		}
    	}
    	return bitmaps;
    }
    
    private Bitmap decodeBitmap(int resId){
    	Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getResources(), resId, options);
		int size = (int)mIconWidth;
		int sampleSize = calculateInSampleSize(options, size, size);
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		return BitmapFactory.decodeResource(getResources(), resId, options);
    }
    
    private int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
            long totalPixels = width * height / inSampleSize;
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }
    
    public void setOrientation(int orientation) {
        switch (orientation) {
            case HORIZONTAL:
            case VERTICAL:
                mOrientation = orientation;
                requestLayout();
                break;

            default:
                throw new IllegalArgumentException("Orientation must be either HORIZONTAL or VERTICAL.");
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setRadius(float radius) {
        mRadius = radius;
        invalidate();
    }

    public float getRadius() {
        return mRadius;
    }

    public void setSnap(boolean snap) {
        mSnap = snap;
        invalidate();
    }

    public boolean isSnap() {
        return mSnap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mViewPager == null) {
            return;
        }
        final int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }

        if (mCurrentPage >= count) {
            setCurrentItem(count - 1);
            return;
        }

        int longSize;
        int longPaddingBefore;
        int longPaddingAfter;
        int shortPaddingBefore;
        if (mOrientation == HORIZONTAL) {
            longSize = getWidth();
            longPaddingBefore = getPaddingLeft();
            longPaddingAfter = getPaddingRight();
            shortPaddingBefore = getPaddingTop();
        } else {
            longSize = getHeight();
            longPaddingBefore = getPaddingTop();
            longPaddingAfter = getPaddingBottom();
            shortPaddingBefore = getPaddingLeft();
        }

        final int length = mNormalIconBitmaps.length;
        final float space = (longSize - length * mIconWidth - longPaddingBefore - longPaddingAfter) / (length - 1);
        final float distance = space + mIconWidth;

        float dX;
        float dY;

        int currentPage = mCurrentPage;
        if(currentPage < 0){
        	currentPage = 0;
        }
        int diffX = (int)((mIconWidth - mNormalIconBitmaps[0].getWidth()) / 2);
        int diffY = (int)((mIconWidth - mNormalIconBitmaps[0].getHeight()) / 2);
        //Draw icons
        for (int iLoop = 0; iLoop < length; iLoop++) {
            float drawLong = longPaddingBefore + diffX + (iLoop * distance);
            if (mOrientation == HORIZONTAL) {
                dX = drawLong;
                dY = shortPaddingBefore + diffY;
            } else {
                dX = shortPaddingBefore + diffX;
                dY = drawLong;
            }
            if(currentPage == iLoop){
                canvas.drawBitmap(mSelectedIconBitmaps[iLoop], dX, dY, mPaintPageFill);
            }else{
                canvas.drawBitmap(mNormalIconBitmaps[iLoop], dX, dY, mPaintPageFill);
            }
        }

        //Draw the filled circle according to the current scroll
        float cx = (mSnap ? mSnapPage : currentPage) * distance;
        if (!mSnap) {
            cx += mPageOffset * distance;
        }
        if (mOrientation == HORIZONTAL) {
        	dX = longPaddingBefore + cx + mIconWidth / 2;
            dY = shortPaddingBefore + mIconWidth / 2;
        } else {
            dX = shortPaddingBefore + mIconWidth / 2;
            dY = longPaddingBefore + cx + mIconWidth / 2;
        }
        mPaintFill.setAlpha(100);
        canvas.drawCircle(dX, dY, mRadius, mPaintFill);
    }

    public boolean onTouchEvent(android.view.MotionEvent ev) {
        if (super.onTouchEvent(ev)) {
            return true;
        }
        if ((mViewPager == null) || (mViewPager.getAdapter().getCount() == 0)) {
            return false;
        }

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mLastMotionX = ev.getX();
                break;

            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float deltaX = x - mLastMotionX;

                if (!mIsDragging) {
                    if (Math.abs(deltaX) > mTouchSlop) {
                        mIsDragging = true;
                    }
                }

                if (mIsDragging) {
                    mLastMotionX = x;
                    if (mViewPager.isFakeDragging() || mViewPager.beginFakeDrag()) {
                        mViewPager.fakeDragBy(deltaX);
                    }
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mIsDragging) {
                    final int count = mViewPager.getAdapter().getCount();
                    final int width = getWidth();
                    final float halfWidth = width / 2f;
                    final float sixthWidth = width / 6f;

                    if ((mCurrentPage > 0) && (ev.getX() < halfWidth - sixthWidth)) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager.setCurrentItem(mCurrentPage - 1);
                        }
                        return true;
                    } else if ((mCurrentPage < count - 1) && (ev.getX() > halfWidth + sixthWidth)) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager.setCurrentItem(mCurrentPage + 1);
                        }
                        return true;
                    }
                }

                mIsDragging = false;
                mActivePointerId = INVALID_POINTER;
                if (mViewPager.isFakeDragging()) mViewPager.endFakeDrag();
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionX = MotionEventCompat.getX(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                mLastMotionX = MotionEventCompat.getX(ev, MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        }

        return true;
    }

    @Override
    public void setViewPager(ViewPager view) {
        if (mViewPager == view) {
            return;
        }
        if (mViewPager != null) {
            mViewPager.setOnPageChangeListener(null);
        }
        if (view.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }
        mViewPager = view;
        mViewPager.setOnPageChangeListener(this);
        invalidate();
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mViewPager.setCurrentItem(item);
        mCurrentPage = item;
        invalidate();
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;

        if (mListener != null) {
            mListener.onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    	if(positionOffset > CENTER_POSITION_OFFSET){
			int index = 0;
			if(positionOffset > previousOffset){
				index = position+1;
				if(index >= mFillColors.length){
					index = mFillColors.length-1;
				}
				setFillColor(mFillColors[index]);
			}
		}else{
			if(positionOffset < previousOffset){
				setFillColor(mFillColors[position]);
			}
		}
		previousOffset = positionOffset;
    	
    	mCurrentPage = position;
        mPageOffset = positionOffset;
        invalidate();

        if (mListener != null) {
            mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (mSnap || mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            mCurrentPage = position;
            mSnapPage = position;
            invalidate();
        }

        if (mListener != null) {
            mListener.onPageSelected(position);
        }
    }

    @Override
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mListener = listener;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOrientation == HORIZONTAL) {
            setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec));
        } else {
            setMeasuredDimension(measureShort(widthMeasureSpec), measureLong(heightMeasureSpec));
        }
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureLong(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if ((specMode == MeasureSpec.EXACTLY) || (mViewPager == null)) {
            //We were told how big to be
            result = specSize;
        } else {
            //Calculate the width according the views count
            final int count = mViewPager.getAdapter().getCount();
            result = (int)(getPaddingLeft() + getPaddingRight()
                    + (count * 2 * mRadius) + (count - 1) * mRadius + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureShort(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            result = specSize;
        } else {
            //Measure the height
            result = (int)(2 * mRadius + getPaddingTop() + getPaddingBottom() + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPage = savedState.currentPage;
        mSnapPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPage);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
