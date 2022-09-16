import java.util.*;

public class STRebalanceInfo {
    private Integer port;
    private Map<String,Set<Integer>> fileNames=new HashMap<>();

    private Set<String> filesToBeRemoved=new HashSet<>();

    public STRebalanceInfo(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }


    public void  addFileName(String fileName,Integer port){
        if(this.fileNames==null){
            this.fileNames=new HashMap<>();
        }
        if(this.fileNames.containsKey(fileName)) {
            Set<Integer> ports = this.fileNames.get(fileName);
            if (ports == null) {
                ports=new HashSet<>();
                ports.add(port);
                fileNames.put(fileName,ports);
            }else {
                ports.add(port);
            }
        }else{
            Set<Integer> ports=new HashSet<>();
            ports.add(port);
            this.fileNames.put(fileName,ports);
        }


    }


    public Map<String, Set<Integer>> getFileNames() {
        return fileNames;
    }

    public void setFileNames(Map<String, Set<Integer>> fileNames) {
        this.fileNames = fileNames;
    }

    public Set<String> getFilesToBeRemoved() {
        return filesToBeRemoved;
    }

    public void setFilesToBeRemoved(Set<String> filesToBeRemoved) {
        this.filesToBeRemoved = filesToBeRemoved;
    }
    public void  addDFilesToBeRemoved(String filesToBeRemoved){
        if(this.filesToBeRemoved==null){
            this.filesToBeRemoved=new HashSet<>();
        }
        this.filesToBeRemoved.add(filesToBeRemoved);
    }

    public String createFileToSent(){
        String s="";
        for(String key:fileNames.keySet()){
            Set<Integer> ports=fileNames.get(key);

            s+=" "+key+" "+ports.size();
            for(Integer port:ports){
                s+=" "+port;
            }

        }
        return s;
    }
    public String createDFilesToBeRemovedFromSet(){
        String fname="";
        for(String s: filesToBeRemoved){
            fname+=" "+s;
        }
        if(fname.length()==0){
            fname=" ";
        }
        return fname;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", STRebalanceInfo.class.getSimpleName() + "[", "]")
                .add("port=" + port)
                .add("fileNames=" + fileNames)
                .add("filesToBeRemoved=" + filesToBeRemoved)
                .toString();
    }
}
