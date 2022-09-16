import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class STIndex {
    private String fileName;
    private Long fileSize;

    private Socket client;

    private List<STDstoreInfo> dstoreInfos=new ArrayList<>();
    private boolean locked=false;

    private Integer tempPort;
    private STStoreOperationEnum storeOperationEnum=STStoreOperationEnum.STORE_COMPLETE;


    public STIndex(String fileName, Long fileSize,Socket client,List<STDstoreInfo> dstoreInfos, boolean locked,STStoreOperationEnum storeOperationEnum) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.locked = locked;
        this.client=client;
        this.dstoreInfos=dstoreInfos;
        this.storeOperationEnum=storeOperationEnum;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked,STStoreOperationEnum storeOperationEnum) {
        this.locked = locked;
        this.storeOperationEnum=storeOperationEnum;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }

    public List<STDstoreInfo> getDstoreInfos() {
        return dstoreInfos;
    }

    public void setDstoreInfos(List<STDstoreInfo> dstoreInfos) {
        this.dstoreInfos = dstoreInfos;
    }


    public STDstoreInfo findDstore(Integer port){
        return  dstoreInfos.stream().filter(d->d.getPort().equals(port)).findAny().orElse(null);
    }
    public void removeDstore(STDstoreInfo dstoreInfo){
        dstoreInfos.remove(dstoreInfo);
    }
    public void removeDstore(int index){
        if(index>-1 && index <dstoreInfos.size()){
            dstoreInfos.remove(index);
        }
    }

   /* public void setLocked(boolean locked) {
        this.locked = locked;
    }

    */

    public STStoreOperationEnum getStoreOperationEnum() {
        return storeOperationEnum;
    }

    public void setStoreOperationEnum(STStoreOperationEnum storeOperationEnum) {
        this.storeOperationEnum = storeOperationEnum;
    }

    public Integer getTempPort() {
        return tempPort;
    }

    public void setTempPort(Integer tempPort) {
        this.tempPort = tempPort;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", STIndex.class.getSimpleName() + "[", "]")
                .add("fileName='" + fileName + "'")
                .add("fileSize=" + fileSize)
                .add("client=" + client)
                .add("dstoreInfos=" + dstoreInfos)
                .add("locked=" + locked)
                .add("tempPort=" + tempPort)
                .add("storeOperationEnum=" + storeOperationEnum)
                .toString();
    }
}
