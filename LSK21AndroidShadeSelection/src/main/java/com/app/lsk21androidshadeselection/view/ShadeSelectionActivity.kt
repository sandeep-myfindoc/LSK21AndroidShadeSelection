package com.app.lsk21androidshadeselection.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.lsk21androidshadeselection.R
import com.app.lsk21androidshadeselection.databinding.ActivityShadeSelectionBinding
import com.app.lsk21androidshadeselection.modal.ModalToParse
import com.app.lsk21androidshadeselection.util.YuvToRgbConverter
import com.app.teethdetectioncameralibrary.viewModel.ShadeSelectionViewModel
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CompletableFuture


class ShadeSelectionActivity : AppCompatActivity() {
    private lateinit var binding:ActivityShadeSelectionBinding
    private lateinit var arFragment: ArFragment
    private val modelFiles = arrayListOf<ModalToParse>()
    private val selectedShades = arrayListOf<String>()
    private val yAxis = -0.034f
    private val shiftYAxis: Float = 0.0040f
    private lateinit var viewModel: ShadeSelectionViewModel
    val modelNode: HashMap<String, TransformableNode> = HashMap()
    private  var base64: String? = null
    private var capturedBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shade_selection)
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        viewModel = ViewModelProvider(this).get(ShadeSelectionViewModel::class.java)
        /*binding.startTest.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("data","Data From App")
            setResult(Activity.RESULT_OK,resultIntent)
            finish()
        }*/
        try{
            var cnt = 1
            for(cnt in 1..5){
                modelFiles.add(ModalToParse("modal/tab".plus("1.glb"),
                    "modal/textures/CB".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..7){
                modelFiles.add(ModalToParse("modal/tab".plus("1.glb"),
                    "modal/textures/LS".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..5){
                modelFiles.add(ModalToParse("modal/tab".plus("1.glb"),
                    "modal/textures/MS".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..3){
                modelFiles.add(ModalToParse("modal/tab".plus("1.glb"),
                    "modal/textures/YS".plus(cnt).plus("_BaseColor.png")))
            }
            GlobalScope.launch(Dispatchers.Main){
                var x: Float = -0.067f//-0.056f
                modelFiles.forEach { modelToParse ->
                    loadModelAsync(modelToParse.modelPath).thenAccept { renderable ->
                        loadTextureAsync(modelToParse.texturePath).thenAccept {texture->
                            loadRoughnessAsync("modal/textures/Tab_Tooth_Roughness.png").thenAccept { roughnessTexture ->
                                loadMetalicAsync("modal/textures/Tab_Tooth_Metallic.png").thenAccept { metallicTexture ->
                                    if (renderable != null) {
                                        runOnUiThread {
                                            val ambientColor = Color(0.5f, 0.5f, 0.5f)
                                            renderable?.material?.setTexture("baseColor", texture)
                                            renderable?.material?.setTexture("metallic", metallicTexture)// metallicTexture
                                            renderable?.material?.setTexture("roughness", roughnessTexture)//roughnessTexture
                                            renderable?.material?.setFloat3("ambientColor", ambientColor)
                                            renderable?.material?.setFloat3("baseColorFactor", Color(1.0f, 1.0f, 1.0f))
                                            /*renderable.isShadowCaster = true
                                            renderable.isShadowReceiver = true*/
                                            var temp = TransformableNode(arFragment.transformationSystem)
                                            modelNode[fetchCode(modelToParse.texturePath)] = temp
                                            addModelToScene(renderable,x,temp)
                                            x += 0.0070f//0.006
                                        }
                                    }
                                }

                            }

                        }

                    }
                }
            }
        }catch(ex: Exception){
            ex.printStackTrace()
        }
        observeData()
    }
    private fun observeData(){
        viewModel.errMessage.observe(this@ShadeSelectionActivity, Observer {
            if(it.toString().isNotEmpty()){
                showToast(it.toString())
                binding.txtMsg.visibility = View.GONE
            }
        })
        viewModel.teetShadeResponseLiveData.observe(this@ShadeSelectionActivity, Observer {
            if(it.status.toString().equals("1")){
                binding.btnAiIcon.isEnabled = true
                if(it.colorRecommendation!=null && it.colorRecommendation.color1!=null){
                    selectedShades.add(it.colorRecommendation.color1.shadeCode)
                    updateOnBasisOfShadeCode(it.colorRecommendation.color1.shadeCode)
                }
                if(it.colorRecommendation!=null && it.colorRecommendation.color2!=null){
                    selectedShades.add(it.colorRecommendation.color2.shadeCode)
                    updateOnBasisOfShadeCode(it.colorRecommendation.color2.shadeCode)
                }
                if(it.colorRecommendation!=null && it.colorRecommendation.color3!=null){
                    selectedShades.add(it.colorRecommendation.color3.shadeCode)
                    updateOnBasisOfShadeCode(it.colorRecommendation.color3.shadeCode)
                }
                binding.btnSubmit.visibility = View.VISIBLE
                binding.txtMsg.visibility = View.GONE
            }else{
                binding.btnAiIcon.isEnabled = true
                showToast(it.message.toString())
                binding.txtMsg.visibility = View.GONE
            }
        })
    }
    private fun loadTextureAsync(modelUri: String): CompletableFuture<Texture?> {
        val future = Texture.builder()
            .setSource(this@ShadeSelectionActivity, Uri.parse(modelUri))
            .build()
        return future.exceptionally { throwable ->
            throwable.printStackTrace()
            null
        }
    }
    private fun loadMetalicAsync(modelUri: String): CompletableFuture<Texture?> {
        val future = Texture.builder()
            .setSource(this@ShadeSelectionActivity, Uri.parse(modelUri))
            .build()
        return future.exceptionally { throwable ->
            throwable.printStackTrace()
            null
        }
    }
    private fun loadRoughnessAsync(modelUri: String): CompletableFuture<Texture?> {
        val future = Texture.builder()
            .setSource(this@ShadeSelectionActivity, Uri.parse(modelUri))
            .build()
        return future.exceptionally { throwable ->
            throwable.printStackTrace()
            null
        }
    }
    private fun loadModelAsync(modelUri: String): CompletableFuture<ModelRenderable?> {
        val future = ModelRenderable.builder()
            .setSource(this, RenderableSource.builder()
                .setSource(this, Uri.parse(modelUri), RenderableSource.SourceType.GLB)
                .build())
            .setRegistryId(modelUri)
            .build()
        return future.exceptionally { throwable ->
            throwable.printStackTrace()
            null
        }
    }
    private fun addModelToScene(modelRenderable: ModelRenderable,x: Float,modelNodeTemp: TransformableNode) {
        // Create a new AnchorNode to holod the model
        val anchorNode = AnchorNode()
        anchorNode.setParent(arFragment.arSceneView.scene)

        // Create a TransformableNode for the model
        //sceneView.scene.view.scene.camera
        //modelNode.setParent(anchorNode)
        //val modelNode = TransformableNode(arFragment.transformationSystem)
        modelNodeTemp.setParent(arFragment.arSceneView.scene.camera)
        modelNodeTemp.renderable = modelRenderable

        // Set the position of the model in the scene
        //modelNodeTemp.localPosition = com.google.ar.sceneform.math.Vector3(x,0.065f,-0.029f)//y increase vlue move upward, z decrease value enlarge it
        //modelNodeTemp.localPosition = com.google.ar.sceneform.math.Vector3(x,0.09f,-0.21f)
        //modelNodeTemp.localPosition = com.google.ar.sceneform.math.Vector3(x,0.084f,-0.35f)//x,0.069f,-0.30f
        //modelNodeTemp.localPosition = com.google.ar.sceneform.math.Vector3(x,0.022f,-0.1f)
        modelNodeTemp.localPosition = com.google.ar.sceneform.math.Vector3(x,yAxis,-0.1f)
        // to scale model
        modelNodeTemp.scaleController.minScale = 0.05f
        modelNodeTemp.scaleController.maxScale = 1.3f
        modelNodeTemp.localScale = com.google.ar.sceneform.math.Vector3(1.0f, 1.0f, 1.0f)
        modelNodeTemp.light = addPointLight()
        arFragment.arSceneView.setOnTouchListener{ _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleTouch(event)
            }
            true
        }
    }
    private fun handleTouch(event: MotionEvent) {
        val hitTestResult = arFragment.arSceneView.scene.hitTest(event)
        if(hitTestResult.node!=null){
            onModelClicked(hitTestResult.node!!)
        }

    }
    private fun getAverageRgbFromBitmap(bitmap: Bitmap): Array<Float> {
        if (bitmap==null){
            val floatArray: Array<Float> = arrayOf(1.0f, 1.0f, 1.0f)
            return floatArray
        }
        val width = bitmap.width
        val height = bitmap.height
        var redTotal = 0
        var greenTotal = 0
        var blueTotal = 0
        var pixelCount = 0

        // Sample pixels to calculate average RGB values
        val sampleSize = 10 // Sample every 10 pixels
        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                redTotal += android.graphics.Color.red(pixel)
                greenTotal += android.graphics.Color.green(pixel)
                blueTotal += android.graphics.Color.blue(pixel)

                pixelCount++
            }
        }

        // Calculate average RGB values
        val averageRed = redTotal / pixelCount
        val averageGreen = greenTotal / pixelCount
        val averageBlue = blueTotal / pixelCount
        val floatArray: Array<Float> = arrayOf(averageRed.toFloat(), averageGreen.toFloat(), averageBlue.toFloat())
        return floatArray
    }
    private fun addPointLight(): Light {
        var pointLight = Light.builder(Light.Type.DIRECTIONAL)
            .setColor(Color(1.0f,1.0f,1.0f))
            //.setColorTemperature(2000f)
            .setShadowCastingEnabled(true)
            .setIntensity(100f)
            .setFalloffRadius(2.0f)
            .build()
        return pointLight
        /*val lightNode = Node()
        lightNode.light = pointLight
        lightNode.worldPosition = Vector3(1f,0f,0f)
        lightNode.setParent(arFragment.arSceneView.scene)*/
    }
    private fun onModelClicked(node: Node) {
        if (node != null) {
            val shadeCode = fetchCodeFromNode(node)
            if(node.localPosition.y == yAxis-yAxis){
                if(shadeCode!=null){
                    selectedShades.remove(shadeCode)
                    placeAtYAxis(node)
                }

                return
            }
            if(selectedShades.size<6){
                if(shadeCode!=null){
                    selectedShades.add(shadeCode)
                    shiftYAxis(node)
                }
            }else{
                showToast("Sorry, you have reached maximum limit")
            }
        }
    }
    public fun fetchShade(view: View) {
        if(selectedShades.size<=3){
            // Launch a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                val result = copyPixels()
                if(result==PixelCopy.SUCCESS && capturedBitmap!=null){
                    runOnUiThread {
                        binding.txtMsg.visibility = View.VISIBLE
                        binding.btnAiIcon.isEnabled = false
                    }
                    base64 = bitmapToBase64(capturedBitmap!!)
                    saveImage(capturedBitmap!!)

                }else{
                    runOnUiThread {
                        Toast.makeText(this@ShadeSelectionActivity,"Fail to Process Bitmap",Toast.LENGTH_LONG).show()
                    }
                }
                /*withContext(Dispatchers.Main) {
                    if (result == PixelCopy.SUCCESS && capturedBitmap!=null) {
                        base64 = bitmapToBase64(capturedBitmap!!)
                        saveImage(capturedBitmap!!)
                        Toast.makeText(this@ShadeSelectionActivity,"Process Bitmap",Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ShadeSelectionActivity,"Fail to Process Bitmap",Toast.LENGTH_LONG).show()
                    }
                }*/
            }

        }else{
            showToast("you already selected more than three tabs manually")
        }
    }
    private fun saveImage(bitmap: Bitmap?){
        val fileOutputStream: FileOutputStream
        var dir = filesDir.absolutePath
        val name = "captured_image_".plus(getCurrentTimeInHHmm()).plus(".png")
        try {
            var file = File(dir.plus("/").plus(name))
            fileOutputStream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
            MainScope().launch {
                viewModel.fetchShades(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun showToast(msg: String){
        Toast.makeText(this@ShadeSelectionActivity,msg, Toast.LENGTH_LONG).show()
    }
    private fun fetchCode(texturePath: String):String{
        var ar = texturePath.split('_')
        var nameWithPath = ar[0]
        var ar1 = nameWithPath.split('/')
        return ar1[2]
    }
    private fun fetchCodeFromNode(node: Node):String?{
        var code: String? = null
        for ((key, value) in modelNode) {
            if(value==node){
                code = key
                break
            }
        }
        return code
    }
    private fun updateOnBasisOfShadeCode(shadeCode: String){
        for ((key, value) in modelNode) {
            if(key.equals(shadeCode)){
                shiftYAxis(value)
                break
            }
        }
    }
    private fun shiftYAxis(node: Node){
        node.localPosition = com.google.ar.sceneform.math.Vector3(
            node.localPosition.x + 0.0f,
            node.localPosition.y - yAxis,
            node.localPosition.z + 0.0f
        )
    }
    private fun placeAtYAxis(node: Node){
        node.localPosition = com.google.ar.sceneform.math.Vector3(
            node.localPosition.x + 0.0f,
            yAxis,
            node.localPosition.z + 0.0f
        )
    }
    public fun moveBack(view: View){
        val resultIntent = Intent()
        val resultString = "back,backbuton"
        resultIntent.putExtra("data",resultString)
        setResult(Activity.RESULT_CANCELED,resultIntent)
        finish()
    }
    public fun moveForward(view: View){
        var resultString = "submit,"
        for (item in selectedShades){
            if(item!=null){
                resultString = resultString.plus(item).plus(",")
            }
        }
        resultString.plus(base64)
        val resultIntent = Intent()
        resultString = resultString.plus(base64)
        resultIntent.putExtra("data",resultString)
        setResult(Activity.RESULT_OK,resultIntent)
        finish()
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP) // Use NO_WRAP to avoid newlines
    }
    private fun getCurrentTimeInHHmm(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    private suspend fun copyPixels(): Int {
        val view: ArSceneView = arFragment.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { continuation ->
            PixelCopy.request(view, bitmap, { result ->
                if(bitmap!=null)
                    this.capturedBitmap = bitmap
                continuation.resume(result) {
                }
            }, Handler(Looper.getMainLooper()))
        }
    }
    private suspend fun fetchBitMap(image: Image): Bitmap?{
        var bitmap: Bitmap? = null
        try{
            val yuvConverter = YuvToRgbConverter()
            bitmap = yuvConverter.imageToBitmap(image)
        }catch (ex: Exception){
            Log.e("Fail to create image :",ex.message.toString())
        }
        return bitmap
    }
    /*@RequiresApi(Build.VERSION_CODES.KITKAT)
    fun captureScreenshot(){

        val view: ArSceneView = arFragment.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult === PixelCopy.SUCCESS) {
                // Save bitmap
                runOnUiThread {
                    Toast.makeText(this@ShadeSelectionActivity,"Process Bitmap",Toast.LENGTH_LONG).show()
                   saveImage(bitmap)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@ShadeSelectionActivity,"Fail to capture image",Toast.LENGTH_LONG).show()
                }
            }
        }, Handler(Looper.getMainLooper()))
    }*/
}


/*
var image: Image? = null
try {
    captureScreenshot()
    val frame = arFragment.arSceneView.arFrame
    if (true) {
        MainScope().launch {
            var bitmap: Bitmap? = null
            var result:Int = -1
            CoroutineScope(Dispatchers.IO).async {
                result = copyPixels()
                image = frame!!.acquireCameraImage()
                if (image != null) {
                    bitmap = fetchBitMap(image!!)
                }
            }.await()
            if (bitmap != null) {
                base64 = bitmapToBase64(bitmap!!)
                saveImage(bitmap!!)
            }
            if(result == PixelCopy.SUCCESS && capturedBitmap!=null){
                base64 = bitmapToBase64(capturedBitmap!!)
                saveImage(capturedBitmap!!)
            }
        }
    }
} catch (ex: DeadlineExceededException) {
    binding.btnAiIcon.isEnabled = true
    Log.e("Dead Line Exp: ", ex.message.toString())
} catch (ex: Exception) {
    Log.e("Fail to Create Bitmap: ", ex.message.toString())
} finally {
    image?.close()
}*/