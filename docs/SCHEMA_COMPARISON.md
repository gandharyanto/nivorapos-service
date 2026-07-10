# Schema Comparison — pos-service

Three-way comparison of the 42 Kotlin JPA entities against two live databases, reached over SSH tunnels. Compiled 2026-07-10 for the `feat/pos-endpoint-sync` branch.

- **CODE** — Kotlin entities, `src/main/kotlin/id/nivorapos/pos_service/entity` (42 files)
- **A** — `midware_pos`, PostgreSQL 18.4, via `localhost:5448`
- **B** — `cz_pos_staging`, MySQL, via `localhost:3384`

## Summary

| Metric | Count |
|---|---|
| Entities in code | 42 |
| Tables aligned in code, A, and B | 26 |
| Tables in code + A, missing in B | 16 |
| DB tables with no entity yet | 15 (1 shared + 6 A-only + 8 B-only) |

## Table coverage

### Aligned in code, A, and B (26)
Core reference and transaction tables that exist everywhere.

`area`, `category`, `company`, `company_group`, `global_parameter`, `merchant`, `merchant_payment_method`, `outlet`, `payment`, `payment_method`, `payment_setting`, `permissions`, `product`, `product_categories`, `product_outlet`, `role_permissions`, `roles`, `stock`, `stock_movement`, `tax`, `transaction`, `transaction_items`, `transaction_queue`, `user_detail`, `user_roles`, `users`

### In code + A, missing in B (16)
Variant/modifier products and the standalone promotion & discount engine. Fully modeled in code and present in A, but staging (B) hasn't grown these tables.

`discount`, `discount_category`, `discount_outlet`, `discount_product`, `discount_usage`, `merchant_role_permissions`, `product_modifier`, `product_variant`, `product_variant_group`, `promotion`, `promotion_buy_category`, `promotion_buy_product`, `promotion_outlet`, `promotion_reward_category`, `promotion_reward_product`, `transaction_item_modifier`

### Shared by A and B, no entity in code (1)
The clearest single gap in the entity layer — exists in **both** live databases already.

`transaction_item_detail`

### A only, no entity in code (6)
`psgs_token_auth_cache` is expected — CLAUDE.md notes PSGS uses raw JDBC, not Hibernate. The rest look like unfinished or retired schema.

`product_modifier_group`, `psgs_token_auth_cache`, `transaction_applied_promotion`, `transaction_item_discount_detail`, `transaction_item_promotion_detail`, `transaction_item_tax_detail`

### B only, no entity in code (8)
Newer staging-side features — media handling, OTP, exports, and product history/archival — that the codebase hasn't caught up to.

`export_job`, `images`, `otp`, `product_archived`, `product_histories`, `product_images`, `transaction_item_promotions`, `transaction_promotions`

## Column-level findings

### Resolved — not a bug
Eight columns first looked like type mismatches (`varchar` in code vs. `text`/`longtext` in the databases): `category.image`, `global_parameter.param_value`, `merchant_payment_method.config_json`, `payment.payment_snapshot`, `product.image_url`, `product.image_thumb_url`, `stock_movement.note`, `transaction_items.product_snapshot`. Each one already carries an explicit `columnDefinition = "text"` in its entity — the mismatch was in the comparison tooling's type-normalizer, not the code.

### Systemic pattern — audit columns
`created_date` is declared `LocalDateTime? = null` across nearly every entity, but is `NOT NULL` in database B on ~20 of those same tables (plus a handful of business columns per-table — `outlet.name`, `product.sku`, `payment_method.provider`, and similar). In practice this is safe as long as B populates it via a column default or the service layer always sets it before insert; it becomes a real bug only if some code path persists one of these entities with the field left `null` against B.

### transaction — 7 columns only in B
B has moved discounting onto the transaction row directly, plus added voucher support. The entity still carries the old discount/promo columns, which B has already dropped.

| Column | Where | Type | Note |
|---|---|---|---|
| discount_scope | B only | varchar | not modeled |
| discount_type | B only | varchar | not modeled |
| discount_value | B only | decimal | not modeled |
| discount_value_type | B only | varchar | not modeled |
| gross_sub_total | B only | decimal | not modeled |
| is_split_payment | B only | tinyint | not modeled |
| notes | B only | varchar | not modeled |
| total_discount | B only | decimal | not modeled |
| total_promotion_amount | B only | decimal | not modeled |
| voucher_code / voucher_amount / voucher_id | B only | varchar / decimal / bigint | not modeled |
| discount_amount | code + A only | decimal | gone in B |
| discount_code | code + A only | varchar | gone in B |
| promo_amount | code + A only | decimal | gone in B |

### transaction_items — 6 columns only in B
Same split as the parent table: B tracks discount/promo per line item, code still tracks variant pricing.

| Column | Where | Type | Note |
|---|---|---|---|
| discount_id / discount_name / discount_amount | B only | bigint / varchar / decimal | not modeled |
| promotion_amount | B only | decimal | not modeled |
| free_qty | B only | int | not modeled |
| is_price_override | B only | tinyint | not modeled |
| original_price | B only | decimal | not modeled |
| variant_name | code + A only | varchar | gone in B |
| variant_additional_price | code + A only | decimal | gone in B |
| modifiers_additional_price | code + A only | decimal | gone in B |

### payment_setting — tax fields only in B

| Column | Where | Type | Note |
|---|---|---|---|
| is_tax | B only | tinyint | not modeled |
| tax_mode | B only | varchar | not modeled |
| tax_name | B only | varchar | not modeled |
| tax_percentage | B only | decimal | not modeled |
| service_charge_source | code + A only | varchar | gone in B |

### product — pricing & flags diverge

| Column | Where | Type | Note |
|---|---|---|---|
| price | B only | decimal | separate from `base_price` |
| is_active | code + A only | boolean | gone in B |
| is_stock | code + A only | boolean | gone in B |
| product_type | code + A only | varchar | gone in B |

### merchant, user_detail, users — new identity/media columns in B

| Table | Column | Type |
|---|---|---|
| merchant | merchant_pos_id | bigint |
| merchant | source_directory | varchar |
| user_detail | merchant_pos_id | bigint |
| users | signature | varchar |

### stock, stock_movement — variant tracking not in B
Consistent with the variant tables being absent from B — nothing to fix, just confirms the pattern.

| Table | Column | Where |
|---|---|---|
| stock | variant_id | code + A only |
| stock_movement | variant_id | code + A only |
| stock_movement | stock_after | code + A only |

## Recommendations

1. **Add a `TransactionItemDetail` entity.** The only table both live databases already agree on that code hasn't caught up to — lowest-risk, highest-value gap to close first.
2. **Decide the fate of the discount/promotion table set.** 16 tables (`discount*`, `promotion*`, `product_variant*`, `product_modifier*`) are fully built in code and A but don't exist in B. Confirm whether staging is behind or whether this feature set is being replaced by B's inline transaction columns before investing further here.
3. **Model B's inline discount/voucher columns on `Transaction` and `TransactionItem`.** If the direction is to follow mobile-apps-cashlez (per the recent sync commits), these are the columns actively used there and currently invisible to the entity layer.
4. **Reconcile `product.price` vs `product.base_price`.** Same concept, two names across databases — pick one before it causes a silent mapping bug.
5. **Audit nullable audit-columns against B.** Spot-check that nothing persists `Transaction`/`Product`/etc. with `createdDate = null` on a path that will hit B's `NOT NULL` constraint.

---
*Generated via `information_schema` introspection over SSH-tunneled connections (5448 → Postgres, 3384 → MySQL) and static parsing of 42 Kotlin entity files. No data rows were read — structure only.*
