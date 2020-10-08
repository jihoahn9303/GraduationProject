package kr.ac.konkuk.smartcafe


import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.ktx.Firebase

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
        Thread(Runnable {
            val subpath = LoginActivity.userEmail?.split("@")!![0]
            val ref = Firebase.database.reference
            ref.child("userInfo").child("$subpath")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var map = snapshot.value
                        if (map != null) {
                            Log.d("snapshot_update", "$token")
                            var update_map = mutableMapOf<String, Any>()
                            update_map["token"] = token!!

                            ref.child("userInfo").child("$subpath")
                                .updateChildren(update_map)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }).start()
    }
}