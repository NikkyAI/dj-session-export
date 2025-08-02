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

## Exporter

[Download exporter](https://github.com/NikkyAI/dj-session-export/releases/download/nightly/dj-session-export.exe)

all the tools listed below bundled together


## [mixxx](./mixxx-export/README.md)

Exporter for sessions from the Mixxx database

## [rekordbox](./rekordbox-export/README.md)

Exporter for sessions from the rekordbox database

## [serato](./serato-export/README.md)

Exporter for Serato History

## [traktor](./traktor-export/README.md)

Exporter for Traktor History (reading `nml` files)

## [virtualdj](./virtualdj-export/README.md)

Exporter / Converter for recorded sets from the VirtualDJ database