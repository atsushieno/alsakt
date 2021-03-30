package dev.atsushieno.alsakt;

public class AlsaPortType {
    public static final int
            /** Messages sent from/to this port have device-specific semantics. */
            Specific = 1,
    /**
     * This port understands MIDI messages.
     */
    MidiGeneric = 1 << 1,
    /**
     * This port is compatible with the General MIDI specification.
     */
    MidiGM = 1 << 2,

    /**
     * This port is compatible with the Roland GS standard.
     */
    MidiGS = 1 << 3,
    /**
     * This port is compatible with the Yamaha XG specification.
     */
    MidiXG = 1 << 4,
    /**
     * This port is compatible with the Roland MT-32.
     */
    MidiMT32 = 1 << 5,
    /**
     * This port is compatible with the General MIDI 2 specification.
     */
    MidiGM2 = 1 << 6,
    /**
     * This port understands SND_SEQ_EVENT_SAMPLE_xxx messages
     * (these are not MIDI messages).
     */
    Synth = 1 << 10,
    /**
     * Instruments can be downloaded to this port
     * (with SND_SEQ_EVENT_INSTR_xxx messages sent directly).
     */
    DirectSample = 1 << 11,
    /**
     * Instruments can be downloaded to this port
     * (with SND_SEQ_EVENT_INSTR_xxx messages sent directly or through a queue).
     */
    Sample = 1 << 12,
    /**
     * This port is implemented in hardware.
     */
    Hardware = 1 << 16,
    /**
     * This port is implemented in software.
     */
    Software = 1 << 17,
    /**
     * Messages sent to this port will generate sounds.
     */
    Synthesizer = 1 << 18,
    /**
     * This port may connect to other devices
     * (whose characteristics are not known).
     */
    Port = 1 << 19,
    /**
     * This port belongs to an application, such as a sequencer or editor.
     */
    Application = 1 << 20;
}
