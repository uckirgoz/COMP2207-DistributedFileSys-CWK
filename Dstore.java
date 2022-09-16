import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dstore extends STServerBase {
    private Integer port = null;

    private Socket socketToController;
    private Socket client;
    private HashMap<String, STIndex> fileIndex = new HashMap<>();

    public Dstore(Integer port, Integer cport, Integer timeout, String file_folderTmp) throws IOException, InterruptedException {
        super(cport, timeout);
        this.port = port;


        initFileFolder(file_folderTmp);
        final String file_folder = this.file_folder;
        ServerSocket ss = new ServerSocket(port);


        this.socketToController = null;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //System.out.println(Thread.currentThread());
                        Socket client = ss.accept();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                    String line;
                                    while ((line = in.readLine()) != null) {
                                        String[] ps = line.split(" ");

                                        System.out.println(" [CONTROLLER] :: " + line);
                                        if (ps.length > 0) {
                                            switch (ps[0]) {
                                                case STClientProtocol.LIST:
                                                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                                                    out.println(listFiles());
                                                    //out.close();
                                                    break;


                                                case STClientProtocol.ACK:
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    out.println("OK");


                                                    //out.close();
                                                    break;
                                                case STClientProtocol.REBALANCE_STORE:
                                                case STClientProtocol.STORE:
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    if (ps.length != 3) {
                                                        out.println(ErrorProtocols.MALFORMED);
                                                    } else {

                                                        out.println(STClientProtocol.ACK);
                                                        try {

                                                            receiveFile(client, ps[1], ps[2]);
                                                            fileRecieved(ps[1], ps[0]);
                                                        } catch (Exception e) {
                                                            out.println(e.getMessage());
                                                        }


                                                    }

                                                    //out.close();
                                                    break;
                                                case STClientProtocol.REBALANCE:
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    System.out.println("------- " + ps.length);
                                                    if (ps.length < 6) {
                                                        out.println(ErrorProtocols.MALFORMED);
                                                    } else {
                                                        int index = 1;

                                                        Integer fileCount = Integer.valueOf(ps[index]);
                                                        System.out.println(fileCount);


                                                        Map<Integer, STIndex> stIndexMap = new HashMap<>();

                                                        if (fileCount > 0) {


                                                            for (int z = 0; z < fileCount; z++) {
                                                                index++;
                                                                String fileToSent = ps[index];
                                                                System.out.println(fileToSent);
                                                                STIndex stIndex = findSTIndex(fileToSent);
                                                                System.out.println(stIndex);


                                                                index++;
                                                                Integer portCount = Integer.valueOf(ps[index]);
                                                                System.out.println(portCount);

                                                                if (portCount > 0) {
                                                                    for (int j = 0; j < portCount; j++) {
                                                                        index++;
                                                                        Integer port = Integer.valueOf(ps[index]);
                                                                        System.out.println(port);
                                                                        stIndexMap.put(port, stIndex);
                                                                    }
                                                                } else {
                                                                    index += 2;
                                                                }


                                                            }
                                                        } else {
                                                            index += 2;
                                                        }


                                                        List<String> filesToRemove = new ArrayList<>();
                                                        index++;
                                                        System.out.println(index);
                                                        Integer filesToRemoveCount = Integer.valueOf(ps[index]);

                                                        if (filesToRemoveCount > 0) {
                                                            index++;
                                                            for (int j = 0; j < filesToRemoveCount; j++) {
                                                                System.out.println(ps[index]);
                                                                filesToRemove.add(ps[index]);
                                                                index++;
                                                            }
                                                        }


                                                        if (filesToRemove.size() > 0) {
                                                            for (String fileName : filesToRemove) {
                                                                removeFromIndex(fileName);
                                                            }

                                                        }


                                                        for (Integer key : stIndexMap.keySet()) {

                                                            STIndex stIndex = stIndexMap.get(key);
                                                            System.out.println(key + " - " + stIndex);
                                                            Socket socket = new Socket(getIp(), key);
                                                            String ack = sendAndReciveMessage(socket, STClientProtocol.REBALANCE_STORE + " " + stIndex.getFileName() + " " + stIndex.getFileSize(), getTimeout());
                                                            if (ack != null && ack.equals(STClientProtocol.ACK)) {
                                                                System.out.println(file_folder);
                                                                sendFile(socket, file_folder + stIndex.getFileName());
                                                            }

                                                        }
                                                        out.println(STClientProtocol.ACK);
                                                    }
                                                    break;

                                                case STClientProtocol.FIND_FILE:
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    if (ps.length < 2) {
                                                        out.println(ErrorProtocols.MALFORMED);
                                                    } else {
                                                        STIndex stIndex = findSTIndex(ps[1]);
                                                        if (stIndex != null && !stIndex.isLocked()) {
                                                            out.println(STClientProtocol.ACK);

                                                        } else {
                                                            out.println(ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);
                                                        }
                                                    }
                                                    //out.close();
                                                    break;
                                                case STClientProtocol.LOAD_DATA:
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    if (ps.length < 2) {
                                                        out.println(ErrorProtocols.MALFORMED);
                                                    } else {
                                                        try {
                                                            sendDataToClient(client, ps[1]);
                                                        } catch (Exception e) {
                                                            out.println(e.getMessage());
                                                        }
                                                    }


                                                    //out.close();
                                                    break;
                                                case STClientProtocol.REMOVE:
                                                    removeFile(client, ps[1]);
                                                    break;

                                            }
                                        }
                                        System.out.println(Thread.currentThread() + "--" + Thread.currentThread().getName() + " --" + line + " received");
                                    }
                                    // Thread.currentThread().wait(9000);


                                } catch (Exception e) {
                                    System.err.println("error: " + e);
                                }
                            }
                        }).start();
                    } catch (Exception e) {

                    }


                }
            }
        }).start();
        Thread.sleep(1000);
        int retry = 3;
        for (int i = 0; i < retry; i++) {
            try {
                this.socketToController=new Socket(getIp(), cport);
                PrintWriter out = new PrintWriter(this.socketToController.getOutputStream(), true);
                out.println("JOIN " + port);

                BufferedReader in = new BufferedReader(new InputStreamReader(socketToController.getInputStream()));
                // socket.setSoTimeout(timeout);
                String result = in.readLine();
                System.out.println(result);
                break;
            } catch (Exception e) {
                System.out.println("-----"+i);
            }

        }

    }

    public void sendDataToClient(Socket client, String fileName) throws Exception {
        File file = new File(this.file_folder + fileName);
        if (!file.isFile()) {
            throw new Exception(ErrorProtocols.NOT_FILE);

        }
        sendFile(client, this.file_folder + fileName);
    }

    public void fileRecieved(String fileName, String message) throws IOException {

        writeMessageToSocket(socketToController, (message.equals(STClientProtocol.STORE) ? STClientProtocol.STORE_ACK : STClientProtocol.REBALANCE_COMPLETE) + " " + fileName, getTimeout());
    }

    public void fileRecieved2(String fileName) throws IOException {
        writeMessageToSocket(getIp(), getCport(), STClientProtocol.STORE_ACK + " " + fileName, getTimeout());
    }

    private void initFileFolder(String file_folder_tmp) {
        /***arg dan full path girilirse bu kommentli**/

        // String userHomeDir = System.getProperty("user.home");
        // String userDirectory = System.getProperty("user.dir");
        //System.out.println(userDirectory);
        //this.file_folder = userDirectory + "/dstore" + file_folder_tmp;
        /***/
        /** değilse aşşağıdaki comment yukarıyı kaldır**/
        this.file_folder = file_folder_tmp;
        ///

        File folder = new File(this.file_folder);
        System.out.println(folder);
        if (!folder.isDirectory()) {

            folder.mkdirs();
        } else {

            for (File f : folder.listFiles())
                f.delete();


        }
    }

    private synchronized void removeFile(Socket client, String fileName) throws Exception {
        STIndex stIndex = findSTIndex(fileName);
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        if (stIndex == null) {

            out.println("ERROR " + ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);
            return;
        }
        if (stIndex.isLocked()) {
            out.println("ERROR " + stIndex.getStoreOperationEnum().description);
            return;
        }
        getStIndices().remove(stIndex);
        //yeni eklediğim fileden silme kısmı
        removeFromFileSystem(fileName);
        // end
        out.println(STClientProtocol.ACK);

    }

    public void removeFromFileSystem(String fileName) throws Exception {
        System.out.println(this.file_folder + fileName);
        File file = new File(this.file_folder + fileName);
        if (!file.isFile()) {
            throw new Exception(ErrorProtocols.NOT_FILE);

        }
        file.delete();


    }

    // @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                String[] ps = line.split(" ");

                System.out.println(" [CONTROLLER] :: " + line);
                if (ps.length > 0) {
                    switch (ps[0]) {
                        case STClientProtocol.LIST:
                            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                            out.println(listFiles());
                            //out.close();
                            break;

                    }
                }
                System.out.println(Thread.currentThread() + "--" + Thread.currentThread().getName() + " --" + line + " received");
            }
            // Thread.currentThread().wait(9000);
            client.close();

        } catch (Exception e) {
            System.err.println("error: " + e);
        }
    }

    protected void receiveFile(Socket clientSocket, String fileName, String sfileSize) throws Exception {


        int bytes = 0;
        Long fileSize = Long.valueOf(sfileSize);

        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());


        FileOutputStream fileOutputStream = new FileOutputStream(file_folder + "/" + fileName);

        long size = dataInputStream.readLong();     // read file size
        if (fileSize.longValue() != size) {
            System.out.println(fileSize.longValue() + "    " + size);
            // dataInputStream.close();
            fileOutputStream.close();
            throw new Exception(ErrorProtocols.FILE_SIZE_NOT_MACHED);
        }
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;      // read upto file size
        }

        STIndex stIndex = addFileToIndex(fileName, fileSize, null, null);
        stIndex.setLocked(false, STStoreOperationEnum.STORE_COMPLETE);
        fileOutputStream.close();
    }

    public static void main(String[] args) {
        ServerSocket ss = null;
        //java -cp  Dstore 4324 4322 200 /Users/serdartarin/dstore/p4343p/
        /*for(String a:args){
            System.out.println(a);
        }
        if(true){
            return;
        }

         */
        Integer port = Integer.valueOf(args[0]);// 4324;
        Integer cport = Integer.valueOf(args[1]);//4322;
        Integer timeout = Integer.valueOf(args[2]);// 200;
        String path = args[3];
        // System.out.println(port +" "+ cport+" "+timeout+" "+path);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Dstore(port, cport, timeout, path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
      /*  new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new Dstore(4324, cport, timeout, "/p" + 4324 + "/");
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
                    new Dstore(4325, cport, timeout, "/p" + 4325 + "/");
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
                    new Dstore(4326, cport, timeout, "/p" + 4326 + "/");
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
