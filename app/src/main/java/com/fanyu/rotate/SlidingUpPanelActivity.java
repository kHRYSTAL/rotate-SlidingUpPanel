package com.fanyu.rotate;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kaichunlin.transition.ViewTransitionBuilder;
import com.kaichunlin.transition.adapter.SlidingUpPanelLayoutAdapter;
import com.kaichunlin.transition.adapter.UnifiedAdapter;
import com.kaichunlin.transition.animation.Animation;
import com.kaichunlin.transition.animation.AnimationListener;
import com.kaichunlin.transition.internal.debug.TraceAnimationListener;
import com.kaichunlin.transition.internal.debug.TraceTransitionListener;
import com.kaichunlin.transition.util.TransitionUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;



public class SlidingUpPanelActivity extends AppCompatActivity implements View.OnClickListener {

    private UnifiedAdapter mUnifiedAdapter;
    private Toolbar mToolbar;
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private View mLastSelection;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Menu mMenu;
    private String curPosition="0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCollapsingToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.ctl);
        SlidingUpPanelLayout supl = ((SlidingUpPanelLayout) findViewById(R.id.sliding_layout));
        //supl.setEnableDragViewTouchEvents(false);

        //set up the adapter
        SlidingUpPanelLayoutAdapter mSlidingUpPanelLayoutAdapter = new SlidingUpPanelLayoutAdapter();
        supl.setPanelSlideListener(mSlidingUpPanelLayoutAdapter);
        //the adapter accepts another SlidingUpPanelLayout.PanelSlideListener so other customizations can be performed
        mSlidingUpPanelLayoutAdapter.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                //TODO 拖动时 设置Toolbar内容隐藏
                curPosition = String.valueOf(v);
                Log.d("SlidePosition",curPosition);
            }

            @Override
            public void onPanelCollapsed(View view) {
                //TODO 收缩事件 可不设置
            }

            @Override
            public void onPanelExpanded(View view) {
                //TODO 展开事件 可不设置
            }

            @Override
            public void onPanelAnchored(View view) {
                //TODO 固定住  判断是否显示ToolBar
            }

            @Override
            public void onPanelHidden(View view) {
                //TODO 隐藏
            }
        });

        mUnifiedAdapter = new UnifiedAdapter(mSlidingUpPanelLayoutAdapter);
        mUnifiedAdapter.setDuration(1000);
        mUnifiedAdapter.addAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animationManager) {
            }

            @Override
            public void onAnimationEnd(Animation animationManager) {
                mUnifiedAdapter.setReverseAnimation(!mUnifiedAdapter.isReverseAnimation());
            }

            @Override
            public void onAnimationCancel(Animation animationManager) {
                mUnifiedAdapter.setReverseAnimation(!mUnifiedAdapter.isReverseAnimation());
            }

            @Override
            public void onAnimationReset(Animation animationManager) {

            }
        });

        //debug
        mUnifiedAdapter.addTransitionListener(new TraceTransitionListener());
        mUnifiedAdapter.addAnimationListener(new TraceAnimationListener());

        //this is required since some transition requires the width/height/position of a view, which is not yet properly initialized until layout is complete
        //in this example, another way of achieving correct behavior without using ViewUtil.executeOnGlobalLayout() would be to change all
        // translationYAsFractionOfHeight() calls to delayTranslationYAsFractionOfHeight() which would defer the calculation until the transition is just about to start
        TransitionUtil.executeOnGlobalLayout(this, new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                updateTransition(null, false);
            }
        });
        setRecyclerView();
    }

    private MyAdapter mAdapter = new MyAdapter();
    private void setRecyclerView() {
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (curPosition.equals("0.0")){
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        updateTransition(v, mUnifiedAdapter.isReverseAnimation() || mUnifiedAdapter.isAnimating());
    }

    @UiThread
    public void updateTransition(View v, boolean animate) {
        if (animate) {
            mUnifiedAdapter.setReverseAnimation(true);
            mUnifiedAdapter.startAnimation();
        }

        //TODO removeAllTransitions() has to be called *after* mAnimationAdapter.resetAnimation(), this subtle order of execution requirement should be removed
        mUnifiedAdapter.removeAllTransitions();
        mLastSelection = v;

        boolean enableAnimationMenu = true;

        //configure a builder with properties shared by all instances and just clone it for future use
        //since adapter(ITransitionAdapter) is set, simply call buildFor(mUnifiedAdapter) would add the resultant ViewTransition to the adapter
        ViewTransitionBuilder baseBuilder = ViewTransitionBuilder.transit(mCollapsingToolbarLayout).interpolator(mInterpolator);
        ViewTransitionBuilder builder;
        // ((ImageView) findViewById(R.id.content_bg)).setColorFilter(null);
        boolean setHalfHeight = true;
        //switch (v.getId()) {
        //TODO visual artifact on Android 5.1 (Nexus 7 2013) when rotationX is ~45, why???
        //    case R.id.rotate_slide:
        builder = baseBuilder.clone();
        builder.scale(0.8f).rotationX(40).translationYAsFractionOfHeight(-1f).buildFor(mUnifiedAdapter);

        builder = baseBuilder.clone().target(findViewById(R.id.ctl)).rotationX(90f).scale(0.8f).translationYAsFractionOfHeight(-0.25f);
        builder.buildFor(mUnifiedAdapter);
        builder.target(findViewById(R.id.ctl)).buildFor(mUnifiedAdapter);

        builder = baseBuilder.clone().target(findViewById(R.id.ctl));
        ViewTransitionBuilder.Cascade cascade = new ViewTransitionBuilder.Cascade(1f);
        cascade.reverse = true;
        builder.transitViewGroup(new ViewTransitionBuilder.ViewGroupTransition() {
            @Override
            public void transit(ViewTransitionBuilder builder, ViewTransitionBuilder.ViewGroupTransitionConfig config) {
                builder.translationYAsFractionOfHeight(config.parentViewGroup, 1f).buildFor(mUnifiedAdapter);
            }
        }, cascade);
        setSlidingUpPanelHeight(setHalfHeight);
    }

    private void setSlidingUpPanelHeight(boolean halfHeight) {
        SlidingUpPanelLayout supl = ((SlidingUpPanelLayout) findViewById(R.id.sliding_layout));
        View panel = supl.getChildAt(1);
        ViewGroup.LayoutParams params = panel.getLayoutParams();
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int height = rectangle.bottom - rectangle.top;
        params.height = halfHeight ? height* 5/6 : height;
        supl.requestLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this,"setting",Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private ArrayList<String> mData = new ArrayList<String>();

        public MyAdapter() {
            for (int i=0;i<20;i++) mData.add("fanyu-ASD-"+i);
        }

        public void insertData(String data, int index) {
            mData.add(index, data);
            notifyItemInserted(index);
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
            TextView tv = new TextView(viewGroup.getContext());
            tv.setPadding(50, 50, 50, 50);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(final MyAdapter.ViewHolder viewHolder, int i) {
            viewHolder.view.setText(mData.get(i));
            viewHolder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int pos = viewHolder.getLayoutPosition();
                    Toast.makeText(viewHolder.view.getContext(),
                            mData.get(pos), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView view;
            public ViewHolder(View itemView) {
                super(itemView);
                view = (TextView) itemView;
            }
        }
    }

}
