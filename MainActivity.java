package com.example.johnxiaofather.newapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.constraint.Constraints;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;

import APPOPO.Info;
import bluetooth.BluetoothCommunication;
import bluetooth.BluetoothProtocol;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends AppCompatActivity {
    LineChartView mChart;
    ArrayList<PointValue> values = new ArrayList<PointValue>();
    Button stop;
    Button starto;
    Button pause;
    TextView BLEstatus;
    CheckBox ch3;
    CheckBox ch4;
    CheckBox ch5;
    TextView Length;
    String RealLength;

    TextView Speed;

    // 变量
    private final static int MSG_STATUES = 100;
    private final static int MSG_TRAINDATA = 101;
    private boolean isTrainStart = false;
    private GetTrainDataThread getTrainDataThread;

    // 蓝牙接口
    private BluetoothProtocol myBluetoothProtocol;
    private int connectStatues = -1;
    private String lastBluetoothName = "";
    private String lastBluetoothMAC = "";
    private UiRefreshHandler uiRefreshHandler;
    private GetConnectStatuesThread getConnectStatuesThread;
    private String[] devName, devMac, devInfo; // Info数组仅用于显示
    private int choicedIndex = 0;
    private BluetoothProtocol.BicycleData myBicycleData;

    public int flag = 0;
    public Info infomation = new Info();






    private static final int history = 2;
    private static final int menu_setting = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }
    public void onResume() {
            super.onResume();
        // 每次都从设置里面加载蓝牙的连接信息
        initControler();
        SetListen();
        myBluetoothProtocol = new BluetoothProtocol(MainActivity.this);
        uiRefreshHandler = new UiRefreshHandler();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        lastBluetoothName = sharedPref.getString("pref_bluetooth_name","");
        lastBluetoothMAC = sharedPref.getString("pref_bluetooth_mac","");
        // TODO:
        if(lastBluetoothMAC == ""){
            myBluetoothProtocol.OpenBluetoothProtocol();
        }
        else {
            myBluetoothProtocol.OpenBluetoothProtocol(lastBluetoothMAC);
        }
        getConnectStatuesThread = new GetConnectStatuesThread(); // 打开查询状态的线程
        getConnectStatuesThread.start();

        if(getTrainDataThread != null){
            getTrainDataThread.isPause = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO: 结束相关线程
        if(getConnectStatuesThread != null){
            getConnectStatuesThread.isStop = true;
            getConnectStatuesThread = null;
        }

        if(getTrainDataThread != null){
            getTrainDataThread.isPause = true;
        }
        myBluetoothProtocol.CloseBluetoothProtocol(); // 关闭蓝牙连接
    }
    public void SetListen(){

        starto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                infomation.setTimeBegin();

                if(!isTrainStart){ // 开始训练
                    if(getTrainDataThread == null){
                        getTrainDataThread = new GetTrainDataThread();
                        getTrainDataThread.start();
                    }
                    else{
                        getTrainDataThread.isStop = true;
                        getTrainDataThread = null;
                        getTrainDataThread = new GetTrainDataThread();
                        getTrainDataThread.start();
                    }
                    stop.setEnabled(true);
                    myBluetoothProtocol.SetTrainBegin();
                    isTrainStart = true;


                }





            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(getTrainDataThread != null){
                    getTrainDataThread.isStop = true;
                    getTrainDataThread = null;
                }
                myBluetoothProtocol.SetTrainStop();
                // TODO 结束训练时，相关数据的保存

                stop.setEnabled(false);
                isTrainStart = false;


                infomation.setTimeEnd();
                Log.i("MainActivity",infomation.End_Time);
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                float weight = Float.valueOf(SP.getString("weight","50"));
                infomation.setWeight(weight);
                infomation.setLength(Float.valueOf(Length.getText().toString().trim()));
                infomation.setK();
                infomation.setCalorie();

                String T = String.valueOf(infomation.totalMin);
                String C = String.valueOf(infomation.Calorie);
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("当前消耗卡路里和时间").setMessage("当前消耗卡路里为"+ C + "(千卡)"+";" +"  当前消耗时间为" + T + "秒").create();
                dialog.show();








            }
        });
        ch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(connectStatues == BluetoothCommunication.ID_CONNECTED) {
                        myBluetoothProtocol.SetLevelSmooth();
                    }
                }
            }
        });
        ch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(connectStatues == BluetoothCommunication.ID_CONNECTED) {
                        myBluetoothProtocol.SetLevelEasy();
                    }
                }
            }
        });
        ch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(connectStatues == BluetoothCommunication.ID_CONNECTED) {
                        myBluetoothProtocol.SetLevelHard();
                    }
                }
            }
        });

        BLEstatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                view.setVisibility(View.INVISIBLE);
                switch (connectStatues){
                    case BluetoothCommunication.ID_NOT_CONNECT:
                        String info = "尝试连接到 " + lastBluetoothName + "[" + lastBluetoothMAC
                                + "] 失败，可选择重连再次尝试\n或者进入“设置”解绑，以绑定新的设备";
                        new android.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("提示")
                                .setMessage(info)
                                .setPositiveButton("重连", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        myBluetoothProtocol.CloseBluetoothProtocol();
                                        myBluetoothProtocol.OpenBluetoothProtocol(lastBluetoothMAC);
                                    }
                                })
                                .setNegativeButton("确定", null)
                                .show();
                        break;
                    case BluetoothCommunication.ID_CONNECTED:
                        break;
                    case BluetoothCommunication.ID_CONNECTING:
                        break;
                    case BluetoothCommunication.ID_SEARCHING:
                        myBluetoothProtocol.CancelSearhNewDev();
                        dialogChoice();
                        break;
                    case BluetoothCommunication.ID_SEARCHFINISHED:
                        dialogChoice();
                        break;
                    default:
                        break;
                }





            }
        });

    }
    private void dialogChoice() {//在未连接
        ArrayList<BluetoothDevice> devs = myBluetoothProtocol.GetDevList();
        devName = new String[devs.size()];
        devMac = new String[devs.size()];
        devInfo = new String[devs.size()];
        for(int i = 0;i<devs.size(); i++){
            devName[i] = devs.get(i).getName();
            devMac[i] = devs.get(i).getAddress();
            devInfo[i] = devName[i] + "\n[" + devMac[i] + "]";
        }
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("设备");
        builder.setSingleChoiceItems(devInfo, choicedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                choicedIndex = which;
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                lastBluetoothName = devName[choicedIndex];
                lastBluetoothMAC = devMac[choicedIndex];
                myBluetoothProtocol.CloseBluetoothProtocol();
                myBluetoothProtocol.OpenBluetoothProtocol(lastBluetoothMAC);
                // 把当前连接的保存到设置中去
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("pref_bluetooth_name",lastBluetoothName);
                editor.putString("pref_bluetooth_mac",lastBluetoothMAC);
                editor.commit();
            }
        });
        builder.show();
    }

//此函数未使用，讲时间界面放入点击结束后的对话框中
//    private class ShowTimeThread extends Thread{
//        public boolean isStop = false;
//        public boolean isPause = false;
//        @Override
//        public void run() {
//            super.run();
//            int h = 0;
//            int m = 0;
//            int s = 0;
//            while (!isStop) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    break;
//                }
//                if(!isPause) {
//                    s++;
//                    if (s > 59) {
//                        m++;
//                        s = 0;
//                    }
//                    if (m > 59) {
//                        h++;
//                        m = 0;
//                    }
//                    String time = String.format("%02d:%02d:%02d", h, m, s);
//                    Message msg = new Message();
//                    msg.what = MSG_TIME;
//                    msg.obj = time;
//                    uiRefreshHandler.sendMessage(msg);
//                }
//            }
//        }
//    }
    private class GetConnectStatuesThread extends Thread{
        public boolean isStop = false;
        @Override
        public void run() {
            super.run();
            while(!isStop){
                int status = myBluetoothProtocol.GetConnectStatus();
                if(status!= connectStatues){
                    connectStatues = status;
                    Message msg = new Message();
                    msg.what = MSG_STATUES;
                    msg.arg1 = connectStatues;
                    uiRefreshHandler.sendMessage(msg);
                }
            }
        }
    }
    private class GetTrainDataThread extends Thread{
        public boolean isStop = false;
        public boolean isPause = false;
        @Override
        public void run() {
            super.run();
            while (!isStop) {
                myBicycleData = myBluetoothProtocol.GetTBicycleData();//从下位机获取数据
                Message msg = new Message();
                values.add(new PointValue(ChangeValue(infomation.getCurrentMin()), Float.valueOf(Speed.getText().toString().trim())));//获取图形中的各个点
                Log.i("TIME",String.valueOf(infomation.getCurrentMin()));//测试时间是否正常运行
                msg.what = MSG_TRAINDATA;
                msg.obj = values;//使用message向handler发送消息
                uiRefreshHandler.sendMessage(msg);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                }
            }
            RealLength = Length.getText().toString().trim();;//将值保存下来防止清除后无法发送到其他Activity或者用SharedPreferencez保存
            boolean B = RealLength.equals("0");
            Log.e("RRR",String.valueOf(B));
            fresh();
        }
    }
    private class UiRefreshHandler extends Handler {//管理UI线程，基本所有UI的管理都在其中
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_STATUES://获取蓝牙状况
                    showStatues(msg.arg1);
                    break;
                case MSG_TRAINDATA://获取训练参数
                    ArrayList<PointValue> V = (ArrayList<PointValue>) msg.obj;//获取图像参数
                    mChart.setVisibility(View.VISIBLE);
                    setmChart(V);

                    Log.i("send",String.valueOf(V));

                    showLength(String.format("%.0f",myBicycleData.length));//输出四舍五入为整数

                    showSpeed(String.format("%.0f",myBicycleData.speed));

                    break;

                default:
                    break;
            }
        }
    }

    public void showLength(String s){
        Length.setText(s);
    }



    public  void showSpeed(String s){
        Speed.setText(s);
    }


    public void showStatues(int i){
        switch (i){
            case BluetoothCommunication.ID_NOT_CONNECT:
                BLEstatus.setText("未连接");
                starto.setEnabled(false);
                stop.setEnabled(false);
                break;
            case BluetoothCommunication.ID_CONNECTED:
                BLEstatus.setText("已连接");
                starto.setEnabled(true);
                if(isTrainStart){
                    stop.setEnabled(true);
                }
                else{
                    stop.setEnabled(false);
                }
                break;
            case BluetoothCommunication.ID_CONNECTING:
                BLEstatus.setText("正在连接");
                starto.setEnabled(false);
                stop.setEnabled(false);
                break;
            case BluetoothCommunication.ID_SEARCHING:
                BLEstatus.setText("正在搜寻设备");
                starto.setEnabled(false);
                stop.setEnabled(false);
                break;
            case BluetoothCommunication.ID_SEARCHFINISHED:
                BLEstatus.setText("搜寻完毕");
                starto.setEnabled(false);
                stop.setEnabled(false);
                break;
            default:
                break;
        }
    }

    //获取组件
    public void initControler(){
        Length = (TextView)findViewById(R.id.Length);
        Speed = (TextView)findViewById(R.id.Speed);
        BLEstatus = (TextView)findViewById(R.id.BlEstatus);
        ch3 = (CheckBox)findViewById(R.id.checkBox3) ;
        ch4 = (CheckBox)findViewById(R.id.checkBox4) ;
        ch5 = (CheckBox)findViewById(R.id.checkBox5) ;
        starto = (Button) findViewById(R.id.starto);
        stop = (Button) findViewById(R.id.stop);
        pause = (Button) findViewById(R.id.pause);
        mChart = (LineChartView) findViewById(R.id.chart);
    }


//    public void ChangeChart(LineChartView mChart){
//        for (int i=0 ; i < 4;i++){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            values.add(new PointValue(ChangeValue(infomation.getCurrentMin()), 1 + ChangeValue(infomation.getCurrentMin())));
//            Log.i("tag",String.valueOf(infomation.getCurrentMin()));
//            line.setValues(values);
//
//
//
//            lines.add(line);
//            mChart.setLineChartData(data);
//
//
//        }
//
//
//        }
    public void setmChart(ArrayList<PointValue> values){

//            ArrayList<PointValue> values = new ArrayList<PointValue>();
//
//
//            values.add(new PointValue(0, 0));
//            values.add(new PointValue(1, 1.5f));
//            values.add(new PointValue(ChangeValue(infomation.getCurrentMin()), 1 + ChangeValue(infomation.getCurrentMin())));
            Log.i("tag",String.valueOf(infomation.getCurrentMin()));
            Line line = new Line(values);
            ArrayList<Line> lines = new ArrayList<Line>();
            lines.add(line);
            line.setColor(ChartUtils.COLOR_GREEN);
            line.setShape(ValueShape.CIRCLE);
            line.setHasPoints(true);
            line.setCubic(true);
            line.setHasLabels(false);
            line.setFilled(true);
            Axis axisX = new Axis();
            Axis axisY = new Axis();
            axisX.setName("锻炼时间（min）");
            axisY.setName("当前速度（km/h）");

            LineChartData data = new LineChartData(lines);
            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
            mChart.setZoomEnabled(false);
            mChart.setLineChartData(data);








    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        menu.add(0,menu_setting,1,"设置").setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0,history,1,"查看最终锻炼结果").setIcon(android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }
    public float ChangeValue(long index){

        return Float.valueOf(String.valueOf(index));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//获取菜单信息
        Intent intent1 = new Intent(this, SettingActivity.class);
        Intent intent2 = new Intent(this, BluetoothView.class);
        // TODO Auto-generated method stub
        if(item.getItemId() == menu_setting)
            startActivity(intent1);
        else if (item.getItemId() == history)


            if(RealLength == null|| RealLength.equals("0")){
                Toast.makeText(MainActivity.this,"你还没锻炼看啥结果侬脑子瓦特辣？？？",Toast.LENGTH_SHORT).show();

            }
            else {
                intent2.putExtra("RealLength", RealLength);
                startActivity(intent2);
                Log.e("Real",RealLength);
            }


        return false;
    }
    public void fresh(){
        showLength("0");
        showSpeed("0");
        values.clear();
    }



}
