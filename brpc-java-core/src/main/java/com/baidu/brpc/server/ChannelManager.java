package com.baidu.brpc.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelManager {
    private static volatile ChannelManager instance;

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, List<Channel>> channelMap = new HashMap<String, List<Channel>>();
    private Set<Channel> channelSet = new HashSet<Channel>();
    private AtomicInteger index = new AtomicInteger(0);

    public static ChannelManager getInstance() {
        if (instance == null) {
            synchronized(ChannelManager.class) {
                if (instance == null) {
                    instance = new ChannelManager();
                }
            }
        }
        return instance;
    }

    private ChannelManager() {
    }

    public void report(String r) {
        log.info("invoke report:" + r);
    }

    public void putChannel(String clientName, Channel channel) {
        lock.writeLock().lock();
        if (!channelSet.contains(channel)) {
            List<Channel> channelList = channelMap.get(clientName);
            if (channelList == null) {
                channelList = new ArrayList<Channel>();
                channelMap.put(clientName, channelList);
            }
            channelMap.get(clientName).add(channel);
        }
        lock.writeLock().unlock();
    }

    public Channel getChannel(String clientName) {
        if (log.isDebugEnabled()) {
            for (Map.Entry<String, List<Channel>> entry : channelMap.entrySet()) {
                log.debug("participantName={}, channelNum={}",
                        entry.getKey(),
                        entry.getValue() == null ? 0 : entry.getValue().size());
            }
        }
        lock.readLock().lock();
        try {
            List<Channel> channelList = channelMap.get(clientName);
            if (channelList == null || channelList.size() == 0) {
                log.info("no available connection for clientName={}", clientName);
                return null;
            }
            int id = index.getAndIncrement() % channelList.size();
            Channel channel = channelList.get(id);
            return channel;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeChannel(Channel channel) {
        // todo
        String participantName = "";
        if (StringUtils.isNotBlank(participantName)) {
            lock.writeLock().lock();
            List<Channel> channelList = channelMap.get(participantName);
            channelList.remove(channel);
            channelSet.remove(channel);
            lock.writeLock().unlock();
        }
    }
}