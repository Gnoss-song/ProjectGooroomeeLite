package kr.co.gooroomeelite.views.statistics.share

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.tarek360.instacapture.Instacapture
import com.tarek360.instacapture.listener.SimpleScreenCapturingListener
import kr.co.gooroomeelite.R
import kr.co.gooroomeelite.databinding.ActivityStickerBinding
import kr.co.gooroomeelite.views.statistics.share.extensions.loadCenterCrop
import java.io.OutputStream

class StickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStickerBinding

    private var root: View? = null

    private val shareButtonViewImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStickerBinding.inflate(layoutInflater)
        root = binding.root
        setContentView(binding.root)
        val pictures = intent.getStringExtra("picture")
        Log.d("aaaas", pictures.toString())
        if (pictures != null) {
            imageContent(pictures)
        }
        val button: Button = findViewById(R.id.share_buttons_confirm)
        button.setOnClickListener { takeAndShareScreenShot(pictures.toString()) }
    }

    private fun imageContent(pictures: String) {
        Log.d("aaaai", pictures)
        Handler(Looper.getMainLooper()).post {
            Log.d("aaaau", pictures)
            binding.previewStickerImageView.loadCenterCrop(url = pictures, corner = 4f)
        }
    }

    // --- 캡처 후 공유 --
    private fun takeAndShareScreenShot(uri: String) {
        val buttonView: Button = findViewById(R.id.share_buttons_confirm)
        Instacapture.capture(this, object : SimpleScreenCapturingListener() {
            override fun onCaptureComplete(captureview: Bitmap) {
                val capture: ImageView = findViewById<ImageView>(R.id.previewStickerImageView)
                capture.buildDrawingCache()
                val captureview: Bitmap = capture.getDrawingCache()
                val uri = saveImageExternal(captureview)
                shareImageURI(uri)
            }
        }, buttonView)

    }


    private fun saveImageExternal(image: Bitmap): Uri? {
        val filename = "gooroomee_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var uri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, getString(R.string.app_content_path))
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        //use application context to get contentResolver
        val contentResolver = this.contentResolver

        contentResolver.also { resolver ->
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = uri?.let { resolver.openOutputStream(it) }
        }

        fos?.use { image.compress(Bitmap.CompressFormat.JPEG, 70, it) }

        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(uri!!, contentValues, null, null)

        return uri!!
    }


    private fun shareImageURI(uri: Uri?): Boolean {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/*"
        }
        startActivity(Intent.createChooser(shareIntent, "Send to"))
        return shareButtonViewImage
    }
}