# Add project specific ProGuard rules here.

# Keep app's own classes
-keep class com.autorelay.app.** { *; }

# Google API client
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.auth.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Apache HttpClient (pulled in by Google API client, not used on Android)
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient

# JGSS / Kerberos (not available on Android)
-dontwarn org.ietf.jgss.**

# javax.naming (not available on Android)
-dontwarn javax.naming.**
-dontwarn javax.naming.directory.**
-dontwarn javax.naming.ldap.**

# General
-dontwarn java.lang.invoke.**
-dontwarn **$$Lambda$*
