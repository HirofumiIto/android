package jp.remote

import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.*
import kotlin.concurrent.timer


class JoystickActivity : AppCompatActivity() {

    lateinit var ble : BluetoothLE

    private var joystickX : Double = 0.0
    private var joystickY : Double = 0.0

    private var originX : Double = 0.0
    private var originY : Double = 0.0

    private var radius : Double = 0.0

    private var angle : Double = 0.0
    private var touchID: Int = -1

    private var barHeight : Int = 0
    private lateinit var emile : ImageView

    private var joystickMax : Double = 0.0
    private var isJoysticRelease:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_joystick)

        ble = this.getApplication() as BluetoothLE
    }

    override fun onWindowFocusChanged(hasFocus : Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // ステータスバーとタイトルバーの高さを取得する（座標指定で必要になってくる）
        var rect :Rect = Rect()
        val window : Window = getWindow()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        barHeight = rect.top + window.findViewById<View>(Window.ID_ANDROID_CONTENT).top

        // 座標画像の原点座標を検出する
        val image : ImageView = findViewById(R.id.imageAxis) as ImageView

        // 座標画像の左上の座標とImageViewの幅、高さを検出
        var axis = intArrayOf(0,0)
        image.getLocationOnScreen(axis)

        originX = axis[0].toDouble() + (image.width / 2)
        originY = axis[1].toDouble() + (image.height / 2)

        radius = image.width / 2.0
        joystickMax = 1000.0 / radius

        // 座標の原点にエミール描画
        emile = findViewById(R.id.imageEmile) as ImageView
        emile.x = (originX - (emile.width / 2)).toFloat()
        emile.y = (originY - (emile.height / 2) - barHeight).toFloat()

        // 定周期タイマ開始
        timer(initialDelay = 0L, period = 200L) {

            if(((joystickX != 0.0) || (joystickY != 0.0)) || (isJoysticRelease == true)){
                var data : String

//                data = "(${(joystickX * joystickMax).roundToInt()}, ${(joystickY * joystickMax).roundToInt()})\n"
                data = "(${joystickX.roundToInt()}, ${joystickY.roundToInt()})\n"
                ble.transmit(data.toByteArray())

                isJoysticRelease = false
            }

        }

    }


    override fun onTouchEvent(motionEvent: MotionEvent) : Boolean {

        if((motionEvent.action == MotionEvent.ACTION_UP) ||
                (motionEvent.action == MotionEvent.ACTION_CANCEL)) {
            // 画面がら指が話された場合、タッチキャンセルされた場合、複数タッチされた場合

            joystickX = 0.0
            joystickY = 0.0

            angle = 0.0

            touchID = -1
            isJoysticRelease = true

        } else if(motionEvent.action == MotionEvent.ACTION_POINTER_UP){
            // 2点以上タッチされている状態で1点タッチが離された

            if(touchID == motionEvent.getPointerId(motionEvent.actionIndex)) {
                // 一番初めにタッチされた指の場合はタッチが離されたものと判断する

                joystickX = 0.0
                joystickY = 0.0

                angle = 0.0

            }

        } else if((touchID == -1) || (touchID == motionEvent.getPointerId(motionEvent.actionIndex))){
            // 画面がタッチされた、ドラッグされた場合

            // 2点押しを無効にするため、初回1点押しのIDを保持しておく。
            touchID = motionEvent.getPointerId(0)

            var axisX : Double = 0.0
            var axisY : Double = 0.0

            axisX = motionEvent.getX() - originX
            axisY = originY - motionEvent.getY()

            /* 原点から現座標の角度を取得する */
            val rad = atan2(axisY, axisX)
            angle = (rad * 180.0) / (atan(1.0) * 4.0)
            if(angle < 0.0) {
                angle += 360.0
            }

            // 円より外側がタッチされた場合、座標は原点から見た角度の円の上限に設定する
            val a : Double = motionEvent.getX() - originX
            val b : Double = originY - motionEvent.getY()
            val c : Double = a * (1.0 / cos(rad))    // a * secΘ

            if(c > radius) {
                // 斜辺が半径より上回った
                // 現在の角度と半径より座標を補正する
                joystickX = radius * cos(rad)
                joystickY = radius * sin(rad)
            } else {
                // 補正の必要なし
                joystickX = a
                joystickY = b
            }



        }

        val txtXaxis:TextView = findViewById(R.id.txtXaxis) as TextView
        val txtYaxis:TextView = findViewById(R.id.txtYaxis) as TextView
        val txtAngle:TextView = findViewById(R.id.txtAngle) as TextView

        txtXaxis.text = "X:  " + joystickX.roundToInt().toString()
        txtYaxis.text = "Y:  " + joystickY.roundToInt().toString()
        txtAngle.text = angle.roundToInt().toString() + "°"


        // エミール移動処理
        emile.x = (originX + joystickX - (emile.width / 2)).toFloat()
        emile.y = (originY - joystickY - (emile.height / 2) - barHeight).toFloat()

        return true
    }



}
