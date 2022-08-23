package com.example.blth;


import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button TurnOn, TurnOff, PairedDevice,ServerStart,send_BTN;
    ListView DeviceListView;
    TextView DeviceStatus_TV,pairdDevice_TV,message_TV;
    EditText send_ET;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice [] paired_device_array;
    String DeviceName;

    ArrayAdapter arrayAdapter;
    static final int REQUEST_CODE_ENABLE = 200;
    static final int Bluetooth_REQ_FOR_PAIRED_DATA = 300;
    static final int BLUETOOTH_ENABLE_FOR_SERVER_START = 400;

    static  final int STATE_LISTENING=1;
    static  final int STATE_CONNECTING=2;
    static  final int STATE_CONNECTED=3;
    static  final int STATE_CONNECTION_FAILED=4;
    static  final int STATE_MESSAGE_RECIEVED=5;

    private  static final String APP_NAME= "Bluetooth App";
    private  static final UUID UUID= java.util.UUID.fromString("9bbb4aaa-c772-4e30-853a-e6a64f5e30f3");

    SendRecieve sendRecieve;

    @SuppressLint({"NewApi", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        TurnOn = findViewById(R.id.turnOn_btn);
//        TurnOff = findViewById(R.id.turnOff_btn);

        PairedDevice = findViewById(R.id.pairdDevice_btn);
        ServerStart=findViewById(R.id.server_start_btn);
        send_BTN=findViewById(R.id.send_BTN);

        send_ET=findViewById(R.id.send_ET);

        pairdDevice_TV=findViewById(R.id.pairdDevice_TV);
        message_TV=findViewById(R.id.message_TV);
        DeviceStatus_TV = findViewById(R.id.deviceStatus);

        DeviceListView = findViewById(R.id.deviceName_LV);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Check devices Bluetooth are On or Off
        isBluetoothOn();

        //Button Clicks
        btnClicks();

    }

    private void btnClicks() {

        //Bluetooth Turn On Button
//        TurnOn.setOnClickListener(new View.OnClickListener() {
//            @SuppressLint("MissingPermission")
//            @Override
//            public void onClick(View view) {
//                isBluetoothOn();
//            }
//        });

        //Bluetooth Turn Off Button

//        TurnOff.setOnClickListener(new View.OnClickListener() {
//            @SuppressLint("MissingPermission")
//            @Override
//            public void onClick(View view) {
//                if (bluetoothAdapter.isEnabled()) {
//                    bluetoothAdapter.disable();
//                    Toast.makeText(MainActivity.this, "Bluetooth is Turning Off", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(MainActivity.this, "Bluetooth is Already Off", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        PairedDevice.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                pairedDeviceData();
            }
        });

        ServerStart.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if(bluetoothAdapter.isEnabled())
                {
                    ServerClass serverClass=new ServerClass();
                    serverClass.start();
                }
                else
                {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,BLUETOOTH_ENABLE_FOR_SERVER_START);
                }

            }
        });

        DeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass connectThread=new ClientClass(paired_device_array[i]);
                connectThread.start();
                DeviceName=paired_device_array[i].getName();
                DeviceStatus_TV.setText("Connecting");
            }
        });

        send_BTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text=String.valueOf(send_ET.getText());

                if(sendRecieve==null)
                {
                    Toast.makeText(MainActivity.this, "Please Pair Device with other devices", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if(text.isEmpty())
                    {
                        Toast.makeText(MainActivity.this, "Message Cannot be Empty", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        sendRecieve.write(text.getBytes());
                    }
                }
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what)
            {
                case STATE_LISTENING:
                    DeviceStatus_TV.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    DeviceStatus_TV.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    if(DeviceName==null)
                    {
                        DeviceStatus_TV.setText("Connected");
                    }
                    else
                    {
                        DeviceStatus_TV.setText("Connected to :"+DeviceName);
                    }

                    break;
                case STATE_CONNECTION_FAILED:
                    DeviceStatus_TV.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECIEVED:
                    byte[] readBuff= (byte[]) message.obj;
                    String tempMsg=new String(readBuff,0,message.arg1);
                    message_TV.setText(tempMsg);
                   break;
            }
            return true;
        }
    });

    @SuppressLint("MissingPermission")
    private void isBluetoothOn() {
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Device Does Not Support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent,REQUEST_CODE_ENABLE);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Bluetooth is Already on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceData() {
        if(bluetoothAdapter.isEnabled())
        {
           @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
           String[] strings=new String[pairedDevices.size()];
           paired_device_array=new BluetoothDevice[pairedDevices.size()];
            int i=0;

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    paired_device_array[i]=device;
                    strings[i]=device.getName();
                    i++;
                }
                arrayAdapter=new ArrayAdapter(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,strings);
                DeviceListView.setAdapter(arrayAdapter);
                pairdDevice_TV.setText("Paired Device List :");

            }
        }
        else
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,Bluetooth_REQ_FOR_PAIRED_DATA);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_ENABLE)
        {
            if(resultCode==RESULT_OK)
            {
                Toast.makeText(MainActivity.this, "Bluetooth is Turned on", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if(requestCode==Bluetooth_REQ_FOR_PAIRED_DATA)
        {
            if(resultCode==RESULT_OK)
            {
                Toast.makeText(MainActivity.this, "Bluetooth is Turned on", Toast.LENGTH_SHORT).show();
                pairedDeviceData();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==BLUETOOTH_ENABLE_FOR_SERVER_START)
        {
            if(resultCode==RESULT_OK)
            {
                ServerClass serverClass=new ServerClass();
                serverClass.start();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public ServerClass()
        {
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            BluetoothSocket socket=null;
            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendRecieve=new SendRecieve(socket);
                    sendRecieve.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ClientClass(BluetoothDevice device1){
            device=device1;
            try {
                socket=device.createRfcommSocketToServiceRecord(UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        public void run()
        {
            try {
                socket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendRecieve=new SendRecieve(socket);
                sendRecieve.start();
            } catch (IOException e) {
                e.printStackTrace();

                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendRecieve extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve(BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECIEVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }


}

