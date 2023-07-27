package db.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V01__init : BaseJavaMigration() {
    override fun migrate(context: Context) {

        context.connection.createStatement().use { create ->
            create.execute(
                // language=sql
                """
                    create table foo
                    (
                        id   UUID primary key,
                        data JSONB not null
                    );
                """.trimIndent()
            )
        }
    }
}
