# Romania places catalogue

© OpenStreetMap contributors. This derived database is available under the Open Database License (ODbL) 1.0: https://opendatacommons.org/licenses/odbl/1-0/

Source: https://download.geofabrik.de/europe/romania.html (free Romania extract), downloaded 2026-10-16.
OSM replication timestamp: 2026-10-15T20:21:00Z.
Source PBF SHA256: a9eb5e8c2c793334c71b81c404620aaea64fe8ffe89fc7afc8d95b80d188426a

The full derived database is romania.tsv alongside this notice, distributed with the app and in this repository under ODbL 1.0. It can be extracted from the APK (assets/places/romania.tsv). The reproducible transformation is tools/build_place_catalog.py in the source repository.

Includes named place nodes (city, town, village, hamlet), named supermarket/convenience nodes and closed ways. Ways use an approximate average of their vertices, not a store entrance. Nearby duplicate store representations are removed within 100 metres. Relation-only shops, untagged shops and addresses, other countries and unmapped changes are excluded. Border spillover from the Romania extract may be included. This is an incomplete community catalogue, not an official retailer directory or a complete address geocoder. Data can be wrong or outdated. Chain names identify places; no retailer affiliation or endorsement is implied.

25,834 catalogue entries. Selected chain counts: Kaufland 200; Lidl 390; Carrefour 370; Auchan 84; Penny 384; Mega Image 436; Profi 1,684.

Search operates locally. The app does not call Nominatim, Overpass, Google Places, Geocoder or a search server. Catalogue updates ship with an app update; users explicitly refresh the stores selected by existing chain reminders.
