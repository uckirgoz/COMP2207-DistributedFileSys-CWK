import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class STServerBase extends STBase{
    private List< STIndex> stIndices = new ArrayList<>();


    public STServerBase( Integer cport,Integer timeout) {
        super(cport,timeout);

    }

    protected synchronized boolean containsFileInIndex(String fileName){
        return stIndices.stream().filter(d->d.getFileName().equals(fileName)).findAny().isPresent();
    }
    protected synchronized STIndex findSTIndex(String fileName){

        return stIndices.stream().filter(d->d.getFileName().equals(fileName)).findAny().orElse(null);
    }
    protected synchronized STIndex addFileToIndex(String fileName, Long fileSize, Socket client, List<STDstoreInfo> stDstoreInfos) throws Exception {

        if (containsFileInIndex(fileName)) {
            throw new Exception(ErrorProtocols.ERROR_FILE_ALREADY_EXISTS);
        }

        STIndex stIndex=new STIndex(fileName, fileSize, client,stDstoreInfos,true,STStoreOperationEnum.STORE_IN_PROGRES);
        stIndices.add( stIndex);
        return stIndex;
    }
    protected synchronized boolean removeFromIndex(String fileName){
        STIndex stIndex=findSTIndex(fileName);
        if(stIndex!=null){
            stIndices.remove(stIndex);
            return true;
        }
        return false;
    }

    protected synchronized boolean changeFileIndexUnlocked(Socket client,String fileName)throws Exception {
        System.out.println("file exist "+client+fileName);

        STIndex stIndex=findSTIndex(fileName);
        if ( stIndex==null) {
            throw new Exception(ErrorProtocols.ERROR_FILE_DOES_NOT_EXIST);
        }

        int index= stIndex.getDstoreInfos().size();
        if(index>0){
            stIndex.removeDstore(index-1);
        }

        //STDstoreInfo stDstoreInfo=stIndex.findDstore(client.getLocalPort());
        //stIndex.removeDstore(stDstoreInfo);
        if(stIndex.getDstoreInfos().size()==0) {
            stIndex.setLocked(false,STStoreOperationEnum.STORE_COMPLETE);
            writeMessageToSocket(stIndex.getClient(),STClientProtocol.STORE_COMPLETE,getTimeout());
        }
        return true;
    }
    public String listFiles() {
        String fileList = "";
        for (STIndex stIndex : stIndices) {
            if (!stIndex.isLocked()) {
                fileList += " " + stIndex.getFileName();
            }
        }
        return "LIST" + fileList;
    }

    public List<STIndex> getStIndices() {
        return stIndices;
    }

    public void setStIndices(List<STIndex> stIndices) {
        this.stIndices = stIndices;
    }


}
