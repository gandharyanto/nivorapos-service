# POS Endpoint Audit — mobile-apps-cashlez ↔ nivorapos-service

## Status kerjaan

**Temuan #8 sudah di-fix di backend** (`PromotionService.kt`, branch
`feat/pos-endpoint-sync`) — mobile jadi source of truth untuk urutan/cascading
(keputusan user), backend disesuaikan:

1. `autoApply()` tambah parameter `discountAmount: BigDecimal = BigDecimal.ZERO`.
2. Sort promotions mirror mobile `PromotionOrchestrator.evaluateAll()`: tier
   BUY_X_GET_Y+FREE dulu (0) baru sisanya (1), lalu `priority` ascending, tie-break
   `id` ascending.
3. Di dalam loop, `effectiveTotal = (transactionTotal - discountAmount - totalPromo).max(ZERO)`
   dipakai sebagai argumen `transactionTotal` ke `computePromoAmount(...)` — cascading
   base subtotal per-promo, mirror mobile.
4. Call site `TransactionService.kt:118` diupdate — kirim `discountAmount = discountAmount`
   (sudah tersedia dari resolve discount baris 107).
5. Verified: `autoApply()` cuma dipanggil dari satu tempat (grep).
6. `./gradlew compileKotlin` — BUILD SUCCESSFUL.

**Belum termasuk** di scope ini (temuan lain, tunggu keputusan terpisah): Temuan #6
(SPECIAL_PRICE), #7 (rounding DISCOUNT_BY_ITEM_SUBTOTAL), #9 (formula BUY_X_GET_Y reward),
serta 5 mismatch endpoint field-level (stock/update variantId, transaction modifierIds,
outletId, payment-setting tax fields, transaction/list sort params).

---


Audit dibuat 2026-07-06. Membandingkan endpoint yang benar-benar dipanggil POS
(`mobile-apps-cashlez`, `pos-core/.../PosService.kt`) terhadap implementasi
backend `nivorapos-service`. Tujuan: cari field/param yang tidak match sehingga
data hilang, salah hitung, atau request gagal.

## Status: sudah di-fix (PR #4, branch `feat/pos-endpoint-sync`)

| Endpoint | Masalah | Fix |
|---|---|---|
| `GET /pos/product/{id}/option-groups` | Tidak ada endpoint sama sekali di backend, padahal ini yang benar-benar dipanggil POS sebelum add-to-cart (bukan `variants`/`modifiers` — itu dummy/legacy di mobile) | Endpoint baru: gabung variant group + modifier jadi bentuk `OptionGroup`/`OptionItem` |
| `GET /pos/discount/available` | Mobile panggil path ini tanpa query param; backend cuma punya `/pos/discount/list-available` | Alias path `@GetMapping("/available", "/list-available")` |
| `GET /pos/promotion/active` | Mobile minta promo aktif saja; backend cuma punya `/pos/promotion/list` (semua promo) | Endpoint baru `listActive()`: filter isActive + rentang tanggal + channel POS/BOTH + hari valid |

## Belum di-fix — butuh keputusan/prioritas

Urutan berdasarkan dampak (paling parah dulu).

### 1. `PUT /pos/stock/update` — update stok varian selalu gagal — **STATUS: no-op, sudah aman**
- Dicek ulang: mobile memang **belum ada UI** pemilihan varian di layar manapun
  (`ProductEditFragment` cuma edit produk level atas, tombol +/- stok tampil tanpa
  syarat productType, tidak ada variant picker). Keputusan user: skip bangun UI,
  cukup pastikan endpoint tidak crash aneh.
- Dicek `StockService.kt:38-40` — pakai `require(...) { "message" }` yang jadi
  `IllegalArgumentException` (subclass `RuntimeException`), dan `GlobalExceptionHandler`
  sudah menangkap semua `RuntimeException` → **400 Bad Request** dengan pesan jelas
  (`"variantId is required for variant product stock update"`), bukan 500. Jadi endpoint
  ini **sudah aman** — tidak ada perubahan kode yang diperlukan.
- **Belum ada** (perlu keputusan produk terpisah, di luar scope saat ini): UI variant
  picker di `ProductEditFragment` (phone) + `pos-tablet` (tablet, wajib mirror) supaya
  fitur ini benar-benar bisa dipakai.

### 2. `POST /pos/transaction/create` — modifier hilang diam-diam — **STATUS: FIXED (backend)**
- Root cause sebenarnya: mobile *sudah* mengirim modifier, tapi lewat array
  `details` (`TransactionItemDetail(detailType="MODIFIER", referenceId=mod.id, ...)`,
  dibangun di `TransactionCalculator.buildItemDetails()`), bukan lewat field flat
  `modifierIds`. `mod.id` di situ adalah id `ProductModifier` backend yang sama
  (dari endpoint option-groups), jadi valid untuk dipakai langsung.
- **Fix** (backend, `TransactionItemRequest.kt`): tambah
  `details: List<TransactionItemDetailRequest>` (`detailType`, `referenceId`) +
  computed property `effectiveModifierIds` = union `modifierIds` flat (legacy) dengan
  `referenceId` dari entry `detailType=="MODIFIER"`, distinct.
- `TransactionService.kt` — 3 titik pakai `itemReq.modifierIds` diganti
  `itemReq.effectiveModifierIds` (baris ~129 prefetch, ~210 pendingModifiers sizing,
  ~223 resolve `selectedModifiers`). Tidak ada perubahan di mobile.
- Verified: Jackson backend sudah toleran unknown JSON properties (banyak field
  lain dari mobile — `netAmount`, `totalDiscount`, `appliedPromotionIds`, dll. — juga
  tidak ada di `TransactionRequest` tapi transaksi tetap jalan), jadi aman tambah field
  baru tanpa breaking existing traffic. `./gradlew compileKotlin` — BUILD SUCCESSFUL.

### 3. `POST /pos/transaction/create` — promo/discount outlet-spesifik salah hitung — **STATUS: known-gap, skip (keputusan user)**
- **Backend**: `TransactionService.kt:112,121,155,170` pakai `request.outletId` untuk
  `discountService.resolveForTransaction` / `promotionService.autoApply`;
  `isOutletEligible()` return `false` kalau `outletId == null` untuk discount/promo
  yang scope-nya `SPECIFIC_OUTLET`.
- **Mobile**: `PosCreateTransactionRequest.kt` — **tidak ada field `outletId`**.
- **Investigasi lebih dalam (2026-07-06)**: ini bukan cuma field yang lupa dikirim —
  **konsep "outlet" tidak ada sama sekali** di codebase mobile (grep `outlet`
  case-insensitive di seluruh project: nol hasil). Tidak ada layar pilih outlet,
  tidak ada penyimpanan outlet aktif, dan login POS (`PsgsUser`) tidak terikat ke
  outlet tertentu — merchant bisa multi-outlet tapi POS tidak tahu sedang di outlet
  mana. Backend juga belum punya endpoint POS-facing untuk list outlet
  (`PsgsCredentialService.findOutletsByMerchantId` cuma dipakai internal saat
  admin membuat discount/promo scope outlet, bukan dari sisi POS).
- **Dampak**: kalau ada **≥1** discount/promo dikonfigurasi `visibility=SPECIFIC_OUTLET`,
  maka **setiap** transaksi checkout dari POS mobile akan diam-diam gagal
  menerapkannya (`resolveForTransaction`/`autoApply` return `Rp0` tanpa error) — bukan
  cuma edge case, karena mobile memang tidak pernah mengirim `outletId` sama sekali.
- **Fix yang benar** (belum dikerjakan, butuh keputusan produk terpisah): bangun
  fitur outlet-selection end-to-end — (1) backend: endpoint baru `GET /pos/outlet/list`
  expose `findOutletsByMerchantId`; (2) mobile: layar pilih outlet + persist lokal;
  (3) alirkan `outletId` ke `discount/available` (query param, backend sudah terima),
  `promotion/active` (backend belum terima query param ini sama sekali — perlu
  ditambah juga), dan `transaction/create` (body, backend sudah terima).
- **Keputusan user (2026-07-06)**: skip untuk sekarang, cukup didokumentasikan di sini
  sebagai known-gap — tidak ada perubahan kode.

### 4. `POST/PUT /pos/payment-setting/{create,update}` — config pajak hilang — **STATUS: FIXED (backend)**
- **Root cause**: bukan cuma field yang belum ditambahkan — arsitekturnya sudah
  ada tapi tidak tersambung. Backend punya entity `Tax` terpisah (merchant-scoped,
  `name/percentage/isActive/isDefault`) dan `PosMerchantDefaultsService.ensureTaxes()`
  auto-seed satu row default (`PPN 11%`, `isDefault=true`) per merchant. Product
  sudah join `taxId` ke `Tax` **live** saat build response (`ProductService.kt:638,719`
  — bukan snapshot), jadi mengedit tax default otomatis kepakai ke semua produk
  yang mereferensikannya. Tapi `PaymentSettingRequest`/`PaymentSettingResponse`
  (yang dipanggil dari layar payment setting mobile) sama sekali tidak menyentuh
  entity `Tax` ini — `isTax`/`taxPercentage`/`taxName` dari mobile jatuh sebagai
  unknown JSON property yang di-drop diam-diam.
- **Fix**: `PaymentSettingRequest`/`Response` tambah `isTax`/`taxPercentage`/`taxName`.
  `PaymentSettingService.create()/update()/get()` sekarang resolve default tax
  merchant via `TaxRepository.findByMerchantIdAndIsDefaultTrue()` (baru ditambah),
  merge+validasi (mirror pola `isServiceCharge`: kalau `isTax=true` wajib
  `taxPercentage` 0.01–100 dan `taxName` tidak blank), lalu tulis balik ke row
  `Tax` itu (bukan ke `PaymentSetting`) via `applyTaxRequest()`. Response gabungkan
  `PaymentSetting` + `Tax` default itu.
- **Di luar scope ini** (bukan bug, keputusan desain terpisah): mengubah tax default
  tidak retroactive mengubah pilihan `taxId` yang sudah di-assign ke produk lain
  (non-default) — itu memang behavior existing yang tidak disentuh.
- `./gradlew compileKotlin` + `compileTestKotlin` — BUILD SUCCESSFUL.

### 5. `GET /pos/transaction/list` — sort diabaikan — **STATUS: FIXED (backend)**
- **Konteks tambahan**: mobile sudah punya UI sort (`TransactionActivity` — sort by
  Date/Amount/Status/Trx ID, ASC/DESC) dan mengirim `sortBy` (`createdDate`,
  `totalAmount`, `status`, `trxId` — persis nama field di `Transaction` entity) +
  `sortType` (`ASC`/`DESC`) di setiap request. Mobile juga re-sort hasil secara
  client-side (`BaseTransactionViewModel.applyFilterAndSortWithSync()`) atas
  seluruh halaman yang sudah ter-load — jadi dampaknya bukan urutan salah di layar,
  tapi **batas antar-halaman (pagination) yang salah** untuk sort selain createdDate:
  hal 1 berisi "10 transaksi terbaru", bukan "10 transaksi ter-tinggi/rendah by
  amount", jadi user yang tidak scroll sampai habis tidak lihat urutan yang benar.
- **Fix**: `TransactionController.list()` tambah `@RequestParam sortBy/sortType`
  (optional). `TransactionService.list()` whitelist `sortBy` ke
  `createdDate|totalAmount|status|trxId` (default `createdDate` kalau kosong/tidak
  dikenal — mencegah sort by field arbitrary), `sortType` ke `ASC` (selain itu
  default `DESC`), dipakai langsung di `Sort.by(...)` karena nama field mobile
  persis sama dengan properti entity `Transaction`.
- `./gradlew compileKotlin` + `compileTestKotlin` — BUILD SUCCESSFUL.

## Confirmed OK (tidak perlu diubah)
- `pos/category/*` — field match (pakai `@JsonAlias` untuk `id`/`categoryId`)
- `pos/auth/login`
- `images/upload` (multipart part name `file` cocok)
- `pos/transaction/update/{merchantTrxId}` — field match
- Field total pajak/service charge di `TransactionRequest` (`priceIncludeTax`,
  `serviceChargePercentage`, dst.) yang tidak diisi mobile — **bukan bug**, karena
  backend menghitung ulang totalnya sendiri dari entity `PaymentSetting`
  tersimpan, bukan dari body request.

## Urutan perhitungan Discount vs Promotion (transaction/create)

Ditelusuri dari `TransactionService.kt`:

1. `discountService.resolveForTransaction(...)` dipanggil **lebih dulu** (baris 107) —
   validasi + hitung `discountAmount`, berdasarkan `prelimSubTotal` (subtotal mentah,
   sebelum ada potongan apapun).
2. `promotionService.autoApply(...)` dipanggil **kedua** (baris 118) — juga dihitung dari
   `prelimSubTotal` yang **sama**, bukan dari hasil setelah discount.
3. Keduanya digabung secara **aditif**, bukan bertingkat/cascading:
   ```
   netAfterDiscount = (calculatedSubTotal - discountAmount - promoAmount).max(ZERO)
   ```
   (`TransactionService.kt:553`)

**Kesimpulan**: tidak ada precedence antara discount dan promotion di level transaksi —
keduanya dihitung independen dari subtotal yang sama lalu langsung dijumlah sebagai
pengurang. Yang punya urutan/priority hanya **antar promotion** (field `priority` di
`Promotion` entity, ascending = duluan dievaluasi; `canCombine=false` menghentikan promo
lain setelah yang pertama match — lihat `PromotionService.autoApply()`). Discount sendiri
hanya satu per transaksi (`request.discountId`/`discountCode`), jadi tidak ada
"priority antar discount".

Perlu diputuskan: apakah memang diinginkan discount dan promo dihitung independen
begini (bisa stacking penuh: 1 discount + N promo non-exclusive, semua dari base yang
sama), atau seharusnya salah satu dihitung dari hasil setelah yang lain (cascading)?
Ini keputusan bisnis, bukan bug — tapi worth dikonfirmasi karena mempengaruhi berapa
besar total potongan yang bisa didapat customer.

---

## Kalkulasi Discount & Promotion di Mobile (verifikasi langsung dari code)

Dicek langsung dari source `mobile-apps-cashlez` (bukan dokumentasi existing
`docs/PERHITUNGAN_DISCOUNT_DAN_PROMOSI.md`, yang beberapa bagiannya sudah tidak akurat
terhadap kode saat ini):

- `pos-core/.../util/TransactionCalculator.kt` — entry point `calculateTransaction()`
- `pos-core/.../util/promotion/PromotionOrchestrator.kt` — evaluator dispatcher, **inilah
  yang benar-benar dipakai** untuk hitung amount checkout
- `pos-core/.../util/promotion/DiscountByOrderEvaluator.kt`,
  `DiscountByItemSubtotalEvaluator.kt`,
  `util/promotion/buyxgety/BuyXGetYEvaluator.kt` + reward strategies (`FreeRewardStrategy`,
  `PercentageRewardStrategy`, `AmountRewardStrategy`, `FixedPriceRewardStrategy`)

### Tipe & subtipe yang ditemukan di code

| Konsep | Tipe/subtipe di mobile | Bandingkan ke backend |
|---|---|---|
| Discount `valueType` | `PERCENTAGE`, `AMOUNT` | Backend: `PERCENTAGE`, `AMOUNT`, **`SPECIAL_PRICE`** (scope=PRODUCT) |
| Discount `scope` | `ALL`, `PRODUCT`, `CATEGORY` | Sama |
| Promotion `promoType` | `DISCOUNT_BY_ORDER`, `DISCOUNT_BY_ITEM_SUBTOTAL`, `BUY_X_GET_Y` | Sama |
| `DISCOUNT_BY_ORDER`/`DISCOUNT_BY_ITEM_SUBTOTAL` `valueType` | `PERCENTAGE`, `AMOUNT` | Backend: `PERCENTAGE`, `AMOUNT`, **`SPECIAL_PRICE`** (hanya utk `DISCOUNT_BY_ITEM_SUBTOTAL`, buyScope≠ALL) |
| `BUY_X_GET_Y` `rewardType` | `FREE`, `PERCENTAGE`, `AMOUNT`, `FIXED_PRICE` | Sama — cocok penuh |

### Temuan #6 (BARU, severity tinggi): `valueType=SPECIAL_PRICE` tidak diimplementasikan sama sekali di mobile — **STATUS: FIXED (mobile)**

- Dicek dengan grep `SPECIAL_PRICE` ke seluruh project mobile: **nol hasil di kode Kotlin**
  (hanya muncul di satu file FSD lama, tidak pernah diimplementasikan).
- `TransactionCalculator.calculateDiscountAmount()` (manual discount, semua scope) dan
  `DiscountByItemSubtotalEvaluator.evaluate()` (live path, dipakai `PromotionOrchestrator`)
  sama-sama punya `when (valueType) { "PERCENTAGE" -> ...; "AMOUNT" -> ...; else -> 0.0 }`
  — `SPECIAL_PRICE` jatuh ke `else -> 0.0`.
- **Backend** (`DiscountService.validate()`/`computeDiscountAmount()` dan
  `PromotionService.computeDiscountByItemSubtotal()`) **fully support** `SPECIAL_PRICE`:
  markdown harga produk ke harga tetap tertentu — `(item.price - value).max(0) × qty`.
- **Dampak**: kalau merchant konfigurasi discount/promo dengan `valueType=SPECIAL_PRICE`
  di backend (endpoint ini sudah ada dan valid), mobile app:
  1. Tetap menampilkan discount/promo itu sebagai "eligible" ke kasir (fungsi
     `isDiscountEligible()` tidak mengecek `valueType` sama sekali),
  2. Tapi menghitung **discountAmount = 0** saat kasir menerapkannya (harga di layar
     kasir tidak berubah sama sekali — customer tidak dapat diskon yang seharusnya),
  3. Saat transaksi dikirim ke `POST /pos/transaction/create`, backend menghitung ulang
     discount/promo **secara independen dari input klien** (`discountService.resolveForTransaction`
     / `promotionService.autoApply` tidak percaya `discountAmount` dari body) dan akan
     mendapat angka **non-zero** yang sesungguhnya.
  4. Karena `TransactionService.computeAndValidate()` punya toleransi validasi hanya
     **Rp 1.00** terhadap apa yang dihitung ulang backend vs apa yang dikirim mobile
     (`totalAmount`, dll.), selisih sebesar nilai `SPECIAL_PRICE` itu (bisa puluhan ribu
     rupiah) **hampir pasti melebihi toleransi** → transaksi kemungkinan besar **ditolak
     (400)**, atau kalau lolos, cashier/customer sudah terlanjur commit ke harga yang salah.
- **Fix** (mobile, `TransactionCalculator.kt` + `DiscountByItemSubtotalEvaluator.kt`):
  tambah cabang `"SPECIAL_PRICE" -> eligible.sumOf { (it.price - value).coerceAtLeast(0.0) * it.quantity }`
  di kedua tempat — `calculateDiscountAmount()` scope=PRODUCT (Discount, mirror
  `DiscountService.computeProductScope`) dan `DiscountByItemSubtotalEvaluator.evaluate()`
  (Promotion `DISCOUNT_BY_ITEM_SUBTOTAL`, mirror `PromotionService.computeDiscountByItemSubtotal`).
  Ditambah test: `TransactionCalculatorTest` (`SPECIAL_PRICE discount marks down eligible
  product to fixed price`) dan `DiscountByItemSubtotalEvaluatorTest` (mark-down + never-negative).
- `./gradlew :pos-core:testDebugUnitTest` — 159 test, hijau (1 kegagalan pre-existing
  tidak terkait, lihat catatan di bawah).

### Temuan #7: metodologi rounding `DISCOUNT_BY_ITEM_SUBTOTAL` PERCENTAGE berbeda — **STATUS: FIXED (mobile)**

- **Mobile** (`DiscountByItemSubtotalEvaluator.evaluate()`): round **per item** lalu
  dijumlah — `eligible.sumOf { Math.round(item.price * item.quantity * value / 100.0) }`.
- **Backend** (`PromotionService.computeDiscountByItemSubtotal()`): jumlahkan dulu semua
  `eligibleSubtotal`, baru dikali `value/100` dan di-round **sekali** di akhir (scale 2,
  HALF_UP).
- **Dampak**: untuk cart dengan banyak item eligible, sum-of-rounded (mobile) vs
  round-of-sum (backend) bisa selisih beberapa rupiah — biasanya di bawah toleransi
  Rp 1.00 tapi tidak terjamin untuk cart besar dengan banyak baris ganjil.
- **Fix**: `DiscountByItemSubtotalEvaluator.evaluate()` PERCENTAGE diubah ke round-of-sum
  2-desimal — `Math.round(eligibleSubTotal * value).toDouble() / 100.0` (bukan
  `Math.round(... / 100.0)` yang membulatkan ke rupiah bulat — backend pakai
  `BigDecimal.divide(100, scale=2, HALF_UP)`, bukan pembulatan ke integer).
  `perItemDeduction()` (breakdown per-item untuk tax base) disederhanakan jadi satu
  jalur distribusi proporsional net-subtotal-share untuk PERCENTAGE/AMOUNT/SPECIAL_PRICE
  sekaligus — sebelumnya PERCENTAGE punya jalur rounding-per-item terpisah yang tidak
  lagi rekonsil ke total round-of-sum yang baru.
- **Test lama diperbarui**: `DISCOUNT_BY_ITEM_SUBTOTAL PERCENTAGE uses per-item integer
  rounding matching server` di `TransactionCalculatorTest` — komentar/nilai expected-nya
  ternyata artefak dari perilaku backend versi lama (sebelum backend berubah ke
  round-of-sum); nilai baru: promotionAmount 3733.32 (bukan 3733.0), tax 977.77 (bukan
  977.80). Test baru ditambah untuk memverifikasi round-of-sum secara eksplisit.

### Temuan #8: stacking multi-promotion — mobile cascading, backend tidak

- **Mobile** (`PromotionOrchestrator.evaluateAll()`): untuk setiap promo dalam urutan
  priority, base subtotal yang dipakai adalah `promoSubTotal = subTotal - totalPromoAmount`
  — **dikurangi promo-promo yang sudah applied sebelumnya di loop yang sama**. Ini
  memengaruhi `DiscountByOrderEvaluator` (satu-satunya evaluator yang membaca `ctx.subTotal`).
- **Backend** (`PromotionService.autoApply()`): setiap promo dalam loop dihitung dari
  `transactionTotal` **yang sama, tidak pernah dikurangi** oleh promo lain yang sudah
  applied di request yang sama (`computePromoAmount(promo, transactionTotal, items)` —
  argumen kedua konstan sepanjang loop).
- **Dampak**: kalau ada ≥2 promo combinable applied bersamaan dan salah satunya
  `DISCOUNT_BY_ORDER` yang dievaluasi setelah promo lain, mobile akan menampilkan
  total discount **lebih kecil** (karena cascading/compounding) dibanding yang
  sebenarnya akan dihitung backend saat transaksi disimpan → preview di kasir tidak
  match dengan yang benar-benar di-charge.
- **Fix**: pilih satu semantik dan samakan kedua sisi — kemungkinan besar backend yang
  benar (promo independen dari base yang sama, sesuai keputusan bisnis di Temuan
  sebelumnya soal discount-vs-promo), jadi mobile perlu hilangkan pengurangan
  `totalPromoAmount` dari `promoSubTotal` di `PromotionOrchestrator.evaluateAll()`.

### Temuan #9 (BARU, severity tinggi): `BUY_X_GET_Y` — reward *type* cocok, tapi rumus hitung reward-nya berbeda signifikan — **STATUS: FIXED (backend)**

Reward type enum (`FREE`/`PERCENTAGE`/`AMOUNT`/`FIXED_PRICE`) identik di kedua sisi. Tapi
formula perhitungan amount-nya berbeda substansial. Dicek dari
`PromotionService.computeBuyXGetY()` (backend, baris 376–418) vs `BuyXGetYEvaluator.kt` +
`buyxgety/*RewardStrategy.kt` (mobile, live path via `PromotionOrchestrator`).

**FREE** — beda cara pilih unit gratis:
- Mobile: urutkan semua kandidat reward ascending by net price, ambil unit **satu-satu**
  dari yang termurah sampai `getQty` terpenuhi — benar menangani reward pool lintas
  beberapa produk dengan harga beda.
- Backend: `rewardItems.minOf { it.price }` — cari **satu** harga terendah di antara semua
  baris reward-eligible, lalu `hargaTerendah × (getQty × multiplier)`. Mengasumsikan
  **semua** unit gratis berharga sama dengan item termurah, tanpa cek apakah qty item
  termurah itu cukup.
- Contoh: cart = Produk A (Rp5.000, qty 1) + Produk B (Rp8.000, qty 5), keduanya
  reward-eligible, promo BUY 1 → GET 3 FREE.
  Mobile: 1 unit A (5.000) + 2 unit B (16.000) = **Rp21.000**.
  Backend: 5.000 × 3 = **Rp15.000**. Selisih Rp6.000 — bisa melebihi toleransi validasi
  Rp1.00 di `computeAndValidate()`.

**PERCENTAGE & FIXED_PRICE** — backend punya bug independen (bukan cuma beda dari mobile):
- Backend: `rewardItems.take(getQty * multiplier)` beroperasi di atas **list baris cart**
  (satu entri per line, bukan per unit), **tanpa sorting**, dan qty baris yang terambil
  **tidak dipangkas** ke sisa unit yang dibutuhkan.
  Kalau reward-eligible cuma 1 baris dengan `qty=10` dan `getQty=2`, `.take(2)` tetap
  mengembalikan baris itu (list cuma 1 elemen), lalu `sumOf { price × qty }` pakai
  **qty penuh (10)**, bukan dibatasi ke 2 unit — reward yang diberikan jauh lebih besar
  dari seharusnya. Ini soal ketidakbenaran backend sendiri, independen dari mobile,
  untuk kasus cart normal (1 baris per produk dengan field `qty`).
- Mobile: selalu urutkan cheapest-first dan hitung tepat sejumlah `effectiveRewardQty` unit.

**AMOUNT** — beda cara cap nilai:
- Mobile: `min(rewardValue, netPricePerUnit)` per unit — potongan tidak bisa melebihi
  harga item itu sendiri (tidak mungkin promo bikin item jadi "negatif").
- Backend: `rewardValue × (getQty × multiplier)` flat, tidak dibatasi harga item apapun.
  Kalau `rewardValue` (misal Rp15.000) > harga reward item (misal Rp8.000/unit), backend
  tetap kasih potongan penuh Rp15.000/unit.

**Overlap buy/reward scope** (mis. "Beli 3 Kopi → Gratis 1 Kopi", produk sama untuk
qualifier & reward):
- Mobile: reservasi eksplisit (`buildAvailableRewardItems`/`computeAvailableRewardQty`) —
  unit yang dipakai memenuhi syarat beli tidak ikut dihitung sebagai reward.
- Backend: `countEligibleBuyQty()` dan `getRewardEligibleItems()` sama-sama menghitung
  qty penuh tanpa saling mengurangi — **tidak ada reservasi sama sekali**. Untuk kasus
  overlap, backend kemungkinan menghitung reward lebih longgar dari yang seharusnya.

**Fix** (backend, `PromotionService.kt`): `computeBuyXGetY()` ditulis ulang, port logic
mobile yang lebih matang:
- `getBuyEligibleItems()` (baru) — sama seperti `countEligibleBuyQty()` tapi return list
  item, bukan cuma count. `countEligibleBuyQty()` sekarang delegasi ke ini.
- `buildAvailableRewardItems()` (baru, top-level `internal fun` — pure function, tidak
  butuh repository) — reservasi qualifier/reward overlap: kalau buyScope & rewardScope
  overlap di produk yang sama, kurangi qty reward-eligible dengan unit yang masih
  dibutuhkan sebagai qualifier (multi-produk overlap: reserve dari yang termahal dulu).
  Mirror `BuyXGetYEvaluator.buildAvailableRewardItems` (mobile).
- `allocateUnits()` (baru, top-level `internal fun` — pure function) — jalan di atas list
  item yang **sudah** diurutkan cheapest-first, ambil unit satu-per-satu sampai
  `qtyNeeded` terpenuhi, memangkas qty per baris (bukan `.take(n)` di atas baris utuh).
  Dipakai untuk FREE/PERCENTAGE/FIXED_PRICE (AMOUNT juga pakai ini dengan cap
  `min(rewardValue, item.price)` per unit).
- Multiplier: diubah dari `floor(totalBuyQty/(buyQty+getQty))` ke
  `min(floor(totalBuyQty/buyQty), floor(availableRewardQty/getQty))` — memperhitungkan
  ketersediaan reward, bukan cuma buy qty (mirror cabang non-overlap mobile
  `computeEffectiveRewardQty`; cabang overlap-multiplier mobile tidak diport — di luar
  scope temuan ini).
- **Sengaja tidak diport**: interaksi FREE reward dengan manual Discount
  (`FreeRewardStrategy.postDiscountPriceForFreeUnit` di mobile) — backend menghitung
  discount dan promo independen dari base yang sama (keputusan bisnis di Temuan #8),
  jadi `computeBuyXGetY()` tidak punya (dan tidak butuh) akses ke discount context.
  `selectedRewardQtyMap` (fitur mobile untuk kasir pilih manual reward item) juga tidak
  diport — backend selalu full-auto, tidak ada mekanisme client memilih reward tertentu.
- **Test**: `PromotionServiceCalculationTest.kt` (baru) — test `allocateUnits` (clamp per
  baris, cheapest-first spread, qtyNeeded=0) dan `buildAvailableRewardItems` (no overlap,
  overlap 1 produk, non-reward buy item mencukupi qualifier, overlap multi-produk reserve
  termahal dulu) langsung tanpa mocking (pure function, top-level).
- `./gradlew test` — BUILD SUCCESSFUL (seluruh suite backend, termasuk test baru).

### Catatan: 1 test pre-existing gagal, di luar scope Temuan #6/#7/#9

Saat menjalankan full suite `:pos-core:testDebugUnitTest` (2026-07-06) ditemukan
`TransactionCalculatorTest.calculateTransaction two BXGY FREE same qualifier same reward
qty 2 both claim one unit` **gagal** — sudah gagal sebelum perubahan sesi ini (dikonfirmasi
dengan `git stash` ke kode asli, gagal juga). Ini soal interaksi 2 promo BUY_X_GET_Y FREE
berbagi reward pool yang sama (bukan mismatch mobile-vs-backend seperti Temuan #9),
kemungkinan regresi dari commit `test(pos-core): add BXGY+BXGY combination calculation
tests` (2026-06-29). Tidak disentuh — di luar scope audit endpoint ini, tapi worth
diinvestigasi terpisah karena berarti ada kasus BUY_X_GET_Y+BUY_X_GET_Y combination yang
salah hitung di mobile sendiri (independen dari backend).

### Catatan (bukan bug): dua implementasi paralel untuk savings badge vs checkout

`TransactionCalculator.kt` masih menyimpan method privat lama (`evaluatePromotions()`,
`calculatePromoByOrder()`, `calculatePromoByItemSubtotal()`, `calculateBuyXGetY()`,
~baris 1014–1312) yang **sudah tidak dipanggil** untuk kalkulasi checkout (`calculateTransaction()`
sepenuhnya delegasi ke `PromotionOrchestrator`), tapi **masih dipakai** oleh
`computePerItemSavings()` untuk badge "hemat Rp X" di UI cart. Ini disengaja (ada komentar
eksplisit di kode: "NOT the BE-matching payment calculation"), tapi berarti dua jalur kode
terpisah yang bisa diam-diam makin berbeda seiring waktu. Bukan bug sekarang, tapi risiko
maintenance — worth dipertimbangkan untuk disatukan kalau ada waktu refactor.
