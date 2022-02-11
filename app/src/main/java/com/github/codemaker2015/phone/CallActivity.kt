package com.github.codemaker2015.phone

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.telecom.Call
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_call.*
import java.util.concurrent.TimeUnit
import android.provider.ContactsContract
import android.util.Log
import android.widget.RelativeLayout





class CallActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()
    private lateinit var number: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        number = intent.data.schemeSpecificPart
    }

    override fun onStart() {
        super.onStart()

        answer.setOnClickListener {
            OngoingCall.answer()
        }

        hangup.setOnClickListener {
            OngoingCall.hangup()
        }

        OngoingCall.state
            .subscribe(::updateUi)
            .addTo(disposables)

        OngoingCall.state
            .filter { it == Call.STATE_DISCONNECTED }
            .delay(1, TimeUnit.SECONDS)
            .firstElement()
            .subscribe { finish() }
            .addTo(disposables)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(state: Int) {
        callInfoHeading1.text = "SIM1 call from"
        callInfoHeading2.text = getContactNameByPhoneNumber(applicationContext, number)


        answer.isVisible = state == Call.STATE_RINGING
        if(!answer.isVisible){
            val lp = hangup.getLayoutParams() as RelativeLayout.LayoutParams
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL)
            hangup.setLayoutParams(lp)
            callInfo.text = "Mobile $number"
        }else
            callInfo.text = "${state.toString().toLowerCase().capitalize()}\nMobile $number"


        hangup.isVisible = state in listOf(
            Call.STATE_DIALING,
            Call.STATE_RINGING,
            Call.STATE_ACTIVE
        )
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    companion object {
        fun start(context: Context, call: Call) {
            Intent(context, CallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(call.details.handle)
                .let(context::startActivity)
        }
    }

    fun getContactNameByPhoneNumber(context: Context, phoneNumber: String): String? {
        var phone = phoneNumber
        if(phoneNumber != null && phoneNumber.length > 0 && phoneNumber[0].equals('+'))
            phone = phoneNumber.substring(3)

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Phone.NUMBER, null, null
        ) ?: return ""
        for (i in 0 until cursor.count) {
            cursor.moveToPosition(i)
            val nameFieldColumnIndex = cursor
                .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val phoneFieldColumnIndex = cursor
                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            if(phone.equals(cursor.getString(phoneFieldColumnIndex)))
                return cursor.getString(nameFieldColumnIndex)
        }
        return "Unknown"
    }
}
