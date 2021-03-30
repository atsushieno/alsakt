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
"dummy_poll.h",
//"asoundlib.h",
"asoundef.h",
"version.h",
"global.h",
"input.h",
"output.h",
///"error.h",
"conf.h",
//"pcm.h",
//"rawmidi.h",
"timer.h",
//"hwdep.h",
//"control.h",
//"mixer.h",
"seq_event.h",
"seq.h",
"seqmid.h",
"seq_midi_event.h",
/*
"mixer_abst.h",
"pcm_external.h",
"pcm_plugin.h",
//"pcm_old.h",
"control_external.h",
"pcm_rate.h",
"sound/sb16_csp.h",
"sound/type_compat.h",
"sound/uapi/sb16_csp.h",
"sound/uapi/asound_fm.h",
"sound/uapi/asoc.h",
"sound/uapi/emu10k1.h",
"sound/uapi/hdspm.h",
"sound/uapi/hdsp.h",
"sound/uapi/sscape_ioctl.h",
"sound/uapi/tlv.h",
"sound/asound_fm.h",
"sound/asoc.h",
"sound/emu10k1.h",
"sound/hdspm.h",
"sound/hdsp.h",
"sound/sscape_ioctl.h",
"sound/tlv.h",
"pcm_extplug.h",
"use-case.h",
"pcm_ioplug.h",
*/
},
            link = "asound",
            preload = "libasound"
        )

    }
)
public class Alsa implements InfoMapper {
    static { Loader.checkVersion("dev.atsushieno", "alsakt"); }

    public void map(InfoMap infoMap) {
        infoMap
            .put(new Info("__inline__").cppTypes().annotations())
            .put(new Info("inline").cppTypes().annotations())
            .put(new Info("ATTRIBUTE_UNUSED").cppTypes().annotations())

            .put(new Info("!defined(DOXYGEN) && !defined(SWIG)").define(false))

            .put(new Info("snd_config_iterator_first").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_next").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_end").skip()) // used only in a macro
            .put(new Info("snd_config_iterator_entry").skip()) // used only in a macro
            
            .put(new Info("SND_CONFIG_DLSYM_VERSION_EVALUATE").skip())
            .put(new Info("SND_CONFIG_DLSYM_VERSION_HOOK").skip())
            .put(new Info("SND_TIMER_DLSYM_VERSION").skip())
            .put(new Info("SND_TIMER_QUERY_DLSYM_VERSION").skip())
            .put(new Info("SND_SEQ_DLSYM_VERSION").skip())

            .put(new Info("snd_shm_area").skip()) // FIXME?
            .put(new Info("snd_seq_real_time").skip()) // FIXME?

//            .put(new Info("__BYTE_ORDER == __BIG_ENDIAN").define(false))
//            .put(new Info("__inline__").skip())
//            .put(new Info("pid_t").cppTypes("int"))
//            .put(new Info("snd_mixer_selem_channel_id_t").pointerTypes("Pointer"))
//            .put(new Info("mixer_abst.h").linePatterns(".*snd_mixer_class_t .*class.*").skip())
            ;
    }
}
