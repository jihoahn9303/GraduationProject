package kr.ac.konkuk.smartcafe


import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class FCMIDService : FirebaseInstanceIdService(){
    override fun onTokenRefresh() {
        LoginActivity.token = FirebaseInstanceId.getInstance().getToken()
        Log.d("new", "Refreshed token: ${LoginActivity.token}")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(LoginActivity.token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d("sendNewToken", "sendRegistrationTokenToServer($token)")
    }
}