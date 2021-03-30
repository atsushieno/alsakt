package dev.atsushieno.alsakt;

public class AlsaPortCapabilities {
    public static final int
            /**< readable from this port */
            Read = 1 << 0,
    /**
     * < writable to this port
     */
    Write = 1 << 1,
    /**
     * < for synchronization (not implemented)
     */
    SyncRead = 1 << 2,
    /**
     * < for synchronization (not implemented)
     */
    SyncWrite = 1 << 3,
    /**
     * < allow read/write duplex
     */
    Duple = 1 << 4,
    /**
     * < allow read subscription
     */
    SubsRead = 1 << 5,
    /**
     * < allow write subscription
     */
    SubsWrite = 1 << 6,
    /**
     * < routing not allowed
     */
    NoExport = 1 << 7;
}
