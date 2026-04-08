package com.autorelay.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoRelay"
        private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        Log.d(TAG, "SMS_RECEIVED broadcast received")

        val bundle = intent.extras ?: run {
            Log.w(TAG, "No extras in SMS intent")
            return
        }

        val format = bundle.getString("format")
        val pdus = bundle.get("pdus") as? Array<*> ?: run {
            Log.w(TAG, "No PDUs found in SMS intent")
            return
        }

        if (pdus.isEmpty()) {
            Log.w(TAG, "PDU array is empty")
            return
        }

        // Group multi-part messages by sender
        val messageMap = mutableMapOf<String, StringBuilder>()
        var lastTimestamp = 0L

        for (pdu in pdus) {
            val pduBytes = pdu as? ByteArray ?: continue
            val smsMessage: SmsMessage = if (format != null) {
                SmsMessage.createFromPdu(pduBytes, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pduBytes)
            }

            val sender = smsMessage.displayOriginatingAddress ?: smsMessage.originatingAddress ?: "Unknown"
            val body = smsMessage.displayMessageBody ?: smsMessage.messageBody ?: ""
            val timestampMillis = smsMessage.timestampMillis

            messageMap.getOrPut(sender) { StringBuilder() }.append(body)
            lastTimestamp = timestampMillis
        }

        val config = RelayConfig(context)

        for ((sender, bodyBuilder) in messageMap) {
            val body = bodyBuilder.toString()
            val timestamp = TIMESTAMP_FORMAT.format(Date(lastTimestamp))

            Log.i(TAG, "─────────────────────────────────────")
            Log.i(TAG, "Incoming SMS")
            Log.i(TAG, "  From      : $sender")
            Log.i(TAG, "  Body      : $body")
            Log.i(TAG, "  Timestamp : $timestamp ($lastTimestamp ms)")
            Log.i(TAG, "─────────────────────────────────────")

            val actions = resolveActions(context, config)
            RelayLog.add(
                sender = sender,
                message = body,
                source = LogEntry.Source.SMS,
                actions = actions
            )
        }
    }

}
