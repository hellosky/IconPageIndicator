# IconPageIndicator
Icon paging indicator widgets that are compatible with the ViewPager from the Android Support Library to improve discoverability of content.

#Basic Usage
1.Include this widget in your view, it usually looks like this,
<pre>&lt;android.support.v4.view.ViewPager
        android:id="@+id/pager"
        android:layout_width="match_parent"
        android:layout_height="match_parent" /&gt;
&lt;com.pageindicator.IconPageIndicator
        android:id="@+id/indicator"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:iconWidth="44dp"
        app:radius="23dp" /&gt;</pre>
        
2.In your onCreate method (or onCreateView for a fragment), bind the indicator to the ViewPager.
<pre> //Set the pager with an adapter
 ViewPager pager = (ViewPager)findViewById(R.id.pager);
 pager.setAdapter(new TestAdapter(getSupportFragmentManager()));

 //Bind the icon indicator to the adapter
 IconPageIndicator iconIndicator = (IconPageIndicator)findViewById(R.id.indicator);
 iconIndicator.setViewPager(pager);
 
 //Set the normal icons
 iconIndicator.setNormalIcons(NORMAL_ICONS);
 
 //Set selected icons if you have
 iconIndicator.setSelectedIcons(SELECTED_ICONS);</pre>
 
 For more usages, please read the code in the sample project IconPageIndicatorDemo.
