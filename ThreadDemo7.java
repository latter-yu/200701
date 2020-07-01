import javafx.concurrent.Worker;

import java.util.concurrent.PriorityBlockingQueue;

public class ThreadDemo7 {

    // 定时器: 多线程编程中的一个 重要且常用 的组件.
    // 定时器的基本构成, 有三个部分.
    // 1. 用一个类来描述 "任务"(Task 类)
    // 2. 用一个阻塞优先队列来组织若干个任务. 让队首元素就是时间最早的任务.
    //    如果队首元素时间未到, 那么其他元素也肯定不能执行.
    // 3. 用一个线程来循环扫描当前的阻塞队列的队首元素, 如果时间到, 就执行指定的任务.
    // 4. 还需要提供一个方法 (schedule), 让调用者能把任务给 "安排" 进来.

    // ps: 优先队列中的元素必须是可比较的.
    // 比较规则的指定主要是两种方式:
    // a. 让 Task 实现 Comparable 接口
    // b. 让优先队列构造的时候, 传入一个比较器对象 (Comparator).

    static class Task implements Comparable<Task>{
        // Runnable 中有一个 run 方法, 就可以借助这个 run 方法来描述要执行的具体的任务
        private Runnable command;
        // time 表示啥时候来执行 command, 是一个绝对时间(ms级别的时间戳)
        private long time;

        // 构造方法的参数表示: 多少毫秒之后执行. (相对时间 after)
        // 这个相对时间的参数是为了方便使用.
        public Task(Runnable command, long after) {
            this.command = command;
            this.time = System.currentTimeMillis() +after;
        }

        public void run() {
            // 用 run 来描述具体任务.
            command.run();
        }
        @Override
        public int compareTo(Task o) {
            // 时间小的先执行
            return (int) (this.time - o.time);
        }
    }

    static class Worker extends Thread {
        private PriorityBlockingQueue<Task> queue = null;
        private Object mailBox = null;

        public Worker(PriorityBlockingQueue<Task> queue, Object mailBox) {
            this.queue = queue;
            this.mailBox = mailBox;
        }

        @Override
        public void run() {
            // 实现具体的线程执行的内容
            while (true) {
                try {
                    // 1. 取出队首元素, 检查时间是否到了
                    Task task = queue.take();
                    // 2. 检查当前任务时间是否到了
                    long curTime = System.currentTimeMillis();
                    if (task.time > curTime) {
                        // 时间还没到, 就把任务再塞回队列中
                        queue.put(task);
                        synchronized (mailBox) {
                            mailBox.wait(task.time - curTime);
                        }
                    } else {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    static class Timer {

        // 为了避免忙等, 需要使用 wait 方法.
        // 使用一个单独的对象来辅助进行 wait
        // 使用 this 也行.
        private Object mailBox = new Object();

        // 标准库阻塞优先队列:
        private PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
        // 标准库内自带定时器, 阻塞队列(BlockingQueen)等编程.

        public Timer() {
            // 创建线程
            Worker worker = new Worker(queue, mailBox);
            worker.start();
        }

        //    schedule => 安排
        public void schedule(Runnable command, long after) {
            Task task = new Task(command, after);
            queue.put(task);
            synchronized (mailBox) {
                mailBox.notify();
            }
        }
    }

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("hehe");
                timer.schedule(this, 2000);
            }
        }, 2000);
    }
}
