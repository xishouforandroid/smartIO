package server;

import java.awt.AWTException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Sudipto Bhattacharjee
 */
public class NetworkManager {

    private Server mServer;
    private BroadcastThread mBroadcastThread;
    private Thread mThread;
    private NetworkState mState;

    public NetworkManager() {
        mState = new NetworkState();
        mBroadcastThread = new BroadcastThread();
    }

    public NetworkState getNetworkState() { return mState; }

    public void startServer() throws IOException, InterruptedException, AWTException {
        mServer = new Server(mState, 1234);
        mThread = new Thread(mBroadcastThread);
        mThread.start();
        System.out.println("Broadcast started ...");
        System.out.println("Server started ...");
        mServer.listen();
    }

    public void stopServer() throws IOException, InterruptedException {
        mBroadcastThread.stopBroadcast();
        mThread = null;
        System.out.println("Broadcast stopped ...");

        mServer.setStopFlag();
        Thread.sleep(mServer.getTimeout());
        mServer.close();

        HashMap<Socket, ServerThread> cMap = mState.getConnectionMap();
        Iterator it = cMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Socket, ServerThread> entry = (Map.Entry<Socket, ServerThread>) it.next();
            ServerThread st = entry.getValue();
            st.setStopFlag();
            Thread.sleep(st.getTimeout());
        }

        System.out.println("Server stopped ...");
    }

    public void disconnect(InetAddress address) throws IOException, InterruptedException {
        ServerThread st = mState.getServerThread(address);
        st.setStopFlag();
        Thread.sleep(st.getTimeout());
        System.out.println(address.getHostAddress() + " disconnected");
    }
}
