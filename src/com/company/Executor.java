package com.company;

import java.io.IOException;
import java.util.Scanner;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import static java.lang.Thread.sleep;


public class Executor implements Runnable, Watcher {

    private static ZooKeeper zooKeeper;
    private final ChildrenExecutor childrenExecutor;
    private String znode;
    private Process currProcess;
    private String[] exec;

    public static void main(String[] args) throws IOException, KeeperException {
        if (args.length < 4) {
            System.err.println("USAGE: Executor hostPort znode program [args ...]");
            System.exit(2);
        }
        String hostPort = args[0];
        String znode = args[1];
        String exec[] = new String[args.length - 2];
        System.arraycopy(args, 2, exec, 0, exec.length);
        zooKeeper = new ZooKeeper(hostPort, 3000, null);

        Thread watcher = new Thread(new Executor(znode, exec));
        watcher.start();

        final Scanner scanner = new Scanner(System.in);

        while (true) {
            final String input = scanner.next();

            if ("tree".equals(input)) {
                try {
                    tree(znode, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if ("exit".equals(input)) {
                watcher.interrupt();
                break;
            }
        }
    }

    public Executor(String znode, String exec[]) throws KeeperException, IOException {
        this.znode = znode;
        this.exec = exec;
        zooKeeper.register(this);
        this.childrenExecutor = new ChildrenExecutor(znode);
        zooKeeper.exists(znode, true, null, this);
    }

    private static void tree(String znode, int indent) throws KeeperException, InterruptedException {
        for (int i =0; i<indent; i++){
            System.out.print(" ");
        }
        System.out.println(znode);

        try {
            for (String child : zooKeeper.getChildren(znode, false)) {
                tree(znode + "/" + child, indent + 1);
            }
        } catch (KeeperException ignored) {
        }
    }

    public void run() {
        while (true) {
            try {
                sleep(60);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        this.serveEvent(watchedEvent);
        zooKeeper.exists(znode, true, null, this);
        zooKeeper.getChildren(znode, true, childrenExecutor, this);
    }

    public void serveEvent(WatchedEvent watchedEvent) {
        final Event.EventType watchedEventType = watchedEvent.getType();

        if (!this.znode.equals(watchedEvent.getPath())) {
            return;
        }

        switch (watchedEventType) {
            case NodeCreated:
                if (this.currProcess != null) {
                    return;
                }
                try {
                    currProcess = Runtime.getRuntime().exec(this.exec);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case NodeDeleted:
                if (this.currProcess == null) {
                    return;
                }
                this.currProcess.destroy();
                try {
                    this.currProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
}