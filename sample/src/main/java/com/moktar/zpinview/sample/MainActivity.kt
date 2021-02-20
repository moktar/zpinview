package com.moktar.zpinview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import com.moktar.zpinview.OnPinCompletionListener
import com.moktar.zpinview.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var view: ConstraintLayout
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        binding.pinViewLine.setPinCompletionListener(object : OnPinCompletionListener {
            override fun onCodeCompletion(otp: String) {
                Snackbar.make(view, otp, Snackbar.LENGTH_SHORT).show()
            }

        })
    }
}