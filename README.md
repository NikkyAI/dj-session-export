# DJ session export

Utilities to create plaintext exports of DJ sets

Simple templating for txt exports

CSV export

### Templating

when exporting the tools will look for a `template.txt` file in the target location or create a default  
this is what is being used to format each line in the `txt` export

you can use any fields from the `.csv` export in this file

default:
```
{time} {artist} - {title}
```

# Tools

## [traktor convert](./traktor-convert/README.md)

Converter for Traktor HTML session export

## [traktor export](./traktor-export/README.md)

Exporter for Traktor History (`nml` files)

## [serato convert](./serato-convert/README.md)

Converter for Serato session export

## [serato export](./serato-export/README.md)

Exporter for Serato History

## [rekordbox](./rekordbox-export/README.md)

Exporter for sessions from the rekordbox database

## [mixxx](./mixxx-export/README.md)

Exporter for sessions from the Mixxx database

## [virtualdj](./virtualdj-export/README.md)

Exporter / Converter for recorded sets from the VirtualDJ database