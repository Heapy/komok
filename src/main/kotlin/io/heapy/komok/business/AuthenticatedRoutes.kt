package io.heapy.komok.business

import io.heapy.komok.business.entity.EntityInsertRoute
import io.heapy.komok.business.entity.EntityModule
import io.heapy.komok.business.entity.GetLatestUnreadRoute
import io.heapy.komok.business.entity.MongoEntityInsertRoute
import io.heapy.komok.business.entity.MongoGetLatestUnreadRoute
import io.heapy.komok.business.entity.MongoUpdateStatusRoute
import io.heapy.komok.business.entity.UpdateStatusRoute
import io.heapy.komok.business.user.UserRoute
import io.heapy.komok.infra.server.KomokRoute
import io.heapy.komok.tech.di.lib.Module
import io.ktor.server.routing.Route

interface AuthenticatedRoutes : KomokRoute

class DefaultAuthenticatedRoutes(
    private val userRoute: UserRoute,
    private val entityInsertRoute: EntityInsertRoute,
    private val mongoEntityInsertRoute: MongoEntityInsertRoute,
    private val getLatestUnreadRoute: GetLatestUnreadRoute,
    private val mongoGetLatestUnreadRoute: MongoGetLatestUnreadRoute,
    private val updateStatusRoute: UpdateStatusRoute,
    private val mongoUpdateStatusRoute: MongoUpdateStatusRoute,
) : AuthenticatedRoutes {
    override fun Route.install() {
        userRoute.run { install() }
        entityInsertRoute.run { install() }
        mongoEntityInsertRoute.run { install() }
        getLatestUnreadRoute.run { install() }
        mongoGetLatestUnreadRoute.run { install() }
        updateStatusRoute.run { install() }
        mongoUpdateStatusRoute.run { install() }
    }
}

@Module
open class AuthenticatedRoutesModule(
    private val entityModule: EntityModule,
) {
    open val userRoute by lazy {
        UserRoute()
    }

    open val authenticatedRoutes by lazy {
        DefaultAuthenticatedRoutes(
            userRoute = userRoute,
            entityInsertRoute = entityModule.entityInsertRoute,
            mongoEntityInsertRoute = entityModule.mongoEntityInsertRoute,
            getLatestUnreadRoute = entityModule.getLatestUnreadRoute,
            mongoGetLatestUnreadRoute = entityModule.mongoGetLatestUnreadRoute,
            updateStatusRoute = entityModule.updateStatusRoute,
            mongoUpdateStatusRoute = entityModule.mongoUpdateStatusRoute,
        )
    }
}
