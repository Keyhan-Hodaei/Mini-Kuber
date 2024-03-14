# Mini-Kuber
A simulated task scheduling system

# Explanation
This project simulates a network. The **server** is the master node of network and connects all other nodes and handles the comminucations between these nodes.
**Workers** are nodes that recieve tasks and **client** is the interface that controls tasks and workers.
At first, the server node must be launched. After, we can launch client and workers.

# Features
* Adding and deleting tasks
* Enabling and disabling worker nodes
* Load balancing between workers
* Distributing tasks from a worker to another worker when one of them loses connection

# Client Commands
* `create task --name=<task> [--node=<worker>]`: creates a new task (you can use `--node=<worker>` to manually select a worker node)
* `delete task --name=<task>`: deletes a task
* `get tasks`: gets all tasks and their status
* `get nodes`: gets all nodes and their status
* `cordon node <worker>`: disables a worker node
* `uncordon node <worker>`: re-enables a worker node
