package libc_presets;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;
import org.bytedeco.javacpp.presets.javacpp;
import org.bytedeco.javacpp.tools.*;

@Properties(
        inherit = javacpp.class,
        target = "dev.atsushieno.alsakt.javacpp",
        global = "dev.atsushieno.alsakt.javacpp.global.HackyPoll",
        value = {
                @Platform(
                        value = {"linux-x86", "linux-x86_64"},
                        // This is so far a limited set of header files which is enough for ktmidi.
                        include = {
                                "dummy_poll.h",
                        },
                        //link = "c",
                        preload = "c"
                )
        }
)
public class HackyPoll implements InfoMapper {
    static { Loader.checkVersion("dev.atsushieno", "alsakt"); }

    public void map(InfoMap infoMap) {
        infoMap
                .put(new Info("__inline__").cppTypes().annotations())
                .put(new Info("inline").cppTypes().annotations())
                .put(new Info("ATTRIBUTE_UNUSED").cppTypes().annotations())
        ;
    }
}
