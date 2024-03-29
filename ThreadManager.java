package com.kcl.osc.imageprocessor;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadManager implements Runnable {

    private int poolSize;
    private ArrayList<ImageProcessorMT>waitingList;
    private ArrayList<Pair<ImageProcessorMT,Thread>> pool;
    private Lock lock;
    private boolean started;
    private boolean finishedJoined;
    /**
     * Constructor to initialise the thread pool.
     * @param poolSize the size of the pool
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
     * @param imageProcessorMT the processor for the image
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
            int sizeToAllow = Math.min(waitingList.size(), poolSize); // This controls the current size the pool should have.

            // Add threads from the waiting list to the pool
            for (int i = 0; i < sizeToAllow; i++) {
                pool.add(new Pair(waitingList.get(i),new Thread(waitingList.get(i))));
            }

            // Popping from the waiting list
            Iterator<ImageProcessorMT> iterator = waitingList.iterator();
            for (int i = 0; i < sizeToAllow; i++) {
                iterator.next();
                iterator.remove();
            }

            // Starts all the threads
            pool.forEach(elem -> elem.getValue().start());
        }
        finally {
            lock.unlock();
        }
        while(!pool.isEmpty() || !waitingList.isEmpty()) {
            managePool();
        }
        started = false;
    }

    /**
     * This method checks the waiting list for processes
     * that are waiting and checks the thread pool for the processes
     * which are vacant. When it finds a vacancy in the thread pool,
     * it brings a thread from the waiting list and starts it.
     */
    private void managePool() {
        if(!waitingList.isEmpty()) {
            if(pool.size() == poolSize) {
                for (int i = 0; i < pool.size(); i++) {
                    if (pool.get(i).getKey().hasFinished()) {
                        exchangeThread(i);
                    }
                }
            }
            if(pool.size() < poolSize) {
                addThreadFromWaitingList();
            }
        }
        if(!pool.isEmpty()) {
            pool.removeIf(elem -> elem.getKey().hasFinished());
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
            pool.add(indexToReplace, new Pair(waitingList.get(0),new Thread(waitingList.get(0))));// Takes the element at the start of the queue and adds a pair of its thread and the image processor itself
            waitingList.remove(0); // Removes that element
            pool.get(indexToReplace).getValue().start(); // Start the threads
        }
        finally {
            lock.unlock();
        }
    }

    public void setJoined(boolean joined) {
        this.finishedJoined = joined;
    }
    public boolean hasJoined() {
        return this.finishedJoined;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean hasStarted() {
        return this.started;
    }

    /**
     * This method takes the first thread in the waiting list and adds it to the thread pool and starts the thread
     * This method should be only called when there is available space in the thread pool
     */
    private void addThreadFromWaitingList() {
        try {
            lock.lock();
            pool.add(new Pair(waitingList.get(0),new Thread(waitingList.get(0))));
            pool.get(pool.size() - 1).getValue().start();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This method allows the caller to wait for the thread pool to exist.
     */
    public synchronized void join() {
        this.setJoined(false);
        while(!hasJoined()) {
            if(hasStarted() && pool.size() == 0 && waitingList.size() == 0) {
                setJoined(true); // This would set the finishJoined to true and the loop would exit
            }
            for(int i = 0; i < pool.size(); i++) {
                if (i < pool.size()) { // This condition ensures that if the threads are popped off then it will not access it
                    try {
                        pool.get(i).getValue().join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * This method sets the thread pool start flag to true
     */
    public void start() {
        setStarted(true);
    }
}
