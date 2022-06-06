package dev.monosoul.jooq

import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType.AS_IS
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersCatalogType
import org.jooq.meta.jaxb.MatchersSchemaType

internal fun Map<String, String>.toMatchersStrategy() = Matchers()
    .withSchemas(
        *toSchemasMatchers()
    )
    .withCatalogs(
        *toCatalogMatchers()
    )

private fun Map<String, String>.toSchemasMatchers() = map { (schema, pkg) ->
    MatchersSchemaType()
        .withExpression(schema)
        .withSchemaIdentifier(
            MatcherRule()
                .withTransform(AS_IS)
                .withExpression(pkg)
        )
}.toTypedArray()

private fun Map<String, String>.toCatalogMatchers() = map { (schema, pkg) ->
    MatchersCatalogType()
        .withExpression(schema)
        .withCatalogIdentifier(
            MatcherRule()
                .withTransform(AS_IS)
                .withExpression(pkg)
        )
}.toTypedArray()
