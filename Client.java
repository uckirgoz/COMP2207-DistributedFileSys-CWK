import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client extends STClientBase{


    public Client(Integer cport, Integer timeout){
        super(cport,timeout);
        initFileFolder("client_folder");
    }
    private void initFileFolder(String file_folder) {
        String userHomeDir = System.getProperty("user.home");
        this.file_folder = userHomeDir + "/dstore" + file_folder;
        File folder = new File(this.file_folder);
      //  System.out.println(folder);
        if (!folder.isDirectory()) {

            folder.mkdirs();
        } else {

            for (File f : folder.listFiles())
                f.delete();


        }
    }
    public void listDstoreFiles(String request) throws IOException {
       // InetAddress address = InetAddress.getLocalHost();
        String result=sendAndReciveMessage(getIp(), getCport(),request, getTimeout());
        System.out.println(result);
    }
    public void storeFileToCcontroller(String request) throws Exception {

        String[] strings=request.split(" ");
        if(strings.length!=3){
            throw new Exception(ErrorProtocols.MALFORMED);
        }
        File file = new File(strings[1]);
        if(!file.isFile()){
            throw new Exception(ErrorProtocols.NOT_FILE);

        }

       // FileInputStream fileInputStream = new FileInputStream(file);
        String command=strings[0];
        String filenNameWithPath=strings[1];
        String filenName=filenNameWithPath.substring(filenNameWithPath.lastIndexOf("/")+1);

        String newRequest=command+" "+file.getName()+" "+file.length();

       // String result=sendAndReciveMessage(getIp(), getCport(),newRequest, getTimeout());
        Socket socketToController=new Socket(getIp(),getCport());
        String result=sendAndReciveMessage(socketToController,newRequest, getTimeout());
        System.out.println(result);
        if(result!=null) {
            String[] ws = result.split(" ");
            if (ws.length > 1) {
                switch (ws[0]) {
                    case STClientProtocol.STORE_TO:
                        for(int i=1;i<ws.length;i++) {
                            storeFileToDstore( ws[i], filenNameWithPath, newRequest);
                        }
                        String controlerMessage=waitForMessage(socketToController,3000);
                        System.out.println(controlerMessage);


                        break;
                }

            }
        }


        socketToController.close();
       // requestFromDstore(result,newRequest);


    }
    public void loadFileFromCcontroller(String request) throws Exception {

        String[] strings=request.split(" ");
        if(strings.length<2){
            throw new Exception(ErrorProtocols.MALFORMED);
        }


        // FileInputStream fileInputStream = new FileInputStream(file);


        // String result=sendAndReciveMessage(getIp(), getCport(),newRequest, getTimeout());
        Socket socketToController=new Socket(getIp(),getCport());
        String result=sendAndReciveMessage(socketToController,request, getTimeout());
        System.out.println(result);
        if(result!=null) {
            String[] ws = result.split(" ");
            if (ws.length > 1) {
                switch (ws[0]) {
                    case STClientProtocol.LOAD_FROM  :
                        Socket socket=new Socket(getIp(),Integer.valueOf(ws[1]));

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(STClientProtocol.LOAD_DATA+" "+strings[1]);
                        //String akn=sendAndReciveMessage(socket,"LOAD_DATA "+strings[1],getTimeout());
                        receiveFile(socket,strings[1],ws[2]);

                        //String controlerMessage=waitForMessage(socketToController,3000);
                       // System.out.println(controlerMessage);


                        break;
                }

            }
        }


        socketToController.close();
        // requestFromDstore(result,newRequest);


    }
    public void requestFromDstore(String result,String request) throws Exception {
        String[] ws = result.split(" ");
        if (ws.length > 1) {
            switch (ws[0]) {
                case STClientProtocol.LIST:
                    listDstoreFiles(request);
                    break;
                case STClientProtocol.STORE:
                    if(ws.length!=3){
                        throw new Exception("Error  : STORE file_name file size");
                    }
                    storeFileToCcontroller(request);
                    break;
                case STClientProtocol.STORE_TO:
                    //storeFileToDstore(ws[1],request);
                    break;
                case STClientProtocol.LOAD:
                    loadFileFromCcontroller(request);
                    break;
                case STClientProtocol.REMOVE:
                    // remove(msg);
                    break;
                default:
                    System.out.println("(!) Unrecognized command");
                    break;
            }
        } else {
            // log ?
            System.out.println("(!) Unrecognized command");
        }
    }
    public void storeFileToDstore(String sport,String fileNameWithPath, String request) throws Exception {
        Integer dport=Integer.parseInt(sport);
        Socket socket = new Socket(getIp(), dport);
        String result=sendAndReciveMessage(socket,request, getTimeout());
        System.out.println(result);

        if(result!=null && result.equals(STClientProtocol.ACK)){
            String[] strings=request.split(" ");
            if(strings.length!=3){
                socket.close();
                throw new Exception(ErrorProtocols.MALFORMED);
            }

            sendFile(socket,fileNameWithPath);


        }
        socket.close();



    }
    public void requestFromController(String request) throws Exception {
        String[] ws = request.split(" ");
        if (ws.length > 0) {
            switch (ws[0]) {
                case STClientProtocol.LIST:
                    listDstoreFiles(request);
                    break;
                case STClientProtocol.STORE:
                    storeFileToCcontroller(request);
                    break;

                case STClientProtocol.LOAD:
                    loadFileFromCcontroller(request);
                    break;
                case STClientProtocol.REMOVE:
                    removeFileFromController(request);
                    break;
                default:
                    System.out.println("(!) Unrecognized command");
                    break;
            }
        } else {
            // log ?
            System.out.println("(!) Unrecognized command");
        }
    }

    public void removeFileFromController(String request) throws Exception {

        String[] strings=request.split(" ");
        if(strings.length<2){
            throw new Exception(ErrorProtocols.MALFORMED);
        }


        // FileInputStream fileInputStream = new FileInputStream(file);


        // String result=sendAndReciveMessage(getIp(), getCport(),newRequest, getTimeout());
        Socket socketToController=new Socket(getIp(),getCport());
        String result=sendAndReciveMessage(socketToController,request, getTimeout());
        System.out.println(result);




        socketToController.close();
        // requestFromDstore(result,newRequest);


    }

    public static void main(String[] args) {
        /*if (args.length != 2) {
            System.out.println("Error: invalid amount of command line arguments provided");
            return;
        }

         */
        System.out.println(args);

        Integer cport=Integer.valueOf(args[0]);// 4322;
        Integer timeout=Integer.valueOf(args[1]);//200;

       /* try {
            cport = Integer.parseInt(args[0]);
            timeout = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: unable to parse command line arguments");
            return;
        }

        */

        Client client;
        try {
            client = new Client(cport, timeout);
        } catch (Exception e) {
            System.out.println("Error: connection failed");
            return;
        }

        Scanner input = new Scanner(System.in);
        String line;
        while (!(line = input.nextLine()).equals("QUIT")) {
            try {
                client.requestFromController(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
