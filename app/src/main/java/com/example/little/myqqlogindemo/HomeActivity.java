package com.example.little.myqqlogindemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

public class HomeActivity extends AppCompatActivity {
    private static Tencent mTencent;
    private TextView mUserInfo;
    private ImageView mUserLogo;
    private String APPID="1107936806";
    private UserInfo mInfo;
    private Button mlogOutButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
//        创建Tencent类实例:
        mTencent= Tencent.createInstance(APPID,this.getApplication());
        mUserInfo  =  findViewById(R.id.nick_name2);
        mUserLogo =  findViewById(R.id.icon2);

        //为按钮绑定点击事件
        mlogOutButton = findViewById(R.id.loginOut2);
        View.OnClickListener listener = new HomeActivity.NewClickListener();
        mlogOutButton.setOnClickListener(listener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInfo();
    }

    class NewClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            Class<?> cls = null;
            boolean isAppbar = false;
            switch (v.getId()) {
//                case R.id.button:
//                    onClickLogin();
//                    return;
                case R.id.loginOut2:
//                    cls = HomeActivity.class;
                    onClickLoginOut();
                    cls = MainActivity.class;
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

    private void onClickLoginOut() {
        if(mTencent !=null){
            mTencent.logout(this);
            mUserInfo.setText("");
            mUserInfo.setVisibility(android.view.View.GONE);
            mUserLogo.setVisibility(android.view.View.GONE);
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
}
