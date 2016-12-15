package com.example.tcpclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

public class ClientThread implements Runnable
{
	private final String TAG = "ClientThread";
	
	private Socket socket;
	private String ip;
	private int port;
	private Handler receiveHandler;
	public Handler sendHandler;
	private InputStream inputStream;
	private OutputStream outputStream;
	public boolean isConnect = false;

	public ClientThread(Handler handler, String ip, String port) {
		this.receiveHandler = handler;
		this.ip = ip;
		this.port = Integer.parseInt(port);
		Log.d(TAG, "ClientThread's construct is OK!!");
	}

	public ClientThread()
	{
		Log.d(TAG, "It is may be construct's problem...");
	}

	public void run()
	{
		try 
		{
			Log.d(TAG, "Into the run()");
			socket = new Socket(ip, port);

			isConnect = socket.isConnected();
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
			
			//To monitor if receive Msg from Server
			new Thread()
			{
				@Override
				public void run()
				{
					byte[] buffer = new byte[1024];

					StringBuilder stringBuilder = new StringBuilder();
					try
					{
						while(socket.isConnected())
						{
							int readSize = inputStream.read(buffer);
							Log.d(TAG, "readSize:" + readSize);
							
							//If Server is stopping
							if(readSize == -1)
							{
								inputStream.close();
								outputStream.close();
							}
							if(readSize == 0)continue;

							stringBuilder.append(new String(buffer, 0, readSize));
							Message msg = new Message();
							msg.what = 0x123;
							msg.obj = stringBuilder.toString();
							receiveHandler.sendMessage(msg);
						}
					}
					catch(IOException e)
					{
						Log.d(TAG, e.getMessage());
						e.printStackTrace();
					}
				}
				
			}.start();
			
			//To Send Msg to Server
			Looper.prepare();
			sendHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == 0x852) { //сообщение чата
						try {
							byte[] mes = msg.obj.toString().getBytes();
							byte[] mesSize = Converter.getBytes(mes.length);
							byte[] mesCode = Converter.getBytes(852);

							outputStream.write(mesCode, 0, mesCode.length);
							Sleeper.milliseconds(100);
							outputStream.write(mesSize, 0, mesSize.length);
							Sleeper.milliseconds(100);
							outputStream.write(mes, 0, mes.length);
							outputStream.flush();
						} catch (Exception e) {
							Log.d(TAG, e.getMessage());
							e.printStackTrace();
						}
					}
					if (msg.what == 0x840) {
						try { //сообщение отправки ника на сервер
							byte[] mes = msg.obj.toString().getBytes();
							byte[] mesSize = Converter.getBytes(mes.length);

							outputStream.write(mesSize, 0, mesSize.length);
							outputStream.write(mes, 0, mes.length);
							outputStream.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (msg.what == 0x800) {
						try {	//отправка выбранного из галереи
							String imagePath = msg.obj.toString();
							File file = new File(imagePath);

							if (file.exists()) {
								Log.d(TAG, "handleMessage: File exists");
							} else {
								Log.d(TAG, "handleMessage: File not exists");
							}

							byte[] temp;
							byte[] size;
							byte[] mesCode = Converter.getBytes(800);

							Log.d(TAG, "handleMessage: sending Image");

							try {
								temp = Converter.FileToBytes(imagePath);
								size = Converter.getBytes(temp.length);

								outputStream.write(mesCode, 0, mesCode.length);
								Sleeper.milliseconds(100);
								outputStream.write(size, 0, size.length);
								Sleeper.milliseconds(100);
								outputStream.write(temp, 0, temp.length);
								outputStream.flush();
							} catch (IOException ex) {
								ex.printStackTrace();
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			Looper.loop();
			
		} catch (SocketTimeoutException e) 
		{
			// TODO Auto-generated catch block
			Log.d(TAG, e.getMessage());
			e.printStackTrace();
		}catch (UnknownHostException e) 
		{
			// TODO Auto-generated catch block
			Log.d(TAG, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			Log.d(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
}
