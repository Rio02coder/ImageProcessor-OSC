package com.kcl.osc.imageprocessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadManager implements Runnable {

    private int poolSize;
    private ArrayList<ImageProcessorMT>waitingList;
    private ArrayList<ImageProcessorMT> pool;
    private Lock lock;
    private Thread thread;

    /**
     * Constructor to initialise the thread pool.
     * @param poolSize
     */
    public ThreadManager(int poolSize) {
        this.poolSize = poolSize;
        waitingList = new ArrayList<>();
        pool = new ArrayList<>();
        lock = new ReentrantLock();
    }

    /**
     * This method allows the caller to add an image processor thread.
     * This passed thread is first added to the waiting list and then the thread pool manages the thread.
     * @param imageProcessorMT
     */
    public void addImageProcessor(ImageProcessorMT imageProcessorMT) {
        try {
            lock.lock();
            waitingList.add(imageProcessorMT);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This thread initially adds all the threads from the waiting list to the pool
     * Then it keeps waiting until the currently running threads finish or a new thread is added in the waiting list.
     * On adding to the waiting list, it would manage that thread and put it in the pool if the pool has space.
     * This method would terminate if the waiting list is empty and the pool is empty.
     */
    public void run() {
        try {
            lock.lock();
            int sizeToAllow = Math.min(waitingList.size(), poolSize);
            for (int i = 0; i < sizeToAllow; i++) {
                pool.add(waitingList.get(i));
            }
            // Popping from the waiting list

            Iterator<ImageProcessorMT> iterator = waitingList.iterator();
            for (int i = 0; i < sizeToAllow; i++) {
                iterator.next();
                iterator.remove();
            }

            // Starting the threads
            for (ImageProcessorMT thread : pool) {
                thread.start();
            }
        }
        finally {
            lock.unlock();
        }
        while(!pool.isEmpty() || !waitingList.isEmpty()) {
            managePool();
        }
    }

    private void managePool() {
        if(!waitingList.isEmpty()) {
            if(pool.size() == poolSize) {
                //int counter = 0;
                for (int i = 0; i < pool.size(); i++) {
                    if (pool.get(i).hasFinished()) {
                        exchangeThread(i);
                    }
                }
//                for (ImageProcessorMT imageProcessorMT:
//                     pool) {
//                    if(imageProcessorMT.hasFinished()) {
//                        exchangeThread(counter);
//                    }
//                    counter ++;
//                }
            }
            if(pool.size() < poolSize) {
                addThreadFromWaitingList();
            }
        }
        if(!pool.isEmpty()) {
            pool.removeIf(ImageProcessorMT::hasFinished);
        }
    }

    /**
     * This method exchanges the thread in the thread pool at a given index with the thread at
     * the start of the waiting list and starts the thread
     * @param indexToReplace
     */
    private void exchangeThread(int indexToReplace) {
        try {
            lock.lock();
            pool.remove(indexToReplace);
            pool.add(indexToReplace, waitingList.get(0));// Takes the element at the start of the queue
            waitingList.remove(0); // Removes that element
            pool.get(indexToReplace).start(); // Start the thread
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This method takes the first thread in the waiting list and adds it to the thread pool and starts the thread
     * This method should be only called when there is available space in the thread pool
     */
    private void addThreadFromWaitingList() {
        try {
            lock.lock();
            pool.add(waitingList.get(0));
            waitingList.remove(0);
            pool.get(pool.size() - 1).start();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This method allows the caller to wait for the thread pool to exist.
     */
    public void join() {
        try {
            thread.join();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * This method creates a new thread and starts the thread pool
     */
    public void start() {
        thread = new Thread(this);
        thread.start();
    }
}
