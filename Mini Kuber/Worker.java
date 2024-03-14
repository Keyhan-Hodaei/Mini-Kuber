package Network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Worker {

	private static final int MAX_TASK_NUMBER = 2;
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
							tasks.add(request[2]);
							System.out.println("scheduled task " + request[2]);
						} else if (type.startsWith("delete task ")) {
							tasks.remove(request[2]);
							System.out.println("deleted task " + request[2]);
						} else if (type.equals("cordon")) {
							System.out.println("cordon");
							tasks.clear();
						} else if (type.equals("uncordon")) {
							System.out.println("uncordon");
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
