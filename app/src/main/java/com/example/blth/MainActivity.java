package com.example.blth;


import static android.bluetooth.BluetoothProfile.GATT;
import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    LinearLayout MainScreen,MainChatScreen;
    Button TurnOn, TurnOff, PairedDevice, ServerStart, send_BTN,disconnectBTN;
    ListView DeviceListView,chatListView;
    TextView DeviceStatus_TV, pairdDevice_TV, message_TV,connectedDeviceNameTV;
    EditText send_ET;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] paired_device_array;
    String DeviceName;

    ArrayAdapter arrayAdapter;

    static final int REQUEST_CODE_ENABLE = 200;
    static final int Bluetooth_REQ_FOR_PAIRED_DATA = 300;
    static final int BLUETOOTH_ENABLE_FOR_SERVER_START = 400;


    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECIEVED = 5;
    static final int STATE_WRITE = 6;
    static final int MESSAGE_DEVICE_NAME = 7;
    static final int STATE_DISCONNECTED = 8;

    private static final String APP_NAME = "Bluetooth App";
    private static final String DEVICE_NAME = "deviceName";
    private static final UUID UUID = java.util.UUID.fromString("9bbb4aaa-c772-4e30-853a-e6a64f5e30f3");

    SendRecieve sendRecieve;
      String DeviceName3;
    ArrayList arrayList = new ArrayList<>();



    @SuppressLint({"NewApi", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PairedDevice = findViewById(R.id.pairdDevice_btn);
        ServerStart = findViewById(R.id.server_start_btn);
        send_BTN = findViewById(R.id.send_BTN);
        send_ET = findViewById(R.id.send_ET);
        pairdDevice_TV = findViewById(R.id.pairdDevice_TV);
        //message_TV = findViewById(R.id.message_TV);
        DeviceStatus_TV = findViewById(R.id.deviceStatus);
        MainScreen=findViewById(R.id.MainScreen);
        MainChatScreen=findViewById(R.id.MainChatScreen);
        chatListView=findViewById(R.id.chatListView);
        connectedDeviceNameTV=findViewById(R.id.connectedDeviceNameTV);
        DeviceListView = findViewById(R.id.deviceName_LV);
        disconnectBTN=findViewById(R.id.disconnectBTN);

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
                if (bluetoothAdapter.isEnabled()) {
                    ServerClass serverClass = new ServerClass();
                    serverClass.start();
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, BLUETOOTH_ENABLE_FOR_SERVER_START);
                }

            }
        });

        DeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               if(bluetoothAdapter.isEnabled())
               {
                   ClientClass connectThread = new ClientClass(paired_device_array[i]);
                   connectThread.start();
                   DeviceName = connectThread.device.getName();
                   //DeviceName = paired_device_array[i].getName();
                   DeviceStatus_TV.setText("Connecting");
               }
               else
               {
                   Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                   startActivityForResult(intent, REQUEST_CODE_ENABLE);
               }
            }
        });

        send_BTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = String.valueOf(send_ET.getText());
                if (sendRecieve == null) {
                    Toast.makeText(MainActivity.this, "Please Pair Device with other devices", Toast.LENGTH_SHORT).show();
                } else {
                    if (text.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Message Cannot be Empty", Toast.LENGTH_SHORT).show();
                    } else {
                        sendRecieve.write(text.getBytes());
                        send_ET.setText("");
                    }
                }
                arrayAdapter.notifyDataSetChanged();
            }
        });

        disconnectBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(DeviceName3==null)
                {
                    DeviceStatus_TV.setText("Diconnect With : " + DeviceName);
                    connectedDeviceNameTV.setText("Disconnect  With : "+DeviceName);
                }
                if(DeviceName==null)
                {
                    connectedDeviceNameTV.setText("Disconnect With : "+DeviceName3);
                    DeviceStatus_TV.setText("Disconnect With : " + DeviceName3);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainChatScreen.setVisibility(View.GONE);
                        MainScreen.setVisibility(View.VISIBLE);
                        sendRecieve.cancel();
                    }
                },5000);

                //sendRecieve.cancel();
            }
        });
    }


    Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("MissingPermission")
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case STATE_LISTENING:
                    DeviceStatus_TV.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    DeviceStatus_TV.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    //DeviceStatus_TV.setText("Connected : "+DeviceName);
                    MainScreen.setVisibility(View.GONE);
                    MainChatScreen.setVisibility(View.VISIBLE);

                     //Devicename2=message.getData().getString(DEVICE_NAME);
                    //connectedDeviceNameTV.setText("Connected To : "+DeviceName);

                    connectedDeviceNameTV.setText("Connected To : "+DeviceName3);

                    if(DeviceName3==null)
                    {
                        connectedDeviceNameTV.setText("Connected To : "+DeviceName);
                    }
                    if(DeviceName==null)
                    {
                        connectedDeviceNameTV.setText("Connected To : "+DeviceName3);
                    }

                    break;
                case STATE_CONNECTION_FAILED:

                    if(DeviceName3==null)
                    {
                        DeviceStatus_TV.setText("Connection Failed With : " + DeviceName);
                        connectedDeviceNameTV.setText("Connection Failed With : "+DeviceName);
                    }
                    if(DeviceName==null)
                    {
                        connectedDeviceNameTV.setText("Connection Failed With : "+DeviceName3);
                        DeviceStatus_TV.setText("Connection Failed With : " + DeviceName3);
                    }


//                    DeviceStatus_TV.setText("Connection Failed " + DeviceName);
//                    connectedDeviceNameTV.setText("Connection Failed : "+DeviceName3);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                           MainChatScreen.setVisibility(View.GONE);
                           MainScreen.setVisibility(View.VISIBLE);

                           sendRecieve.cancel();
                        }
                    },5000);
                    break;
                case STATE_WRITE:
                    byte[] readBuff = (byte[]) message.obj;
                    String tempMsg = new String(readBuff);
                    arrayList.add("Me : " + tempMsg);
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_NAME: //228
                    //DeviceName2 = message.getData().getString(DEVICE_NAME);
//                    String connectedDevice = message.getData().getString(DEVICE_NAME);
//                    connectedDeviceNameTV.setText("Connected Tmo : "+connectedDevice);
                    //connectedDeviceNameTV.setText("Connected Tmo : "+DeviceName3);

                    break;
                case STATE_MESSAGE_RECIEVED:
                    byte[] readBuff1 = (byte[]) message.obj;
                    String tempMsg1 = new String(readBuff1, 0, message.arg1);
                    //message_TV.setText(tempMsg1);

                   if(DeviceName==null)
                   {
                       arrayList.add(DeviceName3 + " : " + tempMsg1);
                   }
                   else
                   {
                       arrayList.add(DeviceName + " : " + tempMsg1);
                   }

                    arrayAdapter = new ArrayAdapter(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,arrayList);
                    chatListView.setAdapter(arrayAdapter);
                    arrayAdapter.notifyDataSetChanged();
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
                startActivityForResult(intent, REQUEST_CODE_ENABLE);
                arrayList.clear();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth is Already on", Toast.LENGTH_SHORT).show();
                arrayList.clear();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceData() {
        if (bluetoothAdapter.isEnabled()) {
            @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            String[] strings = new String[pairedDevices.size()];
            paired_device_array = new BluetoothDevice[pairedDevices.size()];
            int i = 0;

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    paired_device_array[i] = device;
                    strings[i] = device.getName();
                    i++;
                }
                arrayAdapter = new ArrayAdapter(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, strings);
                DeviceListView.setAdapter(arrayAdapter);
                pairdDevice_TV.setText("Paired Device List :");

            }
            else
            {

            }
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Bluetooth_REQ_FOR_PAIRED_DATA);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth is Turned on", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == Bluetooth_REQ_FOR_PAIRED_DATA) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth is Turned on", Toast.LENGTH_SHORT).show();
                pairedDeviceData();
            } else {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == BLUETOOTH_ENABLE_FOR_SERVER_START) {
            if (resultCode == RESULT_OK) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            } else {
                Toast.makeText(MainActivity.this, "Please allow Bluetooth Permissions to use services", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public ServerClass() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        public void run() {
            BluetoothSocket socket = null;
                while (socket == null) {
                    try {
                        Message message = Message.obtain();
                        message.what = STATE_CONNECTING;
                        handler.sendMessage(message);

                        socket = serverSocket.accept();


                    } catch (IOException e) {
                        e.printStackTrace();

                        Message message = Message.obtain();
                        message.what = STATE_CONNECTION_FAILED;
                        handler.sendMessage(message);

                        BluetoothDevice device = null;
                        String a = device.getName();
                        Log.i("Device Name : ", a);
                    }

                    if (socket != null) {
                        Message message = Message.obtain();
                        message.what = STATE_CONNECTED;
                        handler.sendMessage(message);

                        sendRecieve = new SendRecieve(socket);
                        sendRecieve.start();
                        DeviceName3 = socket.getRemoteDevice().getName();

                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;

//                        Message message1 = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
//                        Bundle bundle = new Bundle();
//                        bundle.putString(MainActivity.DEVICE_NAME, String.valueOf(socket.getRemoteDevice().getName()));
//                        message.setData(bundle);
//                        handler.sendMessage(message1);



                    }

                }

            }
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ClientClass extends Thread {

        private BluetoothDevice device;
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ClientClass(BluetoothDevice device1) {
            device = device1;
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        @SuppressLint("MissingPermission")
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {

                    socket.connect();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendRecieve = new SendRecieve(socket);
                    sendRecieve.start();

//                    Message message1 = handler.obtainMessage(MESSAGE_DEVICE_NAME);
//                    Bundle bundle = new Bundle();
//                    bundle.putString(DEVICE_NAME, device.getName());
//                    message.setData(bundle);
//                    handler.sendMessage(message1);

//                Message message1 = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
//                Bundle bundle = new Bundle();
//                bundle.putString(MainActivity.DEVICE_NAME, device.getName());
//                message.setData(bundle);
//                handler.sendMessage(message1);



            } catch (IOException e) {
                e.printStackTrace();

                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);


            }
        }
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    public class SendRecieve extends Thread {

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;


               try {
                   tempIn = bluetoothSocket.getInputStream();
                   tempOut = bluetoothSocket.getOutputStream();
               } catch (IOException e) {
                   e.printStackTrace();
               }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();


                } catch (IOException e) {
                    e.printStackTrace();

                }

            }

        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                handler.obtainMessage(STATE_WRITE, -1, -1, bytes).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
        public void cancel() {
            try {
                inputStream.close();
                outputStream.close();
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        arrayAdapter = new ArrayAdapter(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,arrayList);
        chatListView.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();


    }

}

