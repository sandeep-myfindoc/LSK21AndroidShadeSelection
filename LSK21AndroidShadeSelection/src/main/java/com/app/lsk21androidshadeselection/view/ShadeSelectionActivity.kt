package com.app.lsk21androidshadeselection.view

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.lsk21androidshadeselection.R
import com.app.lsk21androidshadeselection.databinding.ActivityShadeSelectionBinding
import com.app.lsk21androidshadeselection.modal.ModalToParse
import com.app.lsk21androidshadeselection.network.UploadFileToServer
import com.app.lsk21androidshadeselection.util.ResultReceiver
import com.app.lsk21androidshadeselection.util.YuvToRgbConverter
import com.app.teethdetectioncameralibrary.viewModel.ShadeSelectionViewModel
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import vn.luongvo.widget.iosswitchview.SwitchView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.math.min


class ShadeSelectionActivity : BaseActivity(),ResultReceiver {
    private lateinit var binding:ActivityShadeSelectionBinding
    private lateinit var arFragment: ArFragment
    private val modelFiles = arrayListOf<ModalToParse>()
    private val selectedShades = arrayListOf<String>()
    private val yAxis = -0.038f
    private val shiftYAxis: Float = 0.004f
    private val minScale: Float = 0.05f
    private val maxScale: Float = 1.60f
    private val zoomAbleScale: Float = 2.10f
    private lateinit var viewModel: ShadeSelectionViewModel
    val modelNode: HashMap<String, TransformableNode> = HashMap()
    val renderableList = arrayListOf<ModelRenderable>()
    private  var imagePath: String? = null
    private var modelIndex: Int  = 0
    private var x: Float = -0.068f
    private val width = 0.0090f
    private var cntOfMannualSelection: Int = 0
    private var mSensorManager: SensorManager? = null
    private var mLightSensor: Sensor? = null
    private var mLightQuantity = 0f
    private var isFirstTime = true
    private val handler: Handler = Handler()
    private val delay: Int = 2000
    private var isSubmitClicked = false
    private var cntRightMove = 0
    private var cntLeftMove = 0
    val lightNode = Node()
    private var session: Session? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shade_selection)
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        viewModel = ViewModelProvider(this).get(ShadeSelectionViewModel::class.java)
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        arFragment.arSceneView.planeRenderer.isVisible = false
        lightNode.setParent(arFragment.arSceneView.scene)
        checkPermission()
        try{
            var cnt = 1
            for(cnt in 1..5){
                modelFiles.add(ModalToParse("model/CB".plus(cnt).plus(".glb"),
                    "modal/textures/CB".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..7){
                modelFiles.add(ModalToParse("model/LS".plus(cnt).plus(".glb"),
                    "modal/textures/LS".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..5){
                modelFiles.add(ModalToParse("model/MS".plus(cnt).plus(".glb"),
                    "modal/textures/MS".plus(cnt).plus("_BaseColor.png")))
            }
            cnt = 1
            for(cnt in 1..3){
                modelFiles.add(ModalToParse("model/YS".plus(cnt).plus(".glb"),
                    "modal/textures/YS".plus(cnt).plus("_BaseColor.png")))
            }
            // Load 3D Modal
            /*GlobalScope.launch(Dispatchers.Main){
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
                                            //renderable?.material?.setFloat3("ambientColor", ambientColor)
                                            renderable?.material?.setFloat3("baseColorFactor", Color(1.0f, 1.0f, 1.0f))
                                            renderable.isShadowCaster = false
                                            renderable.isShadowReceiver = false
                                            var temp = TransformableNode(arFragment.transformationSystem)
                                            modelNode[fetchCode(modelToParse.texturePath)] = temp
                                            renderableList.add(renderable)
                                            addModelToScene(renderable,x,temp)
                                            x += 0.0067f//0.006
                                        }
                                    }
                                }

                            }

                        }

                    }
                }
            }*/
        }catch(ex: Exception){
            ex.printStackTrace()
        }
        //observeData()
        //updateModalBasedOnLight()
        GlobalScope.launch(Dispatchers.Main) {
            binding.btnAiIcon.isEnabled = false
            loadNextModel()
        }
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT);
        addDirectionalLight(600f)
        binding.swFlash.setOnCheckedChangeListener(checkedListener)
        //arFragment.arSceneView.scene.sunlight?.light?.intensity = 700f
    //arFragment.transformationSystem.selectionVisualizer = BlanckSelectionVisualizer()
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(sensorListener,mLightSensor,SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager?.unregisterListener(sensorListener)
    }
    private fun checkPermission(){
        if(ContextCompat.checkSelfPermission(this@ShadeSelectionActivity,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            session = Session(this@ShadeSelectionActivity)
            setupCameraFocusMode(false)
        }
    }
    private fun loadNextModel() {
        if (modelIndex < modelFiles.size) {
            val modelData = modelFiles[modelIndex]
            ModelRenderable.builder()
                .setSource(this, RenderableSource.builder()
                    .setSource(this, Uri.parse(modelData.modelPath), RenderableSource.SourceType.GLB)
                    .build())
                .build()
                .thenAccept { renderable ->
                    /*val ambientColor = Color(1.0f, 1.0f, 1.0f)
                    renderable.material.setFloat("metallic", 1.0f);
                    renderable.material.setFloat("roughness", 0.1f);
                    renderable?.material?.setFloat3("ambientColor", ambientColor)*/
                    renderable.isShadowCaster = false
                    renderable.isShadowReceiver = false
                    var temp = TransformableNode(arFragment.transformationSystem)
                    modelNode[fetchCode(modelData.texturePath)] = temp
                    renderableList.add(renderable)
                    addModelToScene(renderable,x,temp)
                    modelIndex++
                    x += width
                    loadNextModel()
                }
                .exceptionally { throwable ->
                    throwable.printStackTrace()
                    null
                }
        }else{
            Handler().postDelayed({
                binding.btnAiIcon.isEnabled = true
            }, 1500)
        }
    }
    private fun updateModalBasedOnLight(){
        arFragment.arSceneView.scene.addOnUpdateListener{frameTime->
            val frame = arFragment.arSceneView.arFrame
            if(frame!=null){
                val lightEstimate = frame.lightEstimate
                if(lightEstimate!=null){
                    val ambientIntensity: Float = lightEstimate.pixelIntensity
                    val color = getColorFromAmbientIntensity(ambientIntensity)
                    updateMaterialColor(color)
                }
            }
        }
    }
    private fun getColorFromAmbientIntensity(intensity: Float): Int {
        val r = min((intensity * 2.55).toInt().toDouble(), 255.0).toInt()
        return android.graphics.Color.rgb(r, r, r)
    }
    private fun updateMaterialColor(lightColor: Int){
        val rgb = floatArrayOf(
            ((lightColor shr 16) and 0xFF) / 255.0f,
            ((lightColor shr 8) and 0xFF) / 255.0f,
            (lightColor and 0xFF) / 255.0f
        )
        //addPointLight(rgb)
        Log.e("RGB color is: ",rgb[0].toString().plus(" : ").plus(rgb[1].toString()).plus(" : ").plus(rgb[2].toString()))
        for(item in renderableList){
            var material = item.material
            material.setFloat3("baseColor", Color(rgb[0],rgb[1],rgb[2]))
        }
    }
    private fun writeFileToMediaStore(fileName: String, content: String) {
        // Prepare the ContentValues to define the file
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/MyAppFiles") // Specify the directory
        }

        // Insert the file into MediaStore and get the URI
        val uri: Uri? = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)

        uri?.let {
            // Open an output stream to write content to the file
            contentResolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(content.toByteArray())
                outputStream?.flush()
                //Toast.makeText(this, "File written: $fileName", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
        }
    }
    private fun observeData(){
        viewModel.errMessage.observe(this@ShadeSelectionActivity, Observer {
            if(it.toString().isNotEmpty()){
                showToast(it.toString())
                binding.layoutMsg.visibility = View.GONE
            }
        })
        viewModel.teetShadeResponseLiveData.observe(this@ShadeSelectionActivity, Observer {
            if(it.status.toString().equals("1")){
                /*writeFileToMediaStore("Log.txt", it.toString())*/
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
                    //Toast.makeText(this@ShadeSelectionActivity,"Respose Received: ".plus(it.colorRecommendation.color3.shadeCode.toString()),Toast.LENGTH_LONG).show()
                    updateOnBasisOfShadeCode(it.colorRecommendation.color3.shadeCode)
                }
                binding.btnSubmit.visibility = View.VISIBLE
                binding.layoutMsg.visibility = View.GONE
            }else{
                binding.btnAiIcon.isEnabled = true
                showToast(it.message.toString())
                binding.layoutMsg.visibility = View.GONE
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
        modelNodeTemp.scaleController.minScale = minScale
        modelNodeTemp.scaleController.maxScale = maxScale
        modelNodeTemp.localScale = com.google.ar.sceneform.math.Vector3(1.0f, 1.0f, 1.0f)
        //modelNodeTemp.light = addPointLight()
        /*if(modelNodeTemp.isSelected){
            modelNodeTemp.transformationSystem.selectNode(null)
        }*/
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
            if(!binding.btnAiIcon.isEnabled)
                return
            onModelClicked(hitTestResult.node!! as TransformableNode)
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
    private fun addPointLight(intensity: Float){//: Light {//temp: FloatArray
        var pointLight = Light.builder(Light.Type.POINT)
            //.setColor(Color(1.0f,1.0f,1.0f))
            //.setColorTemperature(2000f)
            .setShadowCastingEnabled(false)
            .setIntensity(intensity)
            //.setFalloffRadius(2.0f)
            .build()
        //return pointLight
        val lightNode = Node()
        lightNode.light = pointLight
        //lightNode.worldPosition = Vector3(1f,0f,0f)
        lightNode.setParent(arFragment.arSceneView.scene)
    }
    private fun addDirectionalLight(intensity: Float){
        arFragment.arSceneView.scene.removeChild(lightNode)
        var pointLight = Light.builder(Light.Type.DIRECTIONAL)
            //.setColor(Color(1.0f,1.0f,1.0f))
            .setShadowCastingEnabled(false)
            .setIntensity(intensity)
            .build()
        lightNode.light = pointLight
        lightNode.setParent(arFragment.arSceneView.scene)
    }
    private fun onModelClicked(node: TransformableNode) {
        if (node != null) {
            val shadeCode = fetchCodeFromNode(node)
            if(node.localPosition.y == yAxis+shiftYAxis){
                if(shadeCode!=null){
                    selectedShades.remove(shadeCode)
                    cntOfMannualSelection--
                    if(selectedShades.size==0){
                        binding.btnAiIcon.isEnabled = true
                    }
                    placeAtYAxis(node)
                }

                return
            }
            if(cntOfMannualSelection==3){
                return
            }
            if(selectedShades.size<6){
                if(shadeCode!=null){
                    selectedShades.add(shadeCode)
                    cntOfMannualSelection++
                    shiftYAxis(node)
                }
            }else{
                showToast("Sorry, you have reached maximum limit")
            }
        }
    }
    public fun fetchShade1(view: View) {
        for ((key, node) in modelNode) {
            if(selectedShades.contains(key)){
                placeAtYAxis(node)
                selectedShades.remove(key)
            }
        }
        cntOfMannualSelection = 0
        binding.btnSubmit.visibility = View.GONE
        // Launch a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val result = copyPixels()
            if(result==PixelCopy.SUCCESS){
                runOnUiThread {
                    binding.layoutMsg.visibility = View.VISIBLE
                    binding.btnAiIcon.isEnabled = false
                }
                //saveImage(capturedBitmap!!)

            }else{
                runOnUiThread {
                    Toast.makeText(this@ShadeSelectionActivity,"Fail to Process Bitmap",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    public fun fetchShade(view: View) {
        for ((key, node) in modelNode) {
            if(selectedShades.contains(key)){
                placeAtYAxis(node)
                selectedShades.remove(key)
            }
        }
        cntOfMannualSelection = 0
        binding.btnSubmit.visibility = View.GONE
        arFragment.arSceneView.scene.addOnUpdateListener(updateListener)
    }
    private fun unRegisterUpdateListener(){
        arFragment.arSceneView.scene.removeOnUpdateListener(updateListener)
    }
    private fun saveImage(bitmap: Bitmap?){
        val fileOutputStream: FileOutputStream
        var dir = filesDir.absolutePath
        val name = "android_".plus(getCurrentTimeInHHmm()).plus(".png")//captured_image
        try {
            var file = File(dir.plus("/").plus(name))
            fileOutputStream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
            bitmap.recycle()
            /*MainScope().launch {
                viewModel.fetchShades(file)
            }*/
            if(isSubmitClicked){
                imagePath = file.absolutePath
            }else{
                UploadFileToServer(file.absolutePath,this).execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    private fun shiftYAxis(node: TransformableNode){
        node.localPosition = com.google.ar.sceneform.math.Vector3(
            node.localPosition.x + 0.0f,
            node.localPosition.y + shiftYAxis,
            node.localPosition.z + 0.0f
        )
        node.scaleController.maxScale = zoomAbleScale
    }
    private fun placeAtYAxis(node: TransformableNode){
        node.localPosition = com.google.ar.sceneform.math.Vector3(
            node.localPosition.x + 0.0f,
            yAxis,
            node.localPosition.z + 0.0f
        )
        node.scaleController.maxScale = maxScale
    }
    public fun moveBack(view: View){
        val resultIntent = Intent()
        val resultString = "back,backbuton"
        resultIntent.putExtra("data",resultString)
        setResult(Activity.RESULT_CANCELED,resultIntent)
        finish()
    }
    public fun moveForward(view: View){
        try{
            var resultString = "submit,"
            if(selectedShades.size == 0){
                showToast("Please select the shade before submitting")
                return
            }
            showProgressBar()
            CoroutineScope(Dispatchers.IO).launch {
                val result = copyPixels()
                try{
                    if(result==PixelCopy.SUCCESS){
                        hideProgressBar()
                        for (item in selectedShades){
                            if(item!=null){
                                resultString = resultString.plus(item).plus(",")
                            }
                        }
                        val resultIntent = Intent()
                        resultString = resultString.plus(imagePath)
                        resultIntent.putExtra("data",resultString)
                        setResult(Activity.RESULT_OK,resultIntent)
                        imagePath = null
                        isSubmitClicked = false
                        finish()
                    }
                }catch(ex: Exception){
                    ex.printStackTrace()
                }
                catch(ex: Throwable){
                    ex.printStackTrace()
                }
            }
        }catch(ex: Exception){
            ex.printStackTrace()
        }catch(ex: Error){
            ex.printStackTrace()
        }catch(ex: DeadlineExceededException){
            ex.printStackTrace()
            //showToast("DeadlineExceededException-MoveForward")
        }
        catch (ex: Throwable){
            ex.printStackTrace()
            //showToast("Throwable-MoveForward")
        }
    }
    public fun moveLeft(view: View){
        if(cntLeftMove==6)
            return
        for ((key, node) in modelNode) {
            node.localPosition = com.google.ar.sceneform.math.Vector3(
                node.localPosition.x-(width*1),
                node.localPosition.y +0.0f,
                node.localPosition.z + 0.0f
            )

        }
        if(cntRightMove!=0){
            cntRightMove--
        }else{
            cntLeftMove++
        }

    }
    public fun moveRight(view: View){
        if(cntRightMove==6)
            return
        for ((key, node) in modelNode) {
            node.localPosition = com.google.ar.sceneform.math.Vector3(
                node.localPosition.x+(width*1),
                node.localPosition.y +0.0f,
                node.localPosition.z + 0.0f
            )

        }
        if(cntLeftMove!=0){
            cntLeftMove--
        }else{
            cntRightMove++
        }
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP) // Use NO_WRAP to avoid newlines
        }catch(ex:Exception){
            ex.printStackTrace()
            return ""
        }catch(ex:Throwable){
            ex.printStackTrace()
            return ""
        }

    }
    private fun getCurrentTimeInHHmm(): String {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat(
                "dd_MMMM_yyyy_HH:mm",
                Locale.getDefault()
            )//    "dd/MMMM/yyyy HH:mm"
            return dateFormat.format(calendar.time)
        }catch(ex: Exception){
            return ""
            ex.printStackTrace()
        }
    }
    private suspend fun copyPixels(): Int {
        val view: ArSceneView = arFragment.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { continuation ->
            try{
                PixelCopy.request(view, bitmap, { result ->
                    if(bitmap!=null){
                        isSubmitClicked = true
                        saveImage(bitmap)
                    }
                    continuation.resume(result) {
                    }
                }, Handler(Looper.getMainLooper()))
            }catch(ex: DeadlineExceededException){
                ex.printStackTrace()
            }catch(ex: Exception){
                ex.printStackTrace()
            }catch(ex: Throwable){
                ex.printStackTrace()
            }
        }
    }
    private suspend fun fetchBitMap(image: Image): Bitmap?{
        var bitmap: Bitmap? = null
        try{
            val yuvConverter = YuvToRgbConverter(this@ShadeSelectionActivity)
            bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            yuvConverter.yuvToRgb(image, bitmap)
        }catch (ex: Exception){
            Log.e("Fail to create image :",ex.message.toString())
        }
        return bitmap
    }

    override fun onSucess(response: String) {
        binding.layoutMsg.visibility = View.GONE
//        writeFileToMediaStore("Report.txt", response.toString())
        //val res = Gson().fromJson<AIResponse>(response.toString(),object : TypeToken<AIResponse>() {}.type)
        val jsonObject = JSONObject(response.toString())
        if(jsonObject.getString("status").equals("1")){
            val colorRecomendation = jsonObject.getJSONObject("color_recommendation")
            val color1Obj = colorRecomendation.getJSONObject("color_1")
            if(color1Obj.getString("shade_code")!=null){
                var shadeCode = color1Obj.getString("shade_code")
                selectedShades.add(shadeCode)
                updateOnBasisOfShadeCode(shadeCode)
            }
            val color2Obj = colorRecomendation.getJSONObject("color_2")
            if(color2Obj.getString("shade_code")!=null){
                var shadeCode = color2Obj.getString("shade_code")
                selectedShades.add(shadeCode)
                updateOnBasisOfShadeCode(shadeCode)
            }
            val color3Obj = colorRecomendation.getJSONObject("color_3")
            if(color3Obj.getString("shade_code")!=null){
                var shadeCode = color3Obj.getString("shade_code")
                selectedShades.add(shadeCode)
                updateOnBasisOfShadeCode(shadeCode)
            }
            binding.btnSubmit.visibility = View.VISIBLE
            binding.layoutMsg.visibility = View.GONE
            binding.btnAiIcon.isEnabled = true
        }else{
            binding.btnAiIcon.isEnabled = true
            showToast(jsonObject.getString("message"))
            binding.layoutMsg.visibility = View.GONE
        }
    }

    override fun onFailure(response: String) {
        binding.layoutMsg.visibility = View.GONE
        binding.btnAiIcon.isEnabled = true
        showToast("Something went wrong please try again later..")
    }
    var updateListener = Scene.OnUpdateListener {frameTime ->
        unRegisterUpdateListener()
        var image: Image? = null//capturedImage
        try {
            val frame = arFragment.arSceneView.arFrame
            if (frame != null) {
                MainScope().launch {
                    var bitmap: Bitmap? = null
                    CoroutineScope(Dispatchers.IO).async {
                        try {
                            image = frame!!.acquireCameraImage()
                        }catch(ex: DeadlineExceededException){
                            //showToast("DeadlineExceededException - IO")
                        }catch(ex: Throwable){
                            //showToast("Throwable - IO")
                        }
                        if (image != null) {
                            bitmap = fetchBitMap(image!!)
                            image!!.close()
                        }
                    }.await()
                    if (bitmap != null) {
                        saveImage(bitmap!!)
                        binding.layoutMsg.visibility = View.VISIBLE
                        binding.btnAiIcon.isEnabled = false
                    }
                }
            }
        } catch (ex: DeadlineExceededException) {
            binding.btnAiIcon.isEnabled = true
            Log.e("Dead Line Exp: ", ex.message.toString())
        }catch (ex: NotYetAvailableException) {
            binding.btnAiIcon.isEnabled = true
            Log.e("Dead Line Exp: ", ex.message.toString())
        } catch (ex: Exception) {
            binding.btnAiIcon.isEnabled = true
            Log.e("Fail to Create Bitmap: ", ex.message.toString())
        } catch(ex: Throwable){
            binding.btnAiIcon.isEnabled = true
            ex.printStackTrace()
            //showToast("Throwable")
        }catch(ex: Error){
            binding.btnAiIcon.isEnabled = true
            ex.printStackTrace()
            //showToast("Error")
        }catch(ex: RuntimeException){
            binding.btnAiIcon.isEnabled = true
            ex.printStackTrace()
            //showToast("RuntimeException")
        }finally {
            image?.close()
        }
    }
    var sensorListener = object: SensorEventListener{
        override fun onSensorChanged(event: SensorEvent?) {
            mLightQuantity = event!!.values[0]
            //showToast(mLightQuantity.toString())
            /*if(!isFirstTime){
                return
            }
            isFirstTime = false
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if(event.sensor.type == Sensor.TYPE_LIGHT){
                        var temp = (mLightQuantity/5.0f).toFloat()
                        if(temp<15.0f){
                            addPointLight(temp)
                        }else{
                            addPointLight(15.0f)
                        }
                        //addDirectionalLight(mLightQuantity)//
                        showToast("Intensity is : ".plus(mLightQuantity.toString()))
                    }
                    handler.postDelayed(this, delay.toLong())
                }
            }, delay.toLong())*/
            if(event?.sensor?.type == Sensor.TYPE_LIGHT){
                var temp = (mLightQuantity/5.0f).toFloat()
                //showToast("DIM      ".plus(temp.toString()))
                if(temp>=11.0f){
                    addDirectionalLight(600f)
                }
                else if(temp>=10.0f){

                }
                else if(temp>=9.0f){
                    addDirectionalLight(595f)
                }
                else if(temp>=8.0f){
                    //addDirectionalLight(585f)
                }else if(temp >= 7.0f){
                    addDirectionalLight(590f)
                }else if(temp >= 6.0f){
                    //addDirectionalLight(575f)
                }else if(temp >= 5.0f){
                    addDirectionalLight(585f)
                }else if(temp >= 4.0f){
                    //addDirectionalLight(565f)
                }else if(temp >= 3.0f){
                    addDirectionalLight(580f)
                }else if(temp >= 2.0f){
                    //addDirectionalLight(555f)
                }else if(temp >= 1.0f){
                    addDirectionalLight(0.0f)
                }else{
                    addDirectionalLight(0.0f)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

    }
    val checkedListener = object: SwitchView.OnCheckedChangeListener{
        override fun onCheckedChanged(buttonView: SwitchView?, isChecked: Boolean) {
            setupCameraFocusMode(isChecked)
        }

    }
    private fun setupCameraFocusMode(flag:Boolean) {
        val config = Config(session)
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.focusMode = Config.FocusMode.AUTO
        if(flag){
            config.flashMode = Config.FlashMode.TORCH
        }else{
            config.flashMode = Config.FlashMode.OFF
        }
        config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY)
        session?.configure(config)
        arFragment.arSceneView.setupSession(session)
    }
    /*class BlanckSelectionVisualizer : SelectionVisualizer {
        override fun applySelectionVisual(var1: BaseTransformableNode) {}
        override fun removeSelectionVisual(var1: BaseTransformableNode) {}
    }*/

    override fun onBackPressed() {
        super.onBackPressed()
        val resultIntent = Intent()
        val resultString = "back,backbuton"
        resultIntent.putExtra("data",resultString)
        setResult(Activity.RESULT_CANCELED,resultIntent)
        finish()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            session = Session(this@ShadeSelectionActivity)
            setupCameraFocusMode(false)
        }
    }

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
//https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/MaterialFactory
//https://github.com/googlesamples/sceneform-samples/tree/master