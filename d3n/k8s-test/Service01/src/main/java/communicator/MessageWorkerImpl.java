package communicator;

public class MessageWorkerImpl extends Thread{
    public void onMessage(String originalMessage) {
        try {
            sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
