package uk.co.cacoethes.lazybones.dagger

import dagger.Module
import dagger.Provides
import uk.co.cacoethes.lazybones.api.PackageCache
import uk.co.cacoethes.lazybones.config.Configuration
import uk.co.cacoethes.lazybones.impl.DefaultPackageCache

import javax.inject.Named

/**
 * Created by pledbrook on 21/01/2016.
 */
@Module(library = true)
class ConfigModule {
    private Configuration settings

    ConfigModule(Configuration c) {
        settings = c
    }

    @Provides @Named("cache.dir") File cacheDir() {
        return new File(settings.getSetting("cache.dir"))
    }

    @Provides @Named("git.name") String gitName() {
        return settings.getSetting("git.name")
    }

    @Provides @Named("git.email") String gitEmail() {
        return settings.getSetting("git.email")
    }
}
