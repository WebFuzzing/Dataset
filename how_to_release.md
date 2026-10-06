### WFD Release

If you are making a new release for WFD, need to update its version:

`python3 scripts/version.py  wfd <VERSION>`

Note: no longer WFD is aligned with EM.
Versions number will be usually different.

On `develop` branch, versions should always be `-SNAPSHOT`.
On `master` they will be release versions.
The `master` branch should always point to last commit of latest release, and no SNAPSHOT.


To make a new release for WFD, you then need to do:
1. in `develop` branch, use `version.py` to set new release versions for both `em` and `wfd`.
2. push `develop`.
3. switch `master`, pull from `develop`, and push.
4. `git tag v<x.y.z>`
5. `git push origin v<x.y.z>`
6. switch to `develop`, and use `version.py` to make new `-SNAPSHOT` versions, for `em` and `wfd`.
7. push `develop`.


### Updating EvoMaster Drivers

After completing a release of a new version of `EvoMaster`, you might, or might not, need to make a new
release for [https://github.com/WebFuzzing/Dataset](https://github.com/WebFuzzing/Dataset) as well.
Changes in EM only impact _white-box_ settings, and __NOT__ the _black-box_ ones.
Ideally, should make a new WFD release, but, if not, still need to update snapshot EM version in the _develop_ branch:

`python3 scripts/version.py  em <VERSION>`
