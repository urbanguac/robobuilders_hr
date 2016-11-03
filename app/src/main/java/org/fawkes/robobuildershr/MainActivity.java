package org.fawkes.robobuildershr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    public TextView text; Handler h = new Handler(); int delay = 600;


    Button Connect;
    ToggleButton OnOff;
    TextView Result;
    private String dataToSend;

    private static final String TAG = "Jon";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static String address = "00:14:02:13:00:10";  //address
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inStream = null;
    Handler handler = new Handler();
    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];


    private void CheckBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",
                    Toast.LENGTH_SHORT).show();
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth null !", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void Connect() {
        Log.d(TAG, address);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        Log.d(TAG, "Connecting to ... " + device);
        mBluetoothAdapter.cancelDiscovery();
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
            Toast.makeText(getApplicationContext(), "Connessione effettuata",
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
                Toast.makeText(getApplicationContext(), "Impossibile terminare la connessione",
                        Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "Socket creation failed");
            Toast.makeText(getApplicationContext(), "Creazione socket fallita.",
                    Toast.LENGTH_SHORT).show();
        }

        beginListenForData();
    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
            Toast.makeText(getApplicationContext(), "Bug prima di mandare i dati",
                    Toast.LENGTH_SHORT).show();
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
            Toast.makeText(getApplicationContext(), "Dati inviati correttamente",
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d(TAG, "Bug while sending stuff", e);
            Toast.makeText(getApplicationContext(), "Bug mandando i dati",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        try {
            btSocket.close();
        } catch (IOException e) {
        }
    }

    public void beginListenForData()   {
        try {
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
        }

        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {

                                            //     if(Result.getText().toString().equals("..")) {
                                            Result.setText(data);
                                            // } else {
                                            //        Result.append("\n"+data);
                                            //}

                                                    /* You also can use Result.setText(data); it won't display multilines
                                                    */

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;

                    }
                }
            }
        });

        workerThread.start();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = new TextView(this);
        text = (TextView)findViewById(R.id.textView);
        text.setText("00");
        h.postDelayed(new Runnable() {
            public void run() {
                text.setText("" + (100 + (Math.random() * 20))); //set random 100 vary 20
                h.postDelayed(this, delay);
            }
        }, delay);

        

        CheckBt();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        Connect();



    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}