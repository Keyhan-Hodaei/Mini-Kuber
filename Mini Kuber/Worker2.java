package Network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Worker2 {

	private static final int MAX_TASK_NUMBER = 4;
	private static Socket worker;
	private static BufferedReader in;
	private static PrintWriter out;
	private static ArrayList<String> tasks = new ArrayList<String>();

	public static void main(String[] args) {
		try {
			worker = new Socket("127.0.0.1", 6666);
			out = new PrintWriter(worker.getOutputStream(), true);
			in =
				new BufferedReader(
					new InputStreamReader(worker.getInputStream())
				);

			// send worker info and start heartbeat
			out.println("new worker " + MAX_TASK_NUMBER);
			new heartbeat().start();

			// listen for tasks
			new listener().start();
		} catch (Exception e) {
			System.out.println("Worker: problem!:" + e.getMessage());
			e.printStackTrace();
		}
	}

	private static class heartbeat extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					out.println("heartbeat");
					// System.out.println("Worker: heartbeat");
					Thread.sleep(2000);
				}
			} catch (Exception e) {
				System.out.println("Worker: problem!:" + e.getMessage());
				e.printStackTrace();
			}
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
						String[] request = type.split(" ");

						// decide connection type with request[0]:
						if (type.startsWith("new task ")) {
							System.out.println("Worker: new task");
							tasks.add(request[2]);
						} else if (type.startsWith("delete task")) {
							System.out.println("Worker: delete task");
							tasks.remove(request[2]);
						}
					}
				} catch (Exception e) {
					System.out.println("Worker: problem!:" + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}
