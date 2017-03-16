package net;

import security.EKEProvider;

/**
 * Encapsulation of the data sent during UDP broadcast.
 *
 * <p>
 *     This data is broadcast by the {@link BroadcastThread}. This
 *     contains :
 *     <ul>
 *         <li>the public key of the server, </li>
 *         <li>status of the broadcast, and</li>
 *         <li>address of the server</li>
 *     </ul>
 * </p>
 * <p>
 *     This message helps a client to discover a server.
 * </p>
 *
 * @see BroadcastThread
 */

public class ServerInfo {
    private byte[] mServerPubKey;
    private String mServerInfo;
    private String mAddress;
    private boolean mStopFlag;
    private boolean mIsSelected;

    static final int SERVER_INFO_LENGTH = 600;

    /**
     * Constructor. <br/>
     * Initializes the {@code ServerInfo}.
     *
     * @param publicKey public key of the server generated by
     *                  {@link EKEProvider#getBase64EncodedPubKey()}.
     * @param serverInfo canonical hostname of the server.
     * @see EKEProvider#getBase64EncodedPubKey()
     */
    public ServerInfo(byte[] publicKey, String serverInfo) {
        mServerPubKey = publicKey;
        mServerInfo = serverInfo;
        mStopFlag = false;
    }

    /**
     * Compares two {@code ServerInfo} objects.
     *
     * @param obj the object to be compared.
     * @return {@code true}, if the two objects are equal, {@code false}, otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerInfo other = (ServerInfo) obj;
        return mAddress.equals(other.mAddress);
    }

    void setServerAddress(String address) { mAddress = address; }
    public void setSelected( boolean value) { mIsSelected = value; }

    /**
     * Sets the status of the <i>broadcast</i> to <i>active</i>.
     */
    void setStopFlag() { mStopFlag = true; }

    /**
     * Resets the status of the <i>broadcast</i> to <i>inactive</i>.
     */
    void clearStopFlag() { mStopFlag = false; }

    public boolean getStopFlag() { return mStopFlag; }
    public boolean isSelected() { return mIsSelected; }
    public byte[] getServerPubKey() { return mServerPubKey; }
    public String getServerInfo() { return mServerInfo; }
    public String getAddress() { return mAddress; }
}