package ro.andrei.arichu.feature.places

/** Catalogue categories. [aliases] make a category searchable by plain words (English and Romanian), accent-free. */
enum class PlaceCategory(val key: String, val label: String, val aliases: List<String>) {
    SUPERMARKET("supermarket", "Supermarkets", listOf("supermarket", "grocery", "shop", "store", "magazin", "alimentara")),
    PHARMACY("pharmacy", "Pharmacies", listOf("pharmacy", "farmacie", "drugstore")),
    FUEL("fuel", "Fuel", listOf("fuel", "petrol", "gas", "benzinarie", "carburant")),
    BANK("bank", "Banks & ATMs", listOf("bank", "atm", "banca", "bancomat")),
    POST("post", "Post offices", listOf("post", "posta", "mail", "oficiu")),
    MALL("mall", "Malls", listOf("mall", "shopping", "centru", "comercial")),
    FOOD("food", "Food & cafes", listOf("restaurant", "cafe", "coffee", "food", "pizza", "cafenea", "fastfood")),
    HEALTH("health", "Hospitals & clinics", listOf("hospital", "clinic", "spital", "clinica", "doctor", "medical")),
    HARDWARE("hardware", "Hardware", listOf("hardware", "diy", "bricolaj", "fierarie")),
    BAKERY("bakery", "Bakeries", listOf("bakery", "bread", "panificatie", "brutarie", "covrigi")),
    TOWN("town", "Towns", listOf("town", "village", "city", "oras", "sat", "comuna", "localitate"));

    companion object {
        private val byKey = entries.associateBy { it.key }
        /** Rows written before the category column existed fall back to their kind. */
        fun of(place: CatalogPlace): PlaceCategory = byKey[place.category] ?: when (place.kind) {
            "town" -> TOWN; "mall" -> MALL; else -> SUPERMARKET
        }
    }
}
