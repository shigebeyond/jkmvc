import java.net.NetworkInterface

/**
 * 获得内网ip
 *
 * @return
 */
public fun getIntranetHost(): String {
    for(netIntf in NetworkInterface.getNetworkInterfaces()){
        for(addr in netIntf.inetAddresses){
            val ip = addr.hostAddress
            if (ip.startsWith("192.168."))
                return ip;

        }
    }
    return "127.0.0.1"
}