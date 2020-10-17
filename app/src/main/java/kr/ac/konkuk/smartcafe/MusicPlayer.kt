package kr.ac.konkuk.smartcafe

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jean.jcplayer.model.JcAudio
import com.example.jean.jcplayer.view.JcPlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_music_player.*
import java.util.*


class MusicPlayer : AppCompatActivity() {
    private var checkPermission = false
    var uri: Uri? = null
    var songName: String? = null
    var songUrl: String? = null
    var listView: ListView? = null

    var arrayListSongsName = ArrayList<String>()
    var arrayListSongsUrl = ArrayList<String>()
    var arrayAdapter: ArrayAdapter<String>? = null

    var jcPlayerView: JcPlayerView? = null
    var jcAudios = ArrayList<JcAudio>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        listView = myListView
        jcPlayerView = jcplayer
        retrieveSongs()
        myListView.setOnItemClickListener { _, _, position, _ ->
            jcplayer.playAudio(jcAudios[position])
            jcplayer.visibility = View.VISIBLE
            jcplayer.createNotification()
        }
    }



    private fun retrieveSongs() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Songs").child("${SetOptions.category}")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    for (ds in dataSnapshot.children) {
                        var map = ds.value as Map<String, Any>
                        arrayListSongsName.add(map["songName"].toString())
                        arrayListSongsUrl.add(map["songUri"].toString())
                        jcAudios.add(
                            JcAudio.createFromURL(
                                map["songName"].toString(),
                                map["songUri"].toString()
                            )
                        )
                    }
                    arrayAdapter = ArrayAdapter(
                        this@MusicPlayer,
                        android.R.layout.simple_list_item_1,
                        arrayListSongsName
                    )
                    jcPlayerView?.initPlaylist(jcAudios, null)
                    listView?.adapter = arrayAdapter
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.custom_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_upload) {
            if (validatePermision()) {
//                 pickSong()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun pickSong() {
        val intent_upload = Intent()
        intent_upload.type = "audio/*"
        intent_upload.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent_upload, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                uri = data!!.data
                val mcursor = applicationContext.contentResolver
                    .query(uri!!, null, null, null, null)
                val indexedname = mcursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                mcursor.moveToFirst()
                songName = mcursor.getString(indexedname)
                mcursor.close()
                uploadSongToFirebaseStorage()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun uploadSongToFirebaseStorage() {
        val storageReference = FirebaseStorage.getInstance().reference
            .child("Songs").child(uri!!.lastPathSegment!!)
        val progressDialog = ProgressDialog(this)
        progressDialog.show()
        storageReference.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isComplete);
            val urlSong = uriTask.result
            songUrl = urlSong.toString()
            uploadDetailsToDatabase()
            progressDialog.dismiss()
        }.addOnFailureListener { e ->
            Toast.makeText(this@MusicPlayer, e.message.toString(), Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }.addOnProgressListener { taskSnapshot ->
            val progres = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
            val currentProgress = progres.toInt()
            progressDialog.setMessage("Uploaded: $currentProgress%")
        }
    }

    private fun uploadDetailsToDatabase() {
        val songObj = Song(songName, songUrl)
        FirebaseDatabase.getInstance().getReference("Songs")
            .push().setValue(songObj).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@MusicPlayer, "Song Uploaded", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this@MusicPlayer,
                    e.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun validatePermision(): Boolean {
        Dexter.withActivity(this@MusicPlayer)
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    checkPermission = true
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    checkPermission = false
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
        return checkPermission
    }
}
