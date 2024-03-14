package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class WorkerInfo {

	String ip;
	int id, port;
	int capacity;
	Socket socket;
	ArrayList<String> tasks;
	boolean isAlive = true, heartbeatReceived = true, cordon = false;
	PrintWriter out;
	BufferedReader in;

	public WorkerInfo(
		int id,
		String ip,
		int port,
		int capacity,
		Socket socket,
		BufferedReader in,
		PrintWriter out
	) {
		this.id = id;
		this.port = port;
		this.ip = ip;
		this.capacity = capacity;
		this.socket = socket;
		this.in = in;
		this.out = out;
		tasks = new ArrayList<String>();
	}
}

class Task {

	String name, state;
	int worker = -1, target = -1;

	public Task(String name, String state, int worker) {
		this.name = name;
		this.state = state;
		this.worker = worker;
	}

	public Task(String name, String state) {
		this.name = name;
		this.state = state;
	}
}

public class Server {

	private static final int port = 6666;
	private static ArrayList<WorkerInfo> workers = new ArrayList<WorkerInfo>();
	private static ArrayList<Task> tasks = new ArrayList<Task>();
	private static Queue<Task> pendQueue = new LinkedList<Task>();
	// private static BufferedReader serverIn;
	private static PrintWriter serverOut;
	private static Thread heartbeatThread = new heartbeat();

	public static void main(String[] args) throws Exception {
		ServerSocket listener;
		try {
			listener = new ServerSocket(port);
			System.out.println("server running!");
			while (true) {
				new handle(listener.accept()).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("port 6666 unavailable?");
		} finally {
			System.out.println();
		}
	}

	public static int createWorker(
		Socket socket,
		int capacity,
		BufferedReader in,
		PrintWriter out
	) {
		workers.add(
			new WorkerInfo(
				workers.size(),
				socket.getInetAddress().toString().substring(1),
				socket.getPort(),
				capacity,
				socket,
				in,
				out
			)
		);
		if (serverOut != null) {
			serverOut.println("worker created");
		}
		if (workers.size() == 1) {
			heartbeatThread.start();
		}
		return workers.size() - 1;
	}

	public static void cordonWorker(int id) {
		if (workers.size() < id) {
			serverOut.println("invalid worker");
			return;
		}
		if (workers.get(id).cordon) {
			serverOut.println("worker already cordoned");
			return;
		}
		WorkerInfo worker = workers.get(id);
		worker.cordon = true;
		worker.out.println("cordon");
		serverOut.println("worker cordoned");
		worker.tasks.clear();
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).worker == id) {
				tasks.get(i).worker = -1;
				tasks.get(i).state = "pending";
				pendQueue.add(tasks.get(i));
				serverOut.println(
					tasks.get(i).name + " added to pending tasks"
				);
				tasks.remove(i);
				i--;
			}
		}
		handlePendQueue();
	}

	public static void uncordonWorker(int id) {
		if (workers.size() < id) {
			serverOut.println("invalid worker");
			return;
		}
		if (!workers.get(id).cordon) {
			serverOut.println("worker already available");
			return;
		}
		workers.get(id).cordon = false;
		workers.get(id).out.println("uncordon");
		serverOut.println("worker uncordoned");
		handlePendQueue();
	}

	public static void deleteTask(String name) {
		int freedWorker = -1;
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).name.equals(name)) {
				workers.get(tasks.get(i).worker).tasks.remove(name);
				freedWorker = tasks.get(i).worker;
				tasks.remove(i);
				serverOut.println("task deleted");
				workers.get(freedWorker).out.println("delete task " + name);
				break;
			}
		}
		if (freedWorker == -1) {
			serverOut.println("task not found");
			return;
		}
		handlePendQueue();
	}

	public static void handlePendQueue() {
		while (!pendQueue.isEmpty()) {
			if (pendQueue.peek().target == -1) { //find any free worker
				int freeWorker = -1;
				for (int j = 0; j < workers.size(); j++) {
					if (workers.get(j).cordon) continue;
					if (workers.get(j).tasks.size() < workers.get(j).capacity) {
						freeWorker = j;
						break;
					}
				}
				if (freeWorker == -1) break;
				Task task = pendQueue.poll();
				createTaskInWorker(task.name, freeWorker);
				serverOut.println(
					"pending task " + task.name + " started running"
				);
			} else if ( //check if the target worker is free
				!workers.get(pendQueue.peek().target).cordon &&
				workers.get(pendQueue.peek().target).tasks.size() <
				workers.get(pendQueue.peek().target).capacity
			) {
				Task task = pendQueue.poll();
				createTaskInWorker(task.name, task.target);
				serverOut.println(
					"pending task " + task.name + " started running"
				);
			} else { //top task can't be run
				break;
			}
		}
	}

	public static void createTask(String name) {
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).name.equals(name)) {
				serverOut.println("task with same name already exists");
				return;
			}
		}
		int minIndex = -1;
		float minCap = Float.MAX_VALUE;
		for (int i = 0; i < workers.size(); i++) {
			if (workers.get(i).cordon) continue;
			if (
				(float) workers.get(i).tasks.size() /
				workers.get(i).capacity <
				minCap &&
				workers.get(i).tasks.size() != workers.get(i).capacity
			) {
				minIndex = i;
				minCap =
					(float) workers.get(i).tasks.size() /
					workers.get(i).capacity;
			}
		}
		if (minIndex == -1) {
			createTaskPending(name, minIndex);
			return;
		}
		createTaskInWorker(name, minIndex);
	}

	public static void createTask(String name, int target) {
		if (target >= workers.size()) {
			serverOut.println("worker not found");
			return;
		} else if (
			workers.get(target).cordon ||
			workers.get(target).tasks.size() >= workers.get(target).capacity
		) {
			createTaskPending(name, target);
			return;
		} else {
			createTaskInWorker(name, target);
			return;
		}
	}

	public static void createTaskInWorker(String name, int worker) {
		workers.get(worker).tasks.add(name);
		tasks.add(new Task(name, "running", worker));
		serverOut.println("task created");
		workers.get(worker).out.println("new task " + name);
	}

	public static void createTaskPending(String name, int target) {
		pendQueue.add(new Task(name, "pending", target));
		serverOut.println("pending task created");
	}

	public static void getTasks() {
		if (tasks.size() == 0) {
			serverOut.println("no tasks");
			return;
		}
		for (int i = 0; i < tasks.size(); i++) {
			serverOut.println(
				tasks.get(i).name +
				" " +
				tasks.get(i).state +
				" on worker " +
				tasks.get(i).worker
			);
		}
		pendQueue.forEach(task -> {
			serverOut.println(task.name + " " + task.state);
		});
	}

	public static void getNodes() {
		if (workers.size() == 0) {
			serverOut.println("no nodes");
			return;
		}
		serverOut.println("ip:port\t\t\t\tid\tcurrent/capacity\tenabled");
		for (WorkerInfo worker : workers) {
			String state = worker.cordon
				? "unschedulable"
				: (worker.isAlive ? "active" : "NetworkUnavailable");
			serverOut.println(
				worker.ip +
				":" +
				worker.port +
				"\t\t" +
				worker.id +
				"\t" +
				worker.tasks.size() +
				"/" +
				worker.capacity +
				"\t\t\t\t\t" +
				state
			);
		}
	}

	private static class handle extends Thread {

		Socket socket;
		BufferedReader in;
		PrintWriter out;
		int id = -1;

		// sets input stream on socket
		public handle(Socket socket) {
			System.out.println("someone connected");
			this.socket = socket;
			try {
				in =
					new BufferedReader(
						new InputStreamReader(socket.getInputStream())
					);
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// main runnable. handles every connection
		@Override
		public void run() {
			try {
				while (true) {
					String type = null;
					type = in.readLine();
					if (null != type) {
						String[] request = type.split(" ");
						// handle request
						if (type.equals("heartbeat")) {
							workers.get(id).heartbeatReceived = true;
						} else if (type.startsWith("new worker ")) {
							try {
								this.id =
									createWorker(
										socket,
										Integer.parseInt(request[2]),
										in,
										out
									);
							} catch (Exception e) {
								out.println("invalid max task number");
								socket.close();
								return;
							}
						} else if (type.startsWith("new client")) {
							// serverIn = in;
							serverOut = out;
						} else if (type.startsWith("k create task --name=")) {
							if (request.length == 4) {
								createTask(request[3].substring(7));
							} else if (request[4].startsWith("--node=")) {
								try {
									createTask(
										request[3].substring(7),
										Integer.parseInt(
											request[4].substring(7)
										)
									);
								} catch (Exception e) {
									serverOut.println("invalid worker");
								}
							}
						} else if (
							type.startsWith("k create delete task --name=")
						) {
							deleteTask(request[4].substring(7));
						} else if (type.equals("k get tasks")) {
							getTasks();
						} else if (type.equals("k get nodes")) {
							getNodes();
						} else if (type.startsWith("k cordon node ")) {
							try {
								cordonWorker(Integer.parseInt(request[3]));
							} catch (Exception e) {
								serverOut.println("invalid worker");
							}
						} else if (type.startsWith("k uncordon node ")) {
							try {
								uncordonWorker(Integer.parseInt(request[3]));
							} catch (Exception e) {
								serverOut.println("invalid worker");
							}
						} else {
							serverOut.println("wrong command!");
						}
					}
				}
				// get rid of useless thread
				// this.socket = null;
				// in = null;
				// out = null;
				// this.interrupt();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class heartbeat extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(8000);
					for (WorkerInfo worker : workers) {
						if (
							worker.heartbeatReceived == false && worker.isAlive
						) {
							worker.isAlive = false;
							serverOut.println(
								"node " +
								worker.id +
								" is now NetworkUnavailable."
							);
						} else if (
							worker.heartbeatReceived && worker.isAlive == false
						) {
							worker.isAlive = true;
							serverOut.println(
								"node " +
								worker.id +
								" is back in network again."
							);
						} else {
							worker.heartbeatReceived = false;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
