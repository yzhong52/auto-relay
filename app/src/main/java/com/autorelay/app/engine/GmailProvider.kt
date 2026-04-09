package com.autorelay.app.engine

import android.content.Context
import android.util.Log
import com.autorelay.app.data.RelayConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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

            val mimeMessage = createEmail(toEmail, accountEmail, subject, bodyText)
            val message = createMessageWithEmail(mimeMessage)

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

    private fun createEmail(
        to: String,
        from: String,
        subject: String,
        bodyText: String
    ): MimeMessage {
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)
        return MimeMessage(session).apply {
            setFrom(InternetAddress(from))
            addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(to))
            setSubject(subject)
            setText(bodyText)
        }
    }

    private fun createMessageWithEmail(content: MimeMessage): Message {
        val buffer = ByteArrayOutputStream()
        content.writeTo(buffer)
        val bytes = buffer.toByteArray()
        val encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(bytes)
        return Message().apply {
            raw = encodedEmail
        }
    }
}
