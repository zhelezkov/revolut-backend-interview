package com.rsredsq.revolut.backend

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rsredsq.revolut.backend.api.AccountController
import com.rsredsq.revolut.backend.api.TransferController
import com.rsredsq.revolut.backend.domain.repository.AccountOrmRepository
import com.rsredsq.revolut.backend.domain.repository.AccountRepository
import com.rsredsq.revolut.backend.domain.repository.TransferOrmRepository
import com.rsredsq.revolut.backend.domain.repository.TransferRepository
import com.rsredsq.revolut.backend.domain.service.AccountService
import com.rsredsq.revolut.backend.domain.service.TransferService
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.JavalinConfig
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.Info
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.exceptions.EntityNotFoundException
import org.jetbrains.exposed.sql.Database
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

val kodein = Kodein {
  bind<AccountService>() with singleton { AccountService() }
  bind<AccountRepository>() with singleton { AccountOrmRepository() }
  bind<TransferService>() with singleton { TransferService() }
  bind<TransferRepository>() with singleton { TransferOrmRepository() }
}

val db = Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1", "org.h2.Driver")

val globalObjectMapper = jacksonObjectMapper().apply {
  registerModule(JodaModule())
  enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
  disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
}

fun main() {
  startJavalin()
}

fun startJavalin(): Javalin =
  Javalin
    .create { config -> initialConfig(config) }
    .start(8080)
    .also { app -> configure(app) }

fun initialConfig(config: JavalinConfig) = config.apply {
  defaultContentType = ContentType.JSON
  JavalinJackson.configure(globalObjectMapper)
  enableWebjars()
  enableDevLogging()
  registerPlugin(
    OpenApiOptions(Info())
      .path("/openapi")
      .swagger(SwaggerOptions("/swagger").title("Revolut backend task"))
      .let { options ->
        OpenApiPlugin(options)
      }
  )
}

fun configure(app: Javalin) {
  app.exception(EntityNotFoundException::class.java) { e, ctx ->
    ctx.json(RuntimeException(e.message))
    ctx.status(HttpStatus.BAD_REQUEST_400)
  }
  router(app)
}

fun router(app: Javalin) {
  app.routes {
    path("api") {
      crud("accounts/:id", AccountController)
      get("accounts/:id/transfers", TransferController::listTransfers)
      path("transfers") {
        post(TransferController::create)
      }
    }
  }
}