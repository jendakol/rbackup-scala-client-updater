# RBackup client updater

This is updater for [RBackup Scala client](https://github.com/jendakol/rbackup-scala-client) (for [RBackup](https://github.com/jendakol/rbackup)).

Readme TBD :-)

## Build (release)
```
#!/usr/bin/fish

env VERSION=$argv[1] \
    SENTRY_DSN="https://abcd@sentry.io/1234" \
    sbt ";clean;setVersionInSources;setSentryDsnInSources;dist"
```

The SENTRY_DSN is optional.