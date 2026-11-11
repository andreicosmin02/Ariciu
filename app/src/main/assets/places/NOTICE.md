# Romania places catalogue

© OpenStreetMap contributors. This derived database is available under the Open Database License (ODbL) 1.0: https://opendatacommons.org/licenses/odbl/1-0/

Source: https://download.geofabrik.de/europe/romania-261108.osm.pbf (free Romania extract dated 2026-11-08), downloaded 2026-11-10.
Source PBF SHA256: 82d35a6467e5955792fa7efd8e94d3c6f39337124f72c9346d99ae5ff7923027 (Geofabrik published MD5 7ae157773cb18ca2f20d2400b70b8115, verified).
Replaces the earlier 2026-10-15 extract (romania.tsv).

The full derived database is towns.tsv (all towns and villages) and grid/c<lon cell>_<lat cell>.tsv (everything else, one file per 0.25 degree cell, listed in grid/index.txt), distributed with the app and in this repository under ODbL 1.0. It can be extracted from the APK (assets/places/). Each row: id, name, kind, address, latitude, longitude, brand, category. The reproducible transformation is tools/build_place_catalog.py in the source repository.

Includes named nodes and closed ways for 11 categories: supermarkets and convenience stores, pharmacies, fuel stations, banks and ATMs, post offices, malls, restaurants/cafes/fast food, hospitals and clinics, hardware/DIY, bakeries, and towns (city, town, village, hamlet). The brand tag is kept as searchable text. Ways use an approximate average of their vertices, not an entrance. Nearby duplicate representations of the same brand and category are removed within 100 metres. Relation-only objects, untagged places and addresses, other countries and unmapped changes are excluded. Border spillover from the Romania extract may be included. This is an incomplete community catalogue, not an official directory or a complete address geocoder. Data can be wrong or outdated. Brand names identify places; no retailer affiliation or endorsement is implied.

52,533 catalogue entries: supermarkets 11,903; pharmacies 3,545; fuel 2,625; banks and ATMs 3,840; post offices 962; malls 231; food and cafes 11,051; hospitals and clinics 2,029; hardware 964; bakeries 1,412; towns 13,971.

Search operates locally. The app does not call Nominatim, Overpass, Google Places, Geocoder or a search server. The bundled Romania catalogue updates with an app update; downloaded packs update from Settings.

Other European countries are available as optional downloads (Settings > Search data), built from Geofabrik country extracts by tools/build_place_packs.py and published as files of the GitHub release `places-data` under the same ODbL 1.0 terms. Downloading a pack is the only time the app contacts that server for places; searching stays local.
