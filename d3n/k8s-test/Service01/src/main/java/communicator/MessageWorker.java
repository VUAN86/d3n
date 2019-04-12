package communicator;


public class MessageWorker extends Thread {
    private String message;
    MessageWorkerImpl mwi;

    public MessageWorker(String message, MessageWorkerImpl mwi) {
        this.message = message;
        this.mwi = mwi;
    }

    @Override
    public void run() {
            System.out.println("thread"+" started");
        Single.increment(2);
//            mwi.onMessage(message);
            System.out.println("thread"+" stoped");

        super.run();
    }
}
