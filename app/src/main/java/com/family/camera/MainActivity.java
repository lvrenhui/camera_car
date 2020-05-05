package com.family.camera;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    Integer found = 0, opened = 0, closed = 0;
    Button conn_bluetooth, btn_refresh, btn_low, btn_normal, btn_high;
    ImageButton car_left_up, car_left_down, car_right_up, car_right_down, car_left, car_right, car_up, car_down, car_stop;
    ImageButton camera_left, camera_up, camera_down, camera_right;
    EditText device_text;
    String device_name, sending_data;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mmDevice;
    BluetoothSocket mmSocket;
    OutputStream mmOutputStream;
    String baseUrl = "http://192.168.3.113";
    String stream_url = baseUrl + ":81/stream";

    private List<String> list = new ArrayList<>();

    private static String TAG = "####:";

    WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //btn_web
        btn_high = findViewById(R.id.btn_high);
        btn_normal = findViewById(R.id.btn_normal);
        btn_low = findViewById(R.id.btn_low);
        btn_refresh = findViewById(R.id.btn_refresh);
        //btn_car
        car_left = findViewById(R.id.car_left);
        car_right = findViewById(R.id.car_right);
        car_up = findViewById(R.id.car_up);
        car_down = findViewById(R.id.car_down);
        car_left_up = findViewById(R.id.car_left_up);
        car_left_down = findViewById(R.id.car_left_down);
        car_right_up = findViewById(R.id.car_right_up);
        car_right_down = findViewById(R.id.car_right_dwon);
        car_stop = findViewById(R.id.car_stop);
        //btn_camera
        camera_left = findViewById(R.id.camera_left);
        camera_right = findViewById(R.id.camera_right);
        camera_up = findViewById(R.id.camera_up);
        camera_down = findViewById(R.id.camera_dwon);


        //click_web
        btn_refresh.setOnClickListener(this);
        btn_low.setOnClickListener(this);
        btn_normal.setOnClickListener(this);
        btn_high.setOnClickListener(this);
        //click_car
        car_left.setOnTouchListener(this);
        car_right.setOnTouchListener(this);
        car_up.setOnTouchListener(this);
        car_down.setOnTouchListener(this);
        car_left_up.setOnTouchListener(this);
        car_left_down.setOnTouchListener(this);
        car_right_up.setOnTouchListener(this);
        car_right_down.setOnTouchListener(this);
        car_stop.setOnTouchListener(this);
        //click_camera
        camera_left.setOnTouchListener(this);
        camera_right.setOnTouchListener(this);
        camera_up.setOnTouchListener(this);
        camera_down.setOnTouchListener(this);

        progressBar = findViewById(R.id.progressbar);//进度条
        webView = findViewById(R.id.webView);
        device_text = findViewById(R.id.device_id);
        conn_bluetooth = findViewById(R.id.btn_connect);
        conn_bluetooth.setOnClickListener(this);

//        Map<String, String> header=new HashMap<>();
//        header.put("User-Agent","Chrome/81.0.4044.122 Safari/537.36");
//        webView.loadUrl(baseUrl + ":81/stream",header);
        webView.loadUrl(stream_url);

//        webView.addJavascriptInterface(this,"android");//添加js监听 这样html就能调用客户端
        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(webViewClient);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);//允许使用js

        webSettings.setUserAgentString("Chrome/81.0.4044.122 Safari/537.36");


        /**
         * LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
         * LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
         * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
         * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
         */
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);//不使用缓存，只从网络获取数据.

        //支持屏幕缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        //不显示webview缩放按钮
        webSettings.setDisplayZoomControls(false);
        //自适应屏幕
//        webView.setInitialScale(135);//为25%，最小缩放等级

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setLoadWithOverviewMode(true);

    }


    @Override
    protected void onDestroy() {

        new BluetoothStuff().execute("Close Device");
        if (webView != null) {
            webView.destroy();

            super.onDestroy();
        }

    }

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView view, String url) {//页面加载完成
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
//            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i("ansen", "拦截url:" + url);
            if (url.equals("http://www.google.com/")) {
                Toast.makeText(MainActivity.this, "国内不能访问google,拦截该url", Toast.LENGTH_LONG).show();
                return true;//表示我已经处理过了
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

    };

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient = new WebChromeClient() {
        //不支持js的alert弹窗，需要自己监听然后通过dialog弹窗
        @Override
        public boolean onJsAlert(WebView webView, String url, String message, JsResult result) {
            AlertDialog.Builder localBuilder = new AlertDialog.Builder(webView.getContext());
            localBuilder.setMessage(message).setPositiveButton("确定", null);
            localBuilder.setCancelable(false);
            localBuilder.create().show();

            //注意:
            //必须要这一句代码:result.confirm()表示:
            //处理结果为确定状态同时唤醒WebCore线程
            //否则不能继续点击按钮
            result.confirm();
            return true;
        }

        //获取网页标题
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.i("ansen", "网页标题:" + title);
        }

        //加载进度回调
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("ansen", "是否有上一个页面:" + webView.canGoBack());
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {//点击返回按钮的时候判断有没有上一页
            webView.goBack(); // goBack()表示返回webView的上一页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                connBluetooth(v);
                break;
            //click_web
            case R.id.btn_refresh:
//                webView.reload();
                if (btn_refresh.getText() == "暂停") {
                    webView.evaluateJavascript("javascript:window.stop()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            //此处为 js 返回的结果
                        }
                    });
                    btn_refresh.setText("开始");
                } else {
                    webView.loadUrl(stream_url);
                    btn_refresh.setText("暂停");
                }
                break;
            case R.id.btn_low:
                setVideoQuality("4");
                break;
            case R.id.btn_normal:
                setVideoQuality("7");
                break;
            case R.id.btn_high:
                setVideoQuality("8");
                break;

        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        sending_data = null;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (v.getId()) {
                //click_car
                case R.id.car_left:
                    sending_data = "A";
                    break;
                case R.id.car_right:
                    sending_data = "B";
                    break;
                case R.id.car_up:
                    sending_data = "C";
                    break;
                case R.id.car_down:
                    sending_data = "D";
                    break;
                case R.id.car_left_up:
                    sending_data = "E";
                    break;
                case R.id.car_left_down:
                    sending_data = "F";
                    break;
                case R.id.car_right_up:
                    sending_data = "G";
                    break;
                case R.id.car_right_dwon:
                    sending_data = "H";
                    break;
                case R.id.car_stop:
                    sending_data = "S";
                    break;
                //click_camera
                case R.id.camera_left:
                    sending_data = "I";
                    break;
                case R.id.camera_right:
                    sending_data = "J";
                    break;
                case R.id.camera_up:
                    sending_data = "K";
                    break;
                case R.id.camera_dwon:
                    sending_data = "L";
                    break;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            sending_data = "S";
        }

        if (sending_data == null || sending_data.isEmpty()) {
            return false;
        }

        try {
            send_command(sending_data);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error!!!", Toast.LENGTH_SHORT).show();
        }


        return false;
    }

    /**
     * 发送请求（使用 OKHttp）
     */
    private void setVideoQuality(final String quality) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(baseUrl + "/control").build();
                HttpUrl.Builder urlBuilder = request.url().newBuilder();
                urlBuilder.addQueryParameter("var", "framesize");
                urlBuilder.addQueryParameter("val", quality);
                builder.url(urlBuilder.build());

                try {
                    Response response = client.newCall(builder.build()).execute();//发送请求
                    int code = response.code();
                    Log.d(TAG, "result: " + code);
                    if (code != 200) {
                        Toast.makeText(getApplicationContext(), "出了点小问题", Toast.LENGTH_SHORT).show();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void connBluetooth(View view) {

        if (conn_bluetooth.getText().toString().equalsIgnoreCase("| √ |")) {

            device_name = device_text.getText().toString();
            new BluetoothStuff().execute("Find Devices");
            new BluetoothStuff().execute("Open Device");

        } else {
            new BluetoothStuff().execute("Close Device");
        }
    }

    void find_devices() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            new BluetoothStuff().execute("Do nothing");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enBT, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice d : pairedDevices) {
                if (d.getName().equalsIgnoreCase(device_name)) {
                    mmDevice = d;
                    found = 1;
                    break;
                }
            }
        }
    }

    // Enable all buttons
    private void enable_buttons(boolean state) {
//        btn1.setClickable(state);
//        btn2.setClickable(state);
//        btn3.setClickable(state);
//        btn4.setClickable(state);
//        btn5.setClickable(state);
//        btn6.setClickable(state);
//        btn7.setClickable(state);
//        btn8.setClickable(state);
//        btn9.setClickable(state);
//        btn10.setClickable(state);
//        btn11.setClickable(state);
//        btn_high.setClickable(state);
//        btn_low.setClickable(state);

    }


    void open_device() throws IOException {

        if (mmDevice == null) {
            return;
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        enable_buttons(true);
        opened = 1;
    }

    void close_device() throws IOException {

        if (mmOutputStream == null) {
            return;
        }

        if (mmSocket != null) {
            mmOutputStream.close();
            mmSocket.close();
            enable_buttons(false);
            closed = 1;
        }
    }

    void send_command(String D_data) throws IOException {
        if (mmOutputStream == null) {
            return;
        }

        mmOutputStream.write(D_data.getBytes());
    }


    private class BluetoothStuff extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... command) {
            if (command[0].equalsIgnoreCase("Find Devices")) {
                find_devices();
                return 1;
            }
            if (command[0].equalsIgnoreCase("Open Device")) {
                try {
                    open_device();
                    return 2;
                } catch (IOException e) {
                    return 3;
                }
            }
            if (command[0].equalsIgnoreCase("Close Device")) {
                try {
                    close_device();
                    return 4;
                } catch (IOException e) {
                    return 5;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case 1:
                    if (found == 1) {
                        Toast.makeText(getApplicationContext(), device_name + " found! :)", Toast.LENGTH_SHORT).show();
                        break;
                    } else {
                        break;
                    }
                case 2:
                    if (opened == 1) {
                        Toast.makeText(getApplicationContext(), "Connection established to " + device_name + " :)", Toast.LENGTH_LONG).show();
                        conn_bluetooth.setText("| × |");
                        break;
                    } else {
                        break;
                    }
                case 3:
                    Toast.makeText(getApplicationContext(), "Cannot find the device :(", Toast.LENGTH_LONG).show();
                case 4:
                    conn_bluetooth.setText("| √ |");
                    Toast.makeText(getApplicationContext(), "Device disconnected!!! :o", Toast.LENGTH_LONG).show();
                    break;
                case 5:
                    conn_bluetooth.setText("| √ |");
                    Toast.makeText(getApplicationContext(), "Error closing the connection!!! :o", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }
}
