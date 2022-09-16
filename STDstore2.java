import java.io.IOException;
import java.net.ServerSocket;

public class STDstore2 {
    public static void main(String[] args) {
        ServerSocket ss = null;

        Integer port = 4324;
        Integer cport = 4322;
        Integer timeout = 200;


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Dstore(4333, cport, timeout, "/p" + 4333 + "/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Dstore(4334, cport, timeout, "/p" + 4334 + "/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new STDstore(4335, cport, timeout, "/p" + 4335 + "/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new STDstore(4336, cport, timeout, "/p" + 4336 + "/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

       */

       /* try {
            ss = new ServerSocket(4323);
            int i=0;
            while (true) {
                try {
                    //System.out.println(Thread.currentThread());
                    Socket client = ss.accept();
                    //new Thread(new ServiceThread(client),"St-"+i).start();
                    new Thread(new STDstore(4323,4322,200,"/p4323")).start();
                    i++;
                } catch (Exception e) {
                    System.err.println("error: " + e);
                }
            }
        } catch (Exception e) {
            System.err.println("error: " + e);
        } finally {
            if (ss != null)
                try {
                    ss.close();
                } catch (IOException e) {
                    System.err.println("error: " + e);
                }
        }

        */
    }
}
