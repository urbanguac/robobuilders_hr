package org.fawkes.robobuildershr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "bluetooth2";

    Button btnOn, btnOff;
    TextView txtArduino;
    Handler h;

    Handler kseth;

    final int RECEIVE_MESSAGE = 1;    //1    // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "20:16:08:10:38:34";
RelativeLayout rl; TextView text; int beatsPerMin = 0; int iterations = 0; String result = ""; int delay = 600;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        rl = (RelativeLayout) findViewById(R.id.relativo);
        text = new TextView(this);
        text = (TextView)findViewById(R.id.textView);
        text.setText("00");
        kseth = new Handler();
        kseth.postDelayed(new Runnable() {
            public void run() {
                beatsPerMin = (result.indexOf("caerus") < 0) ? ((int) (((100 + (Math.random() * 10))))) : ((int) (((170 + (Math.random() * 7)))));
                if (beatsPerMin > 130) {
                    rl.setBackgroundColor(getResources().getColor(R.color.red));
                    text.setTextColor(getResources().getColor(R.color.white));
                    iterations = iterations + 1;
                } else {
                    rl.setBackgroundColor(getResources().getColor(R.color.white));
                    text.setTextColor(getResources().getColor(R.color.black));
                }
                if (result.indexOf("caerus") > -1 && iterations > 16) {
                    result = "";
                    iterations = 0;
                }
                text.setText("" + beatsPerMin + " BPM"); //set random 100 vary 20
                kseth.postDelayed(this, delay);
            }
        }, delay);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1); // create string from bytes array
                        Toast.makeText(getBaseContext(), strIncom, Toast.LENGTH_LONG).show();

                        sb.append(strIncom);                                                // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                        if (endOfLineIndex > 0) {                                            // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                            sb.delete(0, sb.length());                                      // and clear
                            result = result + strIncom;           // update TextView
                            //  btnOff.setEnabled(true);
                            //  btnOn.setEnabled(true);
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                Toast.makeText(this.getApplicationContext(),"Something is wrong with Bluetooth connection.",Toast.LENGTH_LONG);
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }


    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        mConnectedThread.write("1");
        Toast.makeText(getApplicationContext(),"resuming",Toast.LENGTH_LONG);

    }


    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");


    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d(TAG,"GOODJOB");
            } catch (IOException e) {
                Log.d(TAG,"STH IS VERY VERY WRONG");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.d(TAG,"GOODJOB2.0");
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    Log.d(TAG,"Reading");
                    // Read from the InputStream
                    bytes = btSocket.getInputStream().read(buffer);        // Get number of bytes and message in "buffer"
                    Log.d(TAG,"FINISHING A1");
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                    Log.d(TAG,"FINISHING A2");
                    Toast.makeText(getApplicationContext(),"Working.",Toast.LENGTH_LONG);
                } catch (IOException e) {
                    Log.d(TAG,"LOL"+e.getMessage()); Toast.makeText(getApplication(),"something went wrong again",Toast.LENGTH_LONG);
                    break;
                }
            }
            Log.e("BTROBO","EXITED");
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}