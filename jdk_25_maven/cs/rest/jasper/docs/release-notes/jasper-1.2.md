# v1.2 Release Notes

## Server

* Added `admin` profile for running locally. Treats every request as from an admin user
  with user tag `+user/admin`
* Added backup / restore functionality
* Added replication functionality
* Made (origin, modified) a unique key in all tables
* Hierarchical tags now added for all tags, not just public tags
* Removed Feed entity. Now use built-in plugin `+plugin/feed`
* Removed Origin entity. Now use built-in plugin `+plugin/origin`
* Added distinct modified date to Metadata
* Added ROLE_VIEWER to allow authenticated read-only access
* Added linux/arm64 docker platform to support Apple Silicon

## Reference Client

* Added Ref action to view source if user does not have write permission
* Show asterix next to created date if Ref has been edited
* Added custom theme editor at the tag and user level
* Editor adds all links to sources unless they are alts, instead of requiring source format `[1](url)`
* Sources using `[1](url)` format and alts using `[alt1](url)` format are renumbered to match correct index
* Sources missing url `[1]` or `[[1]` will have their url added. Similarly for alts
* Added support for using `^` as superscript
