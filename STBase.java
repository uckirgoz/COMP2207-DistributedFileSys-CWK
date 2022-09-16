import java.io.*;
import java.net.Socket;

public class STBase {
    private String ip = "127.0.0.1";
    private Integer cport=4322;

    private Integer timeout = 200;
    protected String file_folder = "~/dstore";

    public STBase(String ip, Integer cport, Integer timeout) {
        this(cport,timeout);
        this.ip = ip;
    }
    public STBase( Integer cport, Integer timeout) {
        this.cport = cport;
        this.timeout = timeout;
    }

    public String waitForMessage(Socket socket,Integer timeout)throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        if(timeout!=null) {
            socket.setSoTimeout(timeout);
        }
        String result=in.readLine();
        return result;
    }
    public void writeMessageToSocket(Socket socket,String message,Integer timeout)throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        if(timeout!=null){

        }
        out.println(message);
    }
    public void writeMessageToSocket(String ip, Integer port,String message,Integer timeout)throws IOException {
        Socket socket=new Socket(ip,port);
        writeMessageToSocket(socket,message,timeout);

    }
    public String sendAndReciveMessage(String ip, Integer port, String request, Integer timeout) throws IOException {
        Socket socket = new Socket(ip, port);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(request);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        if(timeout!=null) {
            socket.setSoTimeout(timeout);
        }
        String result=in.readLine();

        socket.close();
        return result;
    }
    public String sendAndReciveMessage(Socket socket, String request, Integer timeout) throws IOException {


        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(request);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        if(timeout!=null) {
            socket.setSoTimeout(timeout);
        }
        String result=in.readLine();


        return result;
    }
    public static void sendFile(Socket socket,String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());


        // send file size
        dataOutputStream.writeLong(file.length());
        // break file into chunks
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
        dataOutputStream.close();
    }
    protected void receiveFile(Socket clientSocket, String fileName, String sfileSize) throws Exception {
        int bytes = 0;
        Long fileSize = Long.valueOf(sfileSize);

        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());


        FileOutputStream fileOutputStream = new FileOutputStream(file_folder + "/" + fileName);

        long size = dataInputStream.readLong();     // read file size
        if (fileSize.longValue() != size) {
            // dataInputStream.close();
            fileOutputStream.close();
            throw new Exception(ErrorProtocols.FILE_SIZE_NOT_MACHED);
        }
        byte[] buffer = new byte[4 * 1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes;      // read upto file size
        }


        fileOutputStream.close();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getCport() {
        return cport;
    }

    public void setCport(Integer cport) {
        this.cport = cport;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
