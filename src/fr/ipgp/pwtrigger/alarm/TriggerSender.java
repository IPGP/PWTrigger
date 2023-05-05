/**
 * 
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
 * @author patriceboissier
 *
 */
public class TriggerSender {
	private int port;
	private InetAddress inetAddress = null;
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;

	public TriggerSender(InetAddress inetAddress, int port) {
		this.inetAddress = inetAddress;
		this.port = port;
	}

	public void send(int priority, String confirmCode, String callList, String warningMessage, boolean repeat) {
		byte[] message = new byte[512];
		Date date = new Date();
		SimpleDateFormat  simpleFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		//Format V2 : vv p yyyy/MM/dd HH:mm:ss application calllist repeat confirmcode message<br/>
		String messageString = "02 ";
		messageString += priority + " ";
		messageString += simpleFormat.format(date) + " ";
		messageString += "EarthWorm ";
		messageString += callList + " ";
		messageString += repeat + " ";
		messageString += confirmCode + " ";
		messageString += warningMessage;
		
		System.out.println("Trigger : " + messageString);
		
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
