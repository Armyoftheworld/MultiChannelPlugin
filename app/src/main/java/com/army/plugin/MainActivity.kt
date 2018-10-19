package com.army.plugin

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv_text.setOnClickListener {
            val channelInfo = ChannelHelper.getChannelInfo(File(applicationInfo.sourceDir))
            Toast.makeText(this, "channel = $channelInfo", Toast.LENGTH_SHORT).show()
        }
    }
}
