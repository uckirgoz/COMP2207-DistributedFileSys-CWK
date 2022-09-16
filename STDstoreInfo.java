import java.net.Socket;
import java.util.Objects;
import java.util.StringJoiner;

public class STDstoreInfo {
    private String ip;
    private Integer port;
    private Socket client;
    public STDstoreInfo(String ip, Integer port,Socket client) {
        this.ip = ip;
        this.port = port;
        this.client= client;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof STDstoreInfo)) return false;
        STDstoreInfo that = (STDstoreInfo) o;
        return Objects.equals(getIp(), that.getIp()) && Objects.equals(getPort(), that.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIp(), getPort());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", STDstoreInfo.class.getSimpleName() + "[", "]")
                .add("ip='" + ip + "'")
                .add("port=" + port)
                .add("client=" + client)
                .toString();
    }
}
