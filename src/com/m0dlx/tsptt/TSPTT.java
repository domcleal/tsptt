/**
 * Copyright (c) 2011, Dominic Cleal.
 * 
 * Released under the 2-clause BSD licence, see LICENCE.
 */
package com.m0dlx.tsptt;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

/**
 * Runs a background task that listens to COM3 for a single digit to know when
 * to turn on and off the TeamSpeak PTT.
 * 
 * Uses F5 as the PTT key (to be configured in TS) and COM3 as the serial port.
 * 
 * Requires the rxtx serial library (tested with Cloudhopper x86 binaries):
 *   http://rxtx.qbang.org/wiki/index.php/Download
 */
public class TSPTT {
	private static int KEY_CODE = KeyEvent.VK_F5;
	private static String PORT_NAME =
			System.getProperty("os.name").startsWith("Windows")
			 ? "COM3" : "/dev/ttyUSB0";
	
	private int keyCode_;
	private Robot robot_ = new Robot();
	private SerialPort serial_;
	private Thread reader_ = Thread.currentThread();
	
	/**
	 * Usage: TSPTT [COM port] [Key code (int)]
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 0)
			PORT_NAME = args[0];
		if (args.length > 1)
			KEY_CODE = Integer.parseInt(args[1]);
		
		TSPTT ptt = new TSPTT(PORT_NAME, KEY_CODE);
		ptt.monitor();
	}
	
	public TSPTT(String portName, int keyCode) throws Exception {
		keyCode_ = keyCode;
		initSystemTray();
		serial_ = initSerial(portName);
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				reader_.interrupt();          // stop blocking thread
				robot_.keyRelease(keyCode_);  // stop hanging keys
				serial_.close();
			}
		}));
	}
	
	/** Configure Arduino 9600 8n1 serial connection with rxtx */
	private SerialPort initSerial(String portName) throws Exception {
		CommPortIdentifier portIdentifier =
			CommPortIdentifier.getPortIdentifier(portName);
        if (portIdentifier.isCurrentlyOwned())
        {
            System.err.println("Error: Port " + portName +
            		" is currently in use");
            System.exit(1);
        }
        
        SerialPort serialPort = (SerialPort)
    		portIdentifier.open(this.getClass().getName(), 2000);
        
        serialPort.setSerialPortParams(9600,
        		SerialPort.DATABITS_8,
        		SerialPort.STOPBITS_1,
        		SerialPort.PARITY_NONE);
        
        // Set timeout to ensure reader thread doesn't block forever, as rxtx
        // ignores interrupts
        serialPort.enableReceiveTimeout(1000);
        
        return serialPort;
	}
	
	/** Read switch state and press keys, until the thread is interrupted */
	public void monitor() throws InterruptedException, IOException {
		InputStream is = serial_.getInputStream();
		int data;
		while (!Thread.currentThread().isInterrupted()) {
			data = is.read();  // timeout set above
			if (data > 0)
				robot_.keyPress(keyCode_);
			else if (data == 0)
				robot_.keyRelease(keyCode_);
		}
	}
	
	/** Add a simple system tray icon to close TSPTT easily */
	private void initSystemTray() {
		if (!SystemTray.isSupported()) return;
		
		PopupMenu popup = new PopupMenu();
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				 System.exit(0);
			}
		});
		popup.add(exitItem);
		
		Image image = Toolkit.getDefaultToolkit().getImage(
				TSPTT.class.getResource("icon.png")
		);
		TrayIcon trayIcon = new TrayIcon(image, "TS PTT", popup);
		trayIcon.setImageAutoSize(true);
		
		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
		    System.err.println(e);
		}
	}
}
