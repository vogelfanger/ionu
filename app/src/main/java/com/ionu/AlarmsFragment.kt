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
class AlarmsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mAlarmData: List<String>? = null
    private var param2: String? = null
    private var mListener: OnAlarmsFragmentListener? = null
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>

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

        //TODO remove placeholder list from adapter
        val placeholderAlarms: List<String> = listOf("11:00-12:30", "13:00-14:15",
            "14:30-16:00", "19:00-20:30", "20:30-20:45", "21:00-21:15", "21:45-22:15", "22:30-23:00")

        mViewAdapter = AlarmsAdapter(placeholderAlarms)
        mRecyclerView = alarms_recycler_view.apply{
            // recycler view size doesn't change, set fixed to improve performance
            setHasFixedSize(true)
            layoutManager = linearLayoutManager
            adapter = mViewAdapter
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        mListener?.onAlarmsFragmentInteraction()
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
        // TODO: Update argument type and name
        fun onAlarmsFragmentInteraction()
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