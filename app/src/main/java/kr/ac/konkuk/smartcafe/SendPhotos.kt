package kr.ac.konkuk.smartcafe

import android.app.Activity
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.api.core.ApiFuture
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.Subscriber
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import com.google.gson.*
import kotlinx.android.synthetic.main.activity_send_photos.*
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlinx.coroutines.*
import kr.ac.konkuk.smartcafe.LoginActivity.Companion.token
import kotlin.coroutines.CoroutineContext


class SendPhotos : AppCompatActivity(), CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private var images: ArrayList<Uri?>? = null
    private var position = 0;
    private val PICK_IMAGES_CODE = 0;
    private var count = 0;
    private var storage : FirebaseStorage? = null
    private var auth : FirebaseAuth? = null
    private var firestore : FirebaseFirestore? = null
    private var fileUploadSuccessFlag : Boolean? = false
    private var publisher : Publisher? = null
    private lateinit var emailName : String
    private val projectId = "smartcafe-286310"
    private val topicId = "request-classfier"

    companion object {
        var category : String? = "basic"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_send_photos)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        images = ArrayList()

        imageSwitcher.setFactory { ImageView(applicationContext) }

        pickImagesBtn.setOnClickListener {
            pickImagesIntent()
        }

        nextBtn.setOnClickListener {
            if(position < images!!.size-1){
                position++
                imageSwitcher.setImageURI(images!![position])
            }
            else{
                Toast.makeText(this, "No More images...", Toast.LENGTH_SHORT).show()
            }
        }

        previousBtn.setOnClickListener {
            if (position > 0){
                position--
                imageSwitcher.setImageURI(images!![position])
            }
            else{
                Toast.makeText(this, "No More images...", Toast.LENGTH_SHORT).show()
            }
        }

        sendImagesBtn.setOnClickListener {
            launch {
                val jobSendPhotos = launch(Dispatchers.IO, CoroutineStart.LAZY) {sendImagesIntent()}
                val jobPublish = launch(Dispatchers.IO, CoroutineStart.LAZY) {publishToTopic()}
                Toast.makeText(this@SendPhotos, "사진 전송 완료! 잠시 후 메시지로 카테고리를 알려드립니다", Toast.LENGTH_LONG).show()
                jobSendPhotos.join()
                jobPublish.join()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun sendImagesIntent(){
        if (count == 0){
            Toast.makeText(this, "보낼 수 있는 이미지가 없습니다...", Toast.LENGTH_SHORT).show()
        }
        else{
            Log.d("start", "image send start")
            emailName = intent.getStringExtra("email")  // 인텐트 값 저장(사용자 이메일)
            for (i in 0 until count){
                var photoUri = images?.get(i)

                var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                var imageFileName = "${i}"+"${timestamp}.jpeg"

                var storageRef = storage?.reference?.child(emailName)?.child(imageFileName)
                storageRef?.putFile(photoUri!!)

                if (i == count - 1) { fileUploadSuccessFlag = true }
            }
        }
    }

    private fun pickImagesIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image(s)"), PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_CODE) {
            if(resultCode == Activity.RESULT_OK){
                if (data!!.clipData != null) {
                    count = data.clipData!!.itemCount
                    if (count > 3){
                        Toast.makeText(this, "3장까지의 이미지만 선택 가능합니다...", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        for (i in 0 until count) {
                            val imageUri = data.clipData!!.getItemAt(i).uri
                            images!!.add(imageUri)
                        }
                        imageSwitcher.setImageURI(images!![0])
                        position = 0
                    }
                }
                else {
                    val imageUri = data.data
                    imageSwitcher.setImageURI(imageUri)
                    position = 0
                }
            }
        }
    }

    private fun publishToTopic() {
        if(fileUploadSuccessFlag == true) {
            try {
                val topicName : TopicName = TopicName.of(projectId, topicId)
                val am : AssetManager = resources.assets
                val fileInputStream : InputStream? = am.open("smartcafe-286310-7630ecb69883.json")
                val creds = ServiceAccountCredentials.fromStream(fileInputStream)
                // Create a publisher instance with default settings bound to the topic
                publisher = Publisher.newBuilder(topicName).setCredentialsProvider(FixedCredentialsProvider.create(creds)).build()
                Log.d("publish", "$publisher")

                val jsonObject : JsonObject = JsonObject()
                jsonObject.addProperty("token", token)
                jsonObject.addProperty("email", emailName)
                val data = ByteString.copyFromUtf8(jsonObject.toString())
                val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()
                Log.d("message", "$pubsubMessage")

                // Once published, returns a server-assigned message id (unique within the topic)
                Log.d("start", "start publish message to topic")
                val messageIdFuture: ApiFuture<String>? = publisher?.publish(pubsubMessage)
                val messageId = messageIdFuture?.get()
                Log.d("id", messageId)
            } finally {
                // When finished with the publisher, shutdown to free up resources.
                publisher?.shutdown()
                publisher?.awaitTermination(1, TimeUnit.MINUTES)
                fileUploadSuccessFlag = false
                count = 0
            }
        }
    }
}







