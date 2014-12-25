package uk.co.cacoethes.lazybones.dagger

import dagger.Module
import dagger.Provides
import uk.co.cacoethes.lazybones.api.LazybonesService
import uk.co.cacoethes.lazybones.api.PackageCache
import uk.co.cacoethes.lazybones.api.PackageSourceManager
import uk.co.cacoethes.lazybones.api.TemplateInstaller
import uk.co.cacoethes.lazybones.impl.DefaultLazybonesService
import uk.co.cacoethes.lazybones.impl.DefaultPackageCache
import uk.co.cacoethes.lazybones.impl.DefaultPackageSourceManager
import uk.co.cacoethes.lazybones.impl.DefaultTemplateInstaller

import javax.inject.Singleton

/**
 * Created by pledbrook on 21/01/2016.
 */
@Module(injects = LazybonesService, includes = ConfigModule)
class DefaultModule {
    @Provides @Singleton LazybonesService packageSourceManager(DefaultLazybonesService service) {
        return service
    }

    @Provides @Singleton PackageSourceManager packageSourceManager() {
        return new DefaultPackageSourceManager()
    }

    @Provides @Singleton PackageCache packageCache(DefaultPackageCache pc) {
        return pc
    }

    @Provides @Singleton TemplateInstaller templateInstaller() {
        return new DefaultTemplateInstaller()
    }
}
