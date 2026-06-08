package dev.meoray.client.util.other;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NameGen {
    private final SecureRandom secureRandom = new SecureRandom();
    private String[] names = new String[0];

    public String loadNames() throws IOException {
        String source;
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                resourceManager.getResource(Identifier.of("meoray", "names/names.txt")).orElseThrow().getInputStream()))) {
            source = bufferedReader.lines()
                .filter(str -> !str.isEmpty())
                .map(str -> str.replace("\t", ""))
                .collect(Collectors.joining("\n"));
        }
        this.names = source.split("\n");
        return source;
    }

    public String generate() {
        try {
            this.loadNames();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String name = this.names[this.secureRandom.nextInt(this.names.length)];
        return name + this.secureRandom.nextInt(9) + this.secureRandom.nextInt(9)
            + this.secureRandom.nextInt(9) + this.secureRandom.nextInt(9);
    }

    public String[] getNames() {
        return this.names;
    }
}
