# JSON Conventions

- Global Jackson `SNAKE_CASE` strategy is configured — do NOT add `@JsonProperty` for standard camelCase fields.
- `var countryCode: Int` automatically serializes as `"country_code"` — no annotation needed.
- Only use `@JsonProperty` for genuinely non-standard cases, and document why inline.
- Angular and Compose clients trust the snake_case contract — no client-side casing transforms.
