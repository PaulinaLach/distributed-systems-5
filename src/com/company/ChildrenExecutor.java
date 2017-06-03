package com.company;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.data.Stat;

import java.util.List;


public class ChildrenExecutor implements AsyncCallback.Children2Callback {

    private String znode;

    public ChildrenExecutor(String znode) {
        this.znode = znode;
    }


    @Override
    public void processResult(int i, String s, Object o, List<String> list, Stat stat) {
        if (list == null) return;
        System.out.println("Znode: " + znode + " has " + list.size() +  " children");
    }
}
