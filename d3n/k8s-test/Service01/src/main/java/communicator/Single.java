package communicator;

public class Single {
    public static int i=1;


    synchronized public static void increment(int value){
        System.out.println("old value = " + i);
        i+=value;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("new value = " + i);
    }

}
