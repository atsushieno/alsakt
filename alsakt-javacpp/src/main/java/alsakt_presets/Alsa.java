package alsakt_presets;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;
import org.bytedeco.javacpp.presets.javacpp;
import org.bytedeco.javacpp.tools.*;

@Properties(
    inherit = javacpp.class,
    target = "dev.atsushieno.alsakt.javacpp",
    global = "dev.atsushieno.alsakt.javacpp.global.Alsa",
    value = {
        @Platform(
            value = {"linux-x86", "linux-x86_64"},
            // This is so far a limited set of header files which is enough for ktmidi.
            include = {
                    // We'd like to remove this line, but adding them results in missing related functions
                    //  which is no-go. It seems the resulting library still builds, so we leave them as is. Also...(contd.)
                    "dummy_poll.h",

                    "alsa/asoundef.h",
                    //"alsa/asoundlib.h",

                    // These files are explicitly included from asoundlib.h:

                    "alsa/version.h",
                    "alsa/global.h",
                    "alsa/input.h",
                    "alsa/output.h",
                    // It mostly defines functions with varargs which javacpp does not work well with.
                    //"alsa/error.h",
                    "alsa/conf.h",
                    "alsa/pcm.h", // _snd_pcm_format could not be generated, see comment below
                    "alsa/rawmidi.h",
                    "alsa/ump.h",
                    "alsa/timer.h",
                    "alsa/hwdep.h",
                    "alsa/control.h",
                    "alsa/mixer.h",
                    "alsa/seq_event.h",
                    "alsa/seq.h",
                    "alsa/seqmid.h",
                    "alsa/seq_midi_event.h",

                    // These are not:

                    //"alsa/control_external.h",
                    //"alsa/control_plugin.h",
                    //"alsa/mixer_abst.h",
                    //"alsa/pcm_external.h",
                    //"alsa/pcm_extplug.h",
                    //"alsa/pcm_ioplug.h",
                    //"alsa/pcm_old.h",
                    //"alsa/pcm_plugin.h",
                    //"alsa/pcm_rate.h",
                    //"alsa/sound/asoc.h",
                    //"alsa/sound/asound_fm.h",
                    //"alsa/sound/emu10k1.h",
                    //"alsa/sound/hdsp.h",
                    //"alsa/sound/hdspm.h",
                    //"alsa/sound/sb16_csp.h",
                    //"alsa/sound/sscape_ioctl.h",
                    //"alsa/sound/tlv.h",
                    //"alsa/sound/type_compat.h",
                    //"alsa/sound/uapi/asoc.h",
                    //"alsa/sound/uapi/asound_fm.h",
                    //"alsa/sound/uapi/emu10k1.h",
                    //"alsa/sound/uapi/hdsp.h",
                    //"alsa/sound/uapi/hdspm.h",
                    //"alsa/sound/uapi/sb16_csp.h",
                    //"alsa/sound/uapi/sscape_ioctl.h",
                    //"alsa/sound/uapi/tlv.h",
                    //"alsa/topology.h",
                    //"alsa/ump_msg.h",
                    //"alsa/use-case.h",
            },
            link = {"asound"}
        )

    }
)
public class Alsa implements InfoMapper {
    static { Loader.checkVersion("dev.atsushieno", "alsakt"); }

    public void map(InfoMap infoMap) {
        infoMap
            // (contd.) cannot add these lines...
            // .put(new Info("pollfd").skip()) // it is bound at DummyPoll.java, not here.
            // .put(new Info("poll").skip()) // it is bound at DummyPoll.java, not here.

            .put(new Info("__inline__").cppTypes().annotations())
            .put(new Info("inline").cppTypes().annotations())
            .put(new Info("ATTRIBUTE_UNUSED").cppTypes().annotations())

            .put(new Info("!defined(DOXYGEN) && !defined(SWIG)").define(false))

            .put(new Info("snd_config_iterator_first").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_next").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_end").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_entry").skip()) // used only in a macro

            // It is somewhat tricky; these macros define libdl-related ALSA functions, which are
            // by default bound as declared functions in Java but the actual target functions don't exist
            // (somehow) and causes unresolved symbols... (contd.)
            .put(new Info("SND_LIB_VER").skip())
            .put(new Info("SND_LIB_VERSION").skip())
            .put(new Info("SND_CONFIG_DLSYM_VERSION_EVALUATE").skip())
            .put(new Info("SND_CONFIG_DLSYM_VERSION_HOOK").skip())
            .put(new Info("SND_TIMER_DLSYM_VERSION").skip())
            .put(new Info("SND_TIMER_QUERY_DLSYM_VERSION").skip())
            .put(new Info("SND_SEQ_DLSYM_VERSION").skip())
            .put(new Info("SND_RAWMIDI_DLSYM_VERSION").skip())
            .put(new Info("SND_HWDEP_DLSYM_VERSION").skip())
            .put(new Info("SND_CONTROL_DLSYM_VERSION").skip())
            .put(new Info("SND_PCM_DLSYM_VERSION").skip())
            // ...however, fixing macros does not seem enough. There are actually mapped symbols that are
            // used and then expected to exist within referring libraries. `snd_dlsym_start` seems to be
            // one of such a function that triggers this issue.
            // This, when bound as a glue in `libjniAlsa.so`, references the declared macro above, and
            // if those macros are declared (not skipped), then this function and relevant binding glue
            // is generated, resulting in missing `_dlsym_config_evaluate_001` symbol and then causes
            // UnsatisfiedLinkError at run-time.

            // The enum type contains members that depend on the platform endianness which is statically resolved in C/C++,
            // but not nicely with JavaCPP, and I'm not sure how I can sort them out... so far, skip generating the type.
            .put(new Info("_snd_pcm_format").skip())

            //
            // It looks as if we were missing `libjniAlsa.so` in the loader target paths when it is
            // being loaded by javacpp Loader. How confusing! You can examine if the shared library loads
            // without problem by calling `System.load(full_path_to_so)`.
            .put(new Info("snd_dlsym_start").skip())

            .put(new Info("snd_shm_area").skip()) // FIXME?
            .put(new Info("snd_seq_real_time").skip()) // FIXME?

//            .put(new Info("__BYTE_ORDER == __BIG_ENDIAN").define(false))
//            .put(new Info("__inline__").skip())
            .put(new Info("pid_t").cppTypes("int"))
//            .put(new Info("snd_mixer_selem_channel_id_t").pointerTypes("Pointer"))
//            .put(new Info("mixer_abst.h").linePatterns(".*snd_mixer_class_t .*class.*").skip())
            ;
    }
}
