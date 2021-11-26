import java.io.*;
import java.net.*;

public class Client {

	private static final String DEFAULT_ADDRESS = "127.0.0.1";

	private int port;
	private String address;
	private Socket socket;
	private DataInputStream serverIn;
	private DataInputStream systemIn;
	private DataOutputStream out;

	public Client(String address, int port) {
		this.port = port;
		this.address = address;
	}

	private void run() {
		try {
			socket = new Socket(address, port);
			System.out.println("Connected to server");
			System.out.println("Enter \"END\" to exit");
			serverIn = new DataInputStream(
					new BufferedInputStream(
						socket.getInputStream()));
			systemIn = new DataInputStream(System.in);
			out = new DataOutputStream(
					socket.getOutputStream());
		} catch (UnknownHostException e) {
			System.out.println("Could not connect to given address and/or port");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Could not connect to given address and/or port");
			System.exit(1);
		}
		Thread serverListener = new Thread(() -> {
			try {
				String line = "";
				line = serverIn.readUTF();
				while (!line.equals("END")) {
					System.out.println(line);
					line = serverIn.readUTF();
				}
				out.writeUTF(line);
			} catch (IOException e) {
				System.out.println("Server unexpectedly disconnected");
			}
			synchronized(this) {
				notify();
			}
		});
		serverListener.setDaemon(true);
		Thread systemListener = new Thread(() -> {
			String line = "";
			try {
				while (true) {
					line = systemIn.readLine();
					out.writeUTF(line);
				}
			} catch (IOException e) {
				System.out.println("Server unexpectedly disconnected");
			}
		});
		systemListener.setDaemon(true);
		serverListener.start();
		systemListener.start();
		try {
			synchronized(this) {
				wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		serverListener.interrupt();
		systemListener.interrupt();
		try {
			socket.close();
			System.out.println("Server disconnected");
		} catch (IOException e) {
			System.out.println("Problems occurred while disconnecting");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String address = DEFAULT_ADDRESS;
		int port = Server.DEFAULT_PORT;
		if (args.length == 1) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				address = args[0];
			}
		} else if (args.length >= 2) {
			try {
				port = Integer.valueOf(args[1]);
			} catch (NumberFormatException e) {
				try {
					port = Integer.valueOf(args[0]);
					address = args[1];
				} catch (NumberFormatException e$) {
					System.out.println("Unable to understand the given arguments.\nTo connect to external computer, please provide the target computer's IPv4, and desired port (optional)");
				}
			}
		}
		Client client = new Client(address, port);
		client.run();
	}
}
