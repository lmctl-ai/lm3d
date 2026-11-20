package com.lmctl.lm3d.demo;

import com.lmctl.lm3d.core.Group;
import com.lmctl.lm3d.lmcode.Lm3dHost;
import io.lmctl.lmcode.Context;
import io.lmctl.lmcode.Program;
import io.lmctl.lmcode.Registry;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

final class LmcodeScenes {
    private LmcodeScenes() {
    }

    static Lm3dHost run(String annotation, Consumer<Group> rootConsumer) {
        Registry registry = new Registry(false);
        Lm3dHost host = new Lm3dHost(registry, rootConsumer);
        Program program = Program.load(Base64.getDecoder().decode(LmcodeScenesProgram.BYTECODE),
                LmcodeScenesProgram.SCHEMA.getBytes(StandardCharsets.UTF_8), registry);
        int entry = findEntry(program, annotation);
        program.call(entry, new Context(), registry, List.of());
        if (!host.used()) {
            throw new IllegalStateException("LMCode handler did not call lm3d_use: @" + annotation);
        }
        return host;
    }

    private static int findEntry(Program program, String annotation) {
        for (int i = 0; i < program.entries().size(); i++) {
            if (program.entries().get(i).annotation().equals(annotation)) {
                return i;
            }
        }
        throw new IllegalStateException("LMCode entry is missing: @" + annotation);
    }
}
