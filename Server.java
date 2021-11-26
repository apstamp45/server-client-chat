import java.io.*;
import java.net.*;

public class Server {

	public static final int DEFAULT_PORT = 48128;

	private int port;
	private Socket socket;
	private ServerSocket serverSocket;
	private DataInputStream clientIn;
	private DataInputStream systemIn;
	private DataOutputStream out;

	public Server(int port) {
		this.port = port;
	}

	private void run() {
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Server started");
			socket = serverSocket.accept();
			System.out.println("Client connected");
			System.out.println("Enter \"END\" to exit");
			clientIn = new DataInputStream(
					new BufferedInputStream(
						socket.getInputStream()));
			systemIn = new DataInputStream(System.in);
			out = new DataOutputStream(
					socket.getOutputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread clientListener = new Thread(() -> {
			try {
				String line = "";
				line = clientIn.readUTF();
				while (!line.equals("END")) {
					System.out.println(line);
					line = clientIn.readUTF();
				}
				out.writeUTF(line);
			} catch (IOException e) {
				System.out.println("Client unexpectedly disconnected");
			}
			synchronized(this) {
				notify();
			}
		});
		clientListener.setDaemon(true);
		Thread systemListener = new Thread(() -> {
			String line = "";
			try {
				do {
					line = systemIn.readLine();
					out.writeUTF(line);
				} while (!line.equals("END"));
			} catch (IOException e) {
				System.out.println("Client unexpectedly disconnected");
			}
		});
		systemListener.setDaemon(true);
		clientListener.start();
		systemListener.start();
		try {
			synchronized(this) {
				wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			socket.close();
			System.out.println("Client disconnected");
		} catch (IOException e) {
			System.out.println("Problems occurred while disconnecting");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Unable to understand the given argument/s. If desired, please provide a valid port number.");
			}
		}
		Server server = new Server(port);
		server.run();
	}
}
