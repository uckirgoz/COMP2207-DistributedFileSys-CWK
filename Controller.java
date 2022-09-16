import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Controller extends STServerBase{

    private List< STDstoreInfo> dstoreList = new ArrayList<>();
    private Integer R;
    private Integer rebalance_period=-1;
    private int dstoreIndex=0;



    public Controller(Integer cport, Integer R, Integer timout, Integer rebalance_period) {
        super(cport,timout);
        this.R=R;
        // client = c;
        TimerTask task = new TimerTask() {
            public void run() {
                System.out.println("Auto Rebalance: " + new Date() + "n" +
                        "Thread's name: " + Thread.currentThread().getName());
                try {
                    healtCheckForDstoreConnection();
                    reblalance();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Timer timer = new Timer("Timer");

        long delay = rebalance_period;
        timer.scheduleAtFixedRate(task,0, delay);

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(cport);
            int i = 0;

            while (true) {
                try {
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
                                                if(dstoreList.size()<R){

                                                    out.println(ErrorProtocols.ERROR_NOT_ENOUGH_DSTORES);
                                                    continue;
                                                }
                                                out.println(listFiles());
                                                //out.close();
                                                break;
                                            case STClientProtocol.STORE:
                                                if(dstoreList.size()<R){
                                                     out = new PrintWriter(client.getOutputStream(), true);
                                                    out.println(ErrorProtocols.ERROR_NOT_ENOUGH_DSTORES);
                                                    continue;
                                                }
                                                addFileToDstore(client, ps);

                                                break;
                                            case STClientProtocol.LOAD:
                                                if(dstoreList.size()<R){
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    out.println(ErrorProtocols.ERROR_NOT_ENOUGH_DSTORES);
                                                    continue;
                                                }
                                                loadFileFromDstore(client, ps);

                                                break;
                                            case STClientProtocol.REMOVE:
                                                if(dstoreList.size()<R){
                                                    out = new PrintWriter(client.getOutputStream(), true);
                                                    out.println(ErrorProtocols.ERROR_NOT_ENOUGH_DSTORES);
                                                    continue;
                                                }
                                                removeFileToDstore(client, ps);

                                                break;
                                            case STClientProtocol.STORE_ACK:
                                                if(ps.length<2){
                                                    throw new Exception(ErrorProtocols.MALFORMED);
                                                }
                                                changeFileIndexUnlocked( client,ps[1]);

                                                break;
                                            case STClientProtocol.JOIN:

                                                addDstore(client.getInetAddress().getHostAddress(), ps[1], client);

                                                break;

                                        }
                                    }
                                    System.out.println(Thread.currentThread() + "--" + Thread.currentThread().getName() + " --" + line + " received");
                                }
                                client.close();

                            } catch (Exception e) {
                                System.err.println("error: " + e);
                            }
                        }
                    }).start();
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

    }


    private STDstoreInfo getNextDstore(){
        int dstoreSize=dstoreList.size();
        dstoreIndex = (dstoreIndex) % dstoreSize;
        return dstoreList.get(dstoreIndex);
    }
    private synchronized String getAllDstorePorts(){
        String ports="";
        for(STDstoreInfo stDstoreInfo:dstoreList){
            ports+=" "+stDstoreInfo.getPort();
        }

        return ports;
    }
    private void loadFileFromDstore(Socket client,String[] ps) throws Exception {
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        if (ps.length < 2) {

            out.println(ErrorProtocols.MALFORMED);
            return;
        }
        STIndex stIndex=findSTIndex(ps[1]);
        if(stIndex==null){
            out.println(ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);
            return;
        }
        for(STDstoreInfo stDstoreInfo:dstoreList){
            Socket socket=new Socket(stDstoreInfo.getIp(),stDstoreInfo.getPort());
            String result=sendAndReciveMessage(socket,STClientProtocol.FIND_FILE+" "+ps[1],getTimeout());
            if(result!=null && result.equals(STClientProtocol.ACK)){
                out.println(STClientProtocol.LOAD_FROM+" "+stDstoreInfo.getPort()+" "+stIndex.getFileSize());
                break;
            }
        }

        out.println(ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);

    }
    private void addFileToDstore(Socket client, String[] ps) throws Exception {
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        if (ps.length < 3) {

            out.println("Malformed message received by Client");
            return;
        }
        Long fileSize = null;
        try {


            fileSize = Long.valueOf(ps[2]);
            STIndex stIndex=addFileToIndex(ps[1], fileSize,client,new ArrayList<>(dstoreList));

            //STDstoreInfo dstoreInfo=getNextDstore();
            String ports=getAllDstorePorts();
            out.println(STClientProtocol.STORE_TO+ports);

            String[] messageList= new String[dstoreList.size()];

            stIndex.setLocked(false,STStoreOperationEnum.STORE_COMPLETE);
        } catch (NumberFormatException ne) {
            out.println("File Size is not numeric!");
            return;
        }catch (Exception e){
            out.println(e.getMessage());
            return;
        }


    }
    private synchronized void removeFileToDstore(Socket client, String[] ps) throws Exception {
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        if (ps.length < 2) {

            out.println("Malformed message received by Client");
            return;
        }

        try {



            STIndex stIndex=findSTIndex(ps[1]);
            if(stIndex==null || stIndex.isLocked()){
                out.println(ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);
                return;
            }
            stIndex.setLocked(true,STStoreOperationEnum.REMOVE_IN_PROGRES);
            for(STDstoreInfo stDstore:dstoreList){
                Socket socket=new Socket(stDstore.getIp(),stDstore.getPort());
                String akn=sendAndReciveMessage(socket,STClientProtocol.REMOVE+" "+ps[1],getTimeout());
                if(akn!=null && akn.equals(STClientProtocol.ACK)){

                }
            }

            getStIndices().remove(stIndex);
            out.println(STClientProtocol.REMOVE_COMPLETE);

            //STDstoreInfo dstoreInfo=getNextDstore();


        } catch (Exception e){
            out.println(e.getMessage());
            return;
        }


    }


    protected synchronized STDstoreInfo findSTDstoreInfo(Integer key){
        return dstoreList.stream().filter(d->d.getPort().equals(key)).findAny().orElse(null);
    }



    private synchronized void addDstore(String ip, String sport, Socket dstore) throws Exception {


        Integer port = Integer.valueOf(sport);


        STDstoreInfo stDstoreInfo = findSTDstoreInfo(port);
        if (stDstoreInfo != null) {
            if (checkConneciton(ip, port)) {
                throw new Exception("Already Connected");
            }
            ;

        } else {
            STDstoreInfo stDstoreInfo1=findSTDstoreInfo(port);
            if(stDstoreInfo1==null) {

                dstoreList.add(new STDstoreInfo(ip, port, dstore));
            }else{
                stDstoreInfo1.setClient(dstore);
            }

        }
        Socket socket = new Socket(ip, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("LIST");
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socket.setSoTimeout(getTimeout());
        String result = in.readLine();
        System.out.println(result);
        reblalance();
    }

    public boolean checkConneciton(String ip, Integer port) throws IOException {
        Socket socket = new Socket(ip, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(STClientProtocol.ACK);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socket.setSoTimeout(getTimeout());
        String result = in.readLine();
        if (result != null && result.equals(STClientProtocol.OK)) {
            return true;
        }
        return false;
    }

    public synchronized void healtCheckForDstoreConnection(){
        List<STDstoreInfo> disconnectedDstore=new ArrayList<>();
        for(STDstoreInfo stDstoreInfo:dstoreList){
            try {
               if( !checkConneciton(stDstoreInfo.getIp(),stDstoreInfo.getPort())){
                   disconnectedDstore.add(stDstoreInfo);
               }
            } catch (IOException e) {
                disconnectedDstore.add(stDstoreInfo);
            }
        }
        if(disconnectedDstore.size()>0){
            dstoreList.removeAll(disconnectedDstore);
        }
    }


    public synchronized void reblalance() throws IOException {
        Map<Integer,List<String>> fileListAtDstore=new HashMap<Integer, List<String>>();
        Map<Integer,List<String>> filesToRemove=new HashMap<>();
        List<STDstoreInfo> disconnectedDstore=new ArrayList<>();
        getStIndices().stream().forEach(s->s.setTempPort(null));
        for(STDstoreInfo stDstoreInfo:dstoreList){
            int retry=2;
            int icount=0;
           // System.out.println(stDstoreInfo);
            do {
                try {
                    Socket socket = new Socket(stDstoreInfo.getIp(), stDstoreInfo.getPort());
                    String result = sendAndReciveMessage(socket, STClientProtocol.LIST, getTimeout());

                    String[] fileList=result.split(" ");
                    List<String> files=new ArrayList<>();
                    List<String> filesTobeRemove=new ArrayList<>();
                    for(int i=1;i<fileList.length;i++){
                        files.add(fileList[i]);

                        STIndex stIndex=findSTIndex(fileList[i]);
                        if(stIndex!=null && stIndex.getTempPort()==null){
                            stIndex.setTempPort(stDstoreInfo.getPort());
                        }else{
                            filesTobeRemove.add(fileList[i]);
                        }
                    }
                    fileListAtDstore.put(stDstoreInfo.getPort(),files);
                    filesToRemove.put(stDstoreInfo.getPort(),filesTobeRemove);
                    break;
                } catch (IOException e) {
                    stDstoreInfo.getPort();
                    icount++;
                }
            }while (icount<retry);
            if(icount==retry){
                disconnectedDstore.add(stDstoreInfo);
            }
        }

        if(disconnectedDstore.size()>0){
            dstoreList.removeAll(disconnectedDstore);
        }
        Map<Integer,STRebalanceInfo> stRebalanceInfos=new HashMap<>();
        for(STIndex stIndex:getStIndices()){



            if(stIndex.getTempPort()==null){
                continue;
            }

            STRebalanceInfo stRebalanceInfo=stRebalanceInfos.get(stIndex.getTempPort());
            if(stRebalanceInfo==null){
                stRebalanceInfo = new STRebalanceInfo(stIndex.getTempPort());
                stRebalanceInfos.put(stIndex.getTempPort(),stRebalanceInfo);
            }
            if(!stIndex.isLocked()){


                for(Integer key:fileListAtDstore.keySet()){
                    Integer port=findPortThatFileNotIn(stIndex.getFileName(),fileListAtDstore.get(key),key);
                    if(port!=null){
                        stRebalanceInfo.addFileName(stIndex.getFileName(),port);
                    }
                }


            }
        }
        for(Integer key:stRebalanceInfos.keySet()){
            STRebalanceInfo stRebalanceInfo=stRebalanceInfos.get(key);
            if(stRebalanceInfo.getFileNames().size()>0 || stRebalanceInfo.getFilesToBeRemoved().size()>0){
                Socket socket=new Socket(getIp(), stRebalanceInfo.getPort());
                String ack=sendAndReciveMessage(socket,STClientProtocol.REBALANCE+" "
                        +stRebalanceInfo.getFileNames().size() +stRebalanceInfo.createFileToSent()
                        +" "+stRebalanceInfo.getFilesToBeRemoved().size()+stRebalanceInfo.createDFilesToBeRemovedFromSet(),getTimeout());
                System.out.println(ack);

            }
        }

    }

    private Integer findPortThatFileNotIn(String fileName, List<String> list,Integer port){
        String result=list.stream().filter(s->s.equals(fileName)).findAny().orElse(null);
        return result==null ? port:null;
    }




    public static void main(String[] args) {
       // int cport= Integer.valueOf(args[1]);

        Integer cport = Integer.valueOf(args[0]);//4322;
        Integer R = Integer.valueOf(args[1]);//4322;
        Integer timeout = Integer.valueOf(args[2]);//4322;
        Integer rebalance_periot =Integer.valueOf(args[3]);// 200;
        new Controller(cport,R,timeout,rebalance_periot);

    }
}
