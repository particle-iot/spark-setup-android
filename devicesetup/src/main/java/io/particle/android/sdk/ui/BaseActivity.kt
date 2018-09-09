package io.particle.android.sdk.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import androidx.navigation.Navigation
import io.particle.android.sdk.devicesetup.R

/**
 * This class exists solely to avoid requiring SDK users to have to define
 * anything in an Application subclass.  By (ab)using this custom Activity,
 * we can at least be sure that the custom fonts in the device setup screens
 * work correctly without any special instructions.
 */
// this is a base activity, it shouldn't be registered.
@SuppressLint("Registered")
class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
    }

    override fun onSupportNavigateUp(): Boolean {
        return Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()
    }

}
