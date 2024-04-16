package bku.iot.iotminiproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemp, txtHumi ,txtLight;
    LabeledSwitch btnTuoi, btnPhun;
    CountDownTimer Timer;
    TextView timetuoicay;
    private int timeInMinutes;
    boolean kiemtraphun=false;
    AlertDialog.Builder builder;
    boolean check=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTemp=findViewById(R.id.txtTemperature);
        txtHumi=findViewById(R.id.txtHumidity);
        txtLight=findViewById(R.id.txtLight);
        btnPhun= (LabeledSwitch) findViewById(R.id.btnPhun);
        btnPhun.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                    if(isOn==true){
                        kiemtraphun=true;
                        btnTuoi.setOn(false);
                        if(Timer!=null)
                        {
                            Timer.cancel();
                            timetuoicay.setText("1");
                            sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "0");
                        }
                        sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-2","1");
                    }
                    else{
                        kiemtraphun=false;
                        sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-2","0");
                    }
                }
        });

        startMQTT();
    }


    @Override
    protected void onStart() {
        super.onStart();
        builder=new AlertDialog.Builder(this);
        timetuoicay =(EditText) findViewById(R.id.timetuoicay);
        btnTuoi = (LabeledSwitch)  findViewById(R.id.btnTuoi);

        btnTuoi.setOnToggledListener(new OnToggledListener()  {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if (kiemtraphun == true) {
                    builder.setTitle("Alert!").setMessage("Vui lòng tắt máy phun thuốc").setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            btnTuoi.setOn(false);
                        }
                    }).show();
                }
                else {
                    if (isOn == true) {
                        timeInMinutes = Integer.parseInt(timetuoicay.getText().toString());
                        Timer = new CountDownTimer(timeInMinutes * 60 * 1000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (check == true) {
                                    sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "1");
                                }
                                int remainingMinutes = (int) (millisUntilFinished / (1000 * 60));
                                int remainingSeconds = (int) ((millisUntilFinished / 1000) % 60);
                                timetuoicay.setText(String.format("%d:%02d", remainingMinutes, remainingSeconds));
                                check = false;
                            }

                            @Override
                            public void onFinish() {
                                timetuoicay.setText("Tắt máy bơm");
                                btnTuoi.setOn(false);
                                sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "0");
                                check = true;
                            }
                        }.start();
                    }
                    else {
                        sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "0");
                        Timer.cancel();
                        check = true;
                    }
                }
            }

        });
    }

    public void sendDataMQTT(String topic, String value){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(false);

        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (MqttException e){
        }
    }

    public void startMQTT(){
        mqttHelper=new MQTTHelper(this);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("Test",topic + "***" + message.toString());
                if(topic.contains("cam-bien-nhiet-do")){
                    txtTemp.setText(message.toString() + "°C");
                }else if(topic.contains("cam-bien-do-am")){
                    txtHumi.setText(message.toString() + "%");
                }else if(topic.contains("cam-bien-anh-sang")){
                    txtHumi.setText(message.toString() + "LUX");
                }else if(topic.contains("nut-nhan-1")){
                    if(message.toString().equals("1")){
                        btnTuoi.setOn(true);
                    }else{
                        btnTuoi.setOn(false);
                    }
                }else if(topic.contains("nut-nhan-2")){
                    if(message.toString().equals("1")){
                        btnPhun.setOn(true);
                    }else{
                        btnPhun.setOn(false);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}