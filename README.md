# mongerr

Wrapper over Monger, simpler to use and with some goodies

## Usage

### Installation

Add `[mongerr "1.0.0-SNAPSHOT"]` to your `:dependencies` vector in your `project.clj`

Add a `MONGO_URL` environment variable with the url, including auth and db
Like: `mongodb://USER:PASSWORD@HOST/DB`

And require monger.core

### Crud

`(db) for the list of collections
`(db :collection)` for all data in a collection
`(db :collection match)` for normal queries

`(db-insert :collection document-or-documents)` for inserting document
`(db-update :collection conditions document)` for updating
`(db-delete :collection match)` for deleting

Also `db-text-search` for full text search, `db-geo` for, what else, geographic search

### Other features

#### serialize / deserialize

#### crud-routes (crud routes for compojure)

## License

Copyright © 2016 Laboratorio Fractal SA de CV

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version
