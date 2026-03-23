package com.froobworld.nabconfiguration;

import com.froobworld.nabconfiguration.annotations.Entry;
import com.froobworld.nabconfiguration.annotations.EntryMap;
import com.froobworld.nabconfiguration.annotations.Section;
import com.froobworld.nabconfiguration.annotations.SectionMap;
import com.froobworld.nabconfiguration.utils.InstantFallbackConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;

public class ConfigSectionPopulator {
    private InstantFallbackConfigurationSection configurationSection;
    private ConfigSection workConfigSection;

    public ConfigSectionPopulator(File configFile, ConfigSection configSection) {
        this(new InstantFallbackConfigurationSection(YamlConfiguration.loadConfiguration(configFile)), configSection);
    }

    private ConfigSectionPopulator(InstantFallbackConfigurationSection configurationSection,
            ConfigSection workingConfigSection) {
        this.configurationSection = configurationSection;
        this.workConfigSection = workingConfigSection;
    }

    public void populate() throws Exception {

        for (Field field : workConfigSection.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            {
                Section sectionAnnotation = field.getAnnotation(Section.class);
                if (sectionAnnotation != null) {
                    ConfigSection subConfigSection = (ConfigSection) field.get(workConfigSection);
                    new ConfigSectionPopulator(configurationSection.getSection(sectionAnnotation.key(), null),
                            subConfigSection).populate();
                }
            }

            {
                SectionMap sectionMapAnnotation = field.getAnnotation(SectionMap.class);
                if (sectionMapAnnotation != null) {
                    populateSectionMap(castConfigSectionMap(field.get(workConfigSection)), sectionMapAnnotation);
                }
            }
            {
                Entry entryAnnotation = field.getAnnotation(Entry.class);
                if (entryAnnotation != null) {
                    ConfigEntry<?> configEntry = castConfigEntry(field.get(workConfigSection));
                    configEntry.setValue(configurationSection.get(entryAnnotation.key()));
                }
            }
            {
                EntryMap entryMapAnnotation = field.getAnnotation(EntryMap.class);
                if (entryMapAnnotation != null) {
                    ConfigEntryMap<?, ?> configEntryMap = castConfigEntryMap(field.get(workConfigSection));
                    configEntryMap.clear();
                    InstantFallbackConfigurationSection mapSection = configurationSection
                            .getSection(entryMapAnnotation.key(), null);

                    configEntryMap.setDefault(mapSection.get(entryMapAnnotation.defaultKey()));
                    for (String key : mapSection.getKeys(false)) {
                        configEntryMap.put(key, mapSection.get(key));
                    }
                }
            }
        }
    }

    private <K, C extends ConfigSection> void populateSectionMap(ConfigSectionMap<K, C> configSectionMap,
            SectionMap sectionMapAnnotation) throws Exception {
        configSectionMap.clear();
        InstantFallbackConfigurationSection mapSection = configurationSection.getSection(sectionMapAnnotation.key(),
                null);

        C defaultEntry = newEntry(configSectionMap);
        new ConfigSectionPopulator(mapSection.getSection(sectionMapAnnotation.defaultKey(), null), defaultEntry)
                .populate();
        configSectionMap.setDefaultSection(defaultEntry);

        for (String key : mapSection.getKeys(false)) {
            C newEntry = newEntry(configSectionMap);
            new ConfigSectionPopulator(mapSection.getSection(key, sectionMapAnnotation.defaultKey()), newEntry)
                    .populate();
            configSectionMap.put(key, newEntry);
        }
    }

    private <C extends ConfigSection> C newEntry(ConfigSectionMap<?, C> configSectionMap) throws Exception {
        return configSectionMap.entryType().getDeclaredConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    private ConfigEntry<?> castConfigEntry(Object value) {
        return (ConfigEntry<?>) value;
    }

    @SuppressWarnings("unchecked")
    private ConfigEntryMap<?, ?> castConfigEntryMap(Object value) {
        return (ConfigEntryMap<?, ?>) value;
    }

    @SuppressWarnings("unchecked")
    private <K, C extends ConfigSection> ConfigSectionMap<K, C> castConfigSectionMap(Object value) {
        return (ConfigSectionMap<K, C>) value;
    }

}
