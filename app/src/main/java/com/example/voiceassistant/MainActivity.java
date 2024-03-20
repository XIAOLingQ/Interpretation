package com.example.voiceassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voiceassistant.util.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        HttpGetDataListener, View.OnClickListener {

    private static String TAG = MainActivity.class.getSimpleName();
    private ImageView microphone;
    private TextView tv_textlink;

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private SpeechRecognizer mIat;// 语音听写
    private RecognizerDialog iatDialog;//听写ui
    private SpeechSynthesizer mTts;// 语音合成
    // 0 小燕 青年女声 中英文（普通话） xiaoyan
    // 1 默认 许久 青年男声 中英文（普通话） aisjiuxu
    private String[] voiceName = { "xiaoyan", "aisjiuxu","xiaomei"};

    private HttpData httpData;
    private List<ListData> lists;
    private ListView lv;
    private String content_str;
    private TextAdapter adapter;
    private String[] welcome_array;
    // 做比对时间；老时间
    private double currentTime = 0, oldTime = 0;

    private boolean flag = false;   //语音听写完后，防止多次识别标识
    private boolean isMessage=false;//判断是否是短信内容的标志
    private String msg_number;//发短信时的联系人电话
    private String msg_name;//发短信时的联系人名字

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 定义获取录音的动态权限
        initPermission();
        initView();
    }

    private void initView() {
        // 初始化即创建语音配置对象，只有初始化后才可以使用MSC的各项服务
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5c773723");
        // 语音听写 1.创建SpeechRecognizer对象，第二个参数：本地听写时传InitListener
        mIat = SpeechRecognizer.createRecognizer(this, mTtsInitListener);
        // 带UI 1.创建RecognizerDialog对象，第二个参数：本地听写时传InitListener
        iatDialog = new RecognizerDialog(this, mTtsInitListener);
        // 语音合成 1.创建SpeechSynthesizer对象, 第二个参数：本地合成时传InitListener
        mTts = SpeechSynthesizer.createSynthesizer(this,
                mTtsInitListener);

        //对控件进行赋值
        microphone = this.findViewById(R.id.microphone);

        microphone.setOnClickListener(this);

        lv = (ListView) findViewById(R.id.lv);

        lists = new ArrayList<ListData>();
        adapter = new TextAdapter(lists, this);
        lv.setAdapter(adapter);
        ListData listData;
//        listData = new ListData(getRandomWelcomeTips(), ListData.RECEIVER,
//                getTime());
        listData = new ListData("有什么我可以帮到你的吗？", ListData.RECEIVER,
                getTime());
        lists.add(listData);
        starSpeech("有什么我可以帮到你的吗?");
    }

    @Override
    public void getDataUrl(String data) {
        parseText(data);
    }

    public void parseText(String str) {
        try {
            JSONObject jb = new JSONObject(str);
            String text = jb.getString("text");

            refresh(text,ListData.RECEIVER);

            // 语音合成
            starSpeech(text);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.microphone:
                mIatResults.clear();
                flag = true;
                starWrite();
                break;
            default:
                break;
        }
        getTime();
    }

    /** 获取时间 */
    private String getTime() {
        currentTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date curDate = new Date();
        String str = format.format(curDate);
        // 如果超过5分钟.
        if (currentTime - oldTime >= 5 * 60 * 1000) {
            oldTime = currentTime;
            return str;
        } else {
            return "";
        }

    }

    public void refresh(String text,int flag){
        ListData listData;
        listData = new ListData(text, flag, getTime());
        lists.add(listData);
        if (lists.size() > 30) {
            for (int i = 0; i < lists.size(); i++) {
                // 移除数据
                lists.remove(i);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 初始化语音合成相关数据
     *
     * @Description:
     */
    public void starSpeech(String text) {
        // 2.合成参数设置，详见《科大讯飞MSC API手册(Android)》SpeechSynthesizer 类
        mTts.setParameter(SpeechConstant.VOICE_NAME, voiceName[1]);// 设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "50");// 设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "80");// 设置音量，范围0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); // 设置云端

        // 3.开始合成  合成监听器
        mTts.startSpeaking(text, mSynListener);

    }

    /**
     * 初始化参数开始听写
     *
     * @Description:
     */
    private void starWrite() {

        // 语音识别应用领域（：iat，search，video，poi，music）
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        // 接收语言中文
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 接受的语言是普通话
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        // 设置听写引擎（云端）
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        //自带ui
        iatDialog.setListener(mRecognizerDialogListener);
        iatDialog.show();

        tv_textlink = (TextView)iatDialog.getWindow().getDecorView().findViewWithTag("textlink");
        tv_textlink.setText("");
        tv_textlink.getPaint().setFlags(Paint.SUBPIXEL_TEXT_FLAG);
        tv_textlink.setEnabled(false);

        // 3.开始听写
        //Toast.makeText(getApplication(), "请开始说话…", Toast.LENGTH_SHORT).show();
        //mIat.startListening(mRecoListener);

    }

    /**
     * 语音听写监听
     */
    private RecognizerListener mRecoListener = new RecognizerListener() {
        // 听写结果回调接口(返回Json格式结果，用户可参见附录12.1)；
        // 一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
        // 关于解析Json的代码可参见MscDemo中JsonParser类；
        // isLast等于true时会话结束。
        public void onResult(RecognizerResult results, boolean isLast) {
            getMsg(results);
        }

        // 会话发生错误回调接口
        public void onError(SpeechError error) {
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(error.getErrorCode()==10118){
                Toast.makeText(getApplicationContext(), "你好像没有说话哦",
                        Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(getApplicationContext(), error.getPlainDescription(true),
                    Toast.LENGTH_SHORT).show();

        }// 获取错误码描述

        // 开始录音
        public void onBeginOfSpeech() {
            Log.d(TAG, "开始说话");
            Toast.makeText(getApplicationContext(), "开始说话",
                    Toast.LENGTH_SHORT).show();
        }

        // 结束录音
        public void onEndOfSpeech() {
            Log.d(TAG, "说话结束");
            Toast.makeText(getApplicationContext(), "说话结束",
                    Toast.LENGTH_SHORT).show();
        }

        // 扩展用接口
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        //音量
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            // TODO Auto-generated method stub
            Log.d(TAG, "当前说话音量大小"+volume);

        }

    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            // 听写结果回调接口(返回Json格式结果，用户可参见附录12.1)；
            // 一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
            // 关于解析Json的代码可参见MscDemo中JsonParser类；
            // isLast等于true时会话结束。
            if(flag){
                getMsg(results);
            }
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            //Toast.makeText(getApplication(), error.getPlainDescription(true), Toast.LENGTH_SHORT).show();
            if (error.getErrorCode() == 14002) {
                Toast.makeText(getApplication(),error.getPlainDescription(true) + "\n请确认是否已开通翻译功能", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplication(),error.getPlainDescription(true), Toast.LENGTH_SHORT).show();
            }

            /**
             * 过滤掉没有说话的错误码显示
             */
            TextView tv_error = (TextView)iatDialog.getWindow().getDecorView().findViewWithTag("errtxt");
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(tv_error != null && error.getErrorCode()==10118){
                tv_error.setText("您好像没有说话哦...");
            }
        }

    };

    /**
     * 语音合成监听
     */
    private SynthesizerListener mSynListener = new SynthesizerListener() {
        // 会话结束回调接口，没有错误时，error为null
        public void onCompleted(SpeechError error) {
            if (error != null) {
                Log.d(TAG, error.getErrorCode()+ "");
            } else {
                Log.d(TAG, "0");
            }
        }

        // 缓冲进度回调
        // percent为缓冲进度0~100，beginPos为缓冲音频在文本中开始位置，endPos表示缓冲音频在文本中结束位置，info为附加信息。
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }

        // 开始播放
        public void onSpeakBegin() {
        }

        // 暂停播放
        public void onSpeakPaused() {
        }

        // 播放进度回调
        // percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        // 恢复播放回调接口
        public void onSpeakResumed() {
        }

        // 会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
    };

    /**
     * 初始化语音合成监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @SuppressLint("ShowToast")
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                // showTip("初始化失败,错误码：" + code);
                Toast.makeText(getApplicationContext(), "初始化失败,错误码：" + code,
                        Toast.LENGTH_SHORT).show();
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    private void getMsg(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        flag = false;
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        content_str = resultBuffer.toString();
        refresh(content_str,ListData.SEND);


        //如果是发送的短信内容，必须写在最前面，防止短信内容里面出现关键词
        if (isMessage){
            sendMessage(content_str);
            isMessage=false;
            return;
        }

        //关键词"打开"
        if (content_str.contains("打开")){
            String appName= content_str.substring(content_str.indexOf("开")+1);
            Log.d("tag app name",appName);
            openApp(appName);
            return;
        }

        //关键词"搜索"
        if (content_str.contains("搜索")){
            String searchContent=content_str.substring(content_str.indexOf("索")+1);

            surfTheInternet(searchContent);
            return;
        }

        //关键词"打电话"
        if (content_str.contains("打电话")){
            call();
            return;
        }

        //关键词"发短信"
        if (content_str.contains("发短信")){
            getSendMsgContactInfo();
            return;
        }

        //地图
//        if(content_str.contains("地图")){
//            Uri uri = Uri.parse("androidamap://poi?sourceApplication=语音助手&keywords="+content_str+"&dev=0");
//            startActivity(new Intent(Intent.ACTION_VIEW, uri));
//            return;
//        }

        //没找到关键词 就聊天模式
        refresh("暂时还没有这个功能",ListData.RECEIVER);
        starSpeech("暂时还没有这个功能");

    }

    //打开应用
    private void openApp(String appName) {
        PackageManager packageManager = MainActivity.this.getPackageManager();
        // 获取手机里的应用列表
        List<PackageInfo> pInfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pInfo.size(); i++) {
            PackageInfo p = pInfo.get(i);
            // 获取相关包的<application>中的label信息，也就是-->应用程序的名字
            String label = packageManager.getApplicationLabel(p.applicationInfo).toString();
            Log.d("tag", label);
            if (label.contains(appName)) { //比较label
                String text = appName + "已经为您打开";

                refresh(text,ListData.RECEIVER);
                starSpeech(text);

                String pName = p.packageName; //获取包名
                //获取intent
                Intent intent = packageManager.getLaunchIntentForPackage(pName);
                startActivity(intent);
                return;
            }
        }
        refresh("您没有安装该应用",ListData.RECEIVER);
        starSpeech("您没有安装该应用");
    }

    //网上查找
    private void surfTheInternet(String searchContent) {
        String text = "已经为您上网查找"+"\""+searchContent+"\"";
        refresh(text,ListData.RECEIVER);
        starSpeech(text);
        // 指定intent的action是ACTION_WEB_SEARCH就能调用浏览器
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // 指定搜索关键字是选中的文本
        intent.putExtra(SearchManager.QUERY, searchContent);
        startActivity(intent);
    }

    //打电话
    private void call() {
        List<ContactInfo> contactLists = getContactLists(this);
        if (contactLists.isEmpty()){
            String text = "通讯录为空";
            refresh(text,ListData.RECEIVER);
            starSpeech(text);
            return;
        }
        for (ContactInfo contactInfo:contactLists){
            if (content_str.contains(contactInfo.getName())){
                String text = "已经为您拨通"+contactInfo.getName()+"的电话";
                refresh(text,ListData.RECEIVER);
                starSpeech(text);

                String number = contactInfo.getNumber();
                Intent intent = new Intent();
                intent.setAction("android.intent.action.CALL");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("tel:"+number));
                startActivity(intent);
                return;
            }
        }

        refresh("通讯录中没有此人",ListData.RECEIVER);
        starSpeech("通讯录中没有此人");
    }

    //获取通信录中所有的联系人
    private List<ContactInfo> getContactLists(Context context) {
        List<ContactInfo> lists = new ArrayList<ContactInfo>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        //moveToNext方法返回的是一个boolean类型的数据
        while (cursor.moveToNext()) {
            //读取通讯录的姓名
            String name = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            //读取通讯录的号码
            String number = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            ContactInfo contactInfo = new ContactInfo(name, number);
            lists.add(contactInfo);
        }
        return lists;
    }

    //发送短信的联系人信息
    private void getSendMsgContactInfo() {
        List<ContactInfo> contactLists = getContactLists(this);
        String text = "";
        if (contactLists.isEmpty()){
            text = "通讯录为空";
            refresh(text,ListData.RECEIVER);
            starSpeech(text);
            return;
        }
        for (ContactInfo contactInfo:contactLists){
            if (content_str.contains(contactInfo.getName())){
                msg_name=contactInfo.getName();
                msg_number=contactInfo.getNumber();
                text = "请问您要发送什么给"+msg_name;
                refresh(text,ListData.RECEIVER);
                starSpeech(text);
                isMessage=true;
                return;
            }
        }

        refresh("通讯录中没有此人",ListData.RECEIVER);
        starSpeech("通讯录中没有此人");
    }

    //发送短信的内容
    private void sendMessage(String content) {
        if (msg_number==null){
            return;
        }
        SmsManager manager = SmsManager.getDefault();
        ArrayList<String> list = manager.divideMessage(content);  //因为一条短信有字数限制，因此要将长短信拆分
        for(String text:list){
            manager.sendTextMessage(msg_number, null, text, null, null);
        }
        Log.e("","测试");
        String text = "已经发送"+"\""+content+"\""+"给"+msg_name;
        refresh(text,ListData.RECEIVER);
        starSpeech(text);
    }

    public void chat(){
        // 去掉空格
        String dropk = content_str.replace(" ", "");
        // 去掉回车
        String droph = dropk.replace("\n", "");
        httpData = (HttpData) new HttpData(droph, this).execute();

        // RxVolley,网络请求库 具体网上查
//        String url = "http://www.tuling123.com/openapi/api?key=dce266d8ca114296b3fe5f0fd600de3b&info=" + droph;
//        RxVolley.get(url, new HttpCallback() {
//                    @Override
//                    public void onSuccess(String t) {
//                        //解析返回的JSON数据
//                        Log.d(TAG,t);
//                        pasingJson(t);
//                    }
//                });

    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    // 定义录音的动态权限
//    private void soundPermissions() {
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{
//                    android.Manifest.permission.RECORD_AUDIO}, 1);
//        }
//    }
//
//    /**
//     * 重写onRequestPermissionsResult方法
//     * 获取动态权限请求的结果,再开启录音
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//        } else {
//            Toast.makeText(this, "用户拒绝了权限", Toast.LENGTH_SHORT).show();
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }

}
