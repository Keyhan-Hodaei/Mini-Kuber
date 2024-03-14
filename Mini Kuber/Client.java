package Network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

	private static Socket worker;
	private static BufferedReader in;
	private static PrintWriter out;

	public static void main(String[] args) {
		try {
			worker = new Socket("127.0.0.1", 6666);
			out = new PrintWriter(worker.getOutputStream(), true);
			in =
				new BufferedReader(
					new InputStreamReader(worker.getInputStream())
				);

			// listen for tasks
			new listener().start();
			out.println("new client");
			while (true) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String command = reader.readLine();
				out.println(command);
			}
		} catch (Exception e) {
			System.out.println("Worker: problem!:" + e.getMessage());
			e.printStackTrace();
		}
	}

	private static class listener extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					String type = null;
					type = in.readLine();
					if (null != type) {
						System.out.println(type);
					}
				} catch (Exception e) {
					e.printStackTrace();
					this.interrupt();
				}
			}
		}
	}
}
