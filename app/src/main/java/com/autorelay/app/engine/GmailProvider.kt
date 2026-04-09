package com.autorelay.app.engine

import android.content.Context
import android.util.Base64
import android.util.Log
import com.autorelay.app.data.RelayConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message

object GmailProvider {
    private const val TAG = "GmailProvider"

    fun sendEmail(
        context: Context,
        toEmail: String,
        subject: String,
        bodyText: String
    ): Boolean {
        val config = RelayConfig(context)
        val accountEmail = config.googleAccountEmail

        if (accountEmail.isBlank()) {
            Log.e(TAG, "No Google account configured")
            return false
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null || account.email != accountEmail) {
            Log.e(TAG, "Signed in account mismatch or not found")
            return false
        }

        return try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(GmailScopes.GMAIL_SEND)
            ).apply {
                selectedAccount = account.account
            }

            val service = Gmail.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("AutoRelay").build()

            val message = buildRawMessage(accountEmail, toEmail, subject, bodyText)

            val sentMessage = service.users().messages().send("me", message).execute()
            Log.i(TAG, "Email successfully handed off to Gmail API.")
            Log.i(TAG, "  To: $toEmail")
            Log.i(TAG, "  Message ID: ${sentMessage.id}")
            Log.i(TAG, "  Thread ID: ${sentMessage.threadId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email via Gmail API", e)
            false
        }
    }

    private fun buildRawMessage(from: String, to: String, subject: String, body: String): Message {
        val mime = "From: $from\r\nTo: $to\r\nSubject: ${encodeMimeHeader(subject)}\r\n" +
                "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n$body"
        val encoded = Base64.encodeToString(mime.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return Message().apply { raw = encoded }
    }

    private fun encodeMimeHeader(value: String): String =
        if (value.any { it.code > 127 }) {
            "=?UTF-8?B?${Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}?="
        } else value
}
