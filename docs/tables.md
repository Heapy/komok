# Tables in Komok

Working with tables in Komok

## Inheritance

> @Ruslan: Not sure what I mean here

Using aliases to inherit and visually merge different tables into one.

## Views

### Devices

| Device | Hardware | Storage     | OS       | User | SSH Key | Hostname |
|--------|----------|-------------|----------|------|---------|----------|
| nimbus | RPI 5    | 256 Gb NVMe | Bookworm | pi   | nimbus  | nimbus   |


In fact, to render a table like "Devices", you need to describe a set of entities and their attributes inside. To ensure that all entities have the same set of fields, we need to introduce the concept of an Entity Template.

By creating many entities from one template (or from several different ones), we can then build a table by writing a query that uses different attributes and selecting by entity template or "interface". You can have multiple queries for multiple sets of entities, filter, join, union — in general, classic SQL.
