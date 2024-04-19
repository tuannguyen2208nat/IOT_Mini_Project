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
    AlertDialog.Builder builder1,builder2,builder3;
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
                if(isOn){
                    kiemtraphun=true;
                    btnTuoi.setOn(false);
                    if(Timer!=null)
                    {
                        Timer.cancel();
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

    private void showTemperatureAlert() {
        builder3=new AlertDialog.Builder(this);
        builder3.setTitle("Alert!").setMessage("Nhiệt độ hiện tại đang cao , bạn có muốn bật máy bơm tưới cây không ?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                boolean check_on=btnPhun.isOn();
                if(!check_on)
                {
                    btnTuoi.setOn(true);
                    timetuoicay.setText("1");
                    if(Timer!=null)
                    {
                        Timer.cancel();
                    }
                    sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "1");
                }
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        builder1=new AlertDialog.Builder(this);
        builder2=new AlertDialog.Builder(this);
        timetuoicay =(EditText) findViewById(R.id.timetuoicay);
        btnTuoi = (LabeledSwitch)  findViewById(R.id.btnTuoi);
        final int[] min = {1};
        final int[] sec = { 0 };

        btnTuoi.setOnToggledListener(new OnToggledListener()  {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if (kiemtraphun) {
                    builder1.setTitle("Alert!").setMessage("Vui lòng tắt máy phun thuốc").setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            btnTuoi.setOn(false);
                        }
                    }).show();
                }
                else {
                    if (isOn) {
                        String timeText = timetuoicay.getText().toString();
                        if (!timeText.equals("Tắt máy bơm")) {
                            if (timeText.contains(":")) {
                                String[] timeParts = timeText.split(":");
                                min[0] = Integer.parseInt(timeParts[0].trim());
                                sec[0] = Integer.parseInt(timeParts[1].trim());
                            } else {
                                min[0] = Integer.parseInt(timeText.trim());
                                sec[0]=0;
                            }
                            check = true;
                            timeInMinutes=min[0]*60+sec[0];
                            Timer = new CountDownTimer(timeInMinutes * 1000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    if (check) {
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
                        else
                        {
                            builder2.setTitle("Alert!").setMessage("Vui lòng chỉnh thời gian tưới").setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                    btnTuoi.setOn(false);
                                }
                            }).show();
                        }
                    }
                    else {
                        sendDataMQTT("tuannguyen2208natIOT/feeds/nut-nhan-1", "0");
                        if(Timer!=null) {
                            Timer.cancel();
                        }
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
                    int nhietdo = Integer.parseInt(message.toString());
                    if (nhietdo > 35) {
                        showTemperatureAlert();
                    }
                }else if(topic.contains("cam-bien-do-am")){
                    txtHumi.setText(message.toString() + "%");
                }else if(topic.contains("cam-bien-anh-sang")){
                    txtLight.setText(message.toString() + "LUX");
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