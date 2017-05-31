package com.company;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class Executor implements Runnable, Watcher {

    private static ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, KeeperException {
        if (args.length < 4) {
            System.err.println("USAGE: Executor hostPort znode filename program [args ...]");
            System.exit(2);
        }
        String hostPort = args[0];
        String znode = args[1];
        String exec[] = new String[args.length - 2];
        System.arraycopy(args, 2, exec, 0, exec.length);
        zooKeeper = new ZooKeeper(hostPort, 3000, null);

        Thread watcher = new Thread(new Executor(znode, zooKeeper, exec, Runtime.getRuntime()));
        watcher.start();

        final Scanner scanner = new Scanner(System.in);

        while (true) {
            final String input = scanner.next();

            if ("tree".equals(input)) {
                this.tree(znode, 0);
            }
            else if ("exit".equals(input)) {
                watcher.interrupt();
                break;
            }
        }
    }

    public Executor(String znode, ZooKeeper zooKeeper, String exec[], Runtime context) throws KeeperException, IOException {
        this.znode = znode;
        this.zooKeeper = zooKeeper;
        zooKeeper.register(this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    private void tree(String znode, int indent) throws KeeperException, InterruptedException {
        for (int i =0; i<indent; i++){
            System.out.println(" ");
        }
        System.out.println(znode);

        for (String child : this.zooKeeper.getChildren(znode, false)) {
            this.tree(znode + "/" + child, indent + 1);
        }
    }

    public void run() {
        try {
            synchronized (this) {
                while (!dm.dead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}