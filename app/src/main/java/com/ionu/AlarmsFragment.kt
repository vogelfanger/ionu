package com.ionu

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_alarms.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AlarmsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AlarmsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AlarmsFragment : Fragment(), AlarmsAdapter.OnAlarmItemClickListener {
    // TODO: Rename and change types of parameters
    private var param2: String? = null
    private var mListener: OnAlarmsFragmentListener? = null
    private lateinit var mRealm: Realm
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RealmRecyclerViewAdapter<AlarmPeriod, AlarmsAdapter.AlarmViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alarms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val linearLayoutManager = LinearLayoutManager(context)

        mRealm = Realm.getDefaultInstance()

        mViewAdapter = AlarmsAdapter(this,
            mRealm.where(AlarmPeriod::class.java).findAllAsync())

        mRecyclerView = alarms_recycler_view.apply{
            // must set FixedSize to false, otherwise realm adapter will not auto-update
            setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = mViewAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRealm.close()
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAlarmsFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnAlarmsFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnAlarmsFragmentListener {
        fun onAlarmSelected(alarmPeriod : AlarmPeriod)
        fun onAlarmInListEnabled(alarmID: String, enabled: Boolean)
    }

    override fun onAlarmSwitchToggled(alarmID: String, isChecked: Boolean) {
        // let activity verify alarm
        mListener?.onAlarmInListEnabled(alarmID, isChecked)
    }

    override fun onAlarmTextClicked(item: AlarmPeriod) {
        // Show alarm details in another fragment, let activity handle fragment transaction
        mListener?.onAlarmSelected(item)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AlarmsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param2: String) =
            AlarmsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
