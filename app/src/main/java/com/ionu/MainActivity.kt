package com.ionu

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout

import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AlarmsFragment.OnAlarmsFragmentListener {

    private lateinit var mViewPager: ViewPager
    private lateinit var mPagerAdapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mViewPager = main_viewpager
        mPagerAdapter = MainPagerAdapter(supportFragmentManager, this)
        mViewPager.adapter = mPagerAdapter

        val tabLayout: TabLayout = main_tablayout
        tabLayout.setupWithViewPager(mViewPager)

        fab.setOnClickListener { view ->
            // TODO use current clock time as AlarmPeriod parameter
            // Insert new alarm to Realm, fragments will update the change on their own
            val alarm = AlarmPeriod()
            Realm.getDefaultInstance().use{
                it.executeTransaction(Realm.Transaction {it.insert(alarm)})
            }
            Snackbar.make(view, "New alarm saved", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAlarmSelected(alarmPeriod : AlarmPeriod) {
        Log.d("MainActivity", "onAlarmSelected(), alarmID: " + alarmPeriod.id)
        // Show alarm details in another fragment
        val bundle = Bundle()
        bundle.putString(GlobalVariables.ALARM_PERIOD_ID_BUNDLE, alarmPeriod.id)
        val alarmFragment = AlarmPeriodFragment()
        alarmFragment.arguments = bundle

        val transaction : FragmentTransaction? = supportFragmentManager.beginTransaction()
        transaction?.replace(R.id.page_root_frame, alarmFragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}
