package com.example.iconpageindicatordemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.pageindicator.IconPageIndicator;

public class MainActivity extends Activity {
	
	private static final String[] COLORS = {"#EE5B68", "#F4D037", "#A0C952"};
	private ViewPager mVpContent;
	private IconPageIndicator mIndicator;
	private static final int[] NORMAL_ICONS = {R.drawable.icon_tutorial_home_normal,
		R.drawable.icon_tutorial_guides_normal, R.drawable.icon_tutorial_more_normal};
	private static final int[] SELECTED_ICONS = {R.drawable.icon_tutorial_home_selected,
		R.drawable.icon_tutorial_guides_selected, R.drawable.icon_tutorial_more_selected};
	private int[] mColors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mColors = new int[COLORS.length];
		for(int i = 0; i < COLORS.length; i++){
			mColors[i] = Color.parseColor(COLORS[i]);
		}
		setContentView(R.layout.activity_main);
		
		mIndicator = (IconPageIndicator)findViewById(R.id.indicator);
		mVpContent = (ViewPager) findViewById(R.id.page);
		mVpContent.setAdapter(mOnboardingScreenAdapter);
		mIndicator.setFillColors(mColors);
		mIndicator.setNormalIcons(NORMAL_ICONS);
		mIndicator.setSelectedIcons(SELECTED_ICONS);
		mIndicator.setViewPager(mVpContent);
	}

	private PagerAdapter mOnboardingScreenAdapter = new PagerAdapter() {
		@Override
		public int getCount() {
			return COLORS.length;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == (View) object;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position) {
			View rootView = new View(container.getContext());
			rootView.setBackgroundColor(mColors[position]);
			((ViewPager) container).addView(rootView, 0);

			return rootView;
		}
	};
}
