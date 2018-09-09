package io.particle.android.sdk.utils


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A simple [Fragment] subclass used as a marker for "worker" fragments, while bundling in the
 * fundamental behavior that makes them worker fragments.
 */
open class WorkerFragment : Fragment() {


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return null
    }

    companion object {

        // produce a standardized, unique tag
        fun buildFragmentTag(fragClass: Class<out Fragment>): String {
            // return a unique name
            return "FRAG_" + fragClass.canonicalName
        }

        // Syntactic sugar for simply adding a WorkerFragment
        fun addFragment(activity: FragmentActivity, frag: Fragment, tag: String) {
            activity.supportFragmentManager
                    .beginTransaction()
                    .add(frag, tag)
                    .commit()
        }

        // Syntactic sugar for simply adding a WorkerFragment
        fun addFragment(parent: Fragment, frag: Fragment, tag: String) {
            parent.childFragmentManager
                    .beginTransaction()
                    .add(frag, tag)
                    .commit()
        }
    }

}
