package com.example.little.myqqlogindemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private Button mNewLoginButton;
    private Button mlogOutButton;
    private TextView mUserInfo;
    private ImageView mUserLogo;

    private static Tencent mTencent;
    private UserInfo mInfo;
    private String APPID="1107936806";
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("xxxa", "sss");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        创建Tencent类实例:
        mTencent= Tencent.createInstance(APPID,this.getApplication());
        mNewLoginButton =  findViewById(R.id.button);
        mlogOutButton =  findViewById(R.id.button2);
        mUserInfo  =  findViewById(R.id.nick_name);
         mUserLogo =  findViewById(R.id.icon);
         //为按钮绑定点击事件
        View.OnClickListener listener = new NewClickListener();
        mlogOutButton.setOnClickListener(listener);
        mNewLoginButton.setOnClickListener(listener);
        //检查登录状态
        checkToken();
    }

    private boolean checkToken() {
        //进入应用后，如果缓存的登录态有效，可以直接使用缓存而不需要再次拉起手q
        JSONObject jsonObject = null;
        boolean isValid = mTencent.checkSessionValid(APPID);
        if(!isValid) {
            Util.showResultDialog(MainActivity.this, "token过期，请调用登录接口拉起手Q授权登录", "登录失败");
            return true;
        } else {
            jsonObject = mTencent.loadSession(APPID);
            mTencent.initSessionCache(jsonObject);
//            Util.showResultDialog(MainActivity.this, jsonObject.toString(), "登录成功");
//            updateUserInfo();
            //跳转登陆成功页面
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        }
        return false;
    }

    private void onClickLogin() {
        mTencent.login(this, "all", loginListener);
    }

    private void onClickLoginOut() {
        if(mTencent !=null){
            mTencent.logout(this);
            mUserInfo.setText("");
            mUserInfo.setVisibility(android.view.View.GONE);
            mUserLogo.setVisibility(android.view.View.GONE);
        }
    }


    class NewClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            Class<?> cls = null;
            boolean isAppbar = false;
            switch (v.getId()) {
                case R.id.button:
                    onClickLogin();
                    return;
                case R.id.button2:
//                    cls = HomeActivity.class;
                    onClickLoginOut();
                    break;
            }
            if (cls != null) {
                Intent intent = new Intent(context, cls);
                if (isAppbar) { //APP内应用吧登录需接收登录结果
                    startActivityForResult(intent, Constants.REQUEST_APPBAR);
                } else {
                    context.startActivity(intent);
                }
            }
        }
    }

    IUiListener loginListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            Log.d("SDKQQAgentPref", "AuthorSwitch_SDK:" + SystemClock.elapsedRealtime());
            initOpenidAndToken(values);
//            updateUserInfo();
            //跳转登录成功页面
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    };

    //设置登陆状态
    public static void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch(Exception e) {
        }
    }


    private void updateUserInfo() {
        if (mTencent != null && mTencent.isSessionValid()) {
            IUiListener listener = new IUiListener() {
                @Override
                public void onError(UiError e) {
                }
                @Override
                public void onComplete(final Object response) {
                    Message msg = new Message();
                    msg.obj = response;
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    new Thread(){
                        @Override
                        public void run() {
                            JSONObject json = (JSONObject)response;
                            if(json.has("figureurl")){
                                Bitmap bitmap = null;
                                try {
                                    bitmap = Util.getbitmap(json.getString("figureurl_qq_2"));
                                } catch (JSONException e) {
                                }
                                Message msg = new Message();
                                msg.obj = bitmap;
                                msg.what = 1;
                                mHandler.sendMessage(msg);
                            }
                        }
                    }.start();
                }
                @Override
                public void onCancel() {
                }
            };
            mInfo = new UserInfo(this, mTencent.getQQToken());
            mInfo.getUserInfo(listener);
        }
        else {
            mUserInfo.setText("");
            mUserInfo.setVisibility(android.view.View.GONE);
            mUserLogo.setVisibility(android.view.View.GONE);
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                JSONObject response = (JSONObject) msg.obj;
                if (response.has("nickname")) {
                    try {
                        mUserInfo.setVisibility(android.view.View.VISIBLE);
                        mUserInfo.setText(response.getString("nickname"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else if(msg.what == 1){
                Bitmap bitmap = (Bitmap)msg.obj;
                mUserLogo.setImageBitmap(bitmap);
                mUserLogo.setVisibility(android.view.View.VISIBLE);
            }
        }

    };
    private class BaseUiListener implements IUiListener {
        @Override
        public void onComplete(Object response) {
            if (null == response) {
                Util.showResultDialog(MainActivity.this, "返回为空", "登录失败");
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
                Util.showResultDialog(MainActivity.this, "返回为空", "登录失败");
                return;
            }
//           Util.showResultDialog(MainActivity.this, response.toString(), "登录成功");
            doComplete((JSONObject)response);
        }
        protected void doComplete(JSONObject values) {

        }

        @Override
        public void onError(UiError uiError) {
            Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onCancel() {
            Toast.makeText(getApplicationContext(), "登录取消", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("TAG", "-->onActivityResult " + requestCode  + " resultCode=" + resultCode);
        if (requestCode == Constants.REQUEST_LOGIN ||
                requestCode == Constants.REQUEST_APPBAR) {
            Tencent.onActivityResultData(requestCode,resultCode,data,loginListener);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
