package com.ionu

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PageRootFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_page_root, container, false)

        // use alarm list fragment as the default value for the root frame in viewpager
        val transaction : FragmentTransaction? = fragmentManager?.beginTransaction()
        transaction?.replace(R.id.page_root_frame, AlarmsFragment())
        transaction?.commit()

        return view
    }
}