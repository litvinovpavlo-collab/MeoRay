package dev.meoray.client.core;

import dev.meoray.client.feature.combat.*;
import dev.meoray.client.feature.movement.*;
import dev.meoray.client.feature.player.*;
import dev.meoray.client.feature.render.*;
import dev.meoray.client.feature.visual.wings.Wings;
import dev.meoray.client.feature.misc.Interface;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        register(new KillAura());
        register(new Velocity());
        register(new AutoPotion());
        register(new Flight());
        register(new Sprint());
        register(new NoSlow());
        register(new NoFall());
        register(new ESP());
        register(new ChinaHat());
        register(new TargetHUD());
        register(new NoRender());
        register(new BetterWorld());
        register(new Particles());
        register(new FullBright());
        register(new Wings());
        register(new Interface());
    }

    private void register(Module m) {
        modules.add(m);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category cat) {
        return modules.stream()
                .filter(m -> m.getCategory() == cat)
                .collect(Collectors.toList());
    }

    public Module getByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void onTick() {
        modules.stream()
                .filter(Module::isEnabled)
                .forEach(Module::onTick);
    }
}
