package com.example;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TextView;

public class TimeTrackerActivity extends FragmentActivity 
        implements OnClickListener, ServiceConnection, ViewPager.OnPageChangeListener {
    
    public static final String ACTION_TIME_UPDATE = "ActionTimeUpdate";
    public static final String ACTION_TIMER_FINISHED = "ActionTimerFinished";
    private static final String TAG = "TimeTrackerActivity";
    public static int TIMER_NOTIFICATION = 0;
    public static int DATE_FLAGS = DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE;

    private TimerService mTimerService = null;
    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        
        FragmentManager fm = getSupportFragmentManager();
        final ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new PagerAdapter(fm));
        pager.setOnPageChangeListener(this);
        
        // add tabs. Use ActionBar for 3.0 and above, otherwise use TabWidget
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ActionBar bar = getActionBar();
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            bar.addTab(bar.newTab()
                    .setText("Timer")
                    .setTabListener(new ABTabListener(pager)));
            bar.addTab(bar.newTab()
                    .setText("Tasks")
                    .setTabListener(new ABTabListener(pager)));
            bar.addTab(bar.newTab()
                    .setText("Export")
                    .setTabListener(new ABTabListener(pager)));
        } else {
            // Use TabWidget instead
            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();
            mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                @Override
                public void onTabChanged(String tabId) {
                    if ("timer".equals(tabId)) {
                        pager.setCurrentItem(0);
                    } else if ("tasks".equals(tabId)) {
                        pager.setCurrentItem(1);
                    } else if ("export".equals(tabId)) {
                        pager.setCurrentItem(2);
                    }
                }
            });

            mTabHost.addTab(mTabHost.newTabSpec("timer").setIndicator("Timer").setContent(new DummyTabFactory(this)));
            mTabHost.addTab(mTabHost.newTabSpec("tasks").setIndicator("Tasks").setContent(new DummyTabFactory(this)));
            mTabHost.addTab(mTabHost.newTabSpec("export").setIndicator("Export").setContent(new DummyTabFactory(this)));

        }

        // Register the TimeReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TIME_UPDATE);
        registerReceiver(mTimeReceiver, filter);
    }
    
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        // Bind to the TimerService
        bindTimerService();
    }
    
    @Override
    protected void onDestroy() {
        if (mTimeReceiver != null)
            unregisterReceiver(mTimeReceiver);
        
        if (mTimerService != null) {
            unbindService(this);
            mTimerService = null;
        }
        super.onDestroy();
    }
    
    @Override
    public void onClick(View v) {
        TextView ssButton = (TextView) findViewById(R.id.start_stop);

        if (v.getId() == R.id.start_stop) {
            if (mTimerService == null) {
                ssButton.setText(R.string.stop);
                startService(new Intent(this, TimerService.class));
            } else if (!mTimerService.isTimerRunning()) {
                ssButton.setText(R.string.stop);
                mTimerService.startService(new Intent(this, TimerService.class));
            } else {
                ssButton.setText(R.string.start);
                mTimerService.stopTimer();
            }
        } else if (v.getId() == R.id.edit) {
            // Finish the time input activity
            Intent intent = new Intent(TimeTrackerActivity.this, EditTaskActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.delete) {
            finish();
        }
    }
    
    private void bindTimerService() {
        bindService(new Intent(this, TimerService.class), this, Context.BIND_AUTO_CREATE);
    }

    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long time = intent.getLongExtra("time", 0);

            if (ACTION_TIME_UPDATE.equals(action)) {
                TextView counter = (TextView) TimeTrackerActivity.this.findViewById(R.id.counter);
                if (counter != null)
                    counter.setText(DateUtils.formatElapsedTime(time/1000));
            }
        }
    };
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "onServiceConnected");
        mTimerService = ((TimerService.LocalBinder)service).getService();
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "onServiceDisconnected");
        mTimerService = null;
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ActionBar bar = getActionBar();
            bar.setSelectedNavigationItem(index);
        } else {
            mTabHost.setCurrentTab(index);
        }
    }
}
