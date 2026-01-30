package com.example.caballoloco

import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import java.util.concurrent.TimeUnit
import android.os.Bundle
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.jar.Manifest

private const val CELL_EMPTY = 0
private const val CELL_VISITED = 1
private const val CELL_BONUS = 2
private const val CELL_OPTION = 9
private const val CELL_OPTION_BONUS = 92

class MainActivity : AppCompatActivity() {

    private var cellSellected_x = 0
    private var cellSellected_y = 0

    private var options = 0
    private var moves = 0 // 64 max
    private var levelMoves = 64

    private var checkMovement = true
    private var visitedCells = 0
    private val knightMoves = arrayOf(
        Pair(1, 2), Pair(2, 1),
        Pair(-1, 2), Pair(-2, 1),
        Pair(1, -2), Pair(2, -1),
        Pair(-1, -2), Pair(-2, -1)
    )


    private var width_moves = 64
    //private var lives = 3

    private var bonus = 0
    private var movesTo_bonus = 4

    private lateinit var board: Array<IntArray>

    private var timeHandler: Handler? = null
    private var timeInSeconds: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initScreenGame()
        startGame()
        initAds()
    }


    private fun initAds() {


        MobileAds.initialize(this) {}

        // It is recommended to call AdLoader.Builder on a background thread.
        CoroutineScope(Dispatchers.IO).launch {
            val adLoader =
                //AdLoader.Builder(this@MainActivity, "ca-app-pub-6940378142609500/2070282688")
                AdLoader.Builder(
                    this@MainActivity,
                    "ca-app-pub-3940256099942544/2247696110"
                )  // ID de publicidad de prueba
                    .forNativeAd { nativeAd ->
                        showNativeAd(nativeAd)
                        // The native ad loaded successfully. You can show the ad.
                        Log.d("Ads", "Anuncio Nativo Cargado Correctamente: ${nativeAd.headline}")

                    }
                    .withAdListener(
                        object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                // The native ad load failed. Check the adError message for failure reasons.
                                Log.e("Ads", "Error al cargar anuncio: ${adError.message}")
                                //withContext(Dispatchers.Main) {
                                //    showAdFallback("Publicidad no disponible")
                                //}
                            }
                        }
                    )
                    // Use the NativeAdOptions.Builder class to specify individual options settings.
                    .withNativeAdOptions(NativeAdOptions.Builder().build())
                    .build()

            // Load native ads.
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun showNativeAd(nativeAd: NativeAd) {
        val adContainer = findViewById<LinearLayout>(R.id.adsBanner)

        if (adContainer == null) {
            Log.e("Ads", "No se encontró el contenedor del anuncio (adsBanner)")
            return
        }

        // Inflar el layout del anuncio nativo (deberás crear este XML)
        val adView = LayoutInflater.from(this)
            .inflate(R.layout.native_ad_layout, adContainer, false) as NativeAdView

        // Asignar vistas básicas (esto depende de tu diseño)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        // Completar los textos/imágenes
        (adView.headlineView as? android.widget.TextView)?.text = nativeAd.headline
        (adView.bodyView as? android.widget.TextView)?.text = nativeAd.body
        val iconDrawable = nativeAd.icon?.drawable
        if (iconDrawable != null) {
            (adView.iconView as? ImageView)?.setImageDrawable(iconDrawable)
            adView.iconView?.visibility = View.VISIBLE
        } else {
            adView.iconView?.visibility = View.GONE
        }

        // Asociar el objeto del anuncio con la vista
        adView.setNativeAd(nativeAd)

        // Limpiar y agregar la vista del anuncio al contenedor
        adContainer.removeAllViews()
        adContainer.addView(adView)
    }

    private fun showAdFallback(message: String) {
        val adContainer = findViewById<LinearLayout>(R.id.adsBanner)
        adContainer.removeAllViews()

        val fallbackText = TextView(this).apply {
            text = message
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(getColor(android.R.color.darker_gray))
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }

        adContainer.addView(fallbackText)
    }


    private fun initScreenGame() {
        setSizedBoard()
        resetBoard()
    }

    private fun setSizedBoard() {
        var iv: ImageView
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        var width_dp = (width / getResources().getDisplayMetrics().density)
        var lateralMarginsDP = 10
        val width_cell = (width_dp - lateralMarginsDP) / 8
        val heigth_cell = width_cell

        width_moves = width_cell.toInt() * 2

        for (i in 0..7) {
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))

                var height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heigth_cell,
                    getResources().getDisplayMetrics()
                ).toInt()
                var width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    width_cell,
                    getResources().getDisplayMetrics()
                ).toInt()

                iv.setLayoutParams(TableRow.LayoutParams(width, height))
            }
        }

    }

    private fun startGame() {
        setFirstPosition()
        resetTimer()
        startTimer()
        hide_message()
    }

    private fun gameOver() {
        if (bonus == 0) {
            checkMovement = true
            if (options == 0) {
                showMessage(
                    "Perdiste",
                    "Casillas: $visitedCells / $levelMoves",
                    "Prueba otra vez",
                    true
                )
                stopTimer()
            }
        } else checkMovement = false
    }

    private fun hide_message() {
        var visiblMens = findViewById<LinearLayout>(R.id.mensaje_presentacion)
        visiblMens.visibility = View.INVISIBLE
    }

    private fun show_message() {
        var visiblMens = findViewById<LinearLayout>(R.id.mensaje_presentacion)
        visiblMens.visibility = View.VISIBLE
    }

    private fun setFirstPosition() {
        var x = 0
        var y = 0
        x = (0..7).random()
        y = (0..7).random()
        currentCell(x, y)
        //cellSellected_x = x
        //cellSellected_y = y

        selectCell(cellSellected_x, cellSellected_y)
    }

    private fun resetGame(gameOver: Boolean) {
        initScreenGame()
        moves = 0
        bonus = 0
        options = 0
        visitedCells = 0


        var v = findViewById<View>(R.id.bonus_divider)
        var height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            getResources().getDisplayMetrics()
        ).toInt()
        var width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            0f,
            getResources().getDisplayMetrics()
        ).toInt()
        v.setLayoutParams(TableRow.LayoutParams(width, height))


        var tvTimeData = findViewById<TextView>(R.id.tv_time_data)
        tvTimeData.setBackground(resources.getDrawable(R.drawable.background_data_bottom, null))

        var tvMovesData = findViewById<TextView>(R.id.tv_bonus_data)
        tvMovesData.text = ""

        for (i in 0..7) {
            for (j in 0..7) {
                clearColorCell(i, j)
                var iv: ImageView =
                    findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                iv.setImageDrawable(null)
            }
        }
        if (gameOver) {
            setFirstPosition()
            startTimer()
        } else {
            setFirstPosition()
            startTimer()
        }

    }

    private fun resetBoard() {
        // 0 celda vacía
        // 1 casilla marcada
        // 2 bonus
        // 9 opción de movimiento

        board = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        )
    }



    private var cronometer: Runnable = object : Runnable {
        override fun run() {
            try {
                timeInSeconds++
                var timeInSecondsToLong = timeInSeconds.toLong()
                updateStopWatchView(timeInSecondsToLong)
            } finally {
                timeHandler!!.postDelayed(this, 1000)

                if (timeInSeconds == 240) {
                    showMessage(
                        "Tiempo!",
                        "Casillas: $visitedCells / $levelMoves",
                        "Prueba otra vez",
                        true
                    )
                    stopTimer()
                }
                if (timeInSeconds >= 20) {
                    var tvTimeData = findViewById<TextView>(R.id.tv_time_data)
                    tvTimeData.setBackground(
                        resources.getDrawable(
                            R.drawable.background_data_bottom_red,
                            null
                        )
                    )
                    //tvTimeData.setTextColor(resources.getColor(R.color.light_blue))
                }
            }
        }
    }

    private fun resetTimer() {
        timeHandler?.removeCallbacks(cronometer)
        timeInSeconds = 0

        var tvTimeData = findViewById<TextView>(R.id.tv_time_data)
        tvTimeData.text = "00:00"


    }

    private fun startTimer() {
        resetTimer()
        timeHandler = android.os.Handler(Looper.getMainLooper())
        cronometer.run()
    }

    private fun stopTimer() {
        timeHandler?.removeCallbacks(cronometer)
    }



    fun checkCellClicked(v: View) {
        var nombre_casilla = v.tag.toString()
        var x = nombre_casilla.subSequence(1, 2).toString().toInt()
        //println("x en checkCellClicked => $x")
        var y = nombre_casilla.subSequence(2, 3).toString().toInt()
        //println("y en checkCellClicked => $y")
        checkCell(x, y)
    }

    private fun checkCell(x: Int, y: Int) {
        println("checkCell(${x}, ${y}) called from cellSellected = ($cellSellected_x, $cellSellected_y)")

        if (x !in 0..7 || y !in 0..7) return
        var dif_x = x - cellSellected_x
        var dif_y = y - cellSellected_y
        var checkTrue = knightMoves.any { (dx, dy) -> dx == dif_x && dy == dif_y }
        if (!checkTrue) return

        if (checkMovement && checkTrue == true && board[x][y] == 1) {
            println("en checkmovement")
            //checkMovement = false
            checkTrue = false
        } else {
            if (board[x][y] == 1) {
                println("en else de checkmovement")

                if (checkTrue) bonus--
                var tvBonusData = findViewById<TextView>(R.id.tv_bonus_data)
                tvBonusData.text = " + $bonus"
                if (bonus == 0) tvBonusData.text = ""
            }
        }
        if (checkTrue) selectCell(x, y)
    }

    private fun selectCell(x: Int, y: Int) {
        println("SELECTED board ==  ${board[x][y]}")
        println("visitedCells => $visitedCells")



        if (board[x][y] == CELL_OPTION_BONUS) {
            bonus++
            var tvBonusData = findViewById<TextView>(R.id.tv_bonus_data)
            tvBonusData.text = " + $bonus"
        }

        if (board[x][y] != CELL_VISITED) {
            visitedCells++
        }

        board[x][y] = CELL_VISITED
        clearOptions()
        paintPrevCell(cellSellected_x, cellSellected_y)
        currentCell(x, y)



        checkOptions(x, y)

        paintCurrentCell(x, y)
        setMoves()
        newBonus()
        growProgressMoves()

        gameOver()
        if (visitedCells == 64) {
            checkSucessFullEnd()
        }
    }

    private fun currentCell(x: Int, y: Int) {
        // Borra el caballo de la celda anterior
        val prevIv: ImageView = findViewById(
            resources.getIdentifier("c${cellSellected_x}${cellSellected_y}", "id", packageName)
        )
        prevIv.setImageDrawable(null)  // limpia la imagen anterior

        // Actualiza la posición actual del caballo
        cellSellected_x = x
        cellSellected_y = y

    }

    private fun paintCurrentCell(x: Int, y: Int) {

        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        iv.setImageResource(R.drawable.caballo_negro)
        iv.setBackgroundResource(R.drawable.current_cell)
        iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
        iv.setPadding(8, 8, 8, 8)


    }



    private fun checkOptions(x: Int, y: Int) {
        options = 0

        checkMove(x, y, 1, 2)
        checkMove(x, y, 2, 1)
        checkMove(x, y, 1, -2)
        checkMove(x, y, 2, -1)
        checkMove(x, y, -1, 2)
        checkMove(x, y, -2, 1)
        checkMove(x, y, -1, -2)
        checkMove(x, y, -2, -1)

        var tvOptionsData = findViewById<TextView>(R.id.opciones_cantidad)
        tvOptionsData.text = options.toString()

    }

    private fun checkColorCell(x: Int, y: Int): String {
        return if ((x + y) % 2 == 0) "black" else "white"

        /*var color = ""
        var blackColumn_x = arrayOf(0,2,4,6)
        var blackRow_x = arrayOf(1,3,5,7)
        if((blackColumn_x.contains(x) && blackColumn_x.contains(y)) || (blackRow_x.contains(x) && blackRow_x.contains(y))){
            color = "black"
        } else {
            color = "white"
        }

        return color*/
    }

    private fun checkMove(x: Int, y: Int, mov_x: Int, mov_y: Int) {
        var option_x = x + mov_x
        var option_y = y + mov_y

        if (option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0) {
            if (board[option_x][option_y] == CELL_EMPTY || board[option_x][option_y] == CELL_BONUS) {
                options++
                paintOptions(option_x, option_y, false)
                if (board[option_x][option_y] == CELL_EMPTY) board[option_x][option_y] = CELL_OPTION
                if (board[option_x][option_y] == CELL_BONUS) board[option_x][option_y] =
                    CELL_OPTION_BONUS
            }
        }
        if (bonus > 0) {
            if (option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0 && board[option_x][option_y] == CELL_VISITED) {
                paintOptions(option_x, option_y, true)

                //drawTextOnCell(option_x, option_y, options.toString(), checkColorCell(option_x, option_y))

            }
        }
    }

    private fun paintOptions(x: Int, y: Int, bonusPaint: Boolean) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))

        if (bonusPaint == false) {
            if (checkColorCell(x, y) == "black") {
                iv.setBackgroundResource(R.drawable.option_cell_black)

            } else {
                iv.setBackgroundResource(R.drawable.option_cell_white)
            }
        }


        if (bonusPaint == true) {

            if (checkColorCell(x, y) == "black") {
                iv.setBackgroundResource(R.drawable.option_cell_black_mark)
                //drawTextOnCell(x, y, "$options", "black")

            } else {
                iv.setBackgroundResource(R.drawable.option_cell_white_mark)
                //drawTextOnCell(x, y, "$options", "white")
            }
        }
    }

    private fun clearOptions() {

        for ((dx, dy) in knightMoves) {
            val i = cellSellected_x + dx
            val j = cellSellected_y + dy


            if (i !in 0..7 || j !in 0..7) continue

            if (board[i][j] == CELL_OPTION || board[i][j] == CELL_OPTION_BONUS) {
                board[i][j] = if (board[i][j] == CELL_OPTION_BONUS) CELL_BONUS else CELL_EMPTY
                clearColorCell(i, j)
                println("clearOptions con Options(${i}, ${j})")
            } else if (board[i][j] == CELL_VISITED) {
                paintPrevCell(i, j)
                println("clearOptions con Cell_Visited(${i}, ${j})")
            }

        }
        /*    for (i in 0..7){
            for (j in 0..7){
                if (board[i][j] == 9 ){
                        board[i][j] = 0
                        clearColorCell(i, j,)
                        //println("clearOptions 9 === i: $i -- j: $j")
                }
                if (board[i][j] == 92 ){
                    board[i][j] = 2
                    println("clearOptions 92 === i: $i -- j: $j")
                    //clearColorCell(i, j)
                }
                if (board[i][j] == 1){
                    //println("clearOptions 1 === i: $i -- j: $j")
                    //board[i][j] = 1
                    paintPrevCell(i,j)

                }
            }
        }*/
    }

    private fun clearColorCell(x: Int, y: Int, ) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))

        if (checkColorCell(x, y) == "black") {
            iv.setBackgroundResource(R.drawable.casilla_negra)

        } else {
            iv.setBackgroundResource(R.drawable.casilla_blanca)
        }

        // Borra texto si existía
        val tv = iv.rootView.findViewWithTag<TextView>("tv$x$y")
        tv?.visibility = View.INVISIBLE


    }

    private fun paintPrevCell(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))

        if (checkColorCell(x, y) == "black") {
            iv.setBackgroundResource(R.drawable.marked_cell_black)
        } else {
            iv.setBackgroundResource(R.drawable.marked_cell_white)
        }


    }


    private fun checkSucessFullEnd() {

        showMessage("¡Ganaste!", "Sigue Así", "Siguiente Nivel", false)
        stopTimer()
    }

    private fun showMessage(title: String, subt: String, action: String, gameOver: Boolean) {
        var visiblMens = findViewById<LinearLayout>(R.id.mensaje_presentacion)
        visiblMens.visibility = View.VISIBLE

        var title_message = findViewById<TextView>(R.id.intro_nivel)
        title_message.text = title
        var action_message = findViewById<TextView>(R.id.intro_vidas)
        action_message.text = subt
        var btn_action = findViewById<TextView>(R.id.btn_siguiente_accion)
        btn_action.text = action
        if (gameOver) {
            btn_action.setOnClickListener {
                resetGame(true)
                visiblMens.visibility = View.INVISIBLE
            }
        } else {
            btn_action.setOnClickListener {
                // Pasar al siguiente nivel

                levelMoves -= 5
                resetGame(false)
                visiblMens.visibility = View.INVISIBLE
            }
        }
    }


    private fun setMoves() {
        moves++
        var tvMovesData = findViewById<TextView>(R.id.movimientos_cantidad)
        tvMovesData.text = moves.toString()
    }

    private fun growProgressMoves() {
        if (moves == 0) return
        var v = findViewById<View>(R.id.bonus_divider)
        var widthMoves = (width_moves.toFloat() / levelMoves) * visitedCells
        var height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            getResources().getDisplayMetrics()
        ).toInt()
        var width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widthMoves,
            getResources().getDisplayMetrics()
        ).toInt()
        v.setLayoutParams(TableRow.LayoutParams(width, height))


        //Animador de progreso
        val currentWidth = v.layoutParams.width.takeIf { it > 0 } ?: 0
        val animator = ValueAnimator.ofInt(
            currentWidth, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthMoves,
                resources.displayMetrics
            ).toInt()
        )

        animator.duration = 300 // duración de la animación en ms
        animator.addUpdateListener { animation ->
            val params = v.layoutParams
            params.width = animation.animatedValue as Int
            params.height = height
            v.layoutParams = params
        }

        animator.start()

    }

    private fun newBonus() {
        if (options > 0) {
            if (moves % movesTo_bonus == 0) {
                var bonus_cell_x = 0
                var bonus_cell_y = 0

                var intentos = 0
                var bonus_cell = false
                while (intentos < 64 && bonus_cell == false) {
                    bonus_cell_x = (0..7).random()
                    bonus_cell_y = (0..7).random()

                    if (board[bonus_cell_x][bonus_cell_y] == 0) {
                        bonus_cell = true
                        board[bonus_cell_x][bonus_cell_y] = 2
                        var iv: ImageView = findViewById(
                            resources.getIdentifier(
                                "c$bonus_cell_x$bonus_cell_y",
                                "id",
                                packageName
                            )
                        )
                        iv.setImageResource(R.drawable.peon)

                        iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        iv.setPadding(15, 15, 15, 15)

                        return
                    }
                    intentos++
                }
            }

        }
    }


    private fun updateStopWatchView(timeInSeconds: Long) {
        val formattedTime = getFormattedStopWatch(timeInSeconds * 1000)
        var tvTimeData = findViewById<TextView>(R.id.tv_time_data)
        tvTimeData.text = formattedTime
    }

    private fun getFormattedStopWatch(ms: Long): String {
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        return "%02d:%02d".format(minutes, seconds)
    }


    fun launchShareGame(v: View) {
        shareGame()
    }

    private fun shareGame() {

        var view = findViewById<View>(R.id.principalLayout)
        hide_message()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        view.draw(canvas)
        show_message()

        // Guarda el bitmap en la galería
        val filename = "captura_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CapturasApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            } finally {
                outputStream?.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Toast.makeText(this, "Captura guardada en la galería", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
        }


        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, 1)


        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "¡Estoy jugando a Caballo Loco! ¿Puedes completar el recorrido del caballo en menos movimientos? Descárgalo ahora: https://example.com/caballoloco"
            )
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Compartir juego via    "))
    }


    private fun drawTextOnCell(x: Int, y: Int, text: String, back: String) {
        val iv = findViewById<ImageView>(resources.getIdentifier("c$x$y", "id", packageName))

        // Creamos un TextView si no existe ya para esta celda
        var tv = iv.rootView.findViewWithTag<TextView>("tv$x$y")
        if (tv == null) {
            tv = TextView(this)
            tv.tag = "tv$x$y"
            tv.textSize = 16f
            tv.text = "$text"
            tv.setBackgroundResource(
                if (back == "black") R.drawable.option_cell_black_mark else R.drawable.option_cell_white_mark
            )

            tv.setTextColor(ContextCompat.getColor(this, R.color.black_grey))
            tv.setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            tv.layoutParams = iv.layoutParams

            // Colocamos el TextView en la misma posición que la celda
            val parent = iv.parent as? TableRow
            parent?.addView(tv, parent.indexOfChild(iv))
        }
        tv.text = text
        tv.visibility = View.VISIBLE
    }

}
