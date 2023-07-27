package db.migration

import org.apache.commons.lang3.RandomStringUtils
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.util.UUID

class V02__generate_data : BaseJavaMigration() {
    override fun migrate(context: Context) {

        context.connection.createStatement().use { insert ->
            insert.execute(
                // language=sql
                """
                    insert into foo values ('${UUID.randomUUID()}'::uuid, '${RandomStringUtils.randomAlphanumeric(10)}');
                """.trimIndent()
            )
        }
    }
}
