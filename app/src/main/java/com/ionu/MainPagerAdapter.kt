package com.ionu

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

/**
 * Adapter that manages fragments.
 */
class MainPagerAdapter(fm: FragmentManager, context: Context) : FragmentPagerAdapter(fm) {

    private val fragmentCount: Int = 2
    private var mContext: Context = context

    override fun getItem(position: Int): Fragment {
        // TODO add return for other fragments when they are implemented
        when(position){
            //TODO use a root fragment instead so it can be replaced later
            0 -> return PageRootFragment()
            else -> {
                return HistoryFragment()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when(position){
            0 -> return mContext.getString(R.string.tab_text_alarms)
            else -> {
                return mContext.getString(R.string.tab_text_history)
            }
        }
    }

    override fun getCount(): Int = fragmentCount

}