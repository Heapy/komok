@file:JvmName("RunMigrations")

import org.flywaydb.core.Flyway

fun main() {
    val env = System.getenv()

    val pgHost = env["KOMOK_POSTGRES_HOST"]
    val pgPort = env["KOMOK_POSTGRES_PORT"]
    val pgUser = env["KOMOK_POSTGRES_USER"]
    val pgPassword = env["KOMOK_POSTGRES_PASSWORD"]
    val pgDatabase = env["KOMOK_POSTGRES_DATABASE"]

    Flyway
        .configure()
        .locations("classpath:migrations")
        .dataSource(
            "jdbc:postgresql://$pgHost:$pgPort/$pgDatabase",
            pgUser,
            pgPassword
        )
        .loggers("slf4j")
        .load()
        .migrate()
}
