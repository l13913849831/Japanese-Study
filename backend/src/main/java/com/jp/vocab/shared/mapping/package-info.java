/**
 * API / persistence mapping conventions:
 *
 * <ul>
 *   <li>Database schema follows Flyway snake_case table and column names.</li>
 *   <li>JPA entities expose camelCase Java fields with explicit table/column mapping.</li>
 *   <li>API request and response DTOs follow camelCase names from docs/api-specification.md.</li>
 *   <li>JSON columns map to structured Java types instead of raw string blobs when practical.</li>
 * </ul>
 */
package com.jp.vocab.shared.mapping;
