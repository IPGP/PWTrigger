/**
 * Created May 5, 2023 by Patrice Boissier
 * Copyright 2023 Observatoire volcanologique du Piton de La Fournaise / IPGP
 */
package fr.ipgp.pwtrigger.alarm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is used to send trigger to the alarm server
 * @author patriceboissier
 */
public class TriggerSender {
	private int port;
	private InetAddress inetAddress = null;
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;

	/**
	 * Constructor
	 * @param inetAddress the alarm server Internet address
	 * @param port the alarm server UDP port
	 */
	public TriggerSender(InetAddress inetAddress, int port) {
		this.inetAddress = inetAddress;
		this.port = port;
	}

	/**
	 * Send a trigger to the alarm server
	 * @param priority the trigger priority
	 * @param confirmCode the trigger confirm code
	 * @param callList the trigger call list
	 * @param warningMessage the trigger warning message
	 * @param repeat the trigger repeat flag
	 */
	public void send(int priority, String confirmCode, String callList, String warningMessage, boolean repeat) {
		byte[] message = new byte[512];
		Date date = new Date();
		SimpleDateFormat  simpleFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		//Format V2 : vv p yyyy/MM/dd HH:mm:ss application calllist repeat confirmcode message<br/>
		String messageString = "02 ";
		messageString += priority + " ";
		messageString += simpleFormat.format(date) + " ";
		messageString += "PWTrigger ";
		messageString += callList + " ";
		messageString += repeat + " ";
		messageString += confirmCode + " ";
		messageString += warningMessage;
		
		message = new byte[messageString.length()];
		message = messageString.getBytes();
		try {
			packet = new DatagramPacket(message, message.length, inetAddress, port);
			socket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
		}
		try {
			socket.send(packet);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		socket.close();

	}
}
